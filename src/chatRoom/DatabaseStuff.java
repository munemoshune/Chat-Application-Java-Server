package chatRoom;

import java.sql.DriverManager;
import java.sql.ResultSet;


import com.mysql.jdbc.Connection;
public class DatabaseStuff {
	private Connection con = null;
	DatabaseStuff() throws Exception{
		if(con == null){
			Class.forName("com.mysql.jdbc.Driver");
			con = (Connection) DriverManager.getConnection("jdbc:mysql://localhost/account", "root", "root");
		}
	}
	void close() throws Exception{
		con.close();
		con = null;
	}
	ResultSet query(String statement) throws Exception{
		ResultSet rs = con.prepareStatement(statement).executeQuery();
		return rs;
	}
	int update(String statement) throws Exception{
		return con.prepareStatement(statement).executeUpdate();
	}
	
}