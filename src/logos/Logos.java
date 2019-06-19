package logos;

import java.util.ArrayList;

public class Logos implements java.io.Serializable {
	
	public String name;
	public long id;
	public ArrayList<Link> outwardLinks, inwardLinks;
	
	public Logos(String name, long id, ArrayList<Link> outwardLinks, ArrayList<Link> inwardLinks) {
		this.name = name;
		this.id = id;
		this.outwardLinks = outwardLinks;
		this.inwardLinks = inwardLinks;
		
		//System.out.println("LOGOS: New Logos " + id + " [" + name + "]");
	}
	
	public void actualizeAllLinks(double rememberRate, boolean verbose) {
		for (Link l_in : inwardLinks) {
			l_in.actuality += rememberRate;
			if (l_in.actuality > 1.0)
				l_in.actuality = 1.0;
		}
		for (Link l_out : outwardLinks) {
			l_out.actuality += rememberRate;
			if (l_out.actuality > 1.0)
				l_out.actuality = 1.0;
		}
		if (verbose) {
			System.out.println("LOGOS " + this.id + " [" + this.name  + "]: increased all Links actuality by " + rememberRate);
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public ArrayList<Link> getOutwardLinks() {
		return outwardLinks;
	}

	public void setOutwardLinks(ArrayList<Link> outwardLinks) {
		this.outwardLinks = outwardLinks;
	}

	public ArrayList<Link> getInwardLinks() {
		return inwardLinks;
	}

	public void setInwardLinks(ArrayList<Link> inwardLinks) {
		this.inwardLinks = inwardLinks;
	}

	
	
}
