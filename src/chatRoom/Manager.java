package chatRoom;

import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
public class Manager {

	static Hashtable<String, Socket> clientSocketTable = new Hashtable<String, Socket>();
	static Set<SocketListener> listenerTable = new HashSet<SocketListener>();
	public static void main(String[] args) throws Exception{
		// TODO Auto-generated method stub
		
		socketReceiver();
	}
	public static void socketReceiver() throws Exception{
		ServerSocket serverSocket = new ServerSocket(9999);
		while(true){
			Socket clientSocket = serverSocket.accept();
			System.out.println("Got socket");
			SocketHandler socketHandler = new SocketHandler(clientSocket, clientSocketTable);
			socketHandler.start();
		}
	}
}
