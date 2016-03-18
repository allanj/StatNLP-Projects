package semie.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringTokenizer;

import semie.core.Predictor;
import semie.pref.PreferenceManager;
import semie.types.DocumentReader;
import semie.types.Event;
import semie.types.EventAnnotation;
import semie.types.EventSpan;
import semie.types.FeatureManager;
import semie.types.Manager;
import semie.types.RoleSpan;

/**
 * the tester for preference modeling
 * @author luwei
 * @version 1.0
 */

public class PreferenceTester {
	
	public static void main(String args[])throws Exception{
		
		String configFileName = args[0];
		
		boolean considerEventSpanAsSubSentence = true;
		
		String eventSpanFileName_test = null;
		String eventName = null;
		String outputFileName = null;

		Scanner scan = new Scanner(new File(configFileName));
		while(scan.hasNextLine()){
			String line = scan.nextLine();
			if(line.trim().equals("") || line.startsWith("#"))
				continue;
			
			StringTokenizer st = new StringTokenizer(line);
			String type = st.nextToken();
			String arg = st.nextToken();
			if(type.equals("eventSpanFileName_test")){
				eventSpanFileName_test = arg;
			} else if(type.equals("eventName")){
				eventName = arg.toLowerCase();
			} else if(type.equals("assumeEventSpan")){
				considerEventSpanAsSubSentence = Boolean.parseBoolean(arg);
			} else if(type.equals("outputFileName")){
				outputFileName = arg;
			}
		}
		
		System.err.println("Reading the models from disk.");
		ObjectInputStream in;
		in = new ObjectInputStream(new FileInputStream("data/model/manager-"+eventName+".obj"));
		Manager manager = (Manager)in.readObject();
		in = new ObjectInputStream(new FileInputStream("data/model/fm-"+eventName+".obj"));
		FeatureManager fm = (FeatureManager)in.readObject();
		PreferenceManager cm = new PreferenceManager();
		
		DocumentReader.readStandardFormat(manager, eventSpanFileName_test);
		
		ArrayList<EventSpan> allspans = manager.getAllEventSpans();
		System.err.println(manager.getAllEventSpans().size()+" instances constructed.");
		
		Event event = manager.getEvent(eventName);
		if(event==null){
			System.err.println("The event "+eventName+" was not found.");
			System.exit(1);
		}
		
		ArrayList<EventSpan> spans = new ArrayList<EventSpan>();
		for(EventSpan span : allspans){
			if(span.getEvent().equals(event)){
				spans.add(span);
			}
		}
		
		System.err.println("#events:"+spans.size()+" instances with event name "+event.getMostSpecificName());
		
		ArrayList<EventSpan> testSpans = new ArrayList<EventSpan>();
		for(EventSpan span : spans){
			if(considerEventSpanAsSubSentence){
				testSpans.add(span.getChildEventSpan(manager));
			} else{
				testSpans.add(span);
			}
		}
		
		ArrayList<Integer> allfeatures = fm.getAllFeatures();
		System.err.println(allfeatures.size()+" features.");
		
		System.err.println("===TEST===");
		eval2(testSpans, event, manager, fm, cm);
		
		output(outputFileName, testSpans, event, manager, fm, cm);
	}
	
	private static void eval2(ArrayList<EventSpan> spans, Event event, Manager manager, FeatureManager fm, PreferenceManager cm){
		
		boolean considerPreferences = true;
		
		int topK = 1;//set it to 1
		
		double[] prf = new double[3];
		
		long bTime = System.currentTimeMillis();
		for(int i = 0; i < spans.size(); i++){
//			System.err.print('.');
			EventSpan span = spans.get(i);
			
			Predictor predictor = new Predictor(span, manager, fm, cm, topK, false);
			predictor.decode(event, considerPreferences);
			ArrayList<RoleSpan> rolespans = predictor.getPredictions(event);
			
			EventAnnotation ea_gold = span.getGoldAnnotation();
			
			for(int k = 0; k< rolespans.size(); k++){
				RoleSpan rolespan = rolespans.get(k);
				EventAnnotation ea_pred = rolespan.toEventAnnotation();
				double[] statistics = ea_gold.computePRFStatistics(ea_pred);
				prf[0] += statistics[0];
				prf[1] += statistics[1];
				prf[2] += statistics[2];
				
				System.out.println("SPAN:");
				System.out.println(Manager.viewEventSpan(spans.get(i)));
				System.out.println("GOLD:");
				ea_gold.viewIt(span);
				System.out.println("PRED:");
				ea_pred.viewIt(span);
			}
		}
		System.err.println();
		
		System.err.println("Correct :"+prf[0]);
		System.err.println("Expected :"+prf[1]);
		System.err.println("Predicted :"+prf[2]);
		
		double c = prf[0];
		prf[0] = c / prf[1];
		prf[1] = c / prf[2];
		prf[2] = 2 / (1/prf[0] + 1/prf[1]);
		
		System.err.println("R="+prf[0]);
		System.err.println("P="+prf[1]);
		System.err.println("F="+prf[2]);
		
		long eTime = System.currentTimeMillis();
		System.err.println((eTime-bTime)/1000.0 +" seconds.");
		
		System.err.println("ALL OK");
		
	}
	

	private static void output(String outputFileName, ArrayList<EventSpan> spans, Event event, Manager manager, FeatureManager fm, PreferenceManager cm) throws FileNotFoundException{
		
		PrintWriter p = new PrintWriter(new File(outputFileName));
		
		boolean considerPreferences = true;
		
		int topK = 1;//set it to 1
		
		for(int i = 0; i < spans.size(); i++){
			EventSpan span = spans.get(i);
			
			Predictor predictor = new Predictor(span, manager, fm, cm, topK, false);
			predictor.decode(event, considerPreferences);
			ArrayList<RoleSpan> rolespans = predictor.getPredictions(event);
			
			for(int k = 0; k< rolespans.size(); k++){
				RoleSpan rolespan = rolespans.get(k);
				EventAnnotation ea_pred = rolespan.toEventAnnotation();
				p.println("[event span]");
				p.println(Manager.viewEventSpan(spans.get(i)));
				p.println("[predictions]");
				p.println(ea_pred.viewSpan(span));
				p.println("==========");
				p.flush();
			}
		}
		
		p.close();
	}
	

}
