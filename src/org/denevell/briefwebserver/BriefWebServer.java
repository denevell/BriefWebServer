package org.denevell.briefwebserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.util.Hashtable;

/**
 * A simple web server to use in unit / integration / system tests.
 * You quickly start it up, give it some stub data at a path, and
 * then close it down when your tests are finished.
 * @author denevell
 */
public class BriefWebServer {

	private final ServerSocket mServer;
	private final Hashtable<String, String> mPathAndResults = new Hashtable<String, String>();
	private boolean mSocketIsClosed;
	private long mGetWaitPeriod = 0;

	/**
	 * Start a webserver on port on host
	 * @param hostname
	 * @param port
	 * @throws RuntimeException If we have a problem opening the http server
	 */
	public BriefWebServer(String hostname, int port) {
		try {
			mServer = findSocketWithGoodPort(port, hostname);
			startWaitingForConnections();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Iterates through ports until it finds a good one
	 * @param port
	 * @param hostname
	 * @return
	 * @throws IOException
	 */
	private ServerSocket findSocketWithGoodPort(int port, String hostname) throws IOException {
		try {
			return new ServerSocket(port, 0, InetAddress.getByName(hostname));
		} catch(BindException e) {
			return findSocketWithGoodPort(++port, hostname);
		} 
	}

	private void startWaitingForConnections() {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				Socket clientSocket = null;
				try {
					while(true && !mServer.isClosed()) {
				        clientSocket = mServer.accept();
				        
				        waitForPeriodOfTime(this);
				        
				        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
						BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
						
				        String path = getRequestedPath(in);
				        String stringAtPath = getStringAtPath(path);
				        
				        if(areNoPathsRegistered() || stringAtPath==null) {
							out.close();
							in.close();
							clientSocket.close();
							continue;
				        }
				        
				        writeOutToSocketAsHTTP200(stringAtPath, out);
				        
						out.close();
				        in.close();
						clientSocket.close();
					}
				} catch (Exception e) {
					if(e instanceof SocketException && mSocketIsClosed) {
						//This is to be expected
					} else {
						throw new RuntimeException(e);
					}
				}
					
			}
		}).start();
	}			
	
	/**
	 * Called from every socket accept we get
	 * @param thiz
	 * @throws InterruptedException
	 */
	private void waitForPeriodOfTime(Object thiz) throws InterruptedException {
		if(getGetWaitPeriod()!=0) {
			synchronized (thiz) {
			    thiz.wait(getGetWaitPeriod());
			}
		}
	}
	
	/**
	 * Once you've got the socket's OutputStream, write the response to it.
	 * @param stringAtPath
	 * @param out
	 * @throws IOException
	 */
	private void writeOutToSocketAsHTTP200(String stringAtPath, BufferedWriter out)
			throws IOException {
		out.write("HTTP/1.0 200 OK\r\n");
		out.write("Date: Fri, 31 Dec 1999 23:59:59 GMT\r\n");
		out.write("Server: StubbyTheWebserver/0.1.2\r\n");
		out.write("Content-Type: text/html\r\n");
		out.write("Content-Length: "+stringAtPath.length()+"\r\n");
		out.write("Expires: Sat, 01 Jan 2000 00:59:59 GMT\r\n");
		out.write("Last-modified: Fri, 09 Aug 1996 14:21:40 GMT\r\n");
		out.write("\r\n");
		out.write(stringAtPath);
	}
	
	/**
	 * Used to shut down the client socket if we have nothing to serve.
	 * @return
	 * @throws IOException
	 */
	private boolean areNoPathsRegistered() throws IOException {
		if(mPathAndResults.isEmpty()) {
			return true;
		}
		return false;
	}
	
	/**
	 * Get the string result held at the path
	 * @param path
	 * @return null if not found
	 */
	private String getStringAtPath(String path) {
		return mPathAndResults.get(path);
	}
	
	/**
	 * When we get a InputStream from the socket, we need to 
	 * get its path;
	 * We only get its path if it's a GET request currently
	 * @param in The BufferedReader of the socket
	 * @return The path of the socket's InputStream
	 * @throws IOException
	 */
	private String getRequestedPath(BufferedReader in) throws IOException {
		String s;
		String p = null;
		while ((s = in.readLine()) != null) {
			if(s.contains("GET ")) {
		        p = s.substring(4, s.indexOf(' ', 4));
		        break;
			}
		    if (s.isEmpty()) {
		        break;
		    }
		}
		return p;
	}
	
	/**
	 * Stop listening on the port on the host.
	 * (Note: Start is called by the controller.)
	 * @throws RuntimeException
	 */
	public synchronized void stop() throws RuntimeException {
		mSocketIsClosed=true;
		try {
			if(!mServer.isClosed()) {
				mServer.close();
			}
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Add some sub data that will be found at the path
	 * @param path
	 * @param stubData 
	 */
	public void addStub(final String path, final String stubData) {
		mPathAndResults.put(path, stubData);
	}

	/**
	 * Utility method to skip all the url.openStream, BufferedReader.readLine(), etc rubbish
	 * 
	 * @param path /the/path
	 * @return The entire result of calling hostname:port/path
	 * @throws IOException If there was a problem getting this data i.e couldn't connect, or bad path
	 */
	public String getStringFromContext(String path) {
		try {
			URL url = new URL("http://"+mServer.getInetAddress().getHostName()+":"+mServer.getLocalPort()+path);
			BufferedReader result = new BufferedReader(new InputStreamReader(url.openStream()));
			String stringResult = "";
			String s = "";
			while((s = result.readLine())!=null) {
				stringResult += s + "\n";
			}
			return stringResult.substring(0, stringResult.length()-1); //get rid of the final \n
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns the port we're using. 
	 * May not be the one requested if it was in use.
	 * @return
	 */
	public final int getPort() {
		return mServer.getLocalPort();
	}

	public long getGetWaitPeriod() {
		return mGetWaitPeriod;
	}

	public void setGetWaitPeriod(long mGetWaitPeriod) {
		this.mGetWaitPeriod = mGetWaitPeriod;
	}
}
