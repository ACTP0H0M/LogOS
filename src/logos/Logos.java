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

}
