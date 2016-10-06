package com.statnlp.projects.dep.model.mhp;

import java.util.ArrayList;

import com.statnlp.commons.types.Sentence;
import com.statnlp.projects.dep.DependInstance;
import com.statnlp.projects.dep.Transformer;
import com.statnlp.projects.dep.utils.DPConfig;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.UnnamedDependency;

public class MHPInstance extends DependInstance {

	private static final long serialVersionUID = -7064354495073697344L;
	
	private String NONE = DPConfig.NONE;
	
	
	public MHPInstance(int instanceId, double weight) {
		super(instanceId, weight);
	}

	public MHPInstance(int instanceId, double weight, Sentence sentence) {
		super(instanceId, weight,sentence);
	}
	
	public MHPInstance(int instanceId, double weight, Sentence sentence, ArrayList<UnnamedDependency> dependencies, Tree dependencyRoot,Tree output) {
		super(instanceId, weight,sentence,dependencies,dependencyRoot,output);
		
	}
	
	
	public MHPInstance duplicate() {
		MHPInstance di = new MHPInstance(this._instanceId, this._weight,this.sentence);
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
	
	
	public ArrayList<UnnamedDependency> toDependencies(Tree root){
		//This root is the span root
		ArrayList<UnnamedDependency> list = new ArrayList<UnnamedDependency>();
		findDep(root, list);
		return list;
	}

	
	/**
	 * Find all the dependencies from MHP
	 */
	private void findDep(Tree current, ArrayList<UnnamedDependency> dependencies){
		CoreLabel label = (CoreLabel)(current.label());
		String[] info = label.value().split(",");
		int pa_leftIndex = Integer.valueOf(info[0]);
		int pa_rightIndex = Integer.valueOf(info[1]);
		int pa_completeness = Integer.valueOf(info[2]);
		int pa_direction = Integer.valueOf(info[3]);  
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
			findDep(child,dependencies);
		}
	}
	
	
	public String[] toEntities(Tree spanRoot){
		String[] es = new String[this.sentence.length()];
		String[] res = new String[this.sentence.length()];
		res[0] = O_TYPE;
		es[0] = ONE;
		System.err.println("finding....");
		this.findAll(es, spanRoot);
		for(int i=1;i<es.length;i++){
			if(es[i].equals(ONE)){
				res[i] = O_TYPE;
			}else{
				if(es[i].equals(NONE)) throw new RuntimeException("Invalid decoding..");
				if(es[i-1].equals(ONE))
					res[i] = E_B_PREFIX+es[i];
				else{
					if(es[i].equals(es[i-1]))
						res[i] = E_I_PREFIX+es[i];
					else res[i] = E_B_PREFIX+es[i];
				}
			}
		}
		return res;
	}
	
	
	private void findAll(String[] es, Tree current){
		CoreLabel label = (CoreLabel)(current.label());
		String[] info = label.value().split(",");
		int l = Integer.valueOf(info[0]);
		int r = Integer.valueOf(info[1]);
		int dir = Integer.valueOf(info[3]);
		String lmt = info[5];
		if(l==r && dir==1){
			es[l] = lmt;
		}else{
			for(Tree child: current.children()){
				findAll(es,child);
			}
		}
	}

	
	

}
