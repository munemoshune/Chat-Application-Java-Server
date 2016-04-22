package chatRoom;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

public class FileRWStuff {
	private String fileName;
	private BufferedReader br = null;
	private int readLimit = 10;
	FileRWStuff(String fileName){
		this.fileName = fileName;
	}
	void writeMsg(JSONObject json) throws Exception{
		PrintStream ps = new PrintStream(new FileOutputStream(new File(fileName), true));
		json.put("sentenceCount", lastLine() + 1);
		ps.println(json.toString());
		ps.close();
	}
	int readMsg(JSONArray josnArray) throws Exception{
		String line;
		int lineCount = 0;
		while(lineCount < readLimit){
			if((line = br.readLine()) != null){
				josnArray.put(new JSONObject(line));
				lineCount ++;
			}
			else
				break;
		}
		if(lineCount == 0)
			br.close();
		return lineCount;	
	}
	JSONArray readDialog() throws Exception{
		JSONArray dialog = new JSONArray();
		try{
			br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
		}catch(FileNotFoundException e){
			return null;
		}
		String line;
		while((line = br.readLine()) != null){
			dialog.put(new JSONObject(line));
		}
		return dialog;
	}
	Integer lastLine() throws Exception{
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
		String line;
		ArrayList<JSONObject> jl = new ArrayList<JSONObject>();
		while((line = br.readLine()) != null){
			jl.add(new JSONObject(line));
		}
		br.close();
		if(jl.isEmpty())
			return 0;
		return jl.get(jl.size() - 1).getInt("sentenceCount");	
	}
	
	boolean resetBufferedReader() throws Exception{
		if(br != null)
			br.close();
		try{
			br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
		}catch(FileNotFoundException e){
			System.out.println("filename not found");
			return false;
		}
		return true;
		
	}
}
