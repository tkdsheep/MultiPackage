package domain;

import java.util.List;
import java.util.Map;

public class BugReport implements Comparable<BugReport>{

	private int id;
	private String rawText;
	private String title;
	private String affects;
	private String desc;// description
	private List<String> affectedPackage;

	private List<String> wordList;// original result of tokenizing description (but stopwords removed, stemmed)
	Map<String, Double> tf;// term frequency
	
	public int compareTo(BugReport br){
		if(this.id<br.id)
			return -1;
		else if(this.id>br.id)
			return 1;
		else return 0;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getRawText() {
		return rawText;
	}

	public void setRawText(String rawText) {
		this.rawText = rawText;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAffects() {
		return affects;
	}

	public void setAffects(String affects) {
		this.affects = affects;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public List<String> getAffectedPackage() {
		return affectedPackage;
	}

	public void setAffectedPackage(List<String> affectedPackage) {
		this.affectedPackage = affectedPackage;
	}

	public List<String> getWordList() {
		return wordList;
	}

	public void setWordList(List<String> wordList) {
		this.wordList = wordList;
	}

	public Map<String, Double> getTf() {
		return tf;
	}

	public void setTf(Map<String, Double> tf) {
		this.tf = tf;
	}

	public void printInfo() {
		System.out.println(id);
		System.out.println(title);
		System.out.println(affects);
		System.out.println(desc);
	}

}
