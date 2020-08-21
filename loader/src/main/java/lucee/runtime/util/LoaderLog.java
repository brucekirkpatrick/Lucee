package lucee.runtime.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class LoaderLog {
	static FileWriter fileWriter=null;
	public static final synchronized void log(String ... s){
		for(int i=0;i<s.length;i++) {
			writeToLog(s[i]);
		}
//		try {
//			fileWriter.close();
//		}catch(IOException e){
//			e.printStackTrace();
//		}
	};
	public static final synchronized void log(Object ... s) {
		for(int i=0;i<s.length;i++) {
			writeToLog(String.valueOf(s[i]));
		}
//		try {
//			fileWriter.close();
//		}catch(IOException e){
//			e.printStackTrace();
//		}

	};
	private static final synchronized void writeToLog(String data){
		try {
			String p="/var/jetendo-server/jetendo/sites/luceeloaderlog.txt";
			if(fileWriter==null) {
				File f1 = new File(p);
				if (!f1.exists()) {
					System.out.println("File didn't exist");
					f1.createNewFile();
				}
				fileWriter = new FileWriter(p, false);
			}
			fileWriter.write(data+"\n");
			fileWriter.flush();

		} catch(IOException e){
			e.printStackTrace();
		}
	}

}

