package semie.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringTokenizer;

import semie.core.LBFGS;
import semie.core.LBFGS.ExceptionWithIflag;
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
 * the supervised trainer
 * @author luwei
 * @version 1.0
 */

public class SupervisedTrainer {
	
	private static boolean diagco = false;
	private static double [] diag;
	private static int[] iprint = {0,0};
	private static double eps = 10e-1;
	private static double xtol = 10e-16;
	private static int[] iflag = {0};
	
	//regularization parameter
	//we set it to zero for running all experiments in the paper.
	private static double kappa = 0.00;
	
	public static void main(String args[])throws Exception{
		
		String configFileName = args[0];
		
		String eventSpanFileName_train = null;
		String eventStructureFileName = null;
		String eventName = null;
		boolean considerEventSpanAsSubSentence = true;
		
		Scanner scan = new Scanner(new File(configFileName));
		while(scan.hasNextLine()){
			String line = scan.nextLine();
			if(line.trim().equals("") || line.startsWith("#"))
				continue;
			
			StringTokenizer st = new StringTokenizer(line);
			String type = st.nextToken();
			String arg = st.nextToken();
			if(type.equals("eventSpanFileName_train")){
				eventSpanFileName_train = arg;
			} else if(type.equals("eventStructureFileName")){
				eventStructureFileName = arg;
			} else if(type.equals("eventName")){
				eventName = arg.toLowerCase();
			} else if(type.equals("assumeEventSpan")){
				considerEventSpanAsSubSentence = Boolean.parseBoolean(arg);
			}
		}
		
		Manager manager = toManager(eventSpanFileName_train, eventStructureFileName);
		FeatureManager fm = new FeatureManager();
		PreferenceManager pm = new PreferenceManager();
		
		ArrayList<EventSpan> allspans = manager.getAllEventSpans();
		
		Event event = manager.getEvent(eventName);
		if(event==null){
			System.err.println("The event "+eventName+" was not found.");
			System.exit(1);
		}
		
		ArrayList<EventSpan> spans = new ArrayList<EventSpan>();
		for(EventSpan span : allspans){
			spans.add(span);
		}
		
		System.err.println("#events:"+spans.size()+" instances with event name "+event.getMostSpecificName());
		
		ArrayList<EventSpan> trainSpans = new ArrayList<EventSpan>();
		for(int i = 0; i<spans.size(); i++){
			EventSpan span = spans.get(i);
			if(considerEventSpanAsSubSentence){
				trainSpans.add(span.getChildEventSpan(manager));
			} else{
				trainSpans.add(span);
			}
		}
		
		System.err.println("Done reading. Totally there are "+trainSpans.size()+"/"+spans.size()+" event allspans..");
		
		indexFeatures(trainSpans, manager, fm, event);
		
		ArrayList<Integer> allfeatures = fm.getAllFeatures();
		System.err.println(allfeatures.size()+" features.");
		
		opt(manager, fm, pm, trainSpans, event, allfeatures);
		
		System.err.println("Writing the learned model into the disk.");
		ObjectOutputStream out;
		out = new ObjectOutputStream(new FileOutputStream("data/model/manager-"+eventName+"-pf.obj"));
		out.writeObject(manager);
		out.flush();
		out.close();
		out = new ObjectOutputStream(new FileOutputStream("data/model/fm-"+eventName+"-pf.obj"));
		out.writeObject(fm);
		out.flush();
		out.close();
		
	}

	public static Manager toManager(String eventspan_name, String event_name) throws FileNotFoundException{
		return DocumentReader.readStandardFormat(eventspan_name, event_name);
	}
	
	private static void opt(Manager manager, FeatureManager fm, PreferenceManager cm, ArrayList<EventSpan> trainSpans, Event event, ArrayList<Integer> allfeatures) throws ExceptionWithIflag{

		double old_f = Double.POSITIVE_INFINITY;
		boolean considerPreferences = true;
		while(true){
			//reset the feature counts..
			fm.resetFeatureCounts();
			
			double f = - oneIteration(trainSpans, manager, fm, cm, event);
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
			eval2(trainSpans, event, manager, fm, cm);
			
			if(iflag[0]==0)
				break;
		}
		
	}
	
	private static void indexFeatures(ArrayList<EventSpan> allspans, Manager manager, FeatureManager fm, Event event){
		
		long bTime = System.currentTimeMillis();
		for(EventSpan span : allspans){
			System.err.print('i');
//			System.err.println(span);
			UnconstrainedLearner learner = new UnconstrainedLearner(manager, fm, span, event, 1.0);
			learner.computeStatistics();
		}
		System.err.println();
		long eTime = System.currentTimeMillis();
		System.err.println("Time taken: "+(eTime-bTime)/1000.0 +" seconds.");
		
	}
	
	private static double oneIteration(ArrayList<EventSpan> allspans, Manager manager, FeatureManager fm, PreferenceManager cm, Event event){
		
		double jointlogprob = 0;
		long bTime = System.currentTimeMillis();
		for(EventSpan span : allspans){
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
	

	private static void eval2(ArrayList<EventSpan> allspans, Event event, Manager manager, FeatureManager fm, PreferenceManager cm){
		
		boolean considerPreferences = true;
		
		int topK = 1;//set it to 1
		
		double[] prf = new double[3];
		
		long bTime = System.currentTimeMillis();
		for(int i = 0; i < allspans.size(); i++){
//			System.err.print('.');
			EventSpan span = allspans.get(i);
			
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
				System.out.println(Manager.viewEventSpan(allspans.get(i)));
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

}
