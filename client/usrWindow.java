import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.net.*;
import java.text.*;
import java.io.*;
import java.util.*;
//the first 3 bits of message are for signal, starts with \ 
public class usrWindow extends JFrame implements MouseListener{
	 private static int winWidth=600;
	 private static int winHeight=600;
	 private static int cell=25;//cell length
	 private static int chessR=20;//chess radius
	 private TextField typeArea = new TextField();
	 private TextArea chatContent = new TextArea();
	 private JPanel background = new JPanel();
	 private int[] chessPos=new int[19*19*3];//store chess position (x,y,color)
	 private int chessNum;
	 private int [] newPos=new int[3];//new added chess position
	 private Socket socket = null;
	 private OutputStream out = null;
	 private DataOutputStream dos = null;
	 private InputStream in = null;
	 private DataInputStream dis = null;
	 private boolean flag = false;
	 private int ID;//user ID
	 private int lastTurn;//last player to place chess
	 private int isWin;//note whether win or not
	 private int loseID;//loser ID
	 
	 public static void main(String[] args) {
		 usrWindow user=new usrWindow();
		 user.start();
	 }
	 usrWindow(){
		 for(int i=0;i<19*19*3;i++) {//chessPos init
			 chessPos[i]=-1;
		 }
		 ID=0;
		 chessNum=0;
		 newPos[0]=-1;
		 newPos[1]=-1;
		 newPos[2]=-1;
		 lastTurn=-1;
		 isWin=-1;
		 loseID=-1;
	 }
	 
	 private boolean getMouseCoord(int x, int y) {		 
		 int beginX=68;
		 int beginY=9;
		 for(int i=0;i<19;i++) {
			 if(x>=beginX+(i-0.3)*cell && x<=beginX+(i+0.3)*cell) {//range circle radius=0.3*cell
				 for(int j=0;j<19;j++) {
					 if(y>=beginY+(j-0.3)*cell && y<=beginY+cell*(j+0.3)) {
//						 System.out.println(i+" "+j);
						 if(isOccupied(i,j,ID)!=0) return false;
						 newPos[0]=i;//update chess position
						 newPos[1]=j;
						 newPos[2]=ID;
						 return true;
					 }
				 }		 
			 }
		 }
		 return false;
	 }
	 private int isOccupied(int x, int y, int color) {//check whether there is already a chess here
		 //return 0, not occupied; return 1, occupied & same color; return 2, occupied & diffient color 
		 if(x<0 || y<0 || x>=19 || y>=19) return 0;
		 for(int i=0; i<chessNum; i++) {
			 if(chessPos[i*3]==x && chessPos[i*3+1]==y) {
				 if(chessPos[i*3+2]==color) return 1;
				 else return 2;
			 }
		 }
		 return 0;
	 }
	 private void start() {
		 this.setSize(winWidth, winHeight);
		 this.setLocation(250, 150);
		 this.setVisible(true);
		 this.setTitle("Chess Live");
	
		 //chat area:
		 chatContent.setPreferredSize(new Dimension(winWidth,winHeight/9));
		 this.add(chatContent, BorderLayout.NORTH);//chatting content is in north
		 this.add(background,BorderLayout.CENTER);
		 this.add(typeArea, BorderLayout.SOUTH);//typing area is in south
		 chatContent.setFocusable(false);
		 
		 // try to close
		 addWindowListener(new WindowAdapter() {
			 public void windowClosing(WindowEvent e) {
				 System.out.println("User is closing the window");
				 disconnect();
				 System.exit(0);
			 }
		 });
		 // press "Enter" to send 
		 typeArea.addActionListener(new ActionListener() {
			 public void actionPerformed(ActionEvent e) {
				 onClickEnter();
			 }
		 });
		 //chess placing listener:
		 background.addMouseListener(new MouseListener() {
			@Override	
			public void mousePressed(MouseEvent e) { }
			@Override
			public void mouseClicked(MouseEvent e) { 
				if(loseID!=-1) return;//already end
				if(ID!=0 && ID!=1 ) return;//not chess player
				if(lastTurn==ID) return;//last turn is myself
				if(getMouseCoord(e.getX(), e.getY())==false) return;//not found in chessboard
				chessPos[chessNum*3]=newPos[0];//add to chessPos
				chessPos[chessNum*3+1]=newPos[1];
				chessPos[chessNum*3+2]=newPos[2];
				chessNum++;
				if(checkWin()==true) {
					isWin=1;//check whether win or not
					loseID=1-ID;
					System.out.println("user "+ID+" wins!");
					System.out.println("user "+loseID+" loses!");
				}
				repaint();
				lastTurn=ID;
				//send chess information:
				StringBuffer chessInfo=new StringBuffer("\\\\");//chessInfo starts with \\
				chessInfo.append(String.valueOf(ID));
				chessInfo.append(String.valueOf(newPos[0]));
				chessInfo.append(".");//coordidates divided by .
				chessInfo.append(String.valueOf(newPos[1]));
				chessInfo.append(".");
				chessInfo.append(String.valueOf(newPos[2]));
				sendMessageToServer(chessInfo.toString());//send chess information to server
				//send win information:
				if(isWin==1) sendMessageToServer("\\\\2"+ID);//win info
			}
			@Override
			public void mouseReleased(MouseEvent e) { }
			@Override
			public void mouseEntered(MouseEvent e) { }
			@Override
			public void mouseExited(MouseEvent e) { }
		 });
		 
		 connect();//set connection
		 new Thread(new ReceiveMessage()).start();
	 }
	 private boolean checkWin() {
		 if(chessNum<9) return false;
		 int count=0;
		 int j=0;
		 int x,y;
		 for(int i=0;i<chessNum;i++) {
			 x=chessPos[i*3];
			 y=chessPos[i*3+1];
			 //x-axis:
			 while(isOccupied(x+j,y,ID)==1) {//same color
				 j++;
				 count++;
			 }
			 j=1;
			 while(isOccupied(x-j,y,ID)==1) {//same color
				 j++;
				 count++;
			 }
			 if(count==5) {
				 return true;
			 }
			 //y-axis:
			 j=0;
			 count=0;
			 while(isOccupied(x,y+j,ID)==1) {//same color
				 j++;
				 count++;
			 }
			 j=1;
			 while(isOccupied(x,y-j,ID)==1) {//same color
				 j++;
				 count++;
			 }
			 if(count==5) {
				 return true;
			 }
			 //declined1:
			 j=0;
			 count=0;
			 while(isOccupied(x+j,y+j,ID)==1) {//same color
				 j++;
				 count++;
			 }
			 j=1;
			 while(isOccupied(x-j,y-j,ID)==1) {//same color
				 j++;
				 count++;
			 }
			 if(count==5) {
				 return true;
			 }
			//declined2:
			 j=0;
			 count=0;
			 while(isOccupied(x-j,y+j,ID)==1) {//same color
				 j++;
				 count++;
			 }
			 j=1;
			 while(isOccupied(x+j,y-j,ID)==1) {//same color
				 j++;
				 count++;
			 }
			 if(count==5) {
				 return true;
			 }
		 }
		 return false;
	 }
	 //draw chessboard and chess:
	 public void paint(Graphics g) {
		 super.paint(g);
		 int beginX=winWidth/2-9*cell;//origin:(68,9)
		 int beginY=winHeight/8+30;
		 int endX=beginX+cell*18;
		 int endY=beginY+cell*18;
		 //draw lines:
		 for(int i=0;i<19;i++) {
			 g.drawLine(beginX, beginY+i*cell, endX, beginY+i*cell);//row
			 g.drawLine(beginX+i*cell, beginY, beginX+i*cell, endY);//column
		 }
		 //draw chess
		 for(int i=0;i<chessNum;i++) {
			 if(chessPos[i*3+2]==0) g.setColor(Color.black);
			 else g.setColor(Color.white);
			 g.fillOval(beginX-chessR/2+chessPos[i*3]*cell, beginY-chessR/2+chessPos[i*3+1]*cell, chessR, chessR);
		 }
		 //draw win/lose tips
		 g.setFont(new Font("Times New Roman", Font.BOLD, 50));
		 if(loseID!=-1){//already end
			 if(isWin==1) {//winner
				 g.setColor(Color.orange);
				 g.drawString("Congratulations!", 120, 300);
				 g.drawString("You win!", 200, 350);
			 }
			 else if(isWin==0) {//loser
				 g.setColor(Color.red);
				 g.drawString("Sorry!", 230, 300);
				 g.drawString("You lose!", 200, 350);
			 }
			 else{//not player
				 g.setColor(Color.blue);
				 g.drawString("User "+(1-loseID)+" wins!", 150, 300);
				 g.drawString("User "+loseID+" loses!", 150, 350);
			 }
		 }
	 }
	 
	 private class ReceiveMessage implements Runnable {
		 @Override
		 public void run() {
			 flag = true;
			 try {
				 while (flag) {//keep receiving message
					 String message = dis.readUTF();
					 if(message.charAt(0)=='\\') {//start with \x, chess information
						 if(message.charAt(1)=='\\') {
							 if(message.charAt(2)=='0') {//black chess
								 if(lastTurn==0) continue;//ignore
								 chessPos[chessNum*3]=Integer.parseInt(message.substring(3,message.indexOf(".")));//add to chessPos
								 chessPos[chessNum*3+1]=Integer.parseInt(message.substring(message.indexOf(".")+1, message.lastIndexOf(".")));
								 chessPos[chessNum*3+2]=0;
								 chessNum++; 
								 repaint();//repaint the chessboard
								 lastTurn=0;
							 }
							 else if(message.charAt(2)=='1') {//white chess
								 if(lastTurn==1) continue;//ignore
								 chessPos[chessNum*3]=Integer.parseInt(message.substring(3,message.indexOf(".")));//add to chessPos
								 chessPos[chessNum*3+1]=Integer.parseInt(message.substring(message.indexOf(".")+1, message.lastIndexOf(".")));
								 chessPos[chessNum*3+2]=1;
								 chessNum++;
								 repaint();//repaint the chessboard
								 lastTurn=1;
							 }
							 else if(message.charAt(2)=='2') {//result out
								 loseID=1-Integer.parseInt(message.substring(3,4));
								 System.out.println(loseID);
								 if(ID==loseID)
									 isWin=0;//lose
								 System.out.println("user "+(1-loseID)+" wins!");
								 System.out.println("user "+loseID+" loses!");
								 repaint();
							 }
							 else if(message.charAt(2)=='\\'){//chat content
								 chatContent.append(message.substring(3));
							 }
							
						 }
						 else {//user window ID
							 ID=Integer.parseInt(message.substring(1, 3));
							 System.out.println("user ID = "+ID);
						 }
					 }
				 }
			 } catch (EOFException e) {
				 flag = false;
				 System.out.println("Server is closed");
				 // e.printStackTrace();
			 } catch (SocketException e) {
				 flag = false;
				 System.out.println("Server is closed");
				 // e.printStackTrace();
			 } catch (IOException e) {
				 flag = false;
				 System.out.println("Fail to receive message");
				 e.printStackTrace();
			 }
		 }
	 }
	
	 private void onClickEnter() {
		 StringBuffer message=new StringBuffer("\\\\\\"); //signal
		 String tmp=typeArea.getText().trim();
		 if (tmp != null && !tmp.equals("")) {
		  String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
		  message.append("***user "+ID+"    "+time + "***\n" + tmp + "\n");
		  chatContent.append("***user "+ID+"    "+time + "***\n" + tmp + "\n");
		  typeArea.setText("");
		  sendMessageToServer(message.toString());
		 }
	}
		
	private void sendMessageToServer(String message) {
		try {
			dos.writeUTF(message);
			dos.flush();
		 } catch (IOException e) {
			 System.out.println("Fail to send message");
			 e.printStackTrace();
		 }
	 }
	
	 private void connect() {
		 try {
			 socket = new Socket("localhost", 8000);
			 out = socket.getOutputStream();
			 dos = new DataOutputStream(out);
			 in = socket.getInputStream();
			 dis = new DataInputStream(in);
		 } catch (UnknownHostException e) {
			 System.out.println("Fail to apply connection");
			 e.printStackTrace();
		 } catch (IOException e) {
			 System.out.println("Fail to apply connection");
			 e.printStackTrace();
		 }
	 }
	
	 private void disconnect() {
		 flag = false;
		 if (dos != null) {
			 try {
				 dos.close();
			  } catch (IOException e) {
				  System.out.println("Fail to close dos");
				  e.printStackTrace();
			  }
		 }
		 if (out != null) {
			 try {
				 out.close();
			 } catch (IOException e) {
			  	 System.out.println("Fail to close dos");
			  	 e.printStackTrace();
			 }
		 }
		 if (socket != null) {
			 try {
				 socket.close();
			 } catch (IOException e) {
			  	 System.out.println("Fail to close socket");
			  	 e.printStackTrace();
			 }
		 }
	}
}
