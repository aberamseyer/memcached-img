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
				//System.out.println(requestPage);
				//System.out.println(requestPage.contains("="));
				if(requestPage.contains("=")){//If there is a search then this method handles it.
					requestPage = requestPage.substring(requestPage.indexOf("=") + 1, requestPage.length());
					requestPage = requestPage.replace("+", "");
					requestPage = requestPage.toLowerCase();
					System.out.println(requestPage);
					//^^^Above code gets the search result and deletes spaces and makes it lowercase
					
					String html = createHTML(requestPage);//Gets html that is returned
					
					//Writes to the viewResults.html page (overwrites file)
					File file = new File("viewResults.html");
					FileWriter fw = new FileWriter(file, false);
					fw.write(html);
					fw.close();
					
					//vvvvv Reads from the results page and uploads it
					FileInputStream fis = null;
					try {
						fis = new FileInputStream("./viewResults.html");
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
					//^^^^^
					
				}
				else{
					//vvvvv Returns index page
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
					//^^^^^
				}
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
	
	/*
		This method takes a string and will search the library for files that contain the string.
		The method then dynamically generates the html and returns it as a string.
	*/
	private static String createHTML(String searchString){
		StringBuilder strBld = new StringBuilder();
		File folder = new File("./Pictures/");
		File[] directory = folder.listFiles();
		strBld.append("<html>\n");
		strBld.append("<head>\n\n</head>\n");
		strBld.append("<body>\n");
		strBld.append("<h1>Results for " + searchString + "</h1>\n");
		for(int i = 0; i < directory.length; i++){
			if(directory[i].isFile() && directory[i].getName().contains(searchString)){
				strBld.append("<img src=\"./Pictures/" +  directory[i].getName() + "\" alt=\"" + directory[i].getName() + "\" style=\"height:300px;\"></br>\n");
			}
		}
		strBld.append("</body>\n");
		strBld.append("</html>\n");
		return strBld.toString();
	}

}
/*
for(String s : header.keySet())
				System.out.println(header.get(s));
*/
