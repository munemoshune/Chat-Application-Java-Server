package chatRoom;

import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;

public class PackageHandler extends Thread{
	String data;
	private DatabaseStuff database;
	PackageHandler(String data, DatabaseStuff database){
		this.data = data;
		this.database = database;
	}
	@Override
	public void run(){
		try {
			JSONObject json = new JSONObject(data);
			switch(json.getString("action")){
				case "logOut":
					database.update("update account set log_in = '0' where account = '" + json.getString("account") + "'");
					break;
				case "sendMsg":
					System.out.println("sendMsg");
					new SocketSpeaker(Manager.clientSocketTable.get(json.getString("account")), 
							json.getString("account") + " sends " +  json.getString("Msg") + " to " 
									+ json.getString("target")).start();
					JSONObject response = new JSONObject();
					response.put("dataType", "timeStamp");
					response.put("time", sendMsg(json));
					response.put("Msg#", json.getInt("sentenceCount"));
					new SocketSpeaker(Manager.clientSocketTable.get(json.getString("account")), response.toString()).start();
					if(Manager.clientSocketTable.containsKey(json.getString("target"))){
						System.out.println("Msg send to " + json.getString("target"));
						response.put("dataType", "newMsg");
						response.put("Msg", json.getString("Msg"));
						response.put("account", json.getString("account"));
						new SocketSpeaker(Manager.clientSocketTable.get(json.getString("target")), response.toString()).start();
					}
					break;
				case "sendReq":
					sendReq(json);
					json.put("dataType", "sendReqNotice");
					sendReqNotice(json);
					System.out.println("sendReq");
					break;
				case "approveReq":
					approveReq(json);
					json.put("dataType", "reqApprovedNotice");
					sendReqNotice(json);
					System.out.println("approveReq");
					break;
				case "removeMyReq":
					removeMyReq(json);
					json.put("dataType", "removeMyReqNotice");
					sendReqNotice(json);
					System.out.println("removeMyReq");
					break;
				case "removeRcvReq":
					removeRcvReq(json);
					json.put("dataType", "removeRcvReqNotice");
					sendReqNotice(json);
					System.out.println("removeRcvReq");
					break;
				case "removeFrd":
					removeFrd(json);
					json.put("dataType", "removeFriendNotice");
					sendReqNotice(json);
					System.out.println("removeFrd");
					break;
				case "requestDialogs":
					sendDialogs(json);
					break;
				default:
					System.out.println("default");
					break;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return;
	}
	void sendReqNotice(JSONObject json)throws Exception{
		json.remove("action");
		if(Manager.clientSocketTable.containsKey(json.get("target")))
			new SocketSpeaker(Manager.clientSocketTable.get(json.get("target")), json.toString()).start();
	}
	void sendDialogs(JSONObject json)throws Exception{
		String fileName, account, target, id1, id2;
		account = json.getString("account");
		id1 = accountToId(account);
		ResultSet rs = database.query("select * from conversation where acut_id1 = '" + id1 + "'");
		while(rs.next()){
			//System.out.println(rs.getString("acut_id2"));
			fileName = rs.getString("conv_id") + ".txt";
			JSONObject jsonObject = new JSONObject();
			JSONArray jsonArray = new JSONArray();
			jsonArray = new FileRWStuff(fileName).readDialog();
			jsonObject.put("dataType", "dialog");
			jsonObject.put("target", idToAccount(rs.getString("acut_id2")));
			if(jsonArray != null)
				jsonObject.put("data", jsonArray);
			else
				jsonObject.put("data", "empty");
			new SocketSpeaker(Manager.clientSocketTable.get(account), jsonObject.toString()).start();
		}
		rs = database.query("select * from conversation where acut_id2 = '" + id1 + "'");
		while(rs.next()){
			//System.out.println(rs.getString("acut_id1"));
			fileName = rs.getString("conv_id") + ".txt";
			JSONObject jsonObject = new JSONObject();
			JSONArray jsonArray = new JSONArray();
			jsonArray = new FileRWStuff(fileName).readDialog();
			jsonObject.put("dataType", "dialog");
			jsonObject.put("target", idToAccount(rs.getString("acut_id1")));
			if(jsonArray != null)
				jsonObject.put("data", jsonArray);
			else
				jsonObject.put("data", "empty");
			new SocketSpeaker(Manager.clientSocketTable.get(account), jsonObject.toString()).start();
		}
	}
	String idToAccount(String id) throws Exception{
		ResultSet rs = database.query("select * from account where id = '" + id + "'");
		if(rs.next())
			return rs.getString("account");
		else
			throw new Exception("No such id");
	}
	void removeFrd(JSONObject json)throws Exception{
		String id1, id2;
		id1 = accountToId(json.getString("account"));
		id2 = accountToId(json.getString("target"));
		database.update("delete from friendship where account1_id = '" + id1 + "' and account2_id = '" + id2 + "'");
		database.update("delete from friendship where account1_id = '" + id2 + "' and account2_id = '" + id1 + "'");
	}
	void removeRcvReq(JSONObject json)throws Exception{
		String id1, id2;
		id1 = accountToId(json.getString("account"));
		id2 = accountToId(json.getString("target"));
		database.update("delete from friendship where account1_id = '" + id2 + "' and account2_id = '" + id1 + "'");
	}
	void removeMyReq(JSONObject json)throws Exception{
		String id1, id2;
		id1 = accountToId(json.getString("account"));
		id2 = accountToId(json.getString("target"));
		database.update("delete from friendship where account1_id = '" + id1 + "' and account2_id = '" + id2 + "'");
	}
	void approveReq(JSONObject json)throws Exception{
		String id1, id2;
		id1 = accountToId(json.getString("account"));
		id2 = accountToId(json.getString("target"));
		System.out.println("update friendship set status = 0 where account1_id = '" + id2 + "' and account2_id = '" + id1 + "'");
		database.update("update friendship set status = 0 where account1_id = '" + id2 + "' and account2_id = '" + id1 + "'");
	}
	void sendReq(JSONObject json)throws Exception{
		String id1, id2;
		id1 = accountToId(json.getString("account"));
		id2 = accountToId(json.getString("target"));
		ResultSet rs = database.query("select * from friendship where account1_id = '" + id1 + "' and account2_id = '" + id2 + "'");
		if(rs.next())
			return;
		else{
			rs = database.query("select * from friendship where account1_id = '" + id2 + "' and account2_id = '" + id1 + "'");
			if(rs.next())
				return;
		}
		database.update("insert into friendship (account1_id, account2_id, status) values ('" + id1 + "', '" + id2 + "', 1)");
	}
	/*void sendDialog(JSONObject json) throws Exception{
		String fileName, account;
		account = json.getString("account");
		fileName = dialogFilename(json);
		FileRWStuff fileRWStuff = new FileRWStuff(fileName);
		JSONArray jsonArray = new JSONArray();
		if(fileRWStuff.resetBufferedReader())
			while(fileRWStuff.readMsg(jsonArray) > 0){
				JSONObject jsonPackage = new JSONObject();
				jsonPackage.put("dataType", "msgs");
				jsonPackage.put("data", jsonArray);
				SocketSpeaker ss = new SocketSpeaker(Manager.clientSocketTable.get(account), jsonPackage.toString());
				ss.start();
				ss.join();
				jsonArray = new JSONArray();
			}
		else{
			JSONObject jsonPackage = new JSONObject();
			System.out.println("msgsNotFound");
			jsonPackage.put("dataType", "msgsNotFound");
			new SocketSpeaker(Manager.clientSocketTable.get(account), jsonPackage.toString()).start();
		}
	}*/
	String sendMsg(JSONObject json) throws Exception{
		SimpleDateFormat sdFormat = new SimpleDateFormat("HH:mm:ss");
		Date date = new Date();
		String strDate = sdFormat.format(date);
		json.put("time", strDate);
		writeMsgToServer(json);
		sendMsgToTarget(json);
		return strDate;
		//sendMsgToChatMembers(ChatMembers());
	}
	void writeMsgToServer(JSONObject json) throws Exception{
		String fileName, target, action;
		fileName = dialogFilename(json);
		FileRWStuff fileRWStuff = new FileRWStuff(fileName);
		target = json.getString("target");
		json.remove("target");
		action = json.getString("action");
		json.remove("action");
		fileRWStuff.writeMsg(json);
		json.put("target", target);
		json.put("action", action);
	}
	String dialogFilename(JSONObject json) throws Exception{
		String id1, id2, fileName;
		//find chat filename by account id
		id1 = accountToId(json.getString("account"));
		id2 = accountToId(json.getString("target"));
		ResultSet rs = database.query("select * from conversation where acut_id1 = '" + id1 + "' and acut_id2 = '" + id2 + "'");
		if(rs.next()){
			fileName = rs.getString("conv_id");
		}
		else{
			rs = database.query("select * from conversation where acut_id1 = '" + id2 + "' and acut_id2 = '" + id1 + "'");
			if(rs.next())
				fileName = rs.getString("conv_id");
			else{
				database.update("insert into conversation (acut_id1, acut_id2) values ('" + id1 + "', '" + id2 + "')");
				rs = database.query("select * from conversation where acut_id1 = '" + id1 + "' and acut_id2 = '" + id2 + "'");
				if(rs.next()){
					fileName = rs.getString("conv_id");
				}
				else
					throw new Exception("Insert failed");
			}
		}
		fileName += ".txt";
		return fileName;
	}
	String accountToId(String account) throws Exception{
		ResultSet rs = database.query("select * from account where account = '" + account + "'");
		if(rs.next()){
			return rs.getString("id");
		}
		else
			throw new Exception("No such account");
	}
	void sendMsgToTarget(JSONObject json){
		
	}
}
