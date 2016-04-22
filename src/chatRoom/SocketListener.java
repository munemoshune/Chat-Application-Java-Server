package chatRoom;

import java.net.Socket;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Hashtable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.lang.management.ManagementFactory;
public class SocketListener extends Thread{
	private Socket socket;
	private DatabaseStuff databaseStuff;
	private String account;
	private String id;
	private boolean isRun = false;
	Hashtable<String, Socket> clientSocketTable = Manager.clientSocketTable;
	SocketListener(String account, String id, DatabaseStuff databaseStuff){
		this.account = account;
		this.id = id;
		this.socket = Manager.clientSocketTable.get(account);
		this.databaseStuff = databaseStuff;
		isRun = true;
	}
	public void setIsRun(boolean IsRun){
		this.isRun = IsRun;
	}
	@Override
	public void run(){
		BufferedReader in;
		try {
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String inputLine;
		    while(isRun){
		        //while((inputLine = in.readLine()) == null);
		    	inputLine = in.readLine();
		        System.out.println(inputLine);
		        JSONObject json = new JSONObject(inputLine); 
		        if(json.getString("action").equals("logOut")){
		        	java.lang.management.ThreadMXBean t = ManagementFactory.getThreadMXBean();
		        	System.out.println("Thread count : " + t.getThreadCount());
		        	noticeOffline();
		        	databaseStuff.update("update account set log_in = '0' where account = '" + json.getString("account") + "'");
		        	databaseStuff.close();
		        	Manager.clientSocketTable.remove(account);
		        	//socket.close();
		        	return;
		        }
		        new PackageHandler(inputLine, databaseStuff).start();
		    }
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void noticeOffline() throws Exception{
		JSONObject noticeOffline = new JSONObject();
    	noticeOffline.put("dataType", "friendOffline");
    	noticeOffline.put("account", account);
    	ArrayList<String> friendList = friendList();
    	for(int i = 0; i < friendList.size(); i ++){
    		if(Manager.clientSocketTable.containsKey(friendList.get(i))){
    			System.out.println("Notice " + friendList.get(i));
    			SocketSpeaker speaker = new SocketSpeaker(Manager.clientSocketTable.get(friendList.get(i)), noticeOffline.toString());
    			speaker.start();
    			speaker.join();
    		}
    	}
	}
	private ArrayList<String> friendList() throws Exception{
		ArrayList<String> friendList = new ArrayList<String>();
		System.out.println(id);
		ResultSet frdListRs1 = databaseStuff.query("select * from friendship where account1_id = '" + id + "' and status = '0'");
		while(frdListRs1.next()){
			//System.out.println(frdListRs1.getString("account2_id"));
			friendList.add(queryAccountById(frdListRs1.getString("account2_id")));
		}
		ResultSet frdListRs2 = databaseStuff.query("select * from friendship where account2_id = '" + id + "' and status = 0");
		while(frdListRs2.next()){
			//System.out.println(frdListRs1.getString("account1_id"));
			friendList.add(queryAccountById(frdListRs2.getString("account1_id")));
		}
		return friendList;
	}
	private String queryAccountById(String id) throws Exception{
		ResultSet rs = databaseStuff.query("select * from account where id = '" + id + "'");
		if(rs.next())
			return rs.getString("account");
		else
			throw new Exception("There is ID but no such account");
	}
}