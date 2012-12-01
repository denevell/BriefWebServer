BriefWebServer
==============

A quick web server in Java - used for mocking out web servers with stub data in tests.

Usage
=====

See the tests for its usage. But quickly:

	BriefWebServer bws = new BriefWebServer("localhost", 8882);
	bws.addStub("/thing", "one\ntwo");
	int port = bws.getPort();
	
Then the code you're testing can simply talk to localhost on 8882, or whatever port it was eventually given, with the path /thing to get the data. I.e.

	URL url = new URL("http://localhost:"+port+"/thing");
	BufferedReader result = new BufferedReader(new InputStreamReader(url.openStream()));

Then use result.readLine() until you get a null.

There's a helper method to get the string out quickly: 

	String result = bws.getStringFromContext("thing"); // Return one\ntwo

After you've finished, you must run to stop listening on the port at the host:

	bws.stop();

Installation
============

The jars are located at: http://ivy.denevell.org/denevell/BriefWebServer/

You can install it using the Apache Ivy extension to Apache Ant

Add, in ivysettings.xml, this resolver: 

            <url name="denevell">
		  <ivy      pattern="http://ivy.denevell.org/[organisation]/[module]/[revision]/ivy-[revision].xml" />
		  <artifact pattern="http://ivy.denevell.org/[organisation]/[module]/[revision]/[artifact]-[revision].[ext]" />
	    </url>

And, in ivy.xml, add this dependency: 

	<dependency org="denevell" name="BriefWebServer" rev="0.2.3"/>

News
====

0.2.3 - Added definable wait period

0.2.2 - Uses the first port available to avoid throwing an exception if we can't bind to a port

0.2.0 - Now implemented using sockets so as to be embedded in an Android app

0.1.1 - Added javadoc

0.1 - Basic stubbing of data at URLs.
