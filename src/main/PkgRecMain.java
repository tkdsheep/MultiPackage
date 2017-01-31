package main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import process.BuildArff;
import model.BugReportParser;
import model.Prediction;
import domain.Arff;
import domain.BugReport;
import domain.Package;
import util.Algorithm;
import util.FileUtil;

public class PkgRecMain {
	
	public static void main(String args[]) throws Exception{
		
		String project = "ubuntu";
		
		List<String> list = FileUtil.readLinesFromFile("data/"+project);
		List<BugReport> brs = new ArrayList<BugReport>();
		
		
		
		for(String str:list){
			
			BugReport br = BugReportParser.parseBugReport(str);		
			if(br.getAffectedPackage().isEmpty())
				continue;
			
			brs.add(br);
				
		}
		
		
		Collections.sort(brs);//sort bug reports according to ID
		System.out.println(brs.size()+" "+brs.get(0).getId()+" "+brs.get(brs.size()-1).getId());
		
		Arff arff = BuildArff.buildArff("data/", project, brs);
		BuildArff.buildGroundTruth(arff);
		BuildArff.printStatistics(arff);
		
		double ratios[] = new double[10];
		Arrays.fill(ratios, 1.0/ratios.length);
		List<Arff> folds = BuildArff.stratifyArff(arff, ratios);
		
		int foldIndex = 1;
		Arff trainArff,testArff;
		trainArff = folds.get(0);
		
		double recall5,recall10;
		recall5 = recall10 = 0;
		
		while(foldIndex<folds.size()){
			
			System.out.println("Now test fold "+foldIndex);
			testArff = folds.get(foldIndex);
			
			/*Set<String> set1 = getTrulyLabels(trainArff);
			Set<String> set2 = getTrulyLabels(testArff);
			Set<String> intersect = Algorithm.intersect(set1, set2);
			
			System.out.println(set1.size()+" "+set2.size()+" "+intersect.size());*/
			
			
			
			Prediction predict = new Prediction();
			
			
			predict.runMulan(trainArff, testArff,10,10);//ensembleParam and MLkNNParam
			predict.evaluate(testArff, 5);
			predict.evaluate(testArff, 10);
			
			
			predict.runPackNameMatch(testArff);
			recall5+=predict.evaluate(testArff, 5);
			recall10+=predict.evaluate(testArff, 10);
			
			
			
			//this fold test ended
			trainArff = BuildArff.combineArff(trainArff, testArff);
			foldIndex++;
			
			System.out.println("\n---------\n");
		}
		
		
		
		System.out.println("\nAverage Recall: "+recall5/9+" "+recall10/9);
		
	
		
		
	}
	
	public static Set<String> getTrulyLabels(Arff arff){
		
		Set<String> set = new HashSet<String>();
		for(Package pack:arff.getPacks()){
			if(pack.getBrs().size()<arff.getBrs().size()*0.01)
				continue;
			set.add(pack.getName());
				
		}
		return set;
		
	}

}
