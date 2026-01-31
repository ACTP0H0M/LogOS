package knowledge;

public class Interaction extends AbstractEntity {
	
	// Interaction is an Influence between two Logos. Not to confuse with Event!
	
	public Logos activeSide = new Logos();
	public Logos passiveSide = new Logos();
	public Influence influence = new Influence();

}
