package model;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import util.Algorithm;
import domain.BugReport;

public class LdaModel {
	
	/*
	 * original author : http://blog.csdn.net/yangliuy
	 * modified by qiaohuang
	 */
	public int[][] doc;// word index array
	public int V, K, M;// vocabulary size, topic size, document size
	public int[][] z;// topic label array
	public double alpha; // doc-topic dirichlet prior parameter
	public double beta; // topic-word dirichlet prior parameter
	public int[][] nmk;// given document m, count times of topic k. M*K
	public int[][] nkt;// given topic k, count times of term t. K*V
	public int[] nmkSum;// Sum for each row in nmk
	public int[] nktSum;// Sum for each row in nkt
	public double[][] phi;// Parameters for topic-word distribution K*V
	public double[][] theta;// Parameters for doc-topic distribution M*K
	public int iterations;// Times of iterations
	//private int saveStep;// The number of iterations between two saving
	//private int beginSaveIters;// Begin save model at this iteration
	
	public Map<String,Integer> wordToNumber;//word index
	public List<String> NumberToWord;//global vocabulary
	
	public LdaModel(int K, int iterations) {
		//TODO need global config

		this.iterations = iterations;
		this.K = K;
		this.alpha = 50.0/K;
		this.beta = 0.1;
		
	}
	
	public void trainModel(List<BugReport> brs){
		
		/*
		 * input: a list of documents, represented by words (duplicate words may exist)
		 * output: doc-topic matrix, topic-word matrix, other info including wordToNumber index
		 */
		
		double startTime = System.currentTimeMillis();
		this.initializeModel(brs);
		this.inferenceModel(brs);
		double endTime = System.currentTimeMillis();
		System.out.println("LDA training time cost: "+(endTime-startTime)/1000.0);
		
	}
	
	
	private void initializeModel(List<BugReport> brs){
		
		System.out.println("Start to initialize LDA model");
		
		/*
		 * preprocess all words in docs
		 * assign unique number to each word
		 */	
		wordToNumber = new HashMap<String,Integer>();
		NumberToWord = new ArrayList<String>();
		
		for(BugReport br:brs){
			for(String word:br.getWordList()){
				if(!wordToNumber.containsKey(word)){
					wordToNumber.put(word, NumberToWord.size());
					NumberToWord.add(word);
				}		
			}
		}
		
		/*
		 * initialize model variable
		 */
		
		M = brs.size();//number of documents
		V = wordToNumber.size();//vocabulary size
		nmk = new int[M][K];
		nkt = new int[K][V];
		nmkSum = new int[M];
		nktSum = new int[K];
		phi = new double[K][V];
		theta = new double[M][K];
		
		System.out.println("topic size: "+K+", word size: "+V+", doc size "+M);
		System.out.println("Build index: word to number ");
		
		
		System.out.println("initialize documents index array and topic label");

		// initialize documents index array
		doc = new int[M][];
		for (int m = 0; m < M; m++) {
			// Notice the limit of memory
			int N = brs.get(m).getWordList().size();//number of words in this document
			doc[m] = new int[N];
			for (int n = 0; n < N; n++) {
				String word = brs.get(m).getWordList().get(n);
				doc[m][n] = wordToNumber.get(word);
			}
		}

		// initialize topic lable z for each word
		z = new int[M][];
		for (int m = 0; m < M; m++) {
			int N = brs.get(m).getWordList().size();//number of words in this document
			z[m] = new int[N];
			for (int n = 0; n < N; n++) {
				int initTopic = (int) (Math.random() * K);// From 0 to K - 1
				z[m][n] = initTopic;
				// number of words in doc m assigned to topic initTopic add 1
				nmk[m][initTopic]++;
				// number of terms doc[m][n] assigned to topic initTopic add 1
				nkt[initTopic][doc[m][n]]++;
				// total number of words assigned to topic initTopic add 1
				nktSum[initTopic]++;
			}
			// total number of words in document m is N
			nmkSum[m] = N;
		}
		
		System.out.println("LDA model initialized successfully");
		
	}
	
	private void inferenceModel(List<BugReport> brs){
		
		System.out.println("Start to inference LDA model");
		/*
		if (iterations < saveStep + beginSaveIters) {
			System.err
					.println("Error: the number of iterations should be larger than "
							+ (saveStep + beginSaveIters));
			System.exit(0);
		}*/
		
		System.out.println("LDA Iterations begin: ");
		for (int i = 0; i < iterations; i++) {
			
			System.out.print("|");
			/*if ((i >= beginSaveIters)
					&& (((i - beginSaveIters) % saveStep) == 0)) {
				// Saving the model
				System.out.println("Saving model at iteration " + i + " ... ");
				// Firstly update parameters
				updateEstimatedParameters();
				// Secondly print model variables
				//saveTopicInfo(i,docSet,topics);
			}*/

			// Use Gibbs Sampling to update z[][]
			for (int m = 0; m < M; m++) {
				int N = brs.get(m).getWordList().size();
				for (int n = 0; n < N; n++) {
					// Sample from p(z_i|z_-i, w)
					int newTopic = sampleTopicZ(m, n);
					z[m][n] = newTopic;
				}
			}
		}
		
		//training finished, update phi and theta matrix
		updateEstimatedParameters();
		
		System.out.println("Model training finished");
	}
	
	private void updateEstimatedParameters() {
		
		for (int k = 0; k < K; k++) {
			for (int t = 0; t < V; t++) {
				phi[k][t] = (nkt[k][t] + beta) / (nktSum[k] + V * beta);
			}
		}

		for (int m = 0; m < M; m++) {
			for (int k = 0; k < K; k++) {
				theta[m][k] = (nmk[m][k] + alpha) / (nmkSum[m] + K * alpha);
			}
		}
	}

	private int sampleTopicZ(int m, int n) {
		
		// Sample from p(z_i|z_-i, w) using Gibbs upde rule

		// Remove topic label for w_{m,n}
		int oldTopic = z[m][n];
		nmk[m][oldTopic]--;
		nkt[oldTopic][doc[m][n]]--;
		nmkSum[m]--;
		nktSum[oldTopic]--;

		// Compute p(z_i = k|z_-i, w)
		double[] p = new double[K];
		for (int k = 0; k < K; k++) {
			p[k] = (nkt[k][doc[m][n]] + beta) / (nktSum[k] + V * beta)
					* (nmk[m][k] + alpha) / (nmkSum[m] + K * alpha);
		}

		// Sample a new topic label for w_{m, n} like roulette
		// Compute cumulated probability for p
		for (int k = 1; k < K; k++) {
			p[k] += p[k - 1];
		}
		double u = Math.random() * p[K - 1]; // p[] is unnormalised
		int newTopic;
		for (newTopic = 0; newTopic < K; newTopic++) {
			if (u < p[newTopic]) {
				break;
			}
		}

		// Add new topic label for w_{m, n}
		nmk[m][newTopic]++;
		nkt[newTopic][doc[m][n]]++;
		nmkSum[m]++;
		nktSum[newTopic]++;
		return newTopic;
	}
	
	
	/*public void printInfo(List<Document> docs){
		//private int V, K, M;// vocabulary size, topic size, document size
		//private double[][] phi;// Parameters for topic-word distribution K*V
		//private double[][] theta;// Parameters for doc-topic distribution M*K
		
		
		for(int i=0;i<K;i++){
			
			//print the topk words for the ith topic
			List<Integer> topk = Algorithm.findTopk(phi[i], 15);
			for(int index:topk)
				System.out.print(NumberToWord.get(index)+" ");
			System.out.println("\n");
			
			//print the topk doc for the ith topic
			double[] topicDoc = new double[M];
			for(int j=0;j<M;j++)
				topicDoc[j]=theta[j][i];
			topk = Algorithm.findTopk(topicDoc, 30);
			for(int index:topk){
				if(theta[index][i]<0.2)
					break;
				System.out.println(new java.text.DecimalFormat("0.00").format(theta[index][i])
						+"\t"+docs.get(index).getImportance()+"\t"+docs.get(index).getTitle()
						+" (#"+docs.get(index).getId()+")");
			}
			System.out.println("------------------------------\n");
			
			
			
			
		}
		
	
		
			
		
		
		
		
		
		
	}*/
	
	

}
