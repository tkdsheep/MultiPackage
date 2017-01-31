package util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class FileUtil {
	
	public static List<String> readLinesFromFile(String filePath){
		
		BufferedReader reader = null;
		List<String> lines = new ArrayList<String>();
		try {
			reader = new BufferedReader(new FileReader(new File(filePath)));
			
			String line = null;
			while ((line = reader.readLine()) != null) {
				lines.add(line);
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return lines;

	}
	
	public static void writeLinesToFile(List<String> lines,String filePath){
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(new File(filePath)));
			for(String line:lines){
				writer.write(line);
				writer.newLine();
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public static Set<String> listToHashSet(List<String> list){
		
		Set<String> set = new HashSet<String>();
		for(String str : list)
			set.add(str);
		return set;
		
	}
	
	


}
