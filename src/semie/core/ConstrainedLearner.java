package semie.core;

import semie.pref.PreferenceManager;
import semie.types.Event;
import semie.types.EventSpan;
import semie.types.FeatureManager;
import semie.types.Manager;

/**
 * Constrained learner
 * @author luwei
 * @version 1.0
 */

public abstract class ConstrainedLearner extends UnconstrainedLearner{
	
	protected PreferenceManager _cm;
	
	public ConstrainedLearner(Manager manager, FeatureManager fm, EventSpan span, Event event, double weight, PreferenceManager cm) {
		super(manager, fm, span, event, weight);
		this._cm = cm;
	}
	
	protected abstract void computeL();
}
