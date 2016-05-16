package com.statnlp.dp.model.div;

import java.util.ArrayList;

import com.statnlp.commons.types.Sentence;
import com.statnlp.dp.DependInstance;
import com.statnlp.dp.Transformer;
import com.statnlp.dp.commons.PositionType;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.UnnamedDependency;

public class DIVInstance extends DependInstance {

	private boolean invalid;
	private String[] predEntities; //0 index is rootl
	
	public DIVInstance(int instanceId, double weight) {
		super(instanceId, weight);
	}
	
	public DIVInstance(int instanceId, double weight, Sentence sent) {
		super(instanceId, weight, sent);
	}

	public DIVInstance(int instanceId, double weight, Sentence sentence, ArrayList<UnnamedDependency> dependencies, Tree dependencyRoot, Tree output) {
		super(instanceId,weight,sentence,dependencies,dependencyRoot,output);
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
		
		if(predEntities==null)
			di.predEntities = null;
		else di.predEntities = predEntities.clone();
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
	
	
	@Override
	public String[] toEntities(Tree spanRoot){
//		System.err.println(spanRoot.pennString());
		ArrayList<ArrayList<PositionType>> esArr = new ArrayList<ArrayList<PositionType>>();
		for(int i=0;i<this.sentence.length();i++){
			ArrayList<PositionType> list = new ArrayList<PositionType>();
			esArr.add(list);
		}
		String[] es = new String[this.sentence.length()];
		
		String[] res = new String[this.sentence.length()];
		es[0] = O_TYPE;
		res[0] = es[0];
		ArrayList<PositionType> rootPosType = new ArrayList<PositionType>();
		rootPosType.add(new PositionType(O_TYPE, 1));
		esArr.set(0, rootPosType);
		this.findAllE(spanRoot, esArr);
		for(int i=1;i<es.length;i++){
			if(esArr.get(i).size()==0) es[i] = O_TYPE;
			else if(esArr.get(i).size()==1) es[i] = esArr.get(i).get(0).getType();
			else if(esArr.get(i).size()==2){
				es[i] = esArr.get(i).get(0).getLength() >= esArr.get(i).get(1).getLength()? esArr.get(i).get(0).getType(): esArr.get(i).get(1).getType();
			}else{
				throw new RuntimeException("one word cannot have more two types");
			}
			if(!es[i-1].equals(es[i]) && !es[i].equals(O_TYPE)){
				res[i] = E_B_PREFIX + es[i];
			}else if(es[i-1].equals(es[i]) && !es[i].equals(O_TYPE)){
				res[i] = E_I_PREFIX + es[i];
			}else if(es[i].equals(O_TYPE)){
				res[i] = O_TYPE;
			}
		}
		
		
		return res;
	}
	
	
	private void findAllE(Tree current, ArrayList<ArrayList<PositionType>> esArr){
		CoreLabel label = (CoreLabel)(current.label());
		String[] info = label.value().split(",");
		int l = Integer.valueOf(info[0]);
		int r = Integer.valueOf(info[1]);
		String type = info[4];
		if(!type.startsWith(PARENT_IS) && !type.equals(OE) && !type.equals(ONE) ){
			for(int i=l;i<=r;i++){
				ArrayList<PositionType> posList = esArr.get(i);
				posList.add(new PositionType(type, l-r+1));
			}
			return;
		}else if(type.equals(ONE)){
			return;
		}else{
			for(Tree child: current.children()){
				findAllE(child,esArr);
			}
		}
	}

	public String[] getPredEntities() {
		return predEntities;
	}

	public void setPredEntities(String[] predEntities) {
		this.predEntities = predEntities;
	}

	
	
}
