package chatRoom;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class SocketSpeaker extends Thread{
	private Socket socket;
	private String msg;
	SocketSpeaker(Socket socket, String msg){
		this.socket = socket;
		this.msg = msg;
	}
	@Override
	public void run(){
		try {
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			out.println(msg);
			System.out.println("Send " + msg);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return;
	}
}
