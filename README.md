BriefWebServer
==============

A quick web server in Java - used for mocking out web servers with stub data.

See the tests for its usage. But quickly:

	BriefWebServer bws = new BriefWebServer("localhost", 8882);
	bws.addStub("/thing", "one\ntwo");
	
	String result = bws.getStringFromContext("thing"); // Return one\ntwo

	bws.stop();

Or if you don't want to use the helper to get the data:

	URL url = new URL("http://localhost:8882/");
	BufferedReader result = new BufferedReader(new InputStreamReader(url.openStream()));

Then use result.readLine() until you get a null.
