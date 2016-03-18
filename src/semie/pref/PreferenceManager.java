package semie.pref;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;
import java.util.StringTokenizer;

import semie.types.EventSpan;
import semie.types.IntArray;
import semie.types.Lattice;
import semie.types.Manager;
import semie.types.Role;
import semie.types.RoleSpan;
import semie.types.Type;
import semie.types.Word;

/**
 * Preference manager. Limitation: currently only support count and boolean preferences
 * @author luwei
 * @version 1.0
 */

public class PreferenceManager implements Serializable{
	
	private static final long serialVersionUID = 6663874880980226686L;

	/**
	 * Mappings from integer array into their unique integer id (preference's id)
	 */
	private HashMap<IntArray, Integer> _key2intMap;

	/**
	 * Mappings from preference to their scores
	 */
	private HashMap<Integer, Double> _preference2scoreMap;
	
	/**
	 * Mappings from preference id to actual preference
	 */
	private HashMap<Integer, Preference> _preferenceMap;
	
	private static final int _ROLE = -10009;
	private static final int _TYPE = -10019;
	private static final int _INPUT = -10029;
	
	private static final int _COUNT = -990001;
	private static final int _BOOLEAN = -990002;
	
	public PreferenceManager(){
		this._preferenceMap = new HashMap<Integer, Preference>();
		this._preference2scoreMap = new HashMap<Integer, Double>();
		this._key2intMap = new HashMap<IntArray, Integer>();
	}
	
	/**
	 * Get the number of preferences
	 * @return the number of preferences
	 */
	public int getNumPreferences(){
		return this._preferenceMap.size();
	}
	
	/**
	 * Compute the preference score
	 * @param rolespan the role span
	 * @param eventspan the event span
	 * @return the preference score
	 */
	public double computePreferenceScore(RoleSpan rolespan, EventSpan eventspan){
		
		double totalScore = 0;
		ArrayList<Integer> features = extractPreferenceFeatures(rolespan, eventspan);
		for(int i = 0; i<features.size(); i++){
			int feature = features.get(i);
			if(feature!=-1)
				totalScore += this._preference2scoreMap.get(feature);
		}
		return totalScore;
		
	}
	
	/**
	 * Get the satisfied boolean preference
	 * @param roles the roles
	 * @param types the types
	 * @param inputs the input words
	 * @return the preference's id if satisfied, -1 otherwise
	 */
	public int getSatisfiedPreferenceFeature_Boolean(ArrayList<Role> roles, ArrayList<Type> types, ArrayList<Integer> inputs){
		int[] fv = new int[1+3+(roles.size()+types.size()+inputs.size())*2];
		
		int t = 0;
		fv[t++] = _BOOLEAN;
		fv[t++] = roles.size();
		fv[t++] = types.size();
		fv[t++] = inputs.size();
		for(int i = 0; i<roles.size(); i++){
			fv[t++] = _ROLE;
			fv[t++] = roles.get(i).getId();
		}
		for(int i = 0; i<types.size(); i++){
			fv[t++] = _TYPE;
			fv[t++] = types.get(i).getId();
		}
		for(int i = 0; i<inputs.size(); i++){
			fv[t++] = _INPUT;
			fv[t++] = inputs.get(i);
		}
		IntArray key = new IntArray(fv);
		
		int k;
		if(this._key2intMap.containsKey(key)){
			k = this._key2intMap.get(key);
			BooleanPreference bp = (BooleanPreference)this._preferenceMap.get(k);
			if(bp.satisfy(k, 1))
				return k;
		}
		
		return -1;
		
	}
	
	/**
	 * Get the preference that satisfies the preference constraint
	 * @param roles the list of roles
	 * @param types the list of types
	 * @param inputs the list of inputs (words?)
	 * @param count the count
	 * @return the preference's id if satisfied, -1 otherwise
	 */
	public int getSatisfiedPreferenceFeature_Count(ArrayList<Role> roles, ArrayList<Integer> types, ArrayList<Integer> inputs, int count){
		int[] fv = new int[1+3+(roles.size()+types.size()+inputs.size())*2];
		int t = 0;
		fv[t++] = _COUNT;
		fv[t++] = roles.size();
		fv[t++] = types.size();
		fv[t++] = inputs.size();
		for(int i = 0; i<roles.size(); i++){
			fv[t++] = _ROLE;
			fv[t++] = roles.get(i).getId();
		}
		for(int i = 0; i<types.size(); i++){
			fv[t++] = _TYPE;
			fv[t++] = types.get(i);
		}
		for(int i = 0; i<inputs.size(); i++){
			fv[t++] = _INPUT;
			fv[t++] = inputs.get(i);
		}
		IntArray key = new IntArray(fv);
		
		if(this._key2intMap.containsKey(key)){
			int k = this._key2intMap.get(key);
			CountPreference cp = (CountPreference)this._preferenceMap.get(k);
			if(cp.satisfy(k, count))
				return k;
		}
		
		return -1;
	}
	
	/**
	 * Extract the preferences
	 * @param rolespan the role span
	 * @param eventspan the event span
	 * @return the preferences
	 */
	public ArrayList<Integer> extractPreferenceFeatures(RoleSpan rolespan, EventSpan eventspan){
		
		Lattice lattice = eventspan.getLattice();
		ArrayList<Integer> features = new ArrayList<Integer>();
		this.extractPreferenceFeatures_boolean(rolespan, eventspan, lattice, features);
		this.extractPreferenceFeatures_count(rolespan, eventspan, lattice, features);
		return features;
		
	}
	
	/**
	 * Extract count preferences
	 * @param rolespan the role span
	 * @param eventspan the event span
	 * @param lattice the lattice
	 * @param pfs
	 */
	private void extractPreferenceFeatures_count(RoleSpan rolespan, EventSpan eventspan, Lattice lattice, ArrayList<Integer> pfs){
		
		RoleSpan curr = rolespan;
		HashMap<Role, Integer> roleCountMap = new HashMap<Role, Integer>();
		while(curr!=null){
			Role role = curr.getRole();
			if(!roleCountMap.containsKey(role))
				roleCountMap.put(role, 0);
			int oldCount = roleCountMap.get(role);
			roleCountMap.put(role, oldCount+1);
			curr = curr.getPrev();
		}
		
		Iterator<Role> roles = roleCountMap.keySet().iterator();
		while(roles.hasNext()){
			Role role = roles.next();
			int count = roleCountMap.get(role);
			ArrayList<Role> argnodes = new ArrayList<Role>();
			argnodes.add(role);
			int feature = this.getSatisfiedPreferenceFeature_Count(argnodes, new ArrayList<Integer>(), new ArrayList<Integer>(), count);
			if(feature!=-1)
				pfs.add(feature);
		}
	}
	
	/**
	 * Extract boolean preferences
	 * @param rolespan the role span
	 * @param eventspan the event span
	 * @param lattice the lattice
	 * @param pfs existing preferences
	 */
	private void extractPreferenceFeatures_boolean(RoleSpan rolespan, EventSpan eventspan, Lattice lattice, ArrayList<Integer> pfs){
		
		if(rolespan.getPrev()==null)
			return;
		
		Word[] allwords = eventspan.getWords();
		RoleSpan prevspan = rolespan.getPrev();
		
		int bIndex, eIndex;
		
		//for the node...
		{
			ArrayList<Role> args_node = new ArrayList<Role>();
			args_node.add(rolespan.getRole());
			
			bIndex = rolespan.getBIndex();
			eIndex = rolespan.getEIndex();
			if(bIndex!=allwords.length){
				for(Word word : allwords){
					ArrayList<Integer> words = new ArrayList<Integer>();
					words.add(word.getId());
					ArrayList<Type> types = lattice.getTypes(bIndex, eIndex);
					int feature = this.getSatisfiedPreferenceFeature_Boolean(args_node, types, words);
					if(feature!=-1)
						pfs.add(feature);
				}
			}
		}
		
		//for the path...
		{
			ArrayList<Role> args_path = new ArrayList<Role>();
			args_path.add(prevspan.getRole());
			args_path.add(rolespan.getRole());
			
			ArrayList<Type> types = new ArrayList<Type>();
			
			bIndex = prevspan.getEIndex();
			eIndex = rolespan.getBIndex();
			int word_1 = -1;
			int word;
			for(int i = bIndex; i<eIndex; i++){
				word = allwords[i].getId();
				ArrayList<Integer> words_unigram = new ArrayList<Integer>();
				words_unigram.add(word);
				int feature1 = this.getSatisfiedPreferenceFeature_Boolean(args_path, types, words_unigram);
				if(feature1!=-1)
					pfs.add(feature1);
				
				ArrayList<Integer> words_bigram = new ArrayList<Integer>();
				words_bigram.add(word_1);
				words_bigram.add(word);
				int feature2 = this.getSatisfiedPreferenceFeature_Boolean(args_path, types, words_bigram);
				if(feature2!=-1)
					pfs.add(feature2);
				
				if(i!=eIndex-1){
					int word1 = allwords[i+1].getId();
					ArrayList<Integer> words_trigram = new ArrayList<Integer>();
					words_trigram.add(word_1);
					words_trigram.add(word);
					words_trigram.add(word1);
					int feature3 = this.getSatisfiedPreferenceFeature_Boolean(args_path, types, words_trigram);
					if(feature3!=-1)
						pfs.add(feature3);
				} else {
					int word1 = -1;
					ArrayList<Integer> words_trigram = new ArrayList<Integer>();
					words_trigram.add(word_1);
					words_trigram.add(word);
					words_trigram.add(word1);
					int feature3 = this.getSatisfiedPreferenceFeature_Boolean(args_path, types, words_trigram);
					if(feature3!=-1)
						pfs.add(feature3);
				}
				
				word_1 = word;
			}
			word = -2;
			
			ArrayList<Integer> words_bigram = new ArrayList<Integer>();
			words_bigram.add(word_1);
			words_bigram.add(word);
			int feature2 = this.getSatisfiedPreferenceFeature_Boolean(args_path, types, words_bigram);
			if(feature2!=-1)
				pfs.add(feature2);
			
			int feature = this.getSatisfiedPreferenceFeature_Boolean(args_path, types, new ArrayList<Integer>());
			if(feature!=-1)
				pfs.add(feature);
		}
		{
			ArrayList<Role> args_path = new ArrayList<Role>();
			args_path.add(Role._dummy);
			args_path.add(rolespan.getRole());
			
			ArrayList<Type> types = new ArrayList<Type>();
			
			bIndex = prevspan.getEIndex();
			eIndex = rolespan.getBIndex();
			int word_1 = -1;
			int word;
			for(int i = bIndex; i<eIndex; i++){
				word = allwords[i].getId();
				ArrayList<Integer> words_unigram = new ArrayList<Integer>();
				words_unigram.add(word);
				int feature1 = this.getSatisfiedPreferenceFeature_Boolean(args_path, types, words_unigram);
				if(feature1!=-1)
					pfs.add(feature1);
				
				ArrayList<Integer> words_bigram = new ArrayList<Integer>();
				words_bigram.add(word_1);
				words_bigram.add(word);
				int feature2 = this.getSatisfiedPreferenceFeature_Boolean(args_path, types, words_bigram);
				if(feature2!=-1)
					pfs.add(feature2);
				
				if(i!=eIndex-1){
					int word1 = allwords[i+1].getId();
					ArrayList<Integer> words_trigram = new ArrayList<Integer>();
					words_trigram.add(word_1);
					words_trigram.add(word);
					words_trigram.add(word1);
					int feature3 = this.getSatisfiedPreferenceFeature_Boolean(args_path, types, words_trigram);
					if(feature3!=-1)
						pfs.add(feature3);
				} else {
					int word1 = -1;
					ArrayList<Integer> words_trigram = new ArrayList<Integer>();
					words_trigram.add(word_1);
					words_trigram.add(word);
					words_trigram.add(word1);
					int feature3 = this.getSatisfiedPreferenceFeature_Boolean(args_path, types, words_trigram);
					if(feature3!=-1)
						pfs.add(feature3);
				}
				
				word_1 = word;
			}
			word = -2;
			
			ArrayList<Integer> words_bigram = new ArrayList<Integer>();
			words_bigram.add(word_1);
			words_bigram.add(word);
			int feature2 = this.getSatisfiedPreferenceFeature_Boolean(args_path, types, words_bigram);
			if(feature2!=-1)
				pfs.add(feature2);
		}
		{
			ArrayList<Role> args_path = new ArrayList<Role>();
			args_path.add(prevspan.getRole());
			args_path.add(Role._dummy);
			
			ArrayList<Type> types = new ArrayList<Type>();
			
			bIndex = prevspan.getEIndex();
			eIndex = rolespan.getBIndex();
			int word_1 = -1;
			int word;
			for(int i = bIndex; i<eIndex; i++){
				word = allwords[i].getId();
				ArrayList<Integer> words_unigram = new ArrayList<Integer>();
				words_unigram.add(word);
				int feature1 = this.getSatisfiedPreferenceFeature_Boolean(args_path, types, words_unigram);
				if(feature1!=-1)
					pfs.add(feature1);
				
				ArrayList<Integer> words_bigram = new ArrayList<Integer>();
				words_bigram.add(word_1);
				words_bigram.add(word);
				int feature2 = this.getSatisfiedPreferenceFeature_Boolean(args_path, types, words_bigram);
				if(feature2!=-1)
					pfs.add(feature2);
				
				if(i!=eIndex-1){
					int word1 = allwords[i+1].getId();
					ArrayList<Integer> words_trigram = new ArrayList<Integer>();
					words_trigram.add(word_1);
					words_trigram.add(word);
					words_trigram.add(word1);
					int feature3 = this.getSatisfiedPreferenceFeature_Boolean(args_path, types, words_trigram);
					if(feature3!=-1)
						pfs.add(feature3);
				} else {
					int word1 = -1;
					ArrayList<Integer> words_trigram = new ArrayList<Integer>();
					words_trigram.add(word_1);
					words_trigram.add(word);
					words_trigram.add(word1);
					int feature3 = this.getSatisfiedPreferenceFeature_Boolean(args_path, types, words_trigram);
					if(feature3!=-1)
						pfs.add(feature3);
				}
				
				word_1 = word;
			}
			word = -2;
			
			ArrayList<Integer> words_bigram = new ArrayList<Integer>();
			words_bigram.add(word_1);
			words_bigram.add(word);
			int feature2 = this.getSatisfiedPreferenceFeature_Boolean(args_path, types, words_bigram);
			if(feature2!=-1)
				pfs.add(feature2);
		}
		
		//TODO: add more preference features..
		
		this.extractPreferenceFeatures_boolean(prevspan, eventspan, lattice, pfs);
	}
	
	/**
	 * This method will read in the preferences
	 * @param filename the name of the file from which the preferences will be read
	 * @param manager the manager
	 * @return the preference manager
	 * @throws FileNotFoundException
	 */
	public static PreferenceManager readPreferences(String filename, Manager manager) throws FileNotFoundException{
		
		PreferenceManager pm = new PreferenceManager();
		
		Scanner scan = new Scanner(new File(filename));
		while(scan.hasNext()){
			String line = scan.nextLine().trim();
			//ignore the empty lines..
			if(line.equals("") || line.startsWith("//") || line.startsWith("#"))
				continue;
			
			Preference preference;
			
			StringTokenizer st = new StringTokenizer(line);
			
			int num_tokens = st.countTokens();
			
			String preference_type = st.nextToken();
			
			if(preference_type.equals("Count")){
				
				int type = _COUNT;
				
				// TYPE arg1 arg2 type1 type2 input1 input2 input3 <= 5 1.0
				int[] fv = new int[1+3+(num_tokens-4)*2];
				
				int t = 0;
				fv[t++] = type;
				fv[t++] = 0;//args
				fv[t++] = 0;//types
				fv[t++] = 0;//inputs
				
				String token;
				for(int i = 1; i<=num_tokens-4; i++){
					token = st.nextToken();
					String[] fs = token.split("@");
					String f0 = fs[0];
					if(f0.equals("arg")){
						String[] argument_names = fs[1].split("\\|");
						fv[t++] = _ROLE;
						fv[t++] = manager.getRole(argument_names).getId();
						fv[1] ++;
					} else if(f0.equals("type")){
						String type_name = fs[1];
						fv[t++] = _TYPE;
						fv[t++] = manager.getType(type_name).getId();
						fv[2] ++;
					} else if(f0.equals("input")){
						String input_name = fs[1];
						fv[t++] = _INPUT;
						fv[t++] = manager.getStr(input_name);
						fv[3] ++;
					} else
						throw new RuntimeException("Not supported:"+f0+"\nat"+line);
				}
				
				IntArray key = new IntArray(fv);
				
				int k;
				if(pm._key2intMap.containsKey(key)){
					k = pm._key2intMap.get(key);
				} else {
					k = pm._key2intMap.size();
					pm._key2intMap.put(key, k);
				}
				
				int relation;
				String r = st.nextToken();
				if(r.equals("<="))
					relation = CountPreference._LTEQ;
				else if(r.equals(">="))
					relation = CountPreference._GTEQ;
				else if(r.equals("="))
					relation = CountPreference._EQAL;
				else 
					throw new IllegalStateException("Sorry, the relation for CountPreference is not recognized in:"+line);
				
				int count = Integer.parseInt(st.nextToken()); // e.g. 5
				double preferenceScore = Double.parseDouble(st.nextToken()); // e.g. -1.0
				
				preference = new CountPreference(k, relation, count);
				pm._preference2scoreMap.put(k, preferenceScore);
				pm._preferenceMap.put(k, preference);
			} else if(preference_type.equals("Boolean")){
				int type = _BOOLEAN;

				// TYPE arg1 arg2 type1 type2 input1 input2 input3 true 1.0
				int[] fv = new int[1+3+(num_tokens-3)*2];
				
				int t = 0;
				fv[t++] = type;
				fv[t++] = 0;//args
				fv[t++] = 0;//types
				fv[t++] = 0;//inputs
				
				String token;
				for(int i = 1; i<=num_tokens-3; i++){
					token = st.nextToken();
					String[] fs = token.split("@");
					String f0 = fs[0];
					if(f0.equals("arg")){
						fv[t++] = _ROLE;
						if(fs[1].equals("ARG_ANYTHING")){
							fv[t++] = -100;
						} else {
							String[] role_names = fs[1].split("\\|");
							fv[t++] = manager.getRole(role_names).getId();
						}
						fv[1] ++;
					} else if(f0.equals("type")){
						String type_name = fs[1];
						fv[t++] = _TYPE;
						fv[t++] = manager.getType(type_name).getId();
						fv[2] ++;
					} else if(f0.equals("input")){
						String input_name = fs[1];
						fv[t++] = _INPUT;
						if(input_name.equals("WORD_BEGIN"))
							fv[t++] = -1;
						else if(input_name.equals("WORD_END"))
							fv[t++] = -2;
						else
							fv[t++] = manager.getStr(input_name);
						fv[3] ++;
					} else {
						throw new RuntimeException("Not supported:"+f0+"\nat"+line);
					}
				}
				
				IntArray key = new IntArray(fv);
				
				int k;
				if(pm._key2intMap.containsKey(key)){
					k = pm._key2intMap.get(key);
				} else {
					k = pm._key2intMap.size();
					pm._key2intMap.put(key, k);
				}

				boolean value = Boolean.parseBoolean(st.nextToken());
				double preferenceScore = Double.parseDouble(st.nextToken());
				
				preference = new BooleanPreference(k, value);
				pm._preference2scoreMap.put(k, preferenceScore);
				pm._preferenceMap.put(k, preference);
			} else
				throw new IllegalStateException("Sorry, preference type "+preference_type+" not yet supported:"+line);
			
		}
		
		System.err.println("There are "+pm.getNumPreferences()+" preferences.");
		
		return pm;
	}
	
}
