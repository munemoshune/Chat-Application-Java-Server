package chatRoom;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.net.Socket;
import java.sql.ResultSet;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;

import org.json.JSONArray;
import org.json.JSONObject;

public class SocketHandler extends Thread{
	private Socket socket;
	private String account;
	private String password;
	private String id;
	private Hashtable<String, Socket> clientSocketTable;
	private JSONArray frdList;
	DatabaseStuff database;
	SocketListener socketListener;
	SocketHandler(Socket socket, Hashtable<String, Socket> clientSocketTable)throws Exception{
		this.socket = socket;
		this.clientSocketTable = clientSocketTable;
		database = new DatabaseStuff();
	}
	@Override
	public void run(){
		BufferedReader in;
		PrintWriter out;
		try {
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String inputLine;
			while((inputLine = in.readLine()) == null);
	        System.out.println("Got " + inputLine);
	        int res = logIn(inputLine);
	        if(res == 0){
	        	saveClientSocket();
	        	returnUserInfo();
	        	openListener();
	        	noticeUsersFriends();
	        }
	        else if(res == 2){
	        	out.println("duplicated");
	        }
	        else if(res == 3){
	        	out.println("success");
	        	notifyAllUsers();
	        }
	        else{
	        	out.println("error");
	        }
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return;
	}
	private void notifyAllUsers() throws Exception{
		JSONObject json = new JSONObject();
		json.put("dataType", "newUser");
		json.put("account", account);
		for(Enumeration e = Manager.clientSocketTable.elements(); e.hasMoreElements();)
			new SocketSpeaker((Socket)e.nextElement(), json.toString()).start();
	}
	private void noticeUsersFriends() throws Exception{
		JSONObject json = new JSONObject();
		json.put("dataType", "friendOnline");
		json.put("account", account);
		for(int i = 0; i < frdList.length(); i ++){
			if(Manager.clientSocketTable.containsKey(frdList.get(i))){
				new SocketSpeaker(Manager.clientSocketTable.get(frdList.get(i)), json.toString()).start();
			}
		}
	}
	private int logIn(String data){
		try{
			JSONObject input = new JSONObject(data);
			if(input.getString("action").equals("RESET")){
				for(SocketListener l:Manager.listenerTable){
					l.setIsRun(false);
					l.interrupt();
				}
				Manager.clientSocketTable = new Hashtable<String, Socket>();
				Manager.listenerTable = new HashSet<SocketListener>();
				
				java.lang.management.ThreadMXBean t = ManagementFactory.getThreadMXBean();
	        	System.out.println("Thread count : " + t.getThreadCount());
				return -1;
			}
			if(input.getString("action").equals("signUp")){
				account = input.getString("account");
				ResultSet rs = database.query("select * from account where account = '" + account + "'");
				if(rs.next())
					return 2;
				password = input.getString("password");
				database.update("insert into account (account, password, log_in) values ('" + account + "', '" + password + "', 0)");
				return 3;
			}
			account = input.getString("account");
			password = input.getString("password");
			ResultSet rs = database.query("select * from account where account = '" + account + "' and password = '" + password + "'");
			if(rs.next()){
				id = rs.getString("id");
				System.out.println("Got ID : " + id);
				database.update("update account set log_in = '1' where account = '" + account + "'");
				System.out.println("password is right");
				return 0;
			}
			else{
				System.out.println("Log in error");
				return 1;
			}
		}catch(Exception e){
			return -1;
		}
	}
	private void saveClientSocket(){
		System.out.println("save Socket");
		clientSocketTable.put(account, socket);
	}
	private void openListener(){
		System.out.println("operListener");
		socketListener = new SocketListener(account, id, database);
		socketListener.start();
		Manager.listenerTable.add(socketListener);
	}
	private void returnUserInfo() throws Exception{
		//System.out.println("");
		System.out.println("returnUserInfo");
		JSONObject totalMsg = new JSONObject();
		JSONArray userList = new JSONArray();
		frdList = new JSONArray();
		JSONArray myReqList = new JSONArray();
		JSONArray rcvReqList = new JSONArray();
		JSONArray onlineList = new JSONArray();
		//build userlist
		ResultSet userListRs = database.query("select * from account where account != '" + account + "'");
		while(userListRs.next()){
			userList.put(userListRs.getString("account"));
			if(userListRs.getInt("log_in") == 1)
				onlineList.put(userListRs.getString("account"));
		}
		totalMsg.put("userList", userList);
		totalMsg.put("onlineList", onlineList);
		//build frdList
		ResultSet frdListRs1 = database.query("select * from friendship where account1_id = '" + id + "' and status = '0'");
		while(frdListRs1.next()){
			frdList.put(queryAccountById(frdListRs1.getString("account2_id")));
		}
		ResultSet frdListRs2 = database.query("select * from friendship where account2_id = '" + id + "' and status = 0");
		while(frdListRs2.next()){
			frdList.put(queryAccountById(frdListRs2.getString("account1_id")));
		}
		totalMsg.put("frdList", frdList);
		//build myReqList
		ResultSet myReqListRs = database.query("select * from friendship where account1_id = '" + id + "' and status = 1");
		while(myReqListRs.next()){
			myReqList.put(queryAccountById(myReqListRs.getString("account2_id")));
		}
		totalMsg.put("myReqList", myReqList);
		//build rcvReqListRs
		ResultSet rcvReqListRs = database.query("select * from friendship where account2_id = '" + id + "' and status = 1");
		while(rcvReqListRs.next()){
			rcvReqList.put(queryAccountById(rcvReqListRs.getString("account1_id")));
		}
		totalMsg.put("rcvReqList", rcvReqList);
		//send totalMsg
		PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
		out.println(totalMsg.toString());
		System.out.println(totalMsg.toString());
	}
	private String queryAccountById(String id) throws Exception{
		ResultSet rs = database.query("select * from account where id = '" + id + "'");
		if(rs.next())
			return rs.getString("account");
		else
			throw new Exception("There is ID but no such account");
	}
}
