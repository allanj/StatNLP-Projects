package com.statnlp.projects.dep.model.hyperedge.stat;

import java.util.List;

import com.statnlp.projects.dep.model.hyperedge.HPEInstance;
import com.statnlp.projects.dep.model.hyperedge.Span;

public class Analyzer {

	
	
	//check if head is the multitokens span.
	public static void checkMultiwordsHead(HPEInstance[] instances) {
		for (HPEInstance inst: instances) {
			//System.out.println("Current instance id: " + inst.getInstanceId()+ " " + inst.getInput().toString());
			for (int i = 1; i < inst.getOutput().size(); i++) {
				Span span = inst.getOutput().get(i);
				if (span.length() > 3 && span.headSpan.length() > 3) {
					System.out.println("current span: " + span.toString() + " and head Span: " + span.headSpan.toString());
				}
				
//				if(!span.label.form.equals("O") && !span.headSpan.label.form.equals("O") && span.length() > 2 && span.headSpan.length() > 2) {
//					System.out.println("current span: " + span.toString() + " and head Span: " + span.headSpan.toString());
//				}
			}
		}
	}
		
	private static String[] restoreEntityToken(List<Span> spans, int length) {
		String[] outputE = new String[length];
		for (int i = 0; i < spans.size(); i++) {
			Span span = spans.get(i); 
			for (int p = span.start; p <= span.end; p++) {
				if (!span.label.form.equals("O")) {
					if (p == span.start) {
						outputE[p] = "B-" + span.label.form;
					}else{
						outputE[p] = "I-" + span.label.form;
					}
				} else {
					outputE[p] = span.label.form;
				}
			}
		}
		return outputE;
	}
}
