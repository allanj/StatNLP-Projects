package com.statnlp.dp.model.div;

import java.util.ArrayList;

import com.statnlp.commons.types.Sentence;
import com.statnlp.dp.DependInstance;
import com.statnlp.dp.ModelInstance;
import com.statnlp.dp.Transformer;
import com.statnlp.dp.utils.DPConfig;
import com.statnlp.dp.utils.DPConfig.MODEL;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.UnnamedDependency;

public class DIVInstance extends DependInstance {

	
	public DIVInstance(int instanceId, double weight) {
		super(instanceId, weight);
	}
	
	public DIVInstance(int instanceId, double weight, Sentence sent) {
		super(instanceId, weight, sent);
	}

	public DIVInstance(int instanceId, double weight, Sentence sentence, ArrayList<UnnamedDependency> dependencies, Tree dependencyRoot, Transformer transform) {
		super(instanceId,weight,sentence,dependencies,dependencyRoot,transform);
	}
	
	
	private static final long serialVersionUID = 2175742491946682505L;

	@Override
	public DIVInstance duplicate() {
		DIVInstance di = new DIVInstance(this._instanceId, this._weight,this.sentence);
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
	public ArrayList<UnnamedDependency> toDependencies(Tree root){
		//This root is the span root
		ArrayList<UnnamedDependency> list = new ArrayList<UnnamedDependency>();
		findAllDependencies(root, list);
		return list;
	}

	
	private void findAllDependencies(Tree current, ArrayList<UnnamedDependency> dependencies){
		CoreLabel label = (CoreLabel)(current.label());
		String[] info = label.value().split(",");
		int pa_leftIndex = Integer.valueOf(info[0]);
		int pa_rightIndex = Integer.valueOf(info[1]);
		int pa_direction = Integer.valueOf(info[2]);  //previously the position for direction is 2.
		int pa_completeness = Integer.valueOf(info[3]);
		String type = info[4];
		if(pa_rightIndex==pa_leftIndex) return;
		if(pa_rightIndex<pa_leftIndex) 
			throw new RuntimeException("Your tree is wrongly constructed. The left Index should be smaller than the right in a span");
		CoreLabel governorLabel = new CoreLabel();
		CoreLabel dependentLabel = new CoreLabel();
//		System.err.println("left:"+pa_leftIndex+", right:"+pa_rightIndex);
		if(pa_completeness==0 && type.startsWith(PARENT_IS)){
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
	@Override
	public String[] toEntities(Tree spanRoot){
//		System.err.println(spanRoot.pennString());
		
		String[] es = new String[this.sentence.length()];
		boolean[] set = new boolean[this.sentence.length()];
		es[0] = O_TYPE;
		for(int i=1;i<es.length;i++) {es[i] = "UNDEF"; set[i] =false;}
		this.findAllONE(es, spanRoot);
		resCovered = false;
		this.findAllE(es, spanRoot);
		for(int i=1;i<es.length;i++){
			if(es[i].equals("UNDEF")) {
				System.err.println("Some spans are not finalized the type. Info:\n\t"+this._instanceId+"\n"+this.getInput().toString());
				es[i] = O_TYPE;
			}
			if((es[i-1].startsWith(E_I_PREFIX) || es[i-1].startsWith(E_B_PREFIX)) && es[i].startsWith(E_B_PREFIX) && es[i-1].substring(2, es[i-1].length()).equals(es[i].substring(2, es[i].length())))
				es[i] = E_I_PREFIX+es[i-1].substring(2, es[i-1].length());
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
			if((es[l].startsWith(E_B_PREFIX) || es[l].startsWith(E_I_PREFIX)) && !es[l].substring(2).equals(type)) resCovered = true;
			es[l] = E_B_PREFIX+type;
			for(int i=l+1;i<=r;i++){
				if((es[i].startsWith(E_B_PREFIX) || es[i].startsWith(E_I_PREFIX)) && !es[i].substring(2).equals(type)) resCovered = true;
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


}
