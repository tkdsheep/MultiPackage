package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import process.BuildArff;
import util.Algorithm;
import util.FileUtil;
import domain.BugReport;

public class BugReportParser {
	
	public static void main(String args[]) throws Exception{
		
		List<String> list = FileUtil.readLinesFromFile("H:/research/multipackage/raw-data/ubuntu.txt");
		
		
		HashMap<String,Integer> map = new HashMap<String,Integer>();
		List<BugReport> brs = new ArrayList<BugReport>();
		
		List<String> libre = new ArrayList<String>();
		
		
		
		for(String str:list){
			
			BugReport br = parseBugReport(str);
			
			for(String packName:br.getAffectedPackage()){
				
				if(map.containsKey(packName))
					map.put(packName,map.get(packName)+1);
				else map.put(packName, 1);
				
			}
			if(br.getAffectedPackage().isEmpty())
				continue;
			
			brs.add(br);
			//if(brs.size()==10000)
			//	break;
			//br.printInfo();
			
		}
		
		List<String> words = Algorithm.sortMap(map);
		
		
		for(String word:words){
			System.out.println(word.substring(0,word.indexOf("(Ubuntu)"))+"\t"+map.get(word));
		}
		
		
		int num[] = new int[1000];
		double tot=0;
		for(BugReport br:brs){
			num[br.getAffectedPackage().size()]++;
				
		}
		for(int i=0;i<1000;i++)
			if(num[i]>0)
				System.out.println(i+" "+num[i]);
		
		//BuildArff.buildTrainTestData("H:/research/multipackage/raw-data/","ubuntu",brs);
		
	
	}
	
	public static BugReport parseBugReport(String str){
		
		//convert rawdata to bugreport
		
		//System.out.println(str);
		
		BugReport br = new BugReport();
		br.setRawText(str);
		int id = str.indexOf("\"bug_id\"");
		int title = str.indexOf("\"bug_title\"");
		int affects = str.indexOf("\"affects\"");
		int project = str.indexOf("\"project\"");
		int desc = str.indexOf("\"bug_desc\"");
		
		br.setId(new Integer(str.substring(id+10, title-2)));
		br.setTitle(str.substring(title+13,affects-2));
		br.setAffects(str.substring(affects+11,project-2));
		//br.setProject(str.substring(project+11,desc-2));
		br.setDesc(str.substring(desc+12));
		List<String> packs = parsePackageInfo(br.getAffects());
		br.setAffectedPackage(packs);
		
		
		
		return br;
		
	}
	
	public static List<String> parsePackageInfo(String str){
		
		String tmp = "";
		for(int i=0;i<str.length();i++){
			if(str.charAt(i)=='\\'&&str.charAt(i+1)=='"'){
				i++;
				tmp +="\\q";//quote
			}
			else tmp+=str.charAt(i);
		}
	
		//System.out.println(tmp);
		ArrayList<String> list = new ArrayList<String>();
		
		for(int i=0;i<tmp.length();i++){
			String token = "";
			if(tmp.charAt(i)=='['){
				int cc = 1;
				for(int j=i+1;j<tmp.length();j++){
					if(tmp.charAt(j)=='[')
						cc++;
					if(tmp.charAt(j)==']'){
						cc--;
						if(cc==0){
						token=tmp.substring(i,j+1);
						i=j+1;
						break;
						}
					}
						
				}
			}
			
			//System.out.println(token);
			
			Pattern p=Pattern.compile("\"(.*?)\"");
	        Matcher m=p.matcher(token);
	        
	        ArrayList<String> words = new ArrayList<String>();
	        
	        while(m.find()){
	        	String word = m.group();
	        	word = word.substring(1,word.length()-1);
	        	words.add(word);   	
	        }
	        String packName = words.get(1);
	        String status = words.get(2);
	        if(!status.equals("Fix Released"))
	        	continue;
	        
	        //if(packName!=null&&packName.contains("(Ubuntu)")){
	        if(packName!=null){
	        	
	        	if(packName.contains("("))
	        		packName = packName.substring(0,packName.indexOf("("));
	        	packName = packName.replace('-', ' ');
	        	
	        	StringTokenizer st = new StringTokenizer(packName);
	        	String name = "";
	        	while(st.hasMoreElements()){
	        		String word = st.nextToken();
	        		for(int j=0;j<word.length();j++)
	        			if(word.charAt(j)>='a'&&word.charAt(j)<='z'){
	        				name = name+word+" ";
	        				break;
	        			}
	        	}
	        	//packName = name.toLowerCase().trim();
	        	packName = name.trim();
	        	
	        	packName = packName.replace(' ', '-');
	        	if(packName.isEmpty())
	        		continue;
	        	if(!list.contains(packName+"$$$"))
	        	list.add(packName+"$$$");
	        }
	        	
			
			
			
		}
	
        return list;
   
	}

	
	public static String parseDesc(String str){
		//System.out.println(str);
		
		/*
		replace following special encoding char
		\\			\
		\"			"
		\n			new line
		\u0026gt	>
		\u0026nbsp	space
		\u0009		tab
		\u0026amp	&
		\u0026lt	<	
		\u0026#x27	'
		
		
		*/
		
		String res = "";
		
		for(int i=0;i<str.length();i++){
			if(i!=str.length()-1&&str.charAt(i)=='\\'){
				i++;
				if(str.charAt(i)=='n')
					res+='\n';
				else if(str.charAt(i)=='"')
					res+='"';
				else if(str.charAt(i)=='\\')
					res+='\\';
				else if(str.charAt(i)=='u'){
					if(i+4<str.length()&&str.charAt(i+4)=='9'){
						//u0009
						res+=' ';
						i=i+4;
					}
					else if(i+5<str.length()){
						
						if(str.charAt(i+5)=='g')//u0026gt >
							res+='>';
						if(str.charAt(i+5)=='n')//u0026nbsp space
							res+=' ';
						if(str.charAt(i+5)=='a')//u0026amp &
							res+='&';
						if(str.charAt(i+5)=='l')//u0026lt <
							res+='>';
						if(str.charAt(i+5)=='#')//u0026#x27 '
							res+='\'';
						
						for(int j=i+1;j<str.length();j++)
							if(str.charAt(j)==';'){
								i=j;
								break;
							}
					}				
				}
		
			}
			else res+=str.charAt(i);
		}

		return res;	
	}


	public static String specialParse(String str){
		
		
		String res = "";
		for(int i=0;i<str.length();i++){
			if(str.charAt(i)>='a'&&str.charAt(i)<='z')
				res+=str.charAt(i);
			else if(str.charAt(i)>='A'&&str.charAt(i)<='Z')
				res+=str.charAt(i);
			else if(str.charAt(i)>='0'&&str.charAt(i)<='9')
				res+=str.charAt(i);
			else res+=' ';
			
			
		}
		res = res.replace("Upstream", " ");
		
		return res;
	}

	
	public static void removeNoisySentences(List<BugReport> brs){
		
		Set<String> noise = FileUtil.listToHashSet(
				FileUtil.readLinesFromFile("H:/research/BR summary/ubuntu-data/specialNoise.txt"));
		
		
		for(BugReport br : brs){
			
			List<String> lines = new ArrayList<String>();
			String s = br.getDesc();
			int last = 0;
			for(int i=0;i<s.length();i++){
				if(s.charAt(i)=='\n'){
					lines.add(s.substring(last,i));
					last = i+1;
				}
			}
			lines.add(s.substring(last,s.length()));
			
			List<String> tmpLines = new ArrayList<String>();
			tmpLines.addAll(lines);
			for(int i=0;i<tmpLines.size();i++){
				String line = tmpLines.get(i);
				for(String word:noise){
					
					int index = line.indexOf(word);
					if(index!=-1){
						//System.out.println(line+" "+word+" "+index);
						line = line.substring(0,index);
						tmpLines.set(i, line);
					}
				}
			}
			
			
			int[] score = new int[lines.size()];	
			
			for(int i=0;i<tmpLines.size();i++)
				score[i] = checkNoisySentence(tmpLines.get(i));
			
			
			for(int i=0;i<lines.size();i++){
				if(score[i]>0&&score[i]<10){
					int bad = 0;
					for(int j=i+1;j<=i+10&&j<lines.size();j++){
						if(score[j]==0)
							bad++;
					}
					if(bad>=7)
						score[i] = 0;
					
					bad = 0;
					for(int j=i-1;j>=i-10&&j>=0;j--){
						if(score[j]==0)
							bad++;
					}
					if(bad>=7)
						score[i] = 0;
								
				}
			}
			
			
			//print score info
			
			//System.out.println("Bug ID: "+br.getId()+" "+br.getTitle());
			//System.out.println(br.getDesc());
			
			//for(int i=0;i<lines.size();i++)
			//	System.out.println("score: "+score[i]+"\t"+lines.get(i));		
			//System.out.println("--------------------------");
				
			
			String text = "";
			for(int i=0;i<lines.size();i++){
				if(score[i]!=0)
					text = text + lines.get(i) + " ";
			}
			
			br.setDesc(text);
			
		}
		
	}
	
	public static int checkNoisySentence(String str){
		
		StringTokenizer st=new StringTokenizer(str);
		
		int tokenSum = 0;
		int badToken = 0;
		
		List<String> words = new ArrayList<String>();
		
		while(st.hasMoreElements()){
			tokenSum++;
			String token = st.nextToken();

			if(token.length()>1&&!isChar(token.charAt(token.length()-1)))
				token = token.substring(0,token.length()-1);
			boolean isWord = true;
			for(int i=0;i<token.length();i++)
				if(!isChar(token.charAt(i)))
					isWord = false;
			if(isWord)
				words.add(token);
			if(token.length()>30)
				badToken++;
		}
		
		if(words.size()>=10)
			return words.size();
		if(words.size()*1.0/tokenSum>=0.7&&words.size()>=3)
			return words.size();
		if(words.size()<=3)
			return 0;
		if(badToken*1.0/tokenSum>=0.5)
			return 0;
		
		return 0;
		
	}
	
	public static boolean isChar(char ch){
		if(ch>='a'&&ch<='z')
			return true;
		if(ch>='A'&&ch<='Z')
			return true;
		return false;
	}

	
}
