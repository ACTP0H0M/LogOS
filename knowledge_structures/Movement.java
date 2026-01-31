package knowledge;

public class Movement extends IntransitiveAction {
	
	// The movement done by an object itself (e.g. go, fly). Can consist of consecutive TransitiveActions (moving legs, wings, robotic arms)
	// Movements are characterized by start and end points in space and time (QuantifiedMovement) or human-readable descriptions (FuzzyMovement)
	public Logos mover = new Logos();

}
