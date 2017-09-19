/**
* Web worker: an object of this class executes in its own new thread
* to receive and respond to a single HTTP request. After the constructor
* the object executes on its "run" method, and leaves when it is done.
*
* One WebWorker object is only responsible for one client connection. 
* This code uses Java threads to parallelize the handling of clients:
* each WebWorker runs in its own thread. This means that you can essentially
* just think about what is happening on one client at a time, ignoring 
* the fact that the entirety of the webserver execution might be handling
* other clients, too. 
*
* This WebWorker class (i.e., an object of this class) is where all the
* client interaction is done. The "run()" method is the beginning -- think
* of it as the "main()" for a client interaction. It does three things in
* a row, invoking three methods in this class: it reads the incoming HTTP
* request; it writes out an HTTP header to begin its response, and then it
* writes out some HTML content for the response content. HTTP requests and
* responses are just lines of text (in a very particular format). 
*
**/
package p1;
import java.net.Socket;
import java.lang.Runnable;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Date;
import java.text.DateFormat;
import java.util.TimeZone;

import javax.imageio.ImageIO;

public class WebWorker implements Runnable
{

private Socket socket;

/**
* Constructor: must have a valid open socket
**/
public WebWorker(Socket s)
{
   socket = s;
}

/**
* Worker thread starting point. Each worker handles just one HTTP 
* request and then returns, which destroys the thread. This method
* assumes that whoever created the worker created it with a valid
* open socket object.
* 
* Modified on 9/5/17 by Xitlally Salmon: The changes that I did to this method was to add 
* the boolean WasAbleToRead which is true when the file exists and is readable other wise it is false.
* Another change to the Method was the fact that the writeContent() will not be called unless WasAbleToRead
* is true
* 
* Modified on 9/14/17 by Xitlally Salmon: The changes that I have done to this is method is adding a switch
* to check what the extension on the file is and sending the right content type to the http heard writer.
**/
public void run()
{
 
   System.err.println("Handling connection...");
   try {
      InputStream  is = socket.getInputStream();
      OutputStream os = socket.getOutputStream();
      String Path=readHTTPRequest(is);
      boolean WasAbleToRead=ReadFile(Path);
      String[] ContentType =Path.split("\\.");
      switch(ContentType[2]) {
      case "jpg":
    	  writeHTTPHeader(os,"image/jpeg", WasAbleToRead);
    	  break;
      case "png":
    	  writeHTTPHeader(os,"image/png", WasAbleToRead);
    	  break;
      case "gif":
    	  writeHTTPHeader(os,"image/gif", WasAbleToRead);
    	  break;
      case "html":
    	  writeHTTPHeader(os,"text/html", WasAbleToRead);
    	  break;
      case "txt":
    	  writeHTTPHeader(os,"text/html", WasAbleToRead);
    	  break;
      default:
    		  break;
      }// end switch
      if(WasAbleToRead==true)
    	  writeContent(os,Path,ContentType[1]);
      os.flush();
      socket.close();
   }//end try 
   catch (Exception e) {
      System.err.println("Output error: "+e);
   }// end catch
   System.err.println("Done handling connection.");
   return;
}

/**
* Read the HTTP request header.
* Modified on 9/5/17 by Xitlally Salmon: The changes that I made to this method was to change the return type
* to a string so that it can return the path to the file for the rest of the methods, to do this I added a
* if statement that will only be true when the while loop has iterated only  once because the first line is
* the one with the path, to get just the path I use split on the spaces and get the second entry.
 * @throws Exception 
**/
private String readHTTPRequest(InputStream is)
{
	   String line = null;
	   String[] path = null;
	   int lineNumber=1;
	   BufferedReader r = new BufferedReader(new InputStreamReader(is));
	   while (true) {
	      try {
	         while (!r.ready()) Thread.sleep(1);
	         line = r.readLine();
	         if(lineNumber==1) {
	        	 path=line.split(" ");
	         }//end try
	         System.err.println("Request line: ("+line+")");
	         lineNumber++;
	         if (line.length()==0) break;
	      } catch (Exception e) {
	         System.err.println("Request error: "+e);
	         break;
	      }// end catch
	   }//end while
       return "."+path[1];
}

/*
 *Made on 9/5/17 by Xitlally Salmon: This method is to just check to make sure the file
 *is readable, it returns a boolean that is then saved into WasAbleToRead in run  
 */
private boolean ReadFile(String path)   {
	String line=null;
	try {
		BufferedReader F = new BufferedReader(new FileReader(path));

		try {
			line = F.readLine();
			if(line != null)
			    return true;
		} // end try
		catch (IOException e) {
			System.out.println("Unable to open file ");  
	        return false;
		}   // end catch

        // Always close files.
        try {
			F.close();
		}// end try 
        catch (IOException e) {
			System.out.println("Unable to close file ");   
	        return true;
		} // end catch  
	}// end try
    catch(FileNotFoundException ex) {
        System.out.println("Unable to open file ");   
        return false;
    }// end catch
	return false;
}

/**
* Write the HTTP header lines to the client network connection.
* @param os is the OutputStream object to write to
* @param contentType is the string MIME content type (e.g. "text/html")
* 
* Modified on 9/5/17 by Xitlally Salmon: The only thing that I changed in this was to add a
* parm, which is a boolean and if its try the method will send out a 202 code  it will send
* out a 404 error code 
**/
private void writeHTTPHeader(OutputStream os, String contentType, boolean FileExists) throws Exception
{
   Date d = new Date();
   DateFormat df = DateFormat.getDateTimeInstance();
   df.setTimeZone(TimeZone.getTimeZone("GMT"));
   if(FileExists==true)
	   os.write("HTTP/1.1 200 OK\n".getBytes());
   else {
	   os.write("HTTP/1.1 404: Not Found\n".getBytes());
   }
   os.write("Date: ".getBytes());
   os.write((df.format(d)).getBytes());
   os.write("\n".getBytes());
   os.write("Server: Jon's very own server\n".getBytes());
   //os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
   //os.write("Content-Length: 438\n".getBytes()); 
   os.write("Connection: close\n".getBytes());
   os.write("Content-Type: ".getBytes());
   os.write(contentType.getBytes());
   os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
   return;
}

/**
* Write the data content to the client network connection. This MUST
* be done after the HTTP header has been written out.
* @param os is the OutputStream object to write to
 * @throws IOException 
 * 
 * Modified on 9/5/17 by Xitlally Salmon: The changes that I made to this method was to read the file
 * that was at the end of the file path and do the tag substitution for what is written in the file
 * both when the tags are on new lines and when the tags are right next to each other
 * 
 * Modifies on 9/14/17 by Xitlally Salmon: The changes that I have made to this method was to add
 * the ability to to read images in both the form of being given the file path and if the file
 * is being given in the form of a html tag. Also if the website is asking for the favicon
 * it will provide the html tag to put it.
**/
private void writeContent(OutputStream os, String file,String ContentType) throws IOException
{
   os.write("<html><head></head><body>\n".getBytes());
   if(file.contains("favicon")) {
	   // The html tag to give the favicon
	   os.write("<link rel=\"icon\" type=\"image/png\" href=\"./test/favicon.png\" >".getBytes());
   }//end if
   else {
       //this will read the .txt or .html file and look for the <cs371server>, <cs371date>, and img tag
	   //and do the appropriate switches to get the correct info on the screen
	   if(ContentType.equals("html")||ContentType.equals("txt")) {
		   String line;
			try {
				BufferedReader F = new BufferedReader(new FileReader(file));
				try {
					while((line = F.readLine()) != null) {
						if(line.contains("cs371date"))
							os.write(" <script language=\"javascript\">\nvar today = new Date();\ndocument.write(today);\n</script>".getBytes());
						if(line.contains("cs371server"))
							os.write("<h3>Web Server name: Xitlally's Server</h3>\n".getBytes());
						if(line.contains("<img scr")) {
							try {	
						         FileInputStream reader = new FileInputStream(new File(file));
						         int bytes;
						         while (( bytes = reader.read()) != -1){ 
						             //output bytes
						             os.write(bytes); 
						         }//end while
						         reader.close();
						      }// end try  
						      
						      catch (IOException e) {
							       System.out.println("Unable to open file ");
						      }//end catch
						}// end if
						else
							os.write(line.getBytes());
					}// end if
				}// end try 
				catch (IOException e) {
					System.out.println("Unable to open file ");
				} // end catch

		       // Always close files.
				try {
					F.close();
				}// end try 
				catch (IOException e) {
					System.out.println("Unable to close file ");
				}  // end catch
			}// end try
		   catch(FileNotFoundException ex) {
		       System.out.println("Unable to open file ");
		   }// end catch
	   }
	   
	   else {
		   // this opens and reads the image file
		   try {	
		         FileInputStream reader = new FileInputStream(new File(file));
		         int bytes;
		         while (( bytes = reader.read()) != -1){ 
		             //output bytes
		             os.write(bytes); 
		         }// end while
		         reader.close();
		      }// end try
		      
		      catch (IOException e) {
			       System.out.println("Unable to open file ");
		      }//end catch
	   }//end else
   }//end else
   os.write("</body></html>\n".getBytes());
}//end writeContent

} // end class
