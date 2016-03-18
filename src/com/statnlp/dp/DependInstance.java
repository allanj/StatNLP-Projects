package com.statnlp.dp;

import java.util.ArrayList;
import java.util.HashMap;

import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;
import com.statnlp.hybridnetworks.NetworkConfig;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.UnnamedDependency;

public class DependInstance extends Instance {

	
	private static final long serialVersionUID = 7609419266155327236L;
	protected Sentence sentence;
	protected Tree output; //output is the span tree;
	protected Tree prediction; //prediction is also the dependency spanning tree
	private ArrayList<UnnamedDependency> dependencies; //only for the output
	private Tree dependencyRoot;
	public int[] unValidNum;
	public int[][] outsideHeads;
	public int[] entityNum;
	public int continousNum;
	private Transformer transform;
	
	private boolean haveEntity = false;
	
	
	
	public DependInstance(int instanceId, double weight) {
		super(instanceId, weight);
		// TODO Auto-generated constructor stub
	}
	
	public DependInstance(int instanceId, double weight, Sentence sentence) {
		super(instanceId, weight);
		this.sentence = sentence;
	}
	
	public DependInstance(int instanceId, double weight, Sentence sentence, ArrayList<UnnamedDependency> dependencies, Tree dependencyRoot, Transformer transform) {
		super(instanceId, weight);
		this.sentence = sentence;
		this.dependencies = dependencies;
		this.dependencyRoot = dependencyRoot.deepCopy();
		this.transform = transform;
		this.output = this.transform.toSpanTree(dependencyRoot.deepCopy(), sentence);
		
	}
	
	public void setHaveEntity(){
		this.haveEntity = true;
		this.unValidNum = new int[NetworkConfig.typeMap.size()];
		this.outsideHeads = new int[NetworkConfig.typeMap.size()][10];
		entityNum = new int[NetworkConfig.typeMap.size()];
		continousNum = 0;
	}
	
	public boolean haveEntity(){return this.haveEntity;}
	
	
	
	@Override
	public int size() {
		//Return the length of the input sentence
		return this.sentence.length();
	}

	@SuppressWarnings("unchecked")
	@Override
	public DependInstance duplicate() {
		DependInstance di = new DependInstance(this._instanceId, this._weight,this.sentence);
		if(output==null)
			di.output = null;
		else di.output = output.deepCopy();
		
		if(prediction==null)
			di.prediction = null;
		else di.prediction = prediction.deepCopy();
		
		if(dependencies==null) di.dependencies = null;
		else di.dependencies = (ArrayList<UnnamedDependency>) dependencies.clone();
		
		if(dependencyRoot==null)
			di.dependencyRoot = null;
		else di.dependencyRoot = dependencyRoot.deepCopy();
		return di;
	}

	@Override
	public void removeOutput() {
		this.output = null;
	}

	@Override
	public void removePrediction() {
		this.prediction = null;

	}

	@Override
	public Sentence getInput() {
		// TODO Auto-generated method stub
		return this.sentence;
	}

	@Override
	public Tree getOutput() {
		return this.output;
	}

	@Override
	public Tree getPrediction() {
		return this.prediction;
	}

	@Override
	public boolean hasOutput() {
		return this.output!=null;
	}

	@Override
	public boolean hasPrediction() {
		return this.prediction!=null;
	}

	public ArrayList<UnnamedDependency> getDependencies(){
		return this.dependencies;
	}
	
	@Override
	public void setPrediction(Object o) {
		Tree df = (Tree)o;
		this.prediction = df;

	}
	
	public HashMap<Integer, ArrayList<Integer>> getDependencyMap(ArrayList<UnnamedDependency> dependencies){
		return this.transform.dependencies2Map(dependencies);
	}
	
	public ArrayList<UnnamedDependency> toDependencies(Tree root){
		//This root is the span root
		ArrayList<UnnamedDependency> list = new ArrayList<UnnamedDependency>();
		findAllDependencies(root, list);
		return list;
	}

	
	/**
	 * Find all the dependencies from our span tree and output it as the dependencies list
	 * @param current : The span tree used for extracting the dependencies and entites information
	 * @param dependencies : dependencies list.
	 */
	private void findAllDependencies(Tree current, ArrayList<UnnamedDependency> dependencies){
		CoreLabel label = (CoreLabel)(current.label());
		String[] info = label.value().split(",");
		int pa_leftIndex = Integer.valueOf(info[0]);
		int pa_rightIndex = Integer.valueOf(info[1]);
		int pa_direction = Integer.valueOf(info[2]);
		int pa_completeness = Integer.valueOf(info[3]);
		String type = null;
		if(info.length>4)
			type = info[4];
		if(pa_rightIndex==pa_leftIndex) return;
		if(pa_rightIndex<pa_leftIndex) 
			throw new RuntimeException("Your tree is wrongly constructed. The left Index should be smaller than the right in a span");
		CoreLabel governorLabel = new CoreLabel();
		CoreLabel dependentLabel = new CoreLabel();
//		System.err.println("left:"+pa_leftIndex+", right:"+pa_rightIndex);
		if(pa_completeness==0 && ( (info.length>4 && type.startsWith("pae")) || info.length==4) ){
			if(pa_direction==0){
				governorLabel.setSentIndex(pa_rightIndex);
				governorLabel.setValue("index:"+pa_rightIndex);
				dependentLabel.setSentIndex(pa_leftIndex);
				dependentLabel.setValue("index:"+pa_leftIndex);
			}else{
				governorLabel.setSentIndex(pa_leftIndex);
				governorLabel.setValue("index:"+pa_leftIndex);
				dependentLabel.setSentIndex(pa_rightIndex);
				dependentLabel.setValue("index:"+pa_rightIndex);
			}
			dependencies.add(new UnnamedDependency(governorLabel, dependentLabel));
			
		}
		for(int i=0;i<current.children().length;i++){
			Tree child = current.getChild(i);
			findAllDependencies(child,dependencies);
		}
	}
	
	
	/**
	 * This one is only used by unique model
	 * @param spanRoot
	 * @return
	 */
	public String[] toEntities(Tree spanRoot){
//		System.err.println(spanRoot.pennString());
		
		String[] es = new String[this.sentence.length()];
		boolean[] set = new boolean[this.sentence.length()];
		es[0] = "O";
		for(int i=1;i<es.length;i++) {es[i] = "UNDEF"; set[i] =false;}
		findAllONE(es, spanRoot);
		findAllE(es, spanRoot);
		//System.err.println(Arrays.toString(es));
		for(int i=1;i<es.length;i++){
			if(es[i].equals("UNDEF")) {
				System.err.println("Some spans are not finalized the type. Info:\n\t"+this._instanceId+"\n"+this.getInput().toString());
				es[i] = "O";
			}
			if((es[i-1].startsWith("I-") || es[i-1].startsWith("B-")) && es[i].startsWith("B-") && es[i-1].substring(2, es[i-1].length()).equals(es[i].substring(2, es[i].length())))
				es[i] = "I-"+es[i-1].substring(2, es[i-1].length());
		}
//		System.err.println(Arrays.toString(es));
		return es;
	}
	
	private void findAllONE(String[] es, Tree current){
		CoreLabel label = (CoreLabel)(current.label());
		String[] info = label.value().split(",");
		int l = Integer.valueOf(info[0]);
		int r = Integer.valueOf(info[1]);
		String type = info[4];
		if(type.equals("ONE")){
			for(int i=l;i<=r;i++) es[i] = "O";
			return;
		}else if(!type.startsWith("pae") && !type.equals("OE") && !type.equals("ONE")){
			return;
		}else{
			for(Tree child: current.children()){
				findAllONE(es,child);
			}
		}
	}
	
	private void findAllE(String[] es, Tree current){
		CoreLabel label = (CoreLabel)(current.label());
		String[] info = label.value().split(",");
		int l = Integer.valueOf(info[0]);
		int r = Integer.valueOf(info[1]);
		String type = info[4];
		if(!type.startsWith("pae") && !type.equals("OE") && !type.equals("ONE")){
			es[l] = "B-"+type;
			for(int i=l+1;i<=r;i++)
				es[i]="I-"+type;
			return;
		}else if(type.equals("ONE")){
			return;
		}else{
			for(Tree child: current.children()){
				findAllE(es,child);
			}
		}
	}

	
	
	

}
