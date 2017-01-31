package util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Algorithm {
	
	public static class Pair implements Comparable<Pair>{
		String key;
		int value;
		
		@Override
	    public int compareTo(Pair other) {
	        return Integer.compare(other.value, this.value);
	    }
		
		public Pair(String key,int value){
			this.key = key;
			this.value = value;
		}
		
	}
	

	public static int[] topKindex(double[] v, int k) {

		// return the index of topk value, 
		// the index of highest value ranks the first

		boolean[] mark = new boolean[v.length];
		int[] index = new int[k];

		for (int i = 0; i < k; i++) {
			double mx = -10000000;
			int pos = -1;
			for (int j = 0; j < v.length; j++) {
				if (mark[j])
					continue;
				if (v[j] > mx) {
					mx = v[j];
					pos = j;
				}
			}
			mark[pos] = true;
			index[i] = pos;
		}

		return index;

	}
	
	public static List<String> sortMap(Map<String,Integer> map){
		
		List<Pair> list = new ArrayList<Pair>();
		for(Map.Entry<String, Integer> entry:map.entrySet()){
			list.add(new Pair(entry.getKey(),entry.getValue()));
		}
		Collections.sort(list);
		List<String> res = new ArrayList<String>();
		for(Pair pair:list){
			res.add(pair.key);
		}
		
		return res;

	}
	
	
	public static Set<String> intersect(Set<String> set1, Set<String> set2){
		
		Set<String> set = new HashSet<String>();
		
		for(String s : set1){
			if(set2.contains(s))
				set.add(s);
		}
		
		return set;
		
	}

}
