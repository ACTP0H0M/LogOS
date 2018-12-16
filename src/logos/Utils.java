package logos;

import java.util.ArrayList;

public class Utils {
	
	boolean verbose;
	int k;
	
	public Utils(int verbosity, int k) {
		if (verbosity >= 2) {
			verbose = true;
		}
		this.k = k;
	}
	
	/*public double calcConfidence (int w, int k) {
		return (double) w / (w + k);
	}*/
	
	public Logos emptyNamedLogos (String name, long id) {
		Logos l = new Logos(name, id, new ArrayList<Link>(), new ArrayList<Link>());
		return l;
	}
	
	/*
	 * Given specified source and target Logos as well as relation name, creates a fresh Link,
	 * calculates its f and c, adds the Link to source and target Logos
	 */
	public Link freshLink (Logos from, Logos to, String relationName, long id) {

		// update generality: default generality is 0
		double gen = 0.0;
		// update actuality: default actuality of a fresh link is 1.0
		double act = 1.0;
		
		Link lk = new Link(from, to, relationName, gen, act, id);
		
		// apply changes to Logos
		from.outwardLinks.add(lk);
		to.inwardLinks.add(lk);
		
		return lk;
	}
	
	/*
	 * Empty Link with source_L and target_L
	 */
	public Link emptyNamedLink (String name, long id) {
		
		Link lk = new Link(emptyNamedLogos("source_L", -1), emptyNamedLogos("target_L", -1), name, 0, 0, id);
		
		return lk;
		
	}
	
	/*
	 * Returns a Logos by ID from a list.
	 * If there is no such Logos, returns Logos with empty name, id = -1 and empty link lists.  
	 */
	public Logos findLogosByID (ArrayList<Logos> logosList, long id) {
		Logos ans = new Logos("", -1, new ArrayList<Link>(), new ArrayList<Link>());
		for (Logos l : logosList) {
			if (l.id == id) {
				ans = l;
				break;
			}
		}
		return ans;
	}
	
	/*
	 * Retruns a Logos by name from a list.
	 * It is the Logos with the smallest index and given name.
	 * To use this function properly, ensure that the list has only one Logos with such name.
	 */
	public Logos findLogosByName (ArrayList<Logos> logosList, String name) {
		Logos ans = new Logos("", -1, new ArrayList<Link>(), new ArrayList<Link>());
		for (Logos l : logosList) {
			if (l.name.equals(name)) {
				ans = l;
				break;
			}
		}
		return ans;
	}
	
	/*
	 * Returns a Link by ID from a list.
	 * If there is no such Link, returns Link with empty name, id = -1 and empty logos lists.  
	 */
	public Link findLinkByID (ArrayList<Link> linksList, long id) {
		Link ans = new Link(emptyNamedLogos("", -1), emptyNamedLogos("", -1), "", 0, 0, -1);
		for (Link l : linksList) {
			if (l.id == id) {
				ans = l;
				break;
			}
		}
		return ans;
	}
	
	public Branch emptyNamedBranch (String name, long id) {
		Branch b = new Branch(name, id, new ArrayList<Link>(), new ArrayList<Link>());
		return b;
	}
	
	
	public void printLogosInfo (Logos l) {
		System.out.println("Logos " + l.id + " [" + l.name + "]" );
		System.out.print("DATABASE: Inward Link IDs: " );
		for (Link lk : l.inwardLinks)
			System.out.print(lk.id + " ");
		System.out.println();
		System.out.print("DATABASE: Outward Link IDs: " );
		for (Link lk : l.outwardLinks)
			System.out.print(lk.id + " ");
		System.out.println();
	}
	
	public String[] tokensFromText (String text) {
		// Split spaces
		String[] bSatz = text.split("\\s+");
		int[] p = new int[bSatz.length];
		int punct = 0;
		for (int i = 0; i < bSatz.length; i++) {
			bSatz[i] = bSatz[i].trim();
			bSatz[i] = bSatz[i].toLowerCase();
			if (bSatz[i].contains(".")) {
				punct++;
				p[i] = 1;
			}
			if (bSatz[i].contains(",")) {
				punct++;
				p[i] = 1;
			}
			if (bSatz[i].contains("!")) {
				punct++;
				p[i] = 1;
			}
			if (bSatz[i].contains("?")) {
				punct++;
				p[i] = 1;
			}
			if (bSatz[i].contains(":")) {
				punct++;
				p[i] = 1;
			}
			if (bSatz[i].contains(";")) {
				punct++;
				p[i] = 1;
			}
			if (bSatz[i].contains("...")) {
				punct++;
				p[i] = 1;
			}
		}
		// Tokens
		String[] fSatz = new String[bSatz.length + punct];
		boolean[] isZnak = new boolean[bSatz.length + punct];
		int target = 0;
		for (int i = 0; i < bSatz.length; i++) {
			if (p[i] == 1) {
				target++;
				fSatz[target] = bSatz[i].substring(bSatz[i].toCharArray().length - 1);
				isZnak[target] = true;
				fSatz[target - 1] = bSatz[i].substring(0, bSatz[i].toCharArray().length - 1);
			} else {
				fSatz[target] = bSatz[i];
			}
			target++;
		}
		return fSatz;
	}

}
