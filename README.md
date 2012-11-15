BriefWebServer
==============

A quick web server in Java - used for mocking out web servers with stub data.

See the tests for its usage. But quickly:

	BriefWebServer bws = new BriefWebServer("localhost", 8882);
	bws.addStub("/thing", "one\ntwo");
	
Then the code you're testing can simply talk to localhost on 8882 with the path /thing to get the data. I.e.

	URL url = new URL("http://localhost:8882/thing");
	BufferedReader result = new BufferedReader(new InputStreamReader(url.openStream()));

Then use result.readLine() until you get a null.

There's a helper method to get the string out quickly: 

	String result = bws.getStringFromContext("thing"); // Return one\ntwo

After you've finished, you must run to stop listening on the port at the host:

	bws.stop();

