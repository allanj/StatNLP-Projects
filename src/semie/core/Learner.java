package semie.core;

import semie.types.Event;
import semie.types.EventSpan;
import semie.types.FeatureManager;
import semie.types.Manager;

/**
 * A learner collects information from one individual event span for learning purpose
 * @author luwei
 * @version 1.0
 */

public abstract class Learner {
	
	protected Manager _manager;
	protected FeatureManager _fm;
	protected EventSpan _span;
	protected Event _event;
	protected double _weight;
	
	public Learner(Manager manager, FeatureManager fm, EventSpan span, Event event, double weight){
		this._manager = manager;
		this._fm = fm;
		this._span = span;
		this._event = event;
		this._weight = weight;
	}
	
	public abstract double[] computeStatistics();
	
}
