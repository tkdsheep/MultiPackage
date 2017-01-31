package model;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import process.BuildArff;
import domain.Arff;
import domain.Arff.Pair;
import domain.BugReport;
import domain.Package;
import util.Algorithm;
import weka.attributeSelection.InfoGainAttributeEval;
import weka.attributeSelection.Ranker;
import weka.classifiers.bayes.*;
import weka.classifiers.lazy.*;
import weka.core.DistanceFunction;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.supervised.attribute.AttributeSelection;
import mulan.classifier.MultiLabelLearner;
import mulan.classifier.MultiLabelOutput;
import mulan.classifier.lazy.*;
import mulan.classifier.transformation.*;
import mulan.data.IterativeStratification;
import mulan.data.MultiLabelInstances;
import mulan.dimensionalityReduction.MultiClassAttributeEvaluator;

public class Prediction {
	
	
	public void runPackNameMatch(Arff arff){
		
		System.out.println("Run PackNameMatch prediction. No need trainSet. Now read testSet");

		double tp,tot;
		tot = tp = 0;
		
		for(int i = 0;i<arff.getData().size();i++){
			
			BugReport br = arff.getBrs().get(i);
			String text = br.getTitle() + " " + br.getDesc();
			
		
			for(int j=0;j<arff.getLabels().size();j++){
				String label = arff.getLabels().get(j);
				label = label.substring(0,label.indexOf("$$$"));
				if(label.contains("-("))
					label = label.substring(0,label.indexOf("-("));
				
				label = label.toLowerCase();
				
				label.replaceAll("-", " ");
				StringTokenizer st=new StringTokenizer(label);
				List<String> words = new ArrayList<String>(); 
				while(st.hasMoreElements()){
					String word = st.nextToken();
					if(!word.equals(arff.getRelation()))
						words.add(word);
				}
					
		
				double score = 0;
				for(String word:words){
					if(text.contains(word))
						score+=1.0/words.size();
				}		
				
				
				arff.getConfidences()[i][j] += score;
				if(score>0){
					tot++;
					if(arff.getGroundTruth()[i][j])
						tp++;
				}
				
				//if(score!=0)
					//System.out.print(arff.getLabels().get(j)+" ");
				
			}
			
			
			
		}
		
		System.out.println("precision: "+tp/tot);
	
		
		
	}
	
	
	public void runMulan (Arff trainArff, Arff testArff, int EnsembleParam, int MLkNNParam) throws Exception{
		
		//MultiLabelInstances trainSet, MultiLabelLearner model, Instances testSet, int topK
		
		System.out.println("Run Mulan prediction, Now read trainSet and testSet");
		
		String trainPath = trainArff.getOutputPath()+trainArff.getRelation()+"TrainSet";
		String testPath = testArff.getOutputPath()+testArff.getRelation()+"TestSet";
		String xmlPath = trainArff.getOutputPath()+trainArff.getRelation()+".xml";
		
		BuildArff.outputXML(xmlPath, trainArff);//xml
		BuildArff.outputArff(trainPath, trainArff);//trainSet
		BuildArff.outputArff(testPath, testArff);//testSet
		
		MultiLabelInstances trainSet = new MultiLabelInstances(trainPath, xmlPath);

		FileReader reader = new FileReader(testPath);
        Instances testSet = new Instances(reader);	
        
        System.out.println("trainSet size: "+trainSet.getNumInstances());
		System.out.println("testSet size: "+testSet.numInstances());
		
		
		
		//double ratios[] = new double[Math.max(1, trainArff.getBrs().size()/testArff.getBrs().size())];
		double ratios[] = new double[EnsembleParam];
		//double ratios[] = new double[1];
		for(int i=0;i<ratios.length;i++) ratios[i]=1.0/ratios.length;
		
		double buildTime[] = new double[ratios.length];
		double testTime[] = new double[ratios.length];
		
		List<Arff> folds = BuildArff.stratifyArff(trainArff, ratios);
        
        
		System.out.println("build mockup test set");
		double startTime = System.currentTimeMillis();
		
        Arff mockup = new Arff(trainArff.getRelation());
        mockup.setOutputPath(trainArff.getOutputPath());
        mockup.setAttributes(trainArff.getAttributes());
        mockup.setLabels(trainArff.getLabels());
        
        for(int i=0;i<testArff.getData().size();i++){
        	List<Pair> testPairs = testArff.getData().get(i);
        	
        	double maxSim = -1;
        	int index = -1;
        	
        	for(int j=0;j<trainArff.getData().size();j++){
        		List<Pair> trainPairs = trainArff.getData().get(j);
        		double sim = CosineMeasure.cosineSim(trainArff.getAttributes(), trainPairs, testPairs);
        		if(sim>maxSim){
        			maxSim = sim;
        			index = j;//the index of nearest bug report in training data
        		}
        		
        	}
        	
        	mockup.getData().add(trainArff.getData().get(index));
        	mockup.getBrs().add(trainArff.getBrs().get(index));
  	
        }
        
        BuildArff.buildGroundTruth(mockup);
        String mockupPath = mockup.getOutputPath()+mockup.getRelation()+"MockUp";
        BuildArff.outputArff(mockupPath, mockup);
        reader = new FileReader(mockupPath);
        Instances mockSet = new Instances(reader);	
        
        System.out.println("run mockup test");
        double score[] = new double[folds.size()];
        Arrays.fill(score, 1.0);
        
        
        for(int foldIndex=0;foldIndex<folds.size();foldIndex++){
			Arff fold = folds.get(foldIndex);
			String tmpPath = trainArff.getOutputPath()+trainArff.getRelation()+"tmpFold";
			BuildArff.outputArff(tmpPath, fold);
			MultiLabelInstances subSet = new MultiLabelInstances(tmpPath, xmlPath);
			MLkNN model = new MLkNN(MLkNNParam,1);
			
			model.build(subSet);
			
			for (int instanceIndex = 0; instanceIndex < mockSet.numInstances(); instanceIndex++) {
				
				Instance instance = mockSet.instance(instanceIndex);
				
				MultiLabelOutput output = model.makePrediction(instance);
				
				for(int j=0;j<mockup.getLabels().size();j++){
					mockup.getConfidences()[instanceIndex][j] = output.getConfidences()[j];
				}
				
			}
			
			Prediction predict = new Prediction();
			score[foldIndex] = predict.evaluate(mockup, 5);
			
			for (int instanceIndex = 0; instanceIndex < testSet.numInstances(); instanceIndex++) {
				
				Instance instance = testSet.instance(instanceIndex);
				
				MultiLabelOutput output = model.makePrediction(instance);
				
				for(int j=0;j<testArff.getLabels().size();j++){
					testArff.getConfidences()[instanceIndex][j] = output.getConfidences()[j];
					
				}
				
			}
			
			//predict = new Prediction();
			//predict.evaluate(testArff, 5);
			//System.out.println();
			
			
		}
        
     
        	
        //buildTime[0]+=System.currentTimeMillis()-startTime;	
        
        System.out.println("mockup test ended, now predict real test set");
			
		
		
		double[][] totConfidences = new double[testSet.numInstances()][];
		for(int i=0;i<testSet.numInstances();i++)
			totConfidences[i] = new double[testArff.getLabels().size()];
		
		System.out.println("number of sub-classifiers: "+folds.size());
		
		for(int foldIndex=0;foldIndex<folds.size();foldIndex++){
			//if(score[foldIndex]<0.2)
				//continue;
			Arff fold = folds.get(foldIndex);
			String tmpPath = trainArff.getOutputPath()+trainArff.getRelation()+"tmpFold";
			BuildArff.outputArff(tmpPath, fold);
			MultiLabelInstances subSet = new MultiLabelInstances(tmpPath, xmlPath);
			MLkNN model = new MLkNN(MLkNNParam,1);
			
			startTime = System.currentTimeMillis();
			model.build(subSet);
			buildTime[foldIndex]+=System.currentTimeMillis()-startTime;
			
			double beta = score[foldIndex];
			if(beta==0)
				continue;
			
			startTime = System.currentTimeMillis();
			for (int instanceIndex = 0; instanceIndex < testSet.numInstances(); instanceIndex++) {
				
				Instance instance = testSet.instance(instanceIndex);
				
				MultiLabelOutput output = model.makePrediction(instance);
				
				for(int j=0;j<testArff.getLabels().size();j++){
					testArff.getConfidences()[instanceIndex][j] = beta * output.getConfidences()[j];
					totConfidences[instanceIndex][j] += beta * output.getConfidences()[j];
				}
				
			}
			testTime[foldIndex]+=System.currentTimeMillis()-startTime;
			
			/*Prediction predict = new Prediction();
			predict.evaluate(testArff, 5);
			predict.evaluate(testArff, 10);
			System.out.println();*/
			
			
			
		}
		
		testArff.setConfidences(totConfidences);
		for (int instanceIndex = 0; instanceIndex < testSet.numInstances(); instanceIndex++) {
			for(int j=0;j<testArff.getLabels().size();j++){
				testArff.getConfidences()[instanceIndex][j]/=folds.size();
			}
		}
		
		
		double build,test;
		build = test = 0;
		for(double t:buildTime)
			build+=t;
		for(double t:testTime)
			test+=t;
		System.out.println("Build time cost: "+build/1000.0+", Test time cost: "+test/1000.0);
		
	
		
	}
	
	
	public void runLDA(LdaModel lda, Arff trainArff, Arff testArff){
		
		List<BugReport> brs = new ArrayList<BugReport>();
		brs.addAll(trainArff.getBrs());
		brs.addAll(testArff.getBrs());
		
		
		lda.trainModel(brs);
		
		double pack_topic[][] = new double[trainArff.getLabels().size()][];
		double affects[] = new double[trainArff.getLabels().size()];

		
		for(int i=0;i<pack_topic.length;i++)
			pack_topic[i] = new double[lda.K];
		
		
		for(int i=0;i<trainArff.getBrs().size();i++){
			BugReport br = trainArff.getBrs().get(i);
			
			for(String packName : br.getAffectedPackage()){
				//check whether this bug report affects this package
				int packIndex = trainArff.getLabels().indexOf(packName);
				if(packIndex!=-1){
					add(pack_topic[packIndex],lda.theta[i]);
					affects[packIndex]++;
				}							
			}
		}
		
		for(int i=0;i<trainArff.getLabels().size();i++){
			
			
			for(int j=0;j<pack_topic[i].length;j++){
				
				if(affects[i]!=0)
				pack_topic[i][j]/=affects[i];
				
			}
			
		}
		
		double startTime = System.currentTimeMillis();
		for(int i=0;i<testArff.getBrs().size();i++){
			
			for(int j=0;j<pack_topic.length;j++){
				double p[] = lda.theta[i+trainArff.getBrs().size()]; //conditional probability for testing data
				double q[] = pack_topic[j]; //conditional probability for training data
				double kl = KL(p,q);
				//if(Double.isNaN(kl))
				//System.out.println("KL: "+kl);
				if(kl!=0)
				testArff.getConfidences()[i][j] = -kl;
				else testArff.getConfidences()[i][j] = -1000000000;
			}
			
		}
		
		System.out.println("Test time cost: "+(System.currentTimeMillis()-startTime)/1000.0);
				
			
		
		
		
	}
	
	
	public double evaluate(Arff arff, int topK){
		
		//recall@topK
		
		double[] precision = new double[arff.getData().size()];
		double[] recall = new double[arff.getData().size()];
		
		double testSetSize = 0;
		
		for(int i=0;i<arff.getData().size();i++){
		
			testSetSize++;
			
			double trueLabels=0;
			for(int j=0;j<arff.getGroundTruth()[i].length;j++)
				if(arff.getGroundTruth()[i][j])
					trueLabels++;
			double tp=0;	
			int[] topIndex = Algorithm.topKindex(arff.getConfidences()[i],topK);
			Set<String> fp = new HashSet<String>();
			for(int labelIndex:topIndex){
				if(arff.getGroundTruth()[i][labelIndex]==true)
					tp++;
				else if(arff.getConfidences()[i][labelIndex]>0)
					fp.add(arff.getLabels().get(labelIndex));
					
			}
			precision[i] = tp/topK;	
			recall[i] = tp/trueLabels;
			
			
			/*if(fp.size()>0&&tp==1&&trueLabels>=2&&arff.getBrs().get(i).getAffects().contains("Invalid")){
				System.out.println(arff.getBrs().get(i).getId());
				for(int j=0;j<arff.getGroundTruth()[i].length;j++)
					if(arff.getGroundTruth()[i][j])
						System.out.print(arff.getLabels().get(j));
				System.out.println();
				for(String label:fp)
					System.out.print(label);
				System.out.println();
				
			}*/
			
			//System.out.println(precision[i]+" "+recall[i]);
			
		}
		
		double P,R;
		P=R=0;
	
		for(double p:precision)
				P+=p;
		P/=testSetSize;
			
		for(double r:recall)
			R+=r;
		R/=testSetSize;

		//System.out.println(P+" "+R);
		
		return R;
		
	}
	
	public void printConfidences(Arff testArff, int topk){
		
		for(int i=0;i<testArff.getBrs().size();i++){
		
			int[] topIndex = Algorithm.topKindex(testArff.getConfidences()[i],topk);
			for(int index:topIndex)
				System.out.print(index+" "+new java.text.DecimalFormat("0.000").format(testArff.getConfidences()[i][index])+
						testArff.getGroundTruth()[i][index]+" | ");
			System.out.println();
		}
		System.out.println("--------------------------");
		
	}
	
	
	public static void add(double a[], double b[]){
		for(int i=0;i<a.length;i++){
			a[i]+=b[i];
		}
	}
	
	public static double KL(double p[],double q[]){
		double kl = 0;
		
		for(int i=0;i<p.length;i++){
			if(p[i]==0||q[i]==0)
				continue;
			kl += p[i]*Math.log(p[i]/q[i]);
		}
		return kl/Math.log(2);
		
	}
	

}
