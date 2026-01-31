package knowledge;

public class Influence extends AbstractEntity {
	
	// Influence over time is what causes Change
	
	public Logos targetProperty = new Logos();
	public Logos influenceStrength = new Logos();
	public TimeMoment beginningTime = new TimeMoment();
	public TimeMoment endTime = new TimeMoment();

}
