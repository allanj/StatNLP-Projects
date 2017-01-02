package com.statnlp.projects.entity.semi;

import java.util.ArrayList;
import java.util.List;

import com.statnlp.commons.types.Sentence;

public class SemiCRFInstance extends BaseInstance<SemiCRFInstance, Sentence, List<Span>> {
	
	private static final long serialVersionUID = -5338701879189642344L;
	
	private List<List<Span>> topKOutput;
	
	public SemiCRFInstance(int instanceId, Sentence input, List<Span> output){
		this(instanceId, 1.0, input, output);
	}
	
	public SemiCRFInstance(int instanceId, double weight) {
		this(instanceId, weight, null, null);
	}
	
	public SemiCRFInstance(int instanceId, double weight, Sentence input, List<Span> output){
		super(instanceId, weight);
		this.input = input;
		this.output = output;
	}
	
	
	public List<Span> duplicateOutput(){
		return output == null ? null : new ArrayList<Span>(output);
	}

	public List<Span> duplicatePrediction(){
		return prediction == null ? null : new ArrayList<Span>(prediction);
	}

	@Override
	public int size() {
		return getInput().length();
	}

	public String toString(){
		StringBuilder builder = new StringBuilder();
		builder.append(getInstanceId()+":");
		builder.append(input);
		if(hasOutput()){
			builder.append("\n");
			for(Span span: output){
				builder.append(span+"|");
			}
		}
		return builder.toString();
	}

	@Override
	public Sentence duplicateInput() {
		return this.getInput();
	}
	
	
	public String[] toEntities(List<Span> ss){
		String[] res = new String[this.input.length()];
		for(Span s: ss){
			for(int i= s.start; i<= s.end; i++){
				if (s.label.form.equals("O")) {
					res[i] = s.label.form;
				} else {
					res[i] = i == s.start? "B-"+s.label.form : "I-"+s.label.form;
				}
			}
		}
		return res;
	}
	
	public void setTopKPrediction(List<List<Span>> topKOutput) {
		this.topKOutput = topKOutput;
	}
	
	public List<List<Span>> getTopKPrediction() {
		return this.topKOutput;
	}

}
