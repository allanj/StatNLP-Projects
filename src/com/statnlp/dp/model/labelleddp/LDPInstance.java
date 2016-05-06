package com.statnlp.dp.model.labelleddp;

import java.util.ArrayList;
import java.util.HashMap;

import com.statnlp.commons.types.Sentence;
import com.statnlp.dp.ModelInstance;
import com.statnlp.dp.utils.DPConfig;
import com.statnlp.dp.utils.DPConfig.MODEL;

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
		String type = null;
		if(info.length>4)
			type = info[4];
		if(pa_rightIndex==pa_leftIndex) return;
		if(pa_rightIndex<pa_leftIndex) 
			throw new RuntimeException("Your tree is wrongly constructed. The left Index should be smaller than the right in a span");
		CoreLabel governorLabel = new CoreLabel();
		CoreLabel dependentLabel = new CoreLabel();
//		System.err.println("left:"+pa_leftIndex+", right:"+pa_rightIndex);
		if(pa_completeness==0 && ( (info.length>4 && type.startsWith("pae")) || info.length==4 || validModel()    ) ){
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
		es[0] = O_TYPE;
		for(int i=1;i<es.length;i++) {es[i] = "UNDEF"; set[i] =false;}
		if(DPConfig.currentModel.equals(MODEL.SIMPLE.name())){
			this.findAllE(es, spanRoot);
			for(int i=1;i<es.length;i++){
				if(es[i].equals("UNDEF")) {
					es[i] = O_TYPE;
				}
				if((es[i-1].startsWith(E_I_PREFIX) || es[i-1].startsWith(E_B_PREFIX)) && es[i].startsWith(E_B_PREFIX) && es[i-1].substring(2, es[i-1].length()).equals(es[i].substring(2, es[i].length())))
					es[i] = E_I_PREFIX+es[i-1].substring(2, es[i-1].length());
			}
		}else{
			this.findAllONE(es, spanRoot);
			this.findAllE(es, spanRoot);
			for(int i=1;i<es.length;i++){
				if(es[i].equals("UNDEF")) {
					System.err.println("Some spans are not finalized the type. Info:\n\t"+this._instanceId+"\n"+this.getInput().toString());
					es[i] = O_TYPE;
				}
				if((es[i-1].startsWith(E_I_PREFIX) || es[i-1].startsWith(E_B_PREFIX)) && es[i].startsWith(E_B_PREFIX) && es[i-1].substring(2, es[i-1].length()).equals(es[i].substring(2, es[i].length())))
					es[i] = E_I_PREFIX+es[i-1].substring(2, es[i-1].length());
			}
		}
		
		return es;
	}
	
	private void findAllONE(String[] es, Tree current){
		CoreLabel label = (CoreLabel)(current.label());
		String[] info = label.value().split(",");
		int l = Integer.valueOf(info[0]);
		int r = Integer.valueOf(info[1]);
		String type = info[4];
		if(type.equals(ONE)){
			for(int i=l;i<=r;i++) es[i] = O_TYPE;
			return;
		}else if(!type.startsWith(PARENT_IS) && !type.equals(OE) && !type.equals(ONE)){
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
		if(!type.startsWith(PARENT_IS) && !type.equals(OE) && !type.equals(ONE) && !type.equals(GO)){
			es[l] = E_B_PREFIX+type;
			for(int i=l+1;i<=r;i++){
				es[i]=E_I_PREFIX+type;
			}
				
			return;
		}else if(type.equals(ONE)){
			return;
		}else{
			for(Tree child: current.children()){
				findAllE(es,child);
			}
		}
	}

	
	
	private boolean validModel(){
		return DPConfig.currentModel.equals(MODEL.HYPEREDGE.name()) || DPConfig.currentModel.equals(MODEL.SIMPLE.name());
	}
	
	

}
