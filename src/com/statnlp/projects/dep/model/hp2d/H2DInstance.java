package com.statnlp.projects.dep.model.hp2d;

import java.util.ArrayList;

import com.statnlp.commons.types.Sentence;
import com.statnlp.projects.dep.DependInstance;
import com.statnlp.projects.dep.model.nerwknowndp.NKDInstance;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.UnnamedDependency;

public class H2DInstance extends DependInstance {

	private static final long serialVersionUID = 8752542415739928707L;

	
	private int[] head; //head array for each index
	
	public H2DInstance(int instanceId, double weight) {
		super(instanceId, weight);
	}
	
	public H2DInstance(int instanceId, double weight, Sentence sent) {
		super(instanceId, weight, sent);
	}

	public H2DInstance(int instanceId, double weight, Sentence sentence, ArrayList<UnnamedDependency> dependencies, Tree dependencyRoot, Tree output) {
		super(instanceId,weight,sentence,dependencies,dependencyRoot,output);
		toHead();
	}
	
	private void toHead(){
		head = new int[sentence.length()];
		head[0] = -1;
		if(dependencies==null){
			throw new RuntimeException("Cannot be converted to head since the dependency is null");
		}
		for(UnnamedDependency ud: dependencies){
			CoreLabel mo = (CoreLabel)ud.dependent();
			CoreLabel hl = (CoreLabel)ud.governor();
			head[mo.sentIndex()] = hl.sentIndex();
		}
	}
	
	public int[] getHead(){
		return this.head;
	}
	
	@Override
	public H2DInstance duplicate() {
		H2DInstance di = new H2DInstance(this._instanceId, this._weight,this.sentence);
		if(output==null)
			di.output = null;
		else di.output = output.deepCopy();
		
		if(prediction==null)
			di.prediction = null;
		else di.prediction = prediction.deepCopy();
		
		if(head==null)
			di.head = null;
		else di.head = head.clone();
		
		if(dependencies==null) di.dependencies = null;
		else di.dependencies = (ArrayList<UnnamedDependency>) dependencies.clone();
		
		if(dependencyRoot==null)
			di.dependencyRoot = null;
		else di.dependencyRoot = dependencyRoot.deepCopy();
		return di;
	}
	
}
