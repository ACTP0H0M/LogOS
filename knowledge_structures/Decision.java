package knowledge;

public class Decision extends AbstractEntity {
	
	// Decision is a Choice after certain Option was picked, with time data
	public Option chosenOption = new Option();
	public TimeMoment decisionTimeMoment = new TimeMoment();

}
