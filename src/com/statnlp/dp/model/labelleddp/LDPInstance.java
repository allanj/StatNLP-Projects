package com.statnlp.dp.model.labelleddp;

import java.util.ArrayList;
import java.util.HashMap;

import com.statnlp.commons.types.Sentence;
import com.statnlp.dp.ModelInstance;
import com.statnlp.dp.utils.DPConfig;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.UnnamedDependency;

public class LDPInstance extends ModelInstance {

	
	private static final long serialVersionUID = 7609419266155327236L;
	protected Sentence sentence;
	protected Tree output; //output is the span tree;
	protected Tree prediction; //prediction is also the dependency spanning tree
	protected ArrayList<UnnamedDependency> dependencies; //only for the output
	protected Tree dependencyRoot;
	public int[] unValidNum;
	public int[][] outsideHeads;
	public int[] entityNum;
	public int continousNum;
	
	
	protected boolean haveEntity = false;
	
	protected  String OE = DPConfig.OE;
	protected  String ONE = DPConfig.ONE;
	protected  String GO = DPConfig.GO;
	protected  String O_TYPE = DPConfig.O_TYPE;
	protected  String E_B_PREFIX = DPConfig.E_B_PREFIX;
	protected  String E_I_PREFIX = DPConfig.E_I_PREFIX;
	protected  String PARENT_IS = DPConfig.PARENT_IS;
	
	
	public LDPInstance(int instanceId, double weight) {
		super(instanceId, weight);
		// TODO Auto-generated constructor stub
	}
	
	public LDPInstance(int instanceId, double weight, Sentence sentence) {
		super(instanceId, weight);
		this.sentence = sentence;
	}
	
	public LDPInstance(int instanceId, double weight, Sentence sentence, ArrayList<UnnamedDependency> dependencies, Tree dependencyRoot, Tree output) {
		super(instanceId, weight);
		this.sentence = sentence;
		this.dependencies = dependencies;
		this.dependencyRoot = dependencyRoot.deepCopy();
		this.output = output;
	}
	
	public void setHaveEntity(HashMap<String, Integer> typeMap){
		this.haveEntity = true;
		this.unValidNum = new int[typeMap.size()];
		this.outsideHeads = new int[typeMap.size()][10];
		entityNum = new int[typeMap.size()];
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
	public LDPInstance duplicate() {
		LDPInstance di = new LDPInstance(this._instanceId, this._weight,this.sentence);
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

	
	@Override
	public void setPrediction(Object o) {
		Tree df = (Tree)o;
		this.prediction = df;

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
	 * The index of direction and completeness changed...remember to modify other classes.
	 */
	private void findAllDependencies(Tree current, ArrayList<UnnamedDependency> dependencies){
		CoreLabel label = (CoreLabel)(current.label());
		String[] info = label.value().split(",");
		int pa_leftIndex = Integer.valueOf(info[0]);
		int pa_rightIndex = Integer.valueOf(info[1]);
		int pa_direction = Integer.valueOf(info[2]);  //previously the position for direction is 2.
		int pa_completeness = Integer.valueOf(info[3]);
		String depLabel = info[4];
		if(pa_rightIndex==pa_leftIndex) return;
		if(pa_rightIndex<pa_leftIndex) 
			throw new RuntimeException("Your tree is wrongly constructed. The left Index should be smaller than the right in a span");
		CoreLabel governorLabel = new CoreLabel();
		CoreLabel dependentLabel = new CoreLabel();
//		System.err.println("left:"+pa_leftIndex+", right:"+pa_rightIndex);
		if(pa_completeness==0 ){
			if(pa_direction==0){
				governorLabel.setSentIndex(pa_rightIndex);
				governorLabel.setValue("index:"+pa_rightIndex);
				dependentLabel.setSentIndex(pa_leftIndex);
				dependentLabel.setValue("index:"+pa_leftIndex);
				dependentLabel.setTag(depLabel);
			}else{
				governorLabel.setSentIndex(pa_leftIndex);
				governorLabel.setValue("index:"+pa_leftIndex);
				dependentLabel.setSentIndex(pa_rightIndex);
				dependentLabel.setValue("index:"+pa_rightIndex);
				dependentLabel.setTag(depLabel);
			}
			dependencies.add(new UnnamedDependency(governorLabel, dependentLabel));
			
		}
		for(int i=0;i<current.children().length;i++){
			Tree child = current.getChild(i);
			findAllDependencies(child,dependencies);
		}
	}

	/**
	 * Useless since we don't have entities
	 * @param spanRoot
	 * @return
	 */
	@Override
	public String[] toEntities(Tree spanRoot) {
		// TODO Auto-generated method stub
		return null;
	}
	
	

	
	
	
	

}
