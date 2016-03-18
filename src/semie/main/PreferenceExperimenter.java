package semie.main;

import java.util.ArrayList;
import java.util.Random;

import semie.core.KBestSoftConstrainedLearner;
import semie.core.LBFGS;
import semie.core.Predictor;
import semie.core.UnconstrainedLearner;
import semie.pref.PreferenceManager;
import semie.types.DocumentReader;
import semie.types.Event;
import semie.types.EventAnnotation;
import semie.types.EventSpan;
import semie.types.FeatureManager;
import semie.types.Manager;
import semie.types.RoleSpan;
import semie.util.MathUtil;

/**
 * experimenter for learning with preferences
 * @author luwei
 * @version 1.0
 */

public class PreferenceExperimenter {
	
	private static boolean _debug = false;
	private static boolean diagco = false;
	private static double [] diag;
	private static int[] iprint = {0,0};
	private static double eps = 10e-1;
	private static double xtol = 10e-16;
	private static int[] iflag = {0};
	//regularization parameter
	private static double kappa = 0;//1E-5;
	//parameter for constraint-based learning: alpha=0.00: constrained k-best learner, alpha=1.00: unconstrained learner
	private static double alpha = 0.00;//1E-10;
	
	public static void main(String args[])throws Exception{
		
		int eventSeq = Integer.parseInt(args[0]);
		boolean subspan = Boolean.parseBoolean(args[1]);
		int topK = Integer.parseInt(args[2]);
		
		String eventspan_name = "ace-sentences.txt";
		String event_name = "ace.ontology-coarse.txt";
		String preference_filename = "preference-"+eventSeq+"-"+subspan+".txt";
		
		Manager manager = DocumentReader.readStandardFormat(eventspan_name, event_name);
		
		FeatureManager fm = new FeatureManager();
		
		PreferenceManager cm = PreferenceManager.readPreferences(preference_filename, manager);
		
		ArrayList<Integer> allfeatures;
		
		ArrayList<Event> allevents = manager.getAllEvents();
		ArrayList<EventSpan> allspans = manager.getAllEventSpans();
		
		Event event = allevents.get(eventSeq);
		
		ArrayList<EventSpan> spans = new ArrayList<EventSpan>();
		for(int i = 0; i < allspans.size(); i++){
			EventSpan span = allspans.get(i);
			if(span.getChildEventSpan(manager)==null)
				continue;
			if(span.getEvent()==event){// && manager.viewEventSpan(span.getId()).equals("The EU is set to release 20 million euros ( US $ 21.5 million ) in immediate humanitarian aid for Iraq if war breaks out and may dip into an `` emergency reserve '' of 250 million euros ( US $ 269 million ) for humanitarian relief")){
				spans.add(span);
			}
		}
		System.err.println("#events:"+manager.getEvent(event.getId()).getMostSpecificName()+":"+spans.size());

		ArrayList<EventSpan> trainSpans = new ArrayList<EventSpan>();
		ArrayList<EventSpan> testSpans = new ArrayList<EventSpan>();
		Random rand = new Random(1234);
		for(EventSpan span : spans){
			if(subspan) span = span.getChildEventSpan(manager);
			
			if(rand.nextDouble()<0.7){
				System.err.println("Selected for train :");
				trainSpans.add(span);
			} else {
				System.err.println("Selected for test  :");
				testSpans.add(span);
			}
		}
		
		System.err.println("Done reading..totally "+trainSpans.size()+"/"+spans.size()+" event spans..");
		
		//must do!!!
		indexFeatures(trainSpans, manager, fm, event);
		
		allfeatures = fm.getAllFeatures();
		System.err.println(allfeatures.size()+" features.");
		
		double old_f = Double.POSITIVE_INFINITY;
		
		boolean considerPreferences = true;
		
		while(true){
			//reset the feature counts..
			fm.resetFeatureCounts();
			
			double f = - oneIteration(trainSpans, manager, fm, event, topK, cm, alpha);
			double [] g = new double[allfeatures.size()];
			double [] x = new double[allfeatures.size()];
			if(diag==null){
				diag = new double[allfeatures.size()];
				for(int k = 0; k<allfeatures.size(); k++)
					diag[k] = 1.0;
			}
			for(int k = 0; k<allfeatures.size(); k++){
				int feature = allfeatures.get(k);
				x[k] = fm.getWeight(feature, considerPreferences);
				g[k] = - (fm.getGradient(feature) - 2 * kappa * x[k]);
			}
			int n = g.length;
			int m = 4;
        	
        	f += kappa * MathUtil.square(x);
        	
			LBFGS.lbfgs(n, m, x, f, g, diagco, diag, iprint, eps, xtol, iflag);
			
			System.err.println("f="+f+"\t"+(old_f-f));
			
			for(int k = 0; k<allfeatures.size(); k++)
				fm.setWeight(allfeatures.get(k), x[k]);
			
			old_f = f;
			
			System.out.println("===TEST===");
			eval(testSpans, event, manager, fm, new PreferenceManager());
			
			//stabilized..
			if(iflag[0]==0){
				break;
			}
		}
		
	}
	
	private static void eval(ArrayList<EventSpan> spans, Event event, Manager manager, FeatureManager fm, PreferenceManager cm){
		
		boolean considerPreferences = true;
		
		int topK = 1;//set it to 1
		
		double[] prf = new double[3];
		
		long bTime = System.currentTimeMillis();
		for(int i = 0; i < spans.size(); i++){
			System.err.print('.');
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
				
				if(_debug){
					System.out.println("SPAN:");
					System.out.println(Manager.viewEventSpan(spans.get(i)));
					System.out.println("GOLD:");
					ea_gold.viewIt(span);
					System.out.println("PRED:");
					ea_pred.viewIt(span);
				}
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
	
	private static void indexFeatures(ArrayList<EventSpan> spans, Manager manager, FeatureManager fm, Event event){

		long bTime = System.currentTimeMillis();
		for(EventSpan span : spans){
			System.err.print('i');
			UnconstrainedLearner learner = new UnconstrainedLearner(manager, fm, span, event, 1.0);
			learner.computeStatistics();
		}
		System.err.println();
		long eTime = System.currentTimeMillis();
		System.err.println("Time taken: "+(eTime-bTime)/1000.0 +" seconds.");
		
	}
	
	private static double oneIteration(ArrayList<EventSpan> spans, Manager manager, FeatureManager fm, Event event, int topK, PreferenceManager cm, double alpha){
		
		double jointlogprob = 0;
		long bTime = System.currentTimeMillis();
		for(EventSpan span : spans){
			double weight = 1.0;
			KBestSoftConstrainedLearner learner = new KBestSoftConstrainedLearner(manager, fm, span, event, 1.0, topK, cm, alpha);
			learner.TurnOnPreference();
			double[] scores = learner.computeStatistics();
			jointlogprob += (Math.log(scores[0]) - Math.log(scores[1])) * weight;
		}
		System.err.println();
		long eTime = System.currentTimeMillis();
		System.err.println("Time taken: "+(eTime-bTime)/1000.0 +" seconds.");
		
		return jointlogprob;
		
	}

}
