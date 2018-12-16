package logos;

public class Link {
	
	Logos source, target;
	String relationName;
	double generality, actuality;
	long id;
	
	public Link(Logos source, Logos target, String relationName, double generality, double actuality, long id) {
		this.source = source;
		this.target = target;
		this.relationName = relationName;
		this.generality = generality;
		this.actuality = actuality;
		this.id = id;
	}

}
