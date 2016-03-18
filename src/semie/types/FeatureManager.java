package semie.types;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 * This class manages features.
 * @author luwei
 * @version 1.0
 */

public class FeatureManager implements Serializable{
	
	private static final long serialVersionUID = 7060142186283955214L;
	
	/**
	 * Set this to true if you want to save memory, but will make training slower.
	 */
	public boolean _memoryEfficientMode = false;
	
	private HashMap<IntArray, Integer> _intArray2intMap;
	private HashMap<Integer, double[]> _featureMap;//from index of KEY2 directly to feature's fields: count_L, count_G, weight_old, weight_new
	private HashMap<Integer, Double> _preferenceMap; // from index of KEY2 directly to preference's weight, which is fixed.
	private ArrayList<Integer> _allFeatures;
	private ArrayList<Integer> _allPreferences;
	
	public FeatureManager(){
		this._intArray2intMap = new HashMap<IntArray, Integer>();
		this._featureMap = new HashMap<Integer, double[]>();
		this._preferenceMap = new HashMap<Integer, Double>();
	}

	public void addFeatureCount_L(int feature, double count){
		double[] ffs = this._featureMap.get(feature);
		ffs[0] += count;
	}
	
	public void addFeatureCount_G(int feature, double count){
		double[] ffs = this._featureMap.get(feature);
		ffs[1] += count;
	}
	
	public boolean setWeight(int feature, double weight){
		if(this._featureMap.get(feature)[2] == weight)
			return false;
		this._featureMap.get(feature)[2] = weight;
		return true;
	}
	
	public double getGradient(int feature){
		double[] ffs = this._featureMap.get(feature);
		return ffs[0] - ffs[1];
	}
	
	public double getWeight(int feature, boolean considerPreferences){
		
		//if the feature weight is pre-defined.
		if(this._preferenceMap.containsKey(feature)){
			if(considerPreferences)
				return this._preferenceMap.get(feature);
			else
				return 0.0;
		} else
			return this._featureMap.get(feature)[2];
		
	}
	
	public void resetFeatureCounts(){
		ArrayList<Integer> allfeatures = this.getAllFeatures();
		for(int feature : allfeatures)
			this.resetFeatureCounts(feature);
	}
	
	private void resetFeatureCounts(int feature){
		double[] ffs = this._featureMap.get(feature);
		ffs[0] = 0;
		ffs[1] = 0;
	}
	
	public void readPreferencesFromFile(Manager manager, String filename)throws IOException{
		Scanner scan = new Scanner(new File(filename), "UTF-8");
		while(scan.hasNextLine()){
			String line = scan.nextLine().trim();
			if(line.equals("")||line.startsWith("#")||line.startsWith("//")){
				//do nothing
			} else {
				// arg_type & Agent|Agent_IN_Life:Die & PER & 1.0
				// arg_words & Agent|Agent_IN_Life:Die ARG_ANYTHING & killed & 1.0
				StringTokenizer st = new StringTokenizer(line, "&");
				String category = st.nextToken().trim();
				if(category.equals("arg_type")){
					String arg = st.nextToken().trim();
					String[] argument_names = arg.split("\\|");
					String type_name = st.nextToken().trim();
					int arg_id = manager.getRole(argument_names).getId();
					int type_id = manager.getType(type_name).getId();
					
					int f0 = -1;
					int f1 = this.indexKEY(new IntArray(new int[]{arg_id, type_id}));
					int f2 = -999;
					
					double weight = Double.parseDouble(st.nextToken());
					
					this.toPreference(f0, f1, f2, weight);
					
				} else if(category.equals("args_words")){
					String[] args = st.nextToken().trim().split("\\s");
					int[] arg_ids = new int[args.length];
					for(int k = 0; k<args.length; k++){
						if(args[k].equals("ANYTHING")){
							arg_ids[k] = Role._dummy.getId();
						} else {
							String[] argument_names = args[k].split("\\|");
							arg_ids[k] = manager.getRole(argument_names).getId();
						}
					}
					
					int f0 = +1;
					
					int f1 = -99999;//this will be replaced by something below.
					if(arg_ids.length == 3){
						f1 = indexKEY(new IntArray(new int[]{arg_ids[0], arg_ids[1], arg_ids[2]}));
					} else if(arg_ids.length == 2){
						f1 = this.indexKEY(new IntArray(new int[]{arg_ids[0], arg_ids[1]}));
					} else {
						throw new RuntimeException("This should not happen!"+ arg_ids.length+" args");
					}
					
					String[] words = st.nextToken().trim().split("\\s");
					if(words.length>2){
						throw new RuntimeException("Currently not supported!");
					}
					int[] word_ids = new int[words.length];
					for(int k = 0; k<words.length; k++){
						if(words[k].equals("BEGIN_WORD")){
							word_ids[k] = Word._BEGIN_WORD.getId();
						} else if(words[k].equals("END_WORD")){
							word_ids[k] = Word._END_WORD.getId();
						} else{
							word_ids[k] = manager.toWord(words[k]).getId();
						}
					}
					int f2 = this.indexKEY(new IntArray(word_ids));
					
					double weight = Double.parseDouble(st.nextToken());
					
					this.toPreference(f0, f1, f2, weight);
					
				} else {
					throw new RuntimeException("Unknown category:"+category);
				}
			}
		}
	}
		

	// 2/3
	public int indexKEY(IntArray key){
		if(this._intArray2intMap.containsKey(key))
			return this._intArray2intMap.get(key);
		int id = this._intArray2intMap.size();
		key.setId(id);
		this._intArray2intMap.put(key, id);
		return id;
	}
		

	//for features//
	
	public void toPreference(int f0, int f1, int f2, double weight){
		int f = this.getFeature(f0, f1, f2);
		if(f == Integer.MIN_VALUE){
			System.err.println("[Warning] One preference is not fired as a feature: "+f0+","+f1+","+f2);
			return;
		}
		this._preferenceMap.put(f, weight);
	}
	
	private double getInitialWeight(int f0, int f1, int f2){
		
		return 0.0;

	}
	
	/* here, f1 is the index of the first KEY, and f2 is the index of the second KEY */
	public int toFeature(int type, int f1, int f2){
		IntArray key = new IntArray(new int[]{type, f1, f2});
		int index = this.indexKEY(key);
		if(this._featureMap.containsKey(index)){
			return index;
		}
		double[] ffs = new double[]{0.0, 0.0, this.getInitialWeight(type, f1, f2)};
		this._featureMap.put(index, ffs);
		return index;
	}
	
	/* if not found, simply return Integer.MIN_VALUE */
	public int getFeature(int type, int f1, int f2){
		IntArray key = new IntArray(new int[]{type, f1, f2});
		int index = this.indexKEY(key);
		if(this._featureMap.containsKey(index)){
			return index;
		} else {
			return Integer.MIN_VALUE;
		}
	}

	public ArrayList<Integer> getAllFeatures(){
		
		this._allFeatures = new ArrayList<Integer>();
		Iterator<Integer> fts = this._featureMap.keySet().iterator();		
		while(fts.hasNext()){
			int ft = fts.next();
			this._allFeatures.add(ft);
		}
		
		return this._allFeatures;
		
	}
	
	public ArrayList<Integer> getAllPreferences(){
		
		this._allPreferences = new ArrayList<Integer>();
		Iterator<Integer> fts = this._preferenceMap.keySet().iterator();		
		while(fts.hasNext()){
			int ft = fts.next();
			this._allPreferences.add(ft);
		}
		
		return this._allPreferences;
		
	}
	
	public void resetFeatureExtractorCache(){
		if(_memoryEfficientMode)
			FeatureExtractor.resetCache();
	}
	
	public ArrayList<Integer> extractFeatures(Role[] args, Phrase phrase, boolean createIfNotPresent){
		return FeatureExtractor.extractStructureFeatures(this, args, phrase, createIfNotPresent);
	}
	
	public ArrayList<Integer> extractFeatures(Role arg, Type type, Phrase phrase, EventSpan span, int bIndex, int eIndex, boolean createIfNotPresent){
		return FeatureExtractor.extractStructureFeatures(this, arg, type, phrase, span, bIndex, eIndex, createIfNotPresent);
	}
	
	public double computeScore(ArrayList<Integer> features, boolean considerPreferences){
		double score = 0.0;
		for(int i = 0; i<features.size(); i++){
			int feature = features.get(i);
			double weight = this.getWeight(feature, considerPreferences);
			score += weight;
		}
		score = Math.exp(score);
		return score;
	}
	
}
