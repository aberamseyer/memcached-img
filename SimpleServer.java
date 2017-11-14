import java.io.*;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.Headers;

public class SimpleServer{

	public static void main(String[] args) throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress(12250), 0);
		server.createContext("/", new MyHandler());
		server.setExecutor(null); // creates a default executor
		server.start();
	}

	static class MyHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange t) throws IOException {
			String method = t.getRequestMethod();
			System.out.println(method);
			
			if(method.equals("GET")){
				String requestPage = t.getRequestURI().toString();
				requestPage = "." + requestPage;
				System.out.println(requestPage);
				FileInputStream fis = null;
				try {
					fis = new FileInputStream(requestPage);
				} catch (FileNotFoundException e) {
					System.out.println("Could not open the file");
				}
				byte[] buffer = new byte[1024];
				int bytes = 0;
			
				t.sendResponseHeaders(200, 0);
				OutputStream os = t.getResponseBody();
			
				while((bytes = fis.read(buffer)) != -1){
					os.write(buffer, 0, bytes);
				}	
				fis.close();
				os.close();
			}
			else if(method.equals("POST")){
				Headers header = t.getRequestHeaders();
				for(String s: header.keySet()){
					System.out.println(header.get(s));
				}
				
				InputStream is = t.getRequestBody();
				
				//This try catch is supposed to read incoming file line by line to get the text file.
				//It then attempts to write the information to a file by converting strings to byte arrays. Currently doesn't work.
				/*try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
   					String line = null;
   					String fileName = null;
   					for(int i = 0; i < 4; i++){
   						if(i == 1){
   							fileName = br.readLine();
   							int index = fileName.indexOf("filename=");
   							fileName = fileName.substring(index + 10, fileName.length() -1);
   							System.out.println(fileName);
   						}
   						else
   							System.out.println(br.readLine());
   					}
   					System.out.println("********");
   					
					FileOutputStream fos = null;
					try{
						fos = new FileOutputStream(new File("./" + fileName));
					}
					catch(IOException e){
						System.out.println("Could not write to file.");
					}
					
					while((line = br.readLine()) != null) {
						if(line.contains("-----"))
							break;
						
						fos.write(line.getBytes(), 0, bytes);
						System.out.println(line.getBytes());
					}
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}*/		
				
				//This code works but it has stuff attached to it so writing doesn't create a valid file. Unless it is text.
				//We need to find a way to seperate the rest of the body from file text.
				//This website might be of use
				//https://leonardom.wordpress.com/2009/08/06/getting-parameters-from-httpexchange/
				byte[] buffer = new byte[1024];
				int bytes = 0;
				FileOutputStream fos = null;
				try{
					fos = new FileOutputStream(new File("./testText"));
				}
				catch(IOException e){
					System.out.println("Could not write to file.");
				}
				
				while((bytes = is.read(buffer)) != -1){
					System.out.println("In write loop" + bytes);
					fos.write(buffer, 0, bytes);
				}
				System.out.println("Outside write loop");
				fos.close();
				
				
				is.close();
				t.close();
				
				
			}
		}
	}

}
/*
for(String s : header.keySet())
				System.out.println(header.get(s));
*/
