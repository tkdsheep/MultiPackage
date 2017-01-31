package domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Arff {
	
	public static class Pair{
		private int index;
		private double value;
		public int getIndex() {
			return index;
		}
		public void setIndex(int index) {
			this.index = index;
		}
		public double getValue() {
			return value;
		}
		public void setValue(double value) {
			this.value = value;
		}
	
	}
	
	private String relation;//arff name
	private String outputPath;//notice that this path does NOT include output file name
	private List<String> attributes;
	private List<String> labels;
	private List<List<Pair>> data;//instances data
	private List<BugReport> brs;//original bug report data
	
	//these 3 fields will be built in Arff.buildTrainTestData
	private List<Package> packs;//package data
	private boolean[][] groundTruth;//ground truth labels for each instance (bug report)
	private double[][] confidences;//predicted confidence score for each instance

	
	
	public Arff(String relation) {
		this.relation = relation;
		this.attributes = new ArrayList<String>();
		this.labels = new ArrayList<String>();
		this.data = new ArrayList<List<Pair>>();
		this.brs = new ArrayList<BugReport>();
	}

	public String getRelation() {
		return relation;
	}

	public void setRelation(String relation) {
		this.relation = relation;
	}

	public String getOutputPath() {
		return outputPath;
	}

	public void setOutputPath(String outputPath) {
		this.outputPath = outputPath;
	}

	public List<String> getAttributes() {
		return attributes;
	}

	public void setAttributes(List<String> attributes) {
		this.attributes = attributes;
	}

	public List<String> getLabels() {
		return labels;
	}

	public void setLabels(List<String> labels) {
		this.labels = labels;
	}

	public List<List<Pair>> getData() {
		return data;
	}

	public void setData(List<List<Pair>> data) {
		this.data = data;
	}

	public List<BugReport> getBrs() {
		return brs;
	}

	public void setBrs(List<BugReport> brs) {
		this.brs = brs;
	}

	public List<Package> getPacks() {
		return packs;
	}

	public void setPacks(List<Package> packs) {
		this.packs = packs;
	}

	public boolean[][] getGroundTruth() {
		return groundTruth;
	}

	public void setGroundTruth(boolean[][] groundTruth) {
		this.groundTruth = groundTruth;
	}

	public double[][] getConfidences() {
		return confidences;
	}

	public void setConfidences(double[][] confidences) {
		this.confidences = confidences;
	}


}
