import java.io.*;
import java.net.*;

public class Client implements Runnable {
	
	 private Socket socket = null;
	 InputStream in = null;
	 DataInputStream din = null;
	 OutputStream out = null;
	 DataOutputStream dos = null;
	 boolean flag = true;
	
	 public Client(Socket socket) {
		 this.socket = socket;
		 int order=Server.users.size();
		 if(order>99) {
			 System.out.println("user count is out of range!"); 
		 }
		 StringBuffer msg=new StringBuffer("\\");
		 if(order<10)
			 msg.append("0");
		 msg.append(String.valueOf(order));
		 try {
			 in = socket.getInputStream();
			 din = new DataInputStream(in);
			 out = this.socket.getOutputStream();
			 dos = new DataOutputStream(out);
			 forwardToClient(msg.toString());
			 System.out.println("send user ID successfully! ID: "+order);
		 } catch (IOException e) {
			// System.out.println("Fail to receive message");
			 e.printStackTrace();
		 }
		
	 }

	 public void run() {
		  String message;
		  try {
			  while (flag) {
				  message = din.readUTF();
				  // System.out.println("¿Í»§Ëµ£º" + message);
				  forwardToAllClients(message);
			  }
		  } catch (SocketException e) {
			  flag = false;
			  System.out.println("One client disconnected");
			  Server.users.remove(this);
			  // e.printStackTrace();
		  } catch (EOFException e) {
			  flag = false;
			  System.out.println("One client disconnected");
			  Server.users.remove(this);
			  // e.printStackTrace();
		  } catch (IOException e) {
			  flag = false;
			  System.out.println("Fail to receive message");
			  Server.users.remove(this);
			  e.printStackTrace();
		  }
		
		  if (din != null) {
			  try {
				  din.close();
		  } catch (IOException e) {
			  	  System.out.println("Fail to close din");
			  	  e.printStackTrace();
		  	  }
		  }
		  if (in != null) {
			  try {
				  in.close();
			  } catch (IOException e) {
				  System.out.println("Fail to close din");
				  e.printStackTrace();
			  }
		  }
		  if (socket != null) {
			  try {
				  socket.close();
			  } catch (IOException e) {
				  System.out.println("Fail to close din");
				  e.printStackTrace();
			  }
		  }
	 }
	 
	 private void forwardToAllClients(String message) throws IOException {
		  for (Client c : Server.users) {
			  if (c != this) {//not myself
				  out = c.socket.getOutputStream();
				  dos = new DataOutputStream(out);
				 forwardToClient(message);
				 System.out.println("chatting transmition succeed");
			  }
		  }
	 }
	
	 private void forwardToClient(String message) throws IOException {
		 dos.writeUTF(message);
		 dos.flush();
	 }
}