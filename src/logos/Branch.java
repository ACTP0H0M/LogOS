package logos;

import java.util.ArrayList;


/*
 * A Branch always begins with a Logos, followed by a Link and so on in interchanging order.
 */
public class Branch extends Logos {
	
	ArrayList<Logos> containedLogosList = new ArrayList<Logos>();
	ArrayList<Link> containedLinkList = new ArrayList<Link>();

	public Branch(String name, long id, ArrayList<Link> outwardLinks, ArrayList<Link> inwardLinks) {
		super(name, id, outwardLinks, inwardLinks);
		// TODO Auto-generated constructor stub
	}

	public ArrayList<Logos> getContainedLogosList() {
		return containedLogosList;
	}

	public void setContainedLogosList(ArrayList<Logos> containedLogosList) {
		this.containedLogosList = containedLogosList;
	}

	public ArrayList<Link> getContainedLinkList() {
		return containedLinkList;
	}

	public void setContainedLinkList(ArrayList<Link> containedLinkList) {
		this.containedLinkList = containedLinkList;
	}
	
	

}
