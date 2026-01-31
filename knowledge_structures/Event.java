package knowledge;

import java.util.ArrayList;

public class Event extends AbstractEntity {
	
	/*
	 * An Event represents an occasion in time and space, usually in form of SVO (subject-verb-object) relation.
	 * The Action object can have a defined TimeMoment (for past Events) or imprecise TimeEstimation (for future Events).
	 */
	
	public Logos subject = new Logos();
	public Action verb = new Action();
	public ArrayList<Logos> objects = new ArrayList<Logos>(); // there may be multiple objects influenced by same Object and Action
	public Reason reason = new Reason();
	public Cause cause = new Cause();

}
