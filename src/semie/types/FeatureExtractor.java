package semie.types;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class will extract features, which are of integer representations
 * @author luwei
 * @version 1.0
 */

public class FeatureExtractor{
	
	private static HashMap<Integer, ArrayList<Integer>> _phrase2inputfeatures = new HashMap<Integer, ArrayList<Integer>>();
	private static HashMap<Integer, ArrayList<Integer>> _phrase2inputfeaturesWithContext = new HashMap<Integer, ArrayList<Integer>>();
	private static HashMap<IntArray, ArrayList<Integer>> args_phrase2features = new HashMap<IntArray, ArrayList<Integer>>();
	private static HashMap<IntArray, ArrayList<Integer>> arg_type_phrase2features = new HashMap<IntArray, ArrayList<Integer>>();
	
	private static boolean _cache = true;
	
	/**
	 * Reset the cache
	 */
	public static void resetCache(){
		_phrase2inputfeatures = new HashMap<Integer, ArrayList<Integer>>();
		_phrase2inputfeaturesWithContext = new HashMap<Integer, ArrayList<Integer>>();
		args_phrase2features = new HashMap<IntArray, ArrayList<Integer>>();
		arg_type_phrase2features = new HashMap<IntArray, ArrayList<Integer>>();
	}
	
	/**
	 * Extract structured features
	 * @param manager the manager 
	 * @param role the role
	 * @param type the type
	 * @param phrase the phrase
	 * @param span the event span
	 * @param bIndex the start index of the interval
	 * @param eIndex the end index of the interval
	 * @param createIfNotPresent create new features if true, otherwise do not create new features
	 * @return the list of extracted features
	 */
	public static ArrayList<Integer> extractStructureFeatures(FeatureManager manager, Role role, Type type, Phrase phrase, EventSpan span, int bIndex, int eIndex, boolean createIfNotPresent){
		
		IntArray key = new IntArray(new int[]{role.getId(), type.getId(), phrase.getId()});
		if(_cache && arg_type_phrase2features.containsKey(key))
			return arg_type_phrase2features.get(key);
		
		ArrayList<Integer> features = new ArrayList<Integer>();
		
		int f0 = -1;
		int[] output_fs = extractOutputFeatures(manager, role, type);
		ArrayList<Integer> input_fs = extractInputFeaturesWithContext(manager, phrase, span, bIndex, eIndex);
		
		for(int output : output_fs){
			for(int input: input_fs){
				int f =  createIfNotPresent ? manager.toFeature(f0, output, input) : manager.getFeature(f0, output, input);
				if(f!=Integer.MIN_VALUE)
					features.add(f);
			}
			//finally, an additional feature on arg-type relation.
			int f =  createIfNotPresent ? manager.toFeature(f0, output, -999) : manager.getFeature(f0, output, -999);
			if(f!=Integer.MIN_VALUE)
				features.add(f);
		}
		
		if(_cache)
			arg_type_phrase2features.put(key, features);
		
		return features;
	}
	
	/**
	 * Extract structured features
	 * @param manager the manager
	 * @param roles the list of roles
	 * @param phrase the phrase
	 * @param createIfNotPresent create new features if true, otherwise do not create new features
	 * @return the list of extracted features
	 */
	public static ArrayList<Integer> extractStructureFeatures(FeatureManager manager, Role[] roles, Phrase phrase, boolean createIfNotPresent){
		
		int keys[] = new int[roles.length+1];
		for(int i = 0; i<roles.length; i++)
			keys[i] = roles[i].getId();
		keys[roles.length] = phrase.getId();
		IntArray key = new IntArray(keys);
		
		if(_cache && args_phrase2features.containsKey(key))
			return args_phrase2features.get(key);
		
		ArrayList<Integer> features = new ArrayList<Integer>();
		
		int f0 = +1;
		
		int[] output_fs = extractOutputFeatures(manager, roles);
		ArrayList<Integer> input_fs = extractInputFeatures(manager, phrase);
		
		for(int output : output_fs){
			for(int input: input_fs){
				int f =  createIfNotPresent ? manager.toFeature(f0, output, input) : manager.getFeature(f0, output, input);
				if(f!=Integer.MIN_VALUE)
					features.add(f);
			}
		}
		
		if(_cache)
			args_phrase2features.put(key, features);
		
		return features;
	}
	
	private static void extractContextInputFeatures(FeatureManager manager, EventSpan span_org, int bIndex_org, int eIndex_org, ArrayList<Integer> results){

		int prevwords[] = new int[]{Word._BEGIN_WORD.getId(), Word._BEGIN_WORD.getId(), Word._BEGIN_WORD.getId()};
		int nextwords[] = new int[]{Word._END_WORD.getId(), Word._END_WORD.getId(), Word._END_WORD.getId()};
		int prevpos[] = new int[]{-111, -111, -111};
		int nextpos[] = new int[]{-222, -222, -222};
		
		int subType = -500;
		
		Word[] words = span_org.getWords();
		Tag[][] pos = span_org.getTags("POS");
		
		if(bIndex_org-1>=0){
			prevwords[2] = words[bIndex_org-1].getId();
			prevpos[2] = pos[bIndex_org-1][0].getId();

			if(bIndex_org-2>=0){
				prevwords[1] = words[bIndex_org-2].getId();
				prevpos[1] = pos[bIndex_org-2][0].getId();

				if(bIndex_org-3>=0){
					prevwords[0] = words[bIndex_org-3].getId();
					prevpos[0] = pos[bIndex_org-3][0].getId();
				}
			}
		}
		
		if(eIndex_org<words.length){
			nextwords[0] = words[eIndex_org].getId();
			nextpos[0] = pos[eIndex_org][0].getId();

			if(eIndex_org+1<words.length){
				nextwords[1] = words[eIndex_org+1].getId();
				nextpos[1] = pos[eIndex_org+1][0].getId();

				if(eIndex_org+2<words.length){
					nextwords[2] = words[eIndex_org+2].getId();
					nextpos[2] = pos[eIndex_org+2][0].getId();
				}
			}
		}
		
		for(int i = 0; i<prevwords.length; i++){
			results.add(manager.indexKEY(new IntArray(new int[]{subType+1, i, prevwords[i]})));
			results.add(manager.indexKEY(new IntArray(new int[]{subType+2, i, prevpos[i]})));
		}
		for(int i = 0; i<nextwords.length; i++){
			results.add(manager.indexKEY(new IntArray(new int[]{subType+3, i, nextwords[i]})));
			results.add(manager.indexKEY(new IntArray(new int[]{subType+4, i, nextpos[i]})));
		}
		
	}

	private static void extractUnigramInputFeatures(FeatureManager manager, Phrase phrase, ArrayList<Integer> results){

		EventSpan span = phrase.getEventSpan();
		int bIndex = phrase.getBIndex();
		int eIndex = phrase.getEIndex();
		Word[] words = span.getWords();
		
		int subType = 100;
		
		for(int i = bIndex; i<eIndex; i++)
			results.add(manager.indexKEY(new IntArray(new int[]{subType, words[i].getId()})));
		
	}
	
	private static void extractBigramInputFeatures(FeatureManager manager, Phrase phrase, ArrayList<Integer> results){

		EventSpan span = phrase.getEventSpan();
		int bIndex = phrase.getBIndex();
		int eIndex = phrase.getEIndex();
		Word[] words = span.getWords();
		
		int subType = 200;
		
		int word1 = Word._BEGIN_WORD.getId();
		int word2;
		for(int i = bIndex; i<eIndex; i++){
			word2 = words[i].getId();
			results.add(manager.indexKEY(new IntArray(new int[]{subType, word1, word2})));
			word1 = word2;
		}
		word2 = Word._END_WORD.getId();
		results.add(manager.indexKEY(new IntArray(new int[]{subType, word1, word2})));
	}

	private static void extractTriggerInputFeatures(FeatureManager manager, Phrase phrase, ArrayList<Integer> results){
		
		EventSpan span = phrase.getEventSpan();
		
		int bIndex = phrase.getBIndex();
		int eIndex = phrase.getEIndex();
		
		int trigger_bIndex = span.getTriggerIndices()[0];
		int trigger_eIndex = span.getTriggerIndices()[1];
		
		int subType = -100;
		
		//left
		if(eIndex <= trigger_bIndex)
			results.add(manager.indexKEY(new IntArray(new int[]{subType, -1})));
		//right
		else if(bIndex >= trigger_eIndex)
			results.add(manager.indexKEY(new IntArray(new int[]{subType, -2})));
		//contains
		else if(trigger_bIndex >= bIndex && trigger_eIndex <= eIndex)
			results.add(manager.indexKEY(new IntArray(new int[]{subType, -3})));
		//others
		else
			results.add(manager.indexKEY(new IntArray(new int[]{subType, -4})));
		
	}
	
	private static ArrayList<Integer> extractInputFeaturesWithContext(FeatureManager manager, Phrase phrase, EventSpan span_org, int bIndex_org, int eIndex_org){
		
		if(_cache && _phrase2inputfeaturesWithContext.containsKey(phrase.getId()))
			return _phrase2inputfeaturesWithContext.get(phrase.getId());
		
		if(phrase == Phrase._BEG_PHRASE)
			return new ArrayList<Integer>();
		if(phrase == Phrase._END_PHRASE)
			return new ArrayList<Integer>();
		
		ArrayList<Integer> results = new ArrayList<Integer>();
		
		extractUnigramInputFeatures(manager, phrase, results);
		
		extractBigramInputFeatures(manager, phrase, results);
		
		extractTriggerInputFeatures(manager, phrase, results);
		
		extractContextInputFeatures(manager, span_org, bIndex_org, eIndex_org, results);
		
		//cache it...
		if(_cache)
			_phrase2inputfeaturesWithContext.put(phrase.getId(), results);
		
		return results;
	}
	
	private static ArrayList<Integer> extractInputFeatures(FeatureManager manager, Phrase phrase){
		
		if(_cache && _phrase2inputfeatures.containsKey(phrase.getId()))
			return _phrase2inputfeatures.get(phrase.getId());
		
		if(phrase == Phrase._BEG_PHRASE || phrase == Phrase._END_PHRASE)
			return new ArrayList<Integer>();
		
		ArrayList<Integer> results = new ArrayList<Integer>();
		extractUnigramInputFeatures(manager, phrase, results);
		extractBigramInputFeatures(manager, phrase, results);
		extractTriggerInputFeatures(manager, phrase, results);
		
		if(_cache) _phrase2inputfeatures.put(phrase.getId(), results);
		
		return results;
	}
	
	private static int[] extractOutputFeatures(FeatureManager manager, Role role, Type type){
		IntArray key = new IntArray(new int[]{role.getId(), type.getId()});
		return new int[]{manager.indexKEY(key)};
	}
	
	private static int[] extractOutputFeatures(FeatureManager manager, Role[] roles){
		int res[] = new int[4];
		res[0] = manager.indexKEY(new IntArray(new int[]{roles[0].getId(), roles[1].getId(), roles[2].getId()}));
		res[1] = manager.indexKEY(new IntArray(new int[]{roles[1].getId(), roles[2].getId()}));
		res[2] = manager.indexKEY(new IntArray(new int[]{roles[1].getId(), Word._ANY_WORD.getId()}));
		res[3] = manager.indexKEY(new IntArray(new int[]{Word._ANY_WORD.getId(), roles[2].getId()}));
		return res;
	}
	
}
