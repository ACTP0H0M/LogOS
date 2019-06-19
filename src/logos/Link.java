package logos;

public class Link implements java.io.Serializable {
	
	// used in constructor
	public Logos source, target;
	public String relationName;
	public double generality, actuality;
	public long id;
	
	// used to track deduction process
	public Link arg1;
	public Link arg2;
	
	public Link(Logos source, Logos target, String relationName, double generality, double actuality, long id) {
		this.source = source;
		this.target = target;
		this.relationName = relationName;
		this.generality = generality;
		this.actuality = actuality;
		this.id = id;
		
		//System.out.println("LINK: New Link " + id + " [" + relationName + "]");
	}

	public Logos getSource() {
		return source;
	}

	public void setSource(Logos source) {
		this.source = source;
	}

	public Logos getTarget() {
		return target;
	}

	public void setTarget(Logos target) {
		this.target = target;
	}

	public String getRelationName() {
		return relationName;
	}

	public void setRelationName(String relationName) {
		this.relationName = relationName;
	}

	public double getGenerality() {
		return generality;
	}

	public void setGenerality(double generality) {
		this.generality = generality;
	}

	public double getActuality() {
		return actuality;
	}

	public void setActuality(double actuality) {
		this.actuality = actuality;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Link getArg1() {
		return arg1;
	}

	public void setArg1(Link arg1) {
		this.arg1 = arg1;
	}

	public Link getArg2() {
		return arg2;
	}

	public void setArg2(Link arg2) {
		this.arg2 = arg2;
	}
	
	

}
