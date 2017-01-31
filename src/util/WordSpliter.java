package util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

public class WordSpliter {
	
	private Set<String> stopwords;
	
	public WordSpliter(String stopwordsPath){
		stopwords = new HashSet<String>();
		List<String> words = FileUtil.readLinesFromFile(stopwordsPath);
		for(String word:words){
			stopwords.add(word);
		}
	}
	
	public WordSpliter(){
		stopwords = new HashSet<String>();//empty stopwords
	}
	
	public List<String> alphabetSplit(String str){
		String s = str.toLowerCase();
		List<String> words = new ArrayList<String>();
		
		for(int i=0;i<s.length();i++){
			if(s.charAt(i)>='a'&&s.charAt(i)<='z'){
				for(int j=i+1;j<s.length();j++){
					if(s.charAt(j)>='a'&&s.charAt(j)<='z')
						continue;
					String w = s.substring(i,j);
					i=j;
					if(w.length()<=2||w.length()>=30)
						break;
					PorterStem stemmer = new PorterStem();
					for(int k=0;k<w.length();k++)
						stemmer.add(w.charAt(k));
					stemmer.stem();
					if(stopwords.contains(w)||stopwords.contains(stemmer.toString()))
						break;
					words.add(stemmer.toString());			
					break;
				}
			}
		}
		
		return words;
		
		
	}
	
	public List<String> split(String str){
		
		/**
		 * input: orginal content of a document
		 * output: a list of words
		 * notice: noisy words are filtered
		 */
		
		//String tmp = heuristicSplit(str);
		String tmp = str;
		List<String> words = new ArrayList<String>();
		StringTokenizer st=new StringTokenizer(tmp);
		
		while(st.hasMoreTokens()){
			String word = st.nextToken();
			List<String> tmpWords = extractWords(word);
			for(String w:tmpWords){
				if(w.length()<=2||w.length()>=30)
					continue;
				PorterStem stemmer = new PorterStem();
				for(int i=0;i<w.length();i++)
					stemmer.add(w.charAt(i));
				stemmer.stem();
				if(stopwords.contains(w)||stopwords.contains(stemmer.toString()))
					continue;
				words.add(stemmer.toString());		
			}
			
			
		}	
		
		return words;
	}
	
	private String filter(String word){
		
		String res = word.toLowerCase();
		
		int i,j;
		for(i=0;i<res.length();i++)
			if(res.charAt(i)>='a'&&res.charAt(i)<='z')
				break;
		res = res.substring(i);
		for(j=res.length()-1;j>=0;j--)
			if(res.charAt(j)>='a'&&res.charAt(j)<='z')
				break;
		res = res.substring(0,j+1);
		
		
		for(i=0;i<res.length();i++){
			if(res.charAt(i)>='a'&&res.charAt(i)<='z')
				return res;
		}
		
		return "";
		
	}
	
	private List<String> extractWords(String str){
		
		List<String> words = new ArrayList<String>();
		str = str.toLowerCase();//don't forget this
		
		//path: /usr/lib/firefox/libxpcom.so
		int cc = 0;
		for(int i=0;i<str.length();i++)
			if(str.charAt(i)=='/')
				cc++;
		if(cc>=2)
			return words;// it is a path, don't split 
		
		if(str.contains("https://"))
			str = str.substring(0,str.indexOf("https://"));
		if(str.contains("http://"))
			str = str.substring(0,str.indexOf("http://"));
		if(str.contains("binary package hint: "))
			str = str.substring(20);
		
		for(int i=0;i<str.length();i++){
			if(str.charAt(i)>='a'&&str.charAt(i)<='z'){
				int j;
				for(j=i+1;j<str.length();j++){
					if(str.charAt(j)>='a'&&str.charAt(j)<='z'
							||str.charAt(j)=='-'||str.charAt(j)=='_')
						continue;	
					break;					
				}
				if(!(str.charAt(j-1)>='a'&&str.charAt(j-1)<='z'))
					j--;
				words.add(str.substring(i,j));
				i=j;
			}
		}
		/*System.out.println(str);
		for(String word:words)
			System.out.print(word+" ");
		System.out.println();*/
		return words;
		
	}
	


}
