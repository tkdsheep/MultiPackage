package process;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import util.FileUtil;
import util.WordSpliter;
import domain.Arff;
import domain.Arff.Pair;
import domain.BugReport;
import domain.Package;

public class BuildArff {
	
	public static Arff buildArff(String outputPath, String fileName, List<BugReport> brs){
		
		Arff arff = new Arff(fileName);	
		arff.setOutputPath(outputPath);
		
		//build term frequency for each bug report
		WordSpliter spliter = new WordSpliter("lib/stopwords.txt");
		for(BugReport br : brs){
			br.setWordList(spliter.alphabetSplit(br.getTitle()+" "+br.getDesc()));
			Map<String,Double> tf = new HashMap<String,Double>();
			for(String word:br.getWordList()){
				if(tf.containsKey(word))
					tf.put(word, tf.get(word)+1);
				else tf.put(word, 1.0);
			}	
			for(Map.Entry<String,Double> entry:tf.entrySet())
				entry.setValue(entry.getValue()/br.getWordList().size());//normalization tf
			
			br.setTf(tf);
		}
		
		//build document frequency and attributes
		//notice that we remove noisy words that have df less than a threshold
		Map<String,Double> df = new HashMap<String,Double>();
		List<String> attributes = new ArrayList<String>();
		for(BugReport br : brs){
			for(Map.Entry<String, Double> entry : br.getTf().entrySet()){
				String word = entry.getKey();
				if(df.containsKey(word))
					df.put(word, df.get(word)+1);
				else
					df.put(word, 1.0);		
			}
		}
		for(Map.Entry<String, Double> entry:df.entrySet())//remove noisy words
			if(entry.getValue()>0.001*brs.size())
				attributes.add(entry.getKey());
		
		Collections.sort(attributes);
		arff.setAttributes(attributes);
		
		//build all labels
		//notice that we remove "noisy" labels that are affected by less than 10 bug reports
		Map<String,Integer> labelSet = new HashMap<String,Integer>();
		for(BugReport br: brs)
			for(String packName: br.getAffectedPackage()){
				if(labelSet.containsKey(packName))
					labelSet.put(packName, labelSet.get(packName)+1);
				else labelSet.put(packName, 1);
			}
		List<String> labels = new ArrayList<String>();
		for(Map.Entry<String, Integer> entry:labelSet.entrySet()){
			if(entry.getValue()>=brs.size()*0.001)
				labels.add(entry.getKey());
		}
		Collections.sort(labels);//sort the labels in alphabetic order
		arff.setLabels(labels);
		
		
		//build instance data (attribute and label value)
		List<List<Pair>> data = new ArrayList<List<Pair>>();
		arff.setBrs(new ArrayList<BugReport>());
		
		for(int brIndex = 0; brIndex<brs.size(); brIndex++){
			
			BugReport br = brs.get(brIndex);
			List<Pair> pairs = new ArrayList<Pair>();
			
			//check labels first, because some bug reports may only affect noisy labels
			boolean flag = false;
			for(String label:br.getAffectedPackage()){
				if(labelSet.get(label)>=brs.size()*0.001)
					flag = true;				
			}
			if(!flag)
				continue;
			
			arff.getBrs().add(br);//this bug report is added as an instance into the arff
			
			//build attributes value for this instance (bug report)
			for(int i=0;i<attributes.size();i++){
				
				String word = attributes.get(i);
				if(!br.getTf().containsKey(word))
					continue;
				
				Pair pair = new Pair();
				pair.setIndex(i);
				
				double weight = br.getTf().get(word) * Math.log(brs.size()/df.get(word));
				pair.setValue(weight);
				
				pairs.add(pair);
	
			}		
			
			//build label value for this instance
			for(int i=0;i<labels.size();i++){
				
				if(br.getAffectedPackage().contains(labels.get(i))){
					Pair pair = new Pair();
					pair.setIndex(i+attributes.size());
					pair.setValue(1);
					pairs.add(pair);
				}
			
				
			}
			
			
			data.add(pairs);
		}
		

		arff.setData(data);
		
			
		return arff;
	}
	
	
	public static void printStatistics(Arff arff){
		
		System.out.println("#Reports: "+arff.getBrs().size());
		System.out.println("#Package: "+arff.getLabels().size());
		System.out.println("#Terms: "+arff.getAttributes().size());
		
		double tot = 0;
		for(Package pack:arff.getPacks()){
			tot+=pack.getBrs().size();
		}
		
		System.out.println("Avg.Affected: "+tot/arff.getBrs().size());
		
	}
	
	
	public static void buildGroundTruth(Arff arff){
		
		arff.setPacks(new ArrayList<Package>());
		arff.setGroundTruth(new boolean[arff.getBrs().size()][]);
		arff.setConfidences(new double[arff.getBrs().size()][]);
		
		//first we build index for packages(labels)
		Map<String, Package> packMap = new HashMap<String,Package>();
		for(String label:arff.getLabels()){
			Package pack = new Package(label);
			arff.getPacks().add(pack);
			packMap.put(label, pack);
		}
		
		for(int brIndex = 0; brIndex<arff.getBrs().size(); brIndex++){
			
			BugReport br = arff.getBrs().get(brIndex);
			
			arff.getGroundTruth()[brIndex] = new boolean[arff.getLabels().size()];
			arff.getConfidences()[brIndex] = new double[arff.getLabels().size()];//initial confidence = 0
			
			for(int i=0;i<arff.getLabels().size();i++){
				
				if(br.getAffectedPackage().contains(arff.getLabels().get(i))){		
					//update package info
					Package pack = packMap.get(arff.getLabels().get(i));
					pack.getBrs().add(br);//add this br to this affected package
					//record ground truth
					arff.getGroundTruth()[brIndex][i]=true;		
					
				}
				else arff.getGroundTruth()[brIndex][i]=false;				
			}	
		}
		
	}
	
	public static List<Arff> stratifyArff(Arff arff, double[] ratios){
		
		List<Arff> folds = new ArrayList<Arff>();
		
		int dataIndex = 0;
		
		for(double ratio:ratios){
			
			Arff fold = new Arff(arff.getRelation());
			fold.setAttributes(arff.getAttributes());
			fold.setLabels(arff.getLabels());
			fold.setOutputPath(arff.getOutputPath());
	
			
			for(;dataIndex<arff.getBrs().size();dataIndex++){
				if(fold.getBrs().size()>=arff.getBrs().size()*ratio)
					break;
				fold.getBrs().add(arff.getBrs().get(dataIndex));
				fold.getData().add(arff.getData().get(dataIndex));
				
			}
		
			buildGroundTruth(fold);
			folds.add(fold);
		
		}
		
		
		return folds;
		
		
	}
	
	public static Arff combineArff(Arff a, Arff b){
		
		for(BugReport br:b.getBrs())
			a.getBrs().add(br);
		for(List<Pair> pairs:b.getData())
			a.getData().add(pairs);
		buildGroundTruth(a);
		
		
		return a;
		
		
	}
	
	public static void outputArff(String outputPath, Arff arff){
		
		//notice that outputpath include the file name
		
		
		List<String> output = new ArrayList<String>();
		
		output.add("@relation "+arff.getRelation());
		output.add("");
		for(String att:arff.getAttributes())
			output.add("@attribute "+att+" numeric");
		
		for(String label:arff.getLabels())
			output.add("@attribute "+label+" {0,1}");
		
		output.add("");
		output.add("@data");
		
		for(int dataIndex = 0;dataIndex<arff.getData().size();dataIndex++){
			
			List<Pair> pairs = arff.getData().get(dataIndex);
			String line = "{";
			for(Pair pair:pairs){
				if(line.length()!=1)
					line = line+",";
				String tmp = new Integer(pair.getIndex()).toString() +" ";
				if(pair.getIndex()<arff.getAttributes().size())
					tmp = tmp + new Double(pair.getValue()).toString();
				else tmp = tmp + new Integer((int)pair.getValue()).toString();
				line = line + tmp;
			}
			output.add(line+"}");
		}	
		FileUtil.writeLinesToFile(output, outputPath);
		
	}
	
	public static void outputXML(String outputPath, Arff arff){
		
		//notice that outputpath include the file name
		
		List<String> output = new ArrayList<String>();
		output.add("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		output.add("<labels xmlns=\"http://mulan.sourceforge.net/labels\">");
		for(String label:arff.getLabels())
			output.add("<label name=\""+label+"\"></label>");
		output.add("</labels>");
		
		FileUtil.writeLinesToFile(output, outputPath);
	}
	
	

}
