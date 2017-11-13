import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.lang.NumberFormatException;

public final class Dispatcher {
    public static void main(String args[]) {
        
        // smaller servers probably have no more than 16 physical cores, note that increasing this
        // beyond the physical core count shouldn't increase performance.
        final short THREAD_POOL_SIZE = 30;
        final String[] HOSTS = {"oak.ad.ilstu.edu:12430",
                                "maple.ad.ilstu.edu:12430",
                                "walnut.ad.ilstu.edu:12430",
                                "pine.ad.ilstu.edu:12430"};
        int port = 0;
        ExecutorService pool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        // validate parameters
        if(args.length != 1) {
            System.err.println("Usage: java Dispatcher port");
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
            int index = 0;
            while(true) {
                String redirect = HOSTS[index];
                index++;
                if(index == 4) index = 0;
                try {
                    Callable<Void> request = new Redirect(welcomeSocket.accept(), redirect);
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
    private static class Redirect implements Callable<Void> {
        private static final String CRLF = "\r\n";
        private Socket socket;
        private String redirect;
    
        public Redirect(Socket socket, String redirect) { 
            this.socket = socket;
            this.redirect = redirect;
        }
    
        /**
         * send a response that redirects the client to an available server
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

                // Construct the response message
                String statusLine = "";
                String contentTypeLine = null;
                String entityBody = "";

                // build response page
                statusLine = "HTTP/1.1 200 OK" + CRLF;
                contentTypeLine = "text/html" + CRLF;
                entityBody = "<!DOCTYPE html>\n" +
                             "<HTML>\n" +
                             "<HEAD>\n" +
                             "<meta http-equiv='refresh' content='0;http://" + redirect + "'>" + 
                             "<TITLE>Redirecting</TITLE>\n" +
                             "</HEAD>\n" +
                             "<BODY>\n" +
                             "</BODY>\n" +
                             "</HTML>";
    
                // Send the responses
                outToClient.writeBytes(statusLine + CRLF);
                outToClient.writeBytes(entityBody);
                // log the headers that were sent to client
                // synchronized ensures the order of print statements won't mix with those in other threads
                synchronized(System.out) {
                    System.out.println("---------- Begin server response header ------");
                    System.out.println(statusLine);
                    System.out.println(contentTypeLine);
                    System.out.println("---------- End server response header --------\n\n");
                }

                // close data streams
                inFromClient.close();
                outToClient.flush();
                outToClient.close();
    
            } catch (IOException e) {
                System.err.println("Error while sending response");
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
    }
}

