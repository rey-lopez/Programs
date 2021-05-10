package edu.nmsu.cs.webserver;

/**
 * Web worker: an object of this class executes in its own new thread to receive and respond to a
 * single HTTP request. After the constructor the object executes on its "run" method, and leaves
 * when it is done.
 *
 * One WebWorker object is only responsible for one client connection. This code uses Java threads
 * to parallelize the handling of clients: each WebWorker runs in its own thread. This means that
 * you can essentially just think about what is happening on one client at a time, ignoring the fact
 * that the entirety of the webserver execution might be handling other clients, too.
 *
 * This WebWorker class (i.e., an object of this class) is where all the client interaction is done.
 * The "run()" method is the beginning -- think of it as the "main()" for a client interaction. It
 * does three things in a row, invoking three methods in this class: it reads the incoming HTTP
 * request; it writes out an HTTP header to begin its response, and then it writes out some HTML
 * content for the response content. HTTP requests and responses are just lines of text (in a very
 * particular format).
 * 
 * @author Jon Cook, Ph.D.
 *
 **/

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;


public class WebWorker implements Runnable
{
	private boolean debug = true;
	private String pagestat = "HTTP/1.1 202 OK\n";
	private String testpath;
	private File path;
	private boolean exists;
	private String line;
	private boolean home = true;
	
	
	private Socket socket;
	

	/**
	 * Constructor: must have a valid open socket
	 **/
	public WebWorker(Socket s)
	{
		socket = s;
	}

	/**
	 * Worker thread starting point. Each worker handles just one HTTP request and then returns, which
	 * destroys the thread. This method assumes that whoever created the worker created it with a
	 * valid open socket object.
	 **/
	public void run()
	{
		System.err.println("Handling connection...");
		try
		{
			InputStream is = socket.getInputStream();
			OutputStream os = socket.getOutputStream();
			readHTTPRequest(is);
			writeHTTPHeader(os, "text/html");
			writeContent(os);
			os.flush();
			socket.close();
		}
		catch (Exception e)
		{
			System.err.println("Output error: " + e);
		}
		System.err.println("Done handling connection.");
		return;
	}

	/**
	 * Read the HTTP request header.
	 **/
	private void readHTTPRequest(InputStream is)
	{

		//String line;
		BufferedReader r = new BufferedReader(new InputStreamReader(is));
		while (true)
		{
			try
			{
				while (!r.ready())
					Thread.sleep(1);
				line = r.readLine();
				 String splitline[] = line.split(" ");
				
				 // checks if GET is present
				if(splitline.length > 1 && splitline[0].equals("GET")) {
					
						System.err.println("Get Found: " + line);
					int getItemLength = splitline[1].length();
					if((splitline[1].equals("/") == false))
								home = false;
					if ((splitline[1].substring(getItemLength -5).equals(".html")) || splitline[1].substring(getItemLength -4).equals(".txt")) {
						
							System.err.println("Web Page Found: " + splitline[1]);
							testpath = splitline[1].substring(1);
							home = false;
			
						try {
							File path = new File(testpath); 
							 exists = path.exists();
							
							
							// checks if file exists
							if(exists == false) {
								pagestat = "HTTP/1.1 404 not found\n";
								} // if 
							
							else {
								pagestat = "HTTP/1.1 200 OK\n";
								} // else 
							
						} // second try
									
							catch (Exception e) {
								System.err.println(e.getMessage());
								} // catch
						
					} // if html 
				} // if get
					
							System.err.println("Request line: (" + line + ")");
						
						if (line.length() == 0)
							break;
						
					} // first try 
						catch(Exception e) {
							System.err.println("Request error: " + e);
							break;
						} // catch 
					
				} // while true 
					return; 
				} //HTTPreadrequest 
			

	/**
	 * Write the HTTP header lines to the client network connection.
	 * 
	 * @param os
	 *          is the OutputStream object to write to
	 * @param contentType
	 *          is the string MIME content type (e.g. "text/html")
	 **/
	private void writeHTTPHeader(OutputStream os, String contentType) throws Exception
	{
		Date d = new Date();
		DateFormat df = DateFormat.getDateTimeInstance();
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		os.write(pagestat.getBytes());
		os.write("Date: ".getBytes());
		os.write((df.format(d)).getBytes());
		os.write("\n".getBytes());
		os.write("Server: Rey's very own server\n".getBytes());
		// os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
		// os.write("Content-Length: 438\n".getBytes());
		os.write("Connection: close\n".getBytes());
		os.write("Content-Type: ".getBytes());
		os.write(contentType.getBytes());
		os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
		return;
	}

	/**
	 * Write the data content to the client network connection. This MUST be done after the HTTP
	 * header has been written out.
	 * 
	 * @param os
	 *          is the OutputStream object to write to
	 **/
	private void writeContent(OutputStream os) throws Exception
	{
		
		if ( home == true) {
			os.write("<html><head></head><body>\n".getBytes());
			os.write("<h3>My web server works!</h3>\n".getBytes());
			os.write("</body></html>\n".getBytes());
		}
		
		else {
			String output;
			String date = getDate();
		
		
			if (exists == true) {
				BufferedReader r = new BufferedReader(new FileReader(testpath));
				output = r.readLine();
					
				while (output != null) {
					String replaceServer = output.replaceAll("<cs371server>", "Reys server");
					String replaceDate = replaceServer.replaceAll("<cs371date>", date);
					os.write(replaceDate.getBytes());
					output = r.readLine();
				}
				r.close();
			} // end if
		
			else {
				os.write("Page not found".getBytes()); }
			
		} // else home != true

	} // end write content
	
	private String getDate()
	{
		String date;
		Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
		int day = calendar.get(Calendar.DATE);
		int month = calendar.get(Calendar.MONTH) + 1;
		int year = calendar.get(Calendar.YEAR);
		date = month + "/" + day + "/" + year;
		
		return date;
	}

} // end class
