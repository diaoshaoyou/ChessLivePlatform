import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
	public int maxCount=20;
	public static List<Client> users=new ArrayList<>(); //store users
	public static void main(String[] args) {
			Server serv=new Server();
			serv.start();
	}
	
	 private void start() {
		 System.out.println("Server is ready");
		 Socket socket = null;
		 try {
			 ServerSocket ss = new ServerSocket(8000);
			 while (true) {
				 socket = ss.accept();
				 System.out.println("One client connected");
				 Client client = new Client(socket);
				 users.add(client);
				 new Thread(client).start();
			 }
		 } catch (BindException e) {
			 System.out.println("port occupied");
			 e.printStackTrace();
		 } catch (IOException e1) {
			 e1.printStackTrace();
		 }
	 } 
}
