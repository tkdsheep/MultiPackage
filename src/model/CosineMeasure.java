package model;

import java.util.List;

import domain.Arff.Pair;

public class CosineMeasure {
	
	public static double cosineSim(List<String> attributes, List<Pair> a,List<Pair> b){
		
		double sim = 0;
		
		int i,j;
		i = j = 0;
		
		double squareSumA = 0;
		double squareSumB = 0;
		
		for(Pair pair:a){
			if(pair.getIndex()>=attributes.size())
				break;
			squareSumA += pair.getValue()*pair.getValue();
		}
		
		for(Pair pair:b){
			if(pair.getIndex()>=attributes.size())
				break;
			squareSumB += pair.getValue()*pair.getValue();
		}
		
		
		for(;i<a.size();i++){
			
			int aindex = a.get(i).getIndex();
			double avalue = a.get(i).getValue();
			if(aindex>=attributes.size())
				break;
			
			while(j<b.size()){
				int bindex = b.get(j).getIndex();
				if(bindex>aindex)
					break;
				if(bindex<aindex){
					j++;
					continue;
				}
				double bvalue = b.get(j).getValue();
				sim+=avalue*bvalue;		
				break;
				
			}
			
			
		}
		
		
		return sim / Math.sqrt(squareSumA) / Math.sqrt(squareSumB);
		
	}

}
