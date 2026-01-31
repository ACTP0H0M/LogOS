package knowledge;

import java.util.ArrayList;

public class Relationship {
	
	/*
	 * Between two Logos or sets of Logos.
	 * Subclasses:
	 *   SpatialRelationship
	 *   TemporalRelationship
	 */
	public ArrayList<Logos> memberA = new ArrayList<Logos>();
	public ArrayList<Logos> memberB = new ArrayList<Logos>();
	public ArrayList<Word> relationshipWords = new ArrayList<Word>();

}
