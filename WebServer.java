/**
 * Implements a simple multi-threaded web server that will accept requests and responds to only the filename in the header lines. 
 * It will spawn a default maximum of 10 threads for simultaneous connections to avoid overloading the host machine
 * This operates under the assumption that the method is always GET. 
 * @author Abe Ramseyer
 * 9/28/2017
 */
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.lang.NumberFormatException;

public final class WebServer {
    public static void main(String args[]) {
        
        // smaller servers probably have no more than 16 physical cores, note that increasing this
        // beyond the physical core count shouldn't increase performance.
        final short THREAD_POOL_SIZE = 10;

        int port = 0;
        ExecutorService pool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        // validate parameters
        if(args.length != 1) {
            System.err.println("Usage: java WebServer port");
            System.exit(1);
        }
        try {
            port = Integer.parseInt(args[0]);
            if(port < 1024 || port > 65535) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            System.err.println("ERR - arg 1");
            System.exit(1);
        }
        
        try {
            ServerSocket welcomeSocket = new ServerSocket(port); 
            System.out.println("\nListening for connections on port " + port + "..\n");

            while(true) {
                try {
                    Callable<Void> request = new HttpRequest(welcomeSocket.accept());
                    pool.submit(request);
                } catch (IOException e) {
                    System.err.println("Error while creating thread");
                }
            }
        } catch (IOException e) {
            System.err.println("Couldn't start server");
        }
    
    }

    /**
     * Encapsulates a single HTTP request sent by a browser and sends back an appropriate response. Can handle following file extensions: .txt .css .gif .jpg .png
     * Implementing Callable<Void> allows this to be run multi-threaded
     * @author Abe Ramseyer
     * 9/28/2017
     */
    private static class HttpRequest implements Callable<Void> {
        private static final String CRLF = "\r\n";
        private Socket socket;
    
        public HttpRequest(Socket socket) { 
            this.socket = socket;
        }
    
        /**
         * processes an HttpRequest object, including sending the response
         * @retunrs null every time
         */
        @Override
        public Void call() {
            try {
                BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                DataOutputStream outToClient = new DataOutputStream(socket.getOutputStream());
                
                String requestLine = inFromClient.readLine();

                // synchronized ensures that these print statements won't mix with other threads'
                synchronized(System.out) {
                    System.out.println("---------- Begin client request header -----");
                    System.out.println(requestLine);
    
                    String headerLine = "";
                    while((headerLine = inFromClient.readLine()).length() != 0) {
                        System.out.println(headerLine);
                    }
                    System.out.println("----------- End client header------------\n\n");
                }   

                StringTokenizer tokens = new StringTokenizer(requestLine);
                String method = tokens.nextToken();

                if(!method.equals("GET")) { // verify the method is GET
                    System.err.println("Unsupported method request " + method); 
                    return null; // return because the method should not be handled by this server
                }  

                String fileName = tokens.nextToken();

                // attempt to open the requested file
                FileInputStream file = null;
                File fileObj = null;
                fileObj = locateFile(fileName);
                if(fileObj != null) {
                    fileName = fileObj.getPath();
                    file = new FileInputStream(fileObj);
                }   
    
                // Construct the response message
                String statusLine = "";
                String contentTypeLine = null;
                String entityBody = "";

                // normal response
                if(file != null) {
                    statusLine = "HTTP/1.1 200 OK";
                    String contentType = contentType(fileName);
                    if(!contentType.equals("unknown"))
                        contentTypeLine = "Content-type: " + contentType; // leaves the contentType as null if unkown

                // file not found, build 404 page
                } else {
                    statusLine = "HTTP/1.1 404 Not Found";
                    contentTypeLine = "text/html";
                    entityBody = "<!DOCTYPE html>\n" +
                                 "<HTML>\n" +
                                 "<HEAD>\n" +
                                 "<TITLE>404 Not Found</TITLE>\n" +
                                 "</HEAD>\n" +
                                 "<BODY>The requested file could not be found on the server. Click <a href=\"./index.html\">here</a> to go to home page.\n" +
                                "<p>You will be automatically redirected in 3 seconds.</p>\n" +
                                "<SCRIPT>\n" +
                                "window.setTimeout(function(){ window.location.replace(\'/index.html\'); },3000)\n" + // .replace() because we don't want the browser's back button to return to the 404 page
                                 "</SCRIPT>" +
                                 "<BODY>\n" +
                                 "</HTML>";
                }
    
    
                // Send the responses
                outToClient.writeBytes(statusLine + CRLF);

                // see comment in contentType() for explanation
                //if(contentTypeLine != null)
                    outToClient.writeBytes(contentTypeLine + CRLF + CRLF);

                // log the headers that were sent to client
                // synchronized ensures the order of print statements won't mix with those in other threads
                synchronized(System.out) {
                    System.out.println("---------- Begin server response header ------");
                    System.out.println(statusLine);
                    System.out.println(contentTypeLine);
                    System.out.println("---------- End server response header --------\n\n");
                }

                if(file != null) {
                    try {
                        sendBytes(file, outToClient); // handle exceptions thrown by .read() and .write()
                        file.close();
                        System.out.println("Sent file " + fileName + " to " + socket.getInetAddress() + ":" + socket.getPort() + "\n");
                    } catch (IOException e) {
                        System.err.println("Exception while reading and sending file");
                    } finally {
                        try {
                            file.close();
                        } catch (IOException exc) {
                            // we tried
                        }
                    }
                } else { // entityBody will hold the 404 page if the file doesn't exist
                    outToClient.writeBytes(entityBody);
                }

                // close data streams
                inFromClient.close();
                outToClient.flush();
                outToClient.close();
    
    
            } catch (IOException e) {
                System.err.println("Error while sending response headers");
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    // Give up handling exceptions, something pretty bad happened
                }
            }   
        
            // necessary for implemented method
            return null;
        }
    
        /*
         * determines a the content type of the file request based on file extension
         */
        private static String contentType(String file) {
            if(file.endsWith(".html") || file.endsWith(".htm"))
                return "text/html; charset=UTF-8";
            else if(file.endsWith(".css"))
                return "text/css; charset=UTF-8";
            else if(file.endsWith(".js"))
                return "text/javascript; charset=UTF-8";
            else if(file.endsWith(".gif"))
                return "image/gif";
            else if(file.endsWith(".jpg") || file.endsWith(".jpeg"))
                return "image/jpeg";
            else if(file.endsWith(".png"))
                return "image/png";
            else if(file.endsWith(".pdf"))
                return "application/pdf";
            
            return "unknown"; // This case should never be encountered because this method only executes
                              // if the file is found, and our server should support any kind of file that
                              // is stored on its working directory. If a filetype unsupported by this method is legitimately 
                              // requested, a check where this method is called removes the content-type response line per
                              // tools.ietf.org/html/rfc7231#section-3.1.1.5h 
                              // "A sender that generates a message containing a payload body SHOULD
                              //  generate a Content-Type header field in that message unless the
                              //  intended media type of the enclosed representation is unknown to the
                              //  sender."
        }       
        
        /*
         * writes the contents of a specified file out to the the client
         */
        private static void sendBytes(FileInputStream file, DataOutputStream outToClient) throws IOException {
            byte[] buffer = new byte[1024];
            int bytes = 0;
            // copy requested file into the socket's output stream using a buffer
            // synchronize the FileInputStream object so multiple threads can't read from the same file at a time
            synchronized (file) {
                while((bytes = file.read(buffer)) != -1) {
                    outToClient.write(buffer, 0, bytes);
                }   
            }
        }
    
        /*
         * manipulates the file name to make it readable (if its not)
         * attempts to open the requested file. upon failure, make 
         * some more adjustments to the file name and try again
         */
        private static File locateFile(String fileName) {
            File file = null;
            if(fileName.equals("/")) // user sent blank information after host/port, default to index page
                fileName += "index.html";

            while(fileName.contains("//")) // remove any duplicate '//' characters
                fileName = fileName.replace("//", "/");

            while(fileName.contains("..")) // remove any .. that may allow access to unintended files in other directories 
                fileName = fileName.replace("..", ".");

            if(fileName.endsWith("/"))     // remove any trailing '/' characters
                fileName = fileName.substring(0, fileName.length()-1);

            // case 1: will convert /index.html/styles.css -> styles.css
            // case 2: will convert /index.html/ -> /index.html
            try {
                if(fileName.lastIndexOf("/") != fileName.indexOf("/")) { // checking if multiple '/' exist 
                    if(fileName.lastIndexOf(".") != fileName.indexOf(".")) // checking if multiple '.' exist
                        fileName = fileName.split("/")[2]; // case 1 
                    else
                        fileName = fileName.split("/")[1]; // case 2 
                }
            } catch (ArrayIndexOutOfBoundsException e) { // encountered a case that breaks these checks in case I didn't think of everything
                return null;
            }

            file = new File(fileName);
            if(file.exists())
                return file;

            // manipulate the beginning some more to see if we can locate the file
            if(!fileName.startsWith("./"))
                fileName = "./" + fileName;
            while(fileName.contains(".."))
                fileName = fileName.replace("..", ".");
            while(fileName.contains("//"))
                fileName = fileName.replace("//", "/");
            fileName = fileName.replace("/.", "/");
            if(fileName.lastIndexOf(".") != fileName.indexOf(".", 1) && fileName.indexOf(".") != fileName.lastIndexOf("."))
                fileName = fileName.substring(0, fileName.lastIndexOf("."));

            file = new File(fileName); // retry with preceeding "./" if not found
            if(file.exists())
                return file;
            else
                return null;
        }
    }
}
