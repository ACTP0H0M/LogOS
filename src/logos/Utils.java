package logos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Utils {
	
	boolean verbose;
	
	double eps = 10e-6;
	
	List<String> endOfPhraseMarks = Arrays.asList(".", "!", "?", "...", ";");
	List<String> punctuation = Arrays.asList(".", "!", "?", "...", ";", ",", ":", "-");

	
	public Utils(int verbosity) {
		if (verbosity >= 2) {
			verbose = true;
		}
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
	
	// returns a link with empty name and id=-1 if not found
	public Link findLinkByName (ArrayList<Link> linkList, String name) {
		Link ans = emptyNamedLink("", -1);
		for (Link l : linkList) {
			if (l.relationName.equals(name)) {
				ans = l;
				break;
			}
		}
		return ans;
	}
	
	// returns an empty list if not found
	public ArrayList<Link> linksByName (ArrayList<Link> linkList, String name) {
		ArrayList<Link> ans = new ArrayList<Link>();
		for (Link l : linkList) {
			if (l.relationName.equals(name)) {
				ans.add(l);
			}
		}
		return ans;
	}
	
	public Branch emptyNamedBranch (String name, long id) {
		Branch b = new Branch(name, id, new ArrayList<Link>(), new ArrayList<Link>());
		return b;
	}
	
	
	public void printLogosInfo (Logos l) {
		System.out.println("Logos " + l.id + " [" + l.name + "]");
		System.out.print("          Inward Link IDs: " );
		for (Link lk : l.inwardLinks)
			System.out.print(lk.id + " ");
		System.out.println();
		System.out.print("          Outward Link IDs: " );
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
	
	public boolean isEndOfPhrase(String token) {
		if (endOfPhraseMarks.contains(token))
			return true;
		else
			return false;
	}
	
	public boolean isPunctuation(String token) {
		if (punctuation.contains(token))
			return true;
		else
			return false;
	}
	
	// Make sure that word1 and word2 are in the list!
	public int distanceWithPunctuation(String word1, String word2, List<String> sentenceTokens) {
		int index1 = sentenceTokens.lastIndexOf(word1);	// will be -1 if not found
		int index2 = sentenceTokens.lastIndexOf(word2);
		return index2 - index1;
	}
	
	// Distance = index2 - index1 (from word1 to word2)
	public int distanceWithoutPunctuation(String word1, String word2, List<String> sentenceTokens) {
		ArrayList<String> wordTokens = new ArrayList<String>();
		for (String s : sentenceTokens) {
			if (!isPunctuation(s)) {
				wordTokens.add(s);
			}
		}
		int index1 = wordTokens.lastIndexOf(word1);	// will be -1 if not found
		int index2 = wordTokens.lastIndexOf(word2);
		return index2 - index1;
	}
	
	/*
	 * To minimize the size of the graph, following strategy can be used:
	 * 1) find Logos with same names but different IDs
	 * 2) check for "is_a" links - if same, change links and remove redundancy
	 * 3) if "is_a" sources have a conflict, don't do anything
	 * 4) some link types can penetrate a Branch, others don't
	 * Links with higher generality can penetrate better.
	 */
	
	// true if there is at least 1 same parent
	public boolean haveSameParent(Logos logos1, Logos logos2) {
		
		boolean ans = false;
		
		if (!logos1.outwardLinks.isEmpty() && !logos2.outwardLinks.isEmpty()) {
			
			ArrayList<Link> first = linksByName(logos1.outwardLinks, "is_a");
			ArrayList<Link> second = linksByName(logos2.outwardLinks, "is_a");
			
			if (!(first.isEmpty()) &&
					!(second.isEmpty())) {
				
				for (Link l1 : first) {
					for (Link l2 : second) {
						if (l1.target.name.equals(l2.target.name)) {
							ans = true;
							break;
						}
					}
					if (ans == true)
						break;
				}
				
			}
			
		}
		
		return ans;
		
	}
	
	// Means both Logos have the same target for same relations
	public boolean haveSameTarget(Logos logos1, Logos logos2, String relName) {
		
		boolean ans = false;
		
		if (!logos1.outwardLinks.isEmpty() && !logos2.outwardLinks.isEmpty()) {
			
			ArrayList<Link> first = linksByName(logos1.outwardLinks, relName);
			ArrayList<Link> second = linksByName(logos2.outwardLinks, relName);
			
			if (!(first.isEmpty()) &&
					!(second.isEmpty())) {
				
				for (Link l1 : first) {
					for (Link l2 : second) {
						if (l1.target.name.equals(l2.target.name)) {
							ans = true;
							break;
						}
					}
					if (ans == true)
						break;
				}
				
			}
			
		}
		
		return ans;
		
	}
	
	// Both logos have same sources with same relations
	public boolean haveSameSource(Logos logos1, Logos logos2, String relName) {
		
		boolean ans = false;
		
		if (!logos1.inwardLinks.isEmpty() && !logos2.inwardLinks.isEmpty()) {
			
			ArrayList<Link> first = linksByName(logos1.inwardLinks, relName);
			ArrayList<Link> second = linksByName(logos2.inwardLinks, relName);
			
			if (!(first.isEmpty()) &&
					!(second.isEmpty())) {
				
				for (Link l1 : first) {
					for (Link l2 : second) {
						if (l1.source.name.equals(l2.source.name)) {
							ans = true;
							break;
						}
					}
					if (ans == true)
						break;
				}
				
			}
			
		}
		
		return ans;
		
	}

	// returns empty list if the name is unique
	public ArrayList<Logos> sameNamedLogosList(ArrayList<Logos> list, Logos logos) {
		ArrayList<Logos> ans = new ArrayList<Logos>();
		String name = logos.name;
		long id = logos.id;
		for (Logos l : list) {
			if (l.name.equals(name) && l.id != id) {
				ans.add(l);
			}
		}
		return ans;
	}
	
	// filter logos list by maximum actuality of its links
	public ArrayList<Logos> filterByActuality(ArrayList<Logos> list, double a_min) {
		ArrayList<Logos> ans = new ArrayList<Logos>();
		for (Logos l : list) {
			if (maxLinkActuality(l) >= a_min) {
				ans.add(l);
			}
		}
		return ans;
	}
	
	// returns -1 if no Links
	public double maxLinkActuality (Logos l) {
		double ans = -1.0;
		for (Link lk : l.inwardLinks) {
			if (lk.actuality > ans) {
				ans = lk.actuality;
			}
		}
		for (Link lk : l.outwardLinks) {
			if (lk.actuality > ans) {
				ans = lk.actuality;
			}
		}
		return ans;
	}
	
	public double generality(int evidence, double belief) {
		return 2 * Math.atan(belief * evidence) / Math.PI;
	}
	
	public int evidenceFromGenerality(double g, double b) {
		if (Math.abs(g) <= 1.0 - eps) {
			return (int) (Math.tan(Math.PI * g / 2) / b);
		} else {
			return (int) (Math.signum(g) / eps);
		}
	}
	
	public double updateActuality(double a, boolean forget, double forgetRate, double rememberRate) {
		double a_new = a;
		if (forget) {
			a_new -= forgetRate;
			if (a_new < 0.0)
				a_new = 0.0;
		} else {
			a_new += rememberRate;
			if (a_new > 1.0)
				a_new = 1.0;
		}
		return a_new;
	}
	
}
