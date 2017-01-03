package com.statnlp.projects.entity.semi;

import java.util.ArrayList;
import java.util.List;

import com.statnlp.commons.types.Sentence;

public class SemiCRFInstance extends BaseInstance<SemiCRFInstance, Sentence, List<SemiSpan>> {
	
	private static final long serialVersionUID = -5338701879189642344L;
	
	private List<List<SemiSpan>> topKOutput;
	
	public SemiCRFInstance(int instanceId, Sentence input, List<SemiSpan> output){
		this(instanceId, 1.0, input, output);
	}
	
	public SemiCRFInstance(int instanceId, double weight) {
		this(instanceId, weight, null, null);
	}
	
	public SemiCRFInstance(int instanceId, double weight, Sentence input, List<SemiSpan> output){
		super(instanceId, weight);
		this.input = input;
		this.output = output;
	}
	
	
	public List<SemiSpan> duplicateOutput(){
		return output == null ? null : new ArrayList<SemiSpan>(output);
	}

	public List<SemiSpan> duplicatePrediction(){
		return prediction == null ? null : new ArrayList<SemiSpan>(prediction);
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
			for(SemiSpan span: output){
				builder.append(span+"|");
			}
		}
		return builder.toString();
	}

	@Override
	public Sentence duplicateInput() {
		return this.getInput();
	}
	
	
	public String[] toEntities(List<SemiSpan> ss){
		String[] res = new String[this.input.length()];
		for(SemiSpan s: ss){
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
	
//	public String[] toEntities(List<Span> ss){
//		String[] res = new String[this.input.length()];
//		for(Span s: ss){
//			for(int i= s.start; i<= s.end; i++){
//				res[i] = s.label.form;
//			}
//		}
//		String[] finalRes = new String[this.input.length()];
//		String prev = "O";
//		for(int i=0;i<finalRes.length;i++){
//			if(prev.equals(res[i])){
//				if(prev.equals("O")){
//					finalRes[i] = "O";
//				}else{
//					finalRes[i] = "I-"+res[i];
//				}
//			}else{
//				if(prev.equals("O"))
//					finalRes[i] = "B-"+res[i];
//				else if(res[i].equals("O")){
//					finalRes[i] = "O";
//				}else{
//					finalRes[i] = "B-"+res[i];
//				}
//			}
//			prev = res[i];
//		}
//		//System.err.println(ss.toString());
//		
//		return finalRes;
//	}
	
	public void setTopKPrediction(List<List<SemiSpan>> topKOutput) {
		this.topKOutput = topKOutput;
	}
	
	public List<List<SemiSpan>> getTopKPrediction() {
		return this.topKOutput;
	}

}
