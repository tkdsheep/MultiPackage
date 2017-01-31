package domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Package implements Comparable<Package>{
	
	private String name;
	private List<BugReport> brs;//bug report that affects this package
	
	private Map<String,Double> edge;
	private Map<String,Double> tf;
	private Map<String,Double> keywords;
	
	public Package(String name){
		this.name = name;
		this.brs = new ArrayList<BugReport>();
		this.edge = new HashMap<String,Double>();	
		this.tf = new HashMap<String,Double>();
		this.keywords = new HashMap<String,Double>();
	}
	
	public int compareTo(Package pack){
		if(this.brs.size()<pack.getBrs().size())
			return -1;
		else if(this.brs.size()>pack.getBrs().size())
			return 1;
		else return 0;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<BugReport> getBrs() {
		return brs;
	}

	public void setBrs(List<BugReport> brs) {
		this.brs = brs;
	}

	public Map<String, Double> getEdge() {
		return edge;
	}

	public void setEdge(Map<String, Double> edge) {
		this.edge = edge;
	}

	public Map<String, Double> getTf() {
		return tf;
	}

	public void setTf(Map<String, Double> tf) {
		this.tf = tf;
	}

	public Map<String, Double> getKeywords() {
		return keywords;
	}

	public void setKeywords(Map<String, Double> keywords) {
		this.keywords = keywords;
	}

	
	
	
	
	
	
	

}
