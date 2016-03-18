package semie.main;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Random;

import semie.core.LBFGS;
import semie.core.Predictor;
import semie.core.SupervisedLearner;
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
 * supervised experimenter
 * @author luwei
 * @version 1.0
 */

public class SupervisedExperimenter {
	
	private static boolean _debug = false;
	
	private static boolean diagco = false;
	private static double [] diag;
	private static int[] iprint = {0,0};
	private static double eps = 10e-1;
	private static double xtol = 10e-16;
	private static int[] iflag = {0};
	
	//regularization parameter, we set it to zero in the paper.
	private static double kappa = 0.01;
	
	public static void main(String args[])throws Exception{
		
		int eventSeq = Integer.parseInt(args[0]);
		boolean subspan = Boolean.parseBoolean(args[1]);
		
		String eventspan_name = "data/ace.data";
		String event_name = "data/ace.template";
		
		Manager manager = DocumentReader.readStandardFormat(eventspan_name, event_name);
		FeatureManager fm = new FeatureManager();
		
		PreferenceManager cm = new PreferenceManager();
		
		ArrayList<Integer> allfeatures;
		
		ArrayList<Event> allevents = manager.getAllEvents();
		ArrayList<EventSpan> allspans = manager.getAllEventSpans();
		
		Event event = allevents.get(eventSeq);//the first event..
		
		ArrayList<EventSpan> spans = new ArrayList<EventSpan>();
		for(EventSpan span : allspans)
			if(span.getEvent()==event)
				spans.add(span);
		
		System.err.println("#events:"+event.getMostSpecificName()+":"+spans.size());

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
			
			double f = - oneIteration(trainSpans, manager, fm, event);
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
			
			System.err.println("===TRAN===");
			eval(trainSpans, event, manager, fm, cm);
			
			if(iflag[0]==0)
				break;
		}
		

		ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("manager-"+eventSeq+".obj"));
		out.writeObject(manager);
		out.flush();
		out.close();
		
		ObjectInputStream in = new ObjectInputStream(new FileInputStream("manager-"+eventSeq+".obj"));
		manager = (Manager)in.readObject();

		ObjectOutputStream out1 = new ObjectOutputStream(new FileOutputStream("manager-"+eventSeq+"-v2.obj"));
		out1.writeObject(manager);
		out1.flush();
		out1.close();
		
		System.err.println("===TEST===");
		eval(testSpans, event, manager, fm, cm);
		
	}
	
	private static void eval(ArrayList<EventSpan> spans, Event event, Manager manager, FeatureManager fm, PreferenceManager cm){
		
		boolean considerPreferences = true;
		
		int topK = 1;
		
		double[] prf = new double[3];
		
		long bTime = System.currentTimeMillis();
		for(EventSpan span : spans){
			System.err.print('.');
			
			Predictor predictor = new Predictor(span, manager, fm, cm, topK, false);
			predictor.decode(event, considerPreferences);
			ArrayList<RoleSpan> rolespans = predictor.getPreferredPredictions(event, topK);
			
			EventAnnotation ea_gold = span.getGoldAnnotation();
			
			for(RoleSpan rolespan : rolespans){
				EventAnnotation ea_pred = rolespan.toEventAnnotation();
				double[] statistics = ea_gold.computePRFStatistics(ea_pred);
				prf[0] += statistics[0];
				prf[1] += statistics[1];
				prf[2] += statistics[2];
				
				if(_debug){
					System.out.println("SPAN:");
					System.out.println(Manager.viewEventSpan(span));
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
	
	private static double oneIteration(ArrayList<EventSpan> spans, Manager manager, FeatureManager fm, Event event){
		
		double jointlogprob = 0;
		long bTime = System.currentTimeMillis();
		for(EventSpan span : spans){
			double weight = 1.0;
			SupervisedLearner learner = new SupervisedLearner(manager, fm, span, event, weight);
			double[] scores = learner.computeStatistics();
			jointlogprob += (Math.log(scores[0]) - Math.log(scores[1])) * weight;
		}
		System.err.println();
		long eTime = System.currentTimeMillis();
		System.err.println("Time taken: "+(eTime-bTime)/1000.0 +" seconds.");
		return jointlogprob;
		
	}

}
