package logos;

import java.util.ArrayList;

public class Logos {
	
	String name;
	long id;
	ArrayList<Link> outwardLinks, inwardLinks;
	
	public Logos(String name, long id, ArrayList<Link> outwardLinks, ArrayList<Link> inwardLinks) {
		this.name = name;
		this.id = id;
		this.outwardLinks = outwardLinks;
		this.inwardLinks = inwardLinks;
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

}
