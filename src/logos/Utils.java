package logos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.mit.jwi.item.POS;

public class Utils {
	
	boolean verbose;
	
	public double eps = 10e-6;
	
	public List<String> endOfPhraseMarks = Arrays.asList(".", "!", "?", "...", ";");
	public List<String> punctuation = Arrays.asList(".", "!", "?", "...", ";", ",", ":", "-");
	public String[] confirmationKeywords = {"yes",
			"sure",
			"correct",
			"of course",
			"true",
			"agree",
			"absolutely",
			"exactly",
			"yeah",
			"neither"};
	public String[] denialKeywords = {"no",
			"nope",
			"false",
			"wrong",
			"not",
			"n't",
			"impossible",
			"bullshit"};
	public String[] uncertaintyKeywords = {"maybe",
			"could",
			"possibly",
			"possible",
			"perhaps",
			"perchance"};
	public String[] empathicConfirmationResponses = {"I see...",
			"I understand...",
			"Thank you for opening up, master!"};
	public String[] uninformativeKeywords = {"thanks",
			"thank",
			"alright",
			"fine",
			"ok",
			"okay"};
	public String[] swearWords = {"fuck",
			"motherfucker",
			"asshole",
			"ass",
			"cyka",
			"blyat",
			"bitch"};
	public String[] swearResponses = {"That's rude!",
			"Are you alright?",
			"What?!",
			"Please don't swear."};
	public String[] spatialPrepositions = {"in", "on", "at", "under", "above", "near", "behind", "between", "in front of", "opposite to", "out"};
	public String[] vagueLocationWords = {"nearby", "close", "far", "above", "below", "left", "right", "here", "there"};
	public String[] uninformativeVerbs = {"is", "are", "was", "were", "have", "had"};
	public String[] requestKeywords = {"please", "Please"};
	
	public double[][] behaviourMatrix = new double[3][3];

	public HashMap<String, POS> posMap;
	public Set<Map.Entry<String, POS>> keyset;
	
	public Utils(int verbosity) {
		if (verbosity >= 2) {
			verbose = true;
		}
		
		behaviourMatrix[0][0] = 0.6;	// inWidth -> inWidth
		behaviourMatrix[0][1] = 0.2;	// inWidth -> explore
		behaviourMatrix[0][2] = 0.2;	// inWidth -> failSafe
		behaviourMatrix[1][0] = 0.5;	// explore -> inWidth
		behaviourMatrix[1][1] = 0.25;	// explore -> explore
		behaviourMatrix[1][2] = 0.25;	// explore -> failSafe
		behaviourMatrix[2][0] = 0.6;	// failSafe -> inWidth
		behaviourMatrix[2][1] = 0.3;	// failSafe -> explore
		behaviourMatrix[2][2] = 0.1;	// failSafe -> failSafe
		
		posMap = new HashMap<String, POS>();
		posMap.put("NN", POS.NOUN);
		posMap.put("NNP", POS.NOUN);
		posMap.put("NNS", POS.NOUN);
		posMap.put("NNPS", POS.NOUN);
		posMap.put("JJ", POS.ADJECTIVE);
		posMap.put("JJR", POS.ADJECTIVE);
		posMap.put("JJS", POS.ADJECTIVE);
		posMap.put("RB", POS.ADVERB);
		posMap.put("RBR", POS.ADVERB);
		posMap.put("RBS", POS.ADVERB);
		posMap.put("VB", POS.VERB);
		posMap.put("VBD", POS.VERB);
		posMap.put("VBG", POS.VERB);
		posMap.put("VBN", POS.VERB);
		posMap.put("VBP", POS.VERB);
		keyset = posMap.entrySet();
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
	 * Empty Link with source_L and target_L, g,a = 0
	 */
	public Link emptyNamedLink (String name, long id) {
		
		Link lk = new Link(emptyNamedLogos("", -1), emptyNamedLogos("", -1), name, 0, 0, id);
		
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
	 * Returns a Branch by ID from a list.
	 * If there is no such Branch, returns Branch with empty name, id = -1 and empty link lists.  
	 */
	public Branch findBranchByID (ArrayList<Branch> brList, long id) {
		Branch ans = new Branch("none", -1, new ArrayList<Link>(),
				new ArrayList<Link>());
		for (Branch b : brList) {
			if (b.id == id) {
				ans = b;
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
	
	public ArrayList<Logos> findAllLogosByName (ArrayList<Logos> logosList, String name) {
		ArrayList<Logos> ans = new ArrayList<Logos>();
		for (Logos l : logosList) {
			if (l.name.equals(name)) {
				ans.add(l);
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
	
	// returns a link with maximum actuality for a given name, otherwise id = -1;
	public Link mostActualLink (ArrayList<Link> linkList) {
		Link ans = emptyNamedLink("", -1);
		double min_actuality = -1.0;
		for (Link l : linkList) {
			if (l.actuality > min_actuality && l.id != -1) {
				ans = l;
				min_actuality = l.actuality;
			}
		}
		return ans;
	}
	
	// returns a link with maximum actuality for a given name, otherwise id = -1;
	public Link mostGeneralLink (ArrayList<Link> linkList) {
		Link ans = emptyNamedLink("", -1);
		double min_g = -2.0;
		for (Link l : linkList) {
			if (l.generality > min_g && l.id != -1) {
				ans = l;
				min_g = l.actuality;
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
	
	public ArrayList<Link> linksWithTarget (ArrayList<Link> linkList, String targetLogos) {
		ArrayList<Link> ans = new ArrayList<Link>();
		for (Link l : linkList) {
			if (l.target.name.equals(targetLogos)) {
				ans.add(l);
			}
		}
		return ans;
	}
	
	public ArrayList<Link> linksWithTargetAlsoInBranch (ArrayList<Link> linkList, String targetLogos) {
		ArrayList<Link> ans = new ArrayList<Link>();
		for (Link l : linkList) {
			if (l.target.getClass().getSimpleName().equals("Branch")) {
				try {
					Branch b = (Branch) l.target;
					Logos first = b.containedLogosList.get(0);
					if (first.name.equals(targetLogos)) {
						ans.add(l);
					} 
				} catch (IndexOutOfBoundsException e) {
					return ans;
				}
			}
			if (l.target.name.equals(targetLogos)) {
				ans.add(l);
			}
		}
		return ans;
	}
	
	// returns the links's Logos target (even if it is the first Logos in the target Branch)
	public Logos getLinkTargetLogos(Link link) {
		Logos ans = emptyNamedLogos("", -1);
		Logos target = link.target;
		if (target.getClass().getSimpleName().equals("Branch")) {
			Branch targetBranch = (Branch) target;
			List<Logos> contained = targetBranch.getContainedLogosList();
			ans = contained.get(0);
		} else {
			ans = target;
		}
		return ans;
	}
	
	// matching by Logos and Link names, including children matches
	public ArrayList<Link> linksWithTargetBranch (ArrayList<Link> linkList, Branch branch) {
		ArrayList<Link> ans = new ArrayList<Link>();
		for (Link lk : linkList) {
			if (lk.target.getClass().getSimpleName().equals("Branch")) {
				// target branchesMatch?
				if (branchesMatch(branch, (Branch) lk.target)) {
					ans.add(lk);
				} 
			}
		}
		return ans;
	}
	
	public ArrayList<Link> filterLinksByGeneralitySign (ArrayList<Link> list, boolean positive) {
		for (Link l : (ArrayList<Link>) list.clone()) {
			if (positive) {
				if (l.generality <= 0) {
					list.remove(l);
				}
			} else {
				if (l.generality >= 0) {
					list.remove(l);
				}
			}
		}
		return list;
	}
	
	// matching by Link names and some children matches
	public long mostSimilarTargetBranch(ArrayList<Link> linkList, Branch branch) {
		long idx = -1;
		ArrayList<Link> lks = new ArrayList<Link>();
		for (Link lk : linkList) {
			if (lk.target.getClass().getSimpleName().equals("Branch")) {
				if (similarBranches(branch, (Branch) lk.target)) {
					lks.add(lk);
				} 
			}
		}
		// return the most actual
		double a0 = -1.0;
		for (Link lk : lks) {
			if (lk.actuality > a0) {
				idx = lk.target.id;
			}
		}
		return idx;
	}
	
	// The first Branch must be more "abstract"!!!
	// Returns true if the two Branches have the same structures and Logos (can be inherited).
	public boolean branchesMatch (Branch br1, Branch br2) {
		boolean ans = false;
		List<Logos> cll1 = br1.containedLogosList;
		List<Logos> cll2 = br2.containedLogosList;
		List<Link> clkl1 = br1.containedLinkList;
		List<Link> clkl2 = br2.containedLinkList;
		int i = 0;
		while (i < cll1.size()) {
			Logos log1 = cll1.get(i);
			Logos log2;
			if (i < cll2.size()) {
				log2 = cll2.get(i);
			} else {
				return false;
			}
			if (log1.getClass().getSimpleName().equals("Branch") && log2.getClass().getSimpleName().equals("Branch")) {
				return branchesMatch((Branch) log1, (Branch) log2);
			} else if (log1.getClass().getSimpleName().equals("Logos") && log2.getClass().getSimpleName().equals("Logos")){
				// check if two logos match, or if there are such children...
				if (isChildOf(log2, log1)) {
					ans = true;
				} else if (log1.name.equals(log2.name)){
					ans = true;
				} else {
					return false;
				}
			} else {
				// type mismatch
				return false;
			}
			i++;
		} // logos loop
		
		// check links
		int j = 0;
		while (j < clkl1.size()) {
			Link lk1 = clkl1.get(j);
			Link lk2;
			if (j < clkl2.size()) {
				lk2 = clkl2.get(j);
			} else {
				return false;
			}
			if (lk1.relationName.equals(lk2.relationName) && lk1.generality * lk2.generality > 0) {
				ans = true;
			} else {
				return false;
			}
			j++;
		} // link loop
		
		return ans;
	}
	
	// The first Branch must be more "abstract"!!!
	// Returns true if the two Branches have the same structure but different Logos (not even inherited).
	public boolean similarBranches(Branch br1, Branch br2) {
		// must have at least one common Logos (parent-child)
		boolean ans = false;
		int common = 0;
		List<Logos> cll1 = br1.containedLogosList;
		List<Logos> cll2 = br2.containedLogosList;
		List<Link> clkl1 = br1.containedLinkList;
		List<Link> clkl2 = br2.containedLinkList;
		int i = 0;
		while (i < cll1.size()) {
			Logos log1 = cll1.get(i);
			Logos log2;
			if (i < cll2.size()) {
				log2 = cll2.get(i);
			} else {
				return false;
			}
			if (log1.getClass().getSimpleName().equals("Branch") && log2.getClass().getSimpleName().equals("Branch")) {
				return similarBranches((Branch) log1, (Branch) log2);
			} else if (log1.getClass().getSimpleName().equals("Logos") && log2.getClass().getSimpleName().equals("Logos")){
				// check if two logos match, or if there are such children...
				if (isChildOf(log2, log1)) {
					ans = true;
					common++;
				} else if (log1.name.equals(log2.name)){
					ans = true;
					common++;
				} else {
					// No inheritance relationship, not equal names - not similar
					return false;
				}
			} else {
				// type mismatch
				return false;
			}
			i++;
		} // logos loop
		
		if (common == 0) {
			// no common Logos - not similar, sorry!
			return false;
		}
		
		// check links
		int j = 0;
		while (j < clkl1.size()) {
			Link lk1 = clkl1.get(j);
			Link lk2;
			if (j < clkl2.size()) {
				lk2 = clkl2.get(j);
			} else {
				return false;
			}
			if (lk1.relationName.equals(lk2.relationName)  && lk1.generality * lk2.generality > 0) {
				ans = true;
			} else {
				return false;
			}
			j++;
		} // link loop
		
		return ans;
	}
	
	// they must be similar!
	public void specifyBranch(Branch effect, Branch sibling) {
		int i = 0;
		while (i < effect.containedLogosList.size()) {
			Logos l_eff = effect.containedLogosList.get(i);
			Logos l_sibl = sibling.containedLogosList.get(i);
			if (l_eff.getClass().getSimpleName().equals("Branch")) {
				specifyBranch((Branch) l_eff, (Branch) l_sibl);
			} else {
				if (isChildOf(l_sibl, l_eff)) {
					effect.containedLogosList.set(i, l_sibl);
					if (i < effect.containedLogosList.size() - 1) {
						// all but the last Logos
						if (i > 0) {
							// all but the first Logos
							Link in_lk = effect.containedLinkList.get(i - 1);
							in_lk.target = l_sibl;
						}
						Link out_lk = effect.containedLinkList.get(i);
						out_lk.source = l_sibl;
					} else {
						// the last Logos - only inward Link needs reset
						Link in_lk = effect.containedLinkList.get(i - 1);
						in_lk.target = l_sibl;
					}
				}
			}
			i++;
		}
	}
	
	public Branch findMatchingBranch (ArrayList<Branch> branchlist, Branch br) {
		for (Branch b : branchlist) {
			if (branchesMatch(br, b)) {
				return b;
			}
		}
		return null;
	}
	
	// returns links with same relationName and source Logos
	// excluding argument
	public List<Link> widthFixedSourceLinks(Link link) {
		ArrayList<Link> outs = link.source.outwardLinks;
		List<Link> sameNamed = linksByName(outs, link.relationName);
		sameNamed.remove(link);
		return sameNamed;
	}
	
	// returns links with same relationName and target Logos
	// excluding argument
	public List<Link> widthFixedTargetLink(Link link) {
		ArrayList<Link> ins = link.target.inwardLinks;
		List<Link> sameNamed = linksByName(ins, link.relationName);
		sameNamed.remove(link);
		return sameNamed;
	}
	
	public void removeLinksWithTarget(ArrayList<Link> list, String trg) {
		for (Link l : (ArrayList<Link>) list.clone()) {
			if(l.target.name.equals(trg)) {
				list.remove(l);
			}
		}
	}
	
	// checking by name, only one depth step search...
	public boolean isChildOf (Logos potChild, Logos potParent) {
		boolean ans;
		ans = hasLinkWithTarget(potChild, "is_a", potParent.name);
		return ans;
	}
	
	public Branch emptyNamedBranch (String name, long id) {
		Branch b = new Branch(name, id, new ArrayList<Link>(), new ArrayList<Link>());
		return b;
	}
	
	
	public void printLogosInfo (Logos l) {
		System.out.println("\tLogos " + l.id + " [" + l.name + "]");
		System.out.print("\tInward Link IDs: " );
		for (Link lk : l.inwardLinks)
			System.out.print(lk.id + " ");
		System.out.println();
		System.out.print("\tOutward Link IDs: " );
		for (Link lk : l.outwardLinks)
			System.out.print(lk.id + " ");
		System.out.println();
	}
	
	public void printLinkInfo (Link l) {
		System.out.println("\tLink "
		+ l.id
		+": "
		+ l.source.name
		+ "("
		+ l.source.id
		+ ")"
		+ "--["
		+ l.relationName
		+ " <"
		+ l.generality
		+ ","
		+ l.actuality
		+ ">]-->"
		+ l.target.name
		+ "("
		+ l.target.id
		+ ")");
	}
	
	public void printBranchInfo (Branch br) {
		System.out.println("\tBranch " + br.id + " [" + br.name + "]:");
		int linkIdx = 0;
		String line = "\t";
		for (Logos l_in : br.containedLogosList) {
			// for the last Logos, outward Link id = -1
			if (!br.containedLogosList.get(br.containedLogosList.size() - 1).equals(l_in)) {
				line += l_in.id + "/" + br.containedLinkList.get(linkIdx).id + "/";
				linkIdx++;
			} else {
				line += l_in.id + "/";
			}
		}
		System.out.println(line);
		line = "\t";
		linkIdx = 0;
		for (Logos l_in : br.containedLogosList) {
			// for the last Logos, outward Link id = -1
			if (!br.containedLogosList.get(br.containedLogosList.size() - 1).equals(l_in)) {
				line += l_in.name + "/" + br.containedLinkList.get(linkIdx).relationName + "/";
				linkIdx++;
			} else {
				line += l_in.name + "/";
			}
		}
		System.out.println(line);
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
	
	// Complete match
	public boolean matchesPattern(String[] tokens, String[] posTags, String pattern) {
		String[] patternTokens = pattern.split("_");
		int checksum = 0;	// will be equal patternTokens.length if matches
		int j = 0;	// index in tokens[]
		int i = 0;	// index in patternTokens[]
		while (i < patternTokens.length) {
			if (patternTokens[i].equals("...")) {
				// use up the token from the pattern formula
				checksum++;
				// search for the match with next token, not ... itself
				i++;
				if (i == patternTokens.length)
					break;
				// as long as tokens or POS are not equal for actual i and j
				while (!(tokens[j].equals(patternTokens[i]) || posTags[j].equals(patternTokens[i].replaceAll("~", "")))) {
					// in case of non-end tokens
					if (j < tokens.length - 1) {
						// increase input token index
						j++;
						// repeat while loop
						continue;
					} else {	// for the last token index
						break;	// escape loop
					}
				}
				// if we finally hit a match
				if (tokens[j].equals(patternTokens[i]) || posTags[j].equals(patternTokens[i].replaceAll("~", ""))) {
					// step to the next sentence token
					j++;
					// count
					checksum++;
					
				}

			} else {
				if (j == tokens.length)
					break;
				if (tokens[j].equals(patternTokens[i]) || posTags[j].equals(patternTokens[i].replaceAll("~", ""))) {
					checksum++;
					j++;
				}
			}
			i++;
		}
		if (checksum == patternTokens.length) {
			return true;
		} else {
			return false;
		}
	}

	// Lowercase match and POS tags using contains()
	public boolean matchesPatternLowercase(String[] tokensArg, String[] posTagsArg, String pattern) {
		String[] patternTokens = pattern.split("_");
		String[] tokens = new String[tokensArg.length];
		for (int tIdx = 0; tIdx < tokensArg.length; tIdx++) {
			tokens[tIdx] = tokensArg[tIdx];
		}
		String[] posTags = posTagsArg;
		int checksum = 0;	// will be equal patternTokens.length if matches
		int j = 0;	// index in tokens[]
		int i = 0;	// index in patternTokens[]
		
		for (int tIdx = 0; tIdx < tokens.length; tIdx++) {
			tokens[tIdx] = tokens[tIdx].toLowerCase();
		}
		
		for (int tIdx = 0; tIdx < patternTokens.length; tIdx++) {
			if (!patternTokens[tIdx].contains("~")) {
				patternTokens[tIdx] = patternTokens[tIdx].toLowerCase();
			}
		}
		
		while (i < patternTokens.length) {
			if (patternTokens[i].equals("...")) {
				// use up the token from the pattern formula
				checksum++;
				// search for the match with next token, not ... itself
				i++;
				if (i == patternTokens.length)
					break;
				// as long as tokens or POS are not equal for actual i and j
				while (!(tokens[j].equals(patternTokens[i]) || posTags[j].contains(patternTokens[i].replaceAll("~", "")))) {
					// in case of non-end tokens
					if (j < tokens.length - 1) {
						// increase input token index
						j++;
						// repeat while loop
						continue;
					} else {	// for the last token index
						break;	// escape loop
					}
				}
				// if we finally hit a match
				if (tokens[j].equals(patternTokens[i]) || posTags[j].contains(patternTokens[i].replaceAll("~", ""))) {
					// step to the next sentence token
					j++;
					// count
					checksum++;
					
				}

			} else {
				if (j == tokens.length)
					break;
				if (tokens[j].equals(patternTokens[i]) || posTags[j].contains(patternTokens[i].replaceAll("~", ""))) {
					checksum++;
					j++;
				}
			}
			i++;
		}
		if (checksum == patternTokens.length) {
			if (verbose) {
				System.out.println("UTILS: Matched the pattern " + pattern);
			}
			return true;
		} else {
			return false;
		}
	}
	
	// Returns a list of words that match POS tags (full match) in a given pattern
	public ArrayList<String> wordsMatchingPOSTag(String[] tokensArg, String[] posTagsArg, String pattern) {
		ArrayList<String> ans = new ArrayList<String>();
		
		// basically, repeat pattern matching and add words matching POS to the answer list 
		String[] patternTokens = pattern.split("_");
		String[] tokens = new String[tokensArg.length];
		for (int tIdx = 0; tIdx < tokensArg.length; tIdx++) {
			tokens[tIdx] = tokensArg[tIdx];
		}
		String[] posTags = posTagsArg;
		
		int j = 0;	// index in tokens[]
		int i = 0;	// index in patternTokens[]
		
		for (int tIdx = 0; tIdx < tokens.length; tIdx++) {
			tokens[tIdx] = tokens[tIdx].toLowerCase();
		}
		
		for (int tIdx = 0; tIdx < patternTokens.length; tIdx++) {
			if (!patternTokens[tIdx].contains("~")) {
				patternTokens[tIdx] = patternTokens[tIdx].toLowerCase();
			}
		}
		
		while (i < patternTokens.length) {
			if (patternTokens[i].equals("...")) {
				// use up the token from the pattern formula
				
				// search for the match with next token, not ... itself
				i++;
				if (i == patternTokens.length)
					break;
				// as long as tokens or POS are not equal for actual i and j
				while (!(tokens[j].equals(patternTokens[i]) || posTags[j].contains(patternTokens[i].replaceAll("~", "")))) {
					// in case of non-end tokens
					if (j < tokens.length - 1) {
						// increase input token index
						j++;
						// repeat while loop
						continue;
					} else {	// for the last token index
						break;	// escape loop
					}
				}
				// if we finally hit a match
				if (tokens[j].equals(patternTokens[i]) || posTags[j].contains(patternTokens[i].replaceAll("~", ""))) {
					// in this case, only POS tags are relevant
					if (posTags[j].equals(patternTokens[i].replaceAll("~", ""))) {
						ans.add(tokens[j]);
					}
					// step to the next sentence token
					j++;
				}

			} else {
				if (j == tokens.length)
					break;
				if (tokens[j].equals(patternTokens[i]) || posTags[j].contains(patternTokens[i].replaceAll("~", ""))) {
					// in this case, only POS tags are relevant
					if (posTags[j].equals(patternTokens[i].replaceAll("~", ""))) {
						ans.add(tokens[j]);
					}
					// step to the next sentence token
					j++;
				}
			}
			i++;
		}
		return ans;
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
	
	public boolean hasLinkWithTarget (Logos a, String linkName, String targetName) {
		boolean ans = false;
		for (Link l : a.outwardLinks) {
			if (l.relationName.equals(linkName) && l.target.name.equals(targetName)) {
				ans = true;
				break;
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
	
	// filter link list by maximum actuality of its links
	public ArrayList<Link> filterLinksByActuality(ArrayList<Link> list, double a_min) {
		ArrayList<Link> ans = new ArrayList<Link>();
		for (Link l : list) {
			if (l.actuality >= a_min) {
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
	
	public int randomChoice (double[] probabilityDistribution) {
		int ans = 0;
		double rnd = Math.random();
		double sum1 = 0;
		double sum2 = 0;
		for (int i = 0; i < probabilityDistribution.length; i++) {
			if (i != 0)
				sum1 += probabilityDistribution[i-1];
			sum2 += probabilityDistribution[i];
			if (rnd >= sum1 && rnd < sum2) {
				ans = i;
				break;
			}
		}
		return ans;
	}
	
	public String randomStringFromArray (String[] array) {
		int idx = (int) (Math.random() * array.length);
		return array[idx];
	}
	
	public Logos randomLogosFromList (List<Logos> list) {
		double[] distr = new double[list.size()];
		for (int i = 0; i < list.size(); i++) {
			distr[i] = 1 / (double) list.size();
		}
		Logos ans = list.get(randomChoice(distr));
		return ans;
	}
	
	public Link randomLinkFromList (List<Link> list) {
		double[] distr = new double[list.size()];
		for (int i = 0; i < list.size(); i++) {
			distr[i] = 1 / (double) list.size();
		}
		Link ans = list.get(randomChoice(distr));
		return ans;
	}
	
	public boolean stringArrayContains(String[] array, String word) {
		for (int i = 0; i < array.length; i++) {
			if (array[i].equals(word)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean stringArrayContainsIgnoreCase(String[] array, String word) {
		for (int i = 0; i < array.length; i++) {
			if (array[i].equalsIgnoreCase(word)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean stringArraysCut(String[] array1, String[] array2) {
		for (int i = 0; i < array1.length; i++) {
			if (stringArrayContains(array2, array1[i])) {
				return true;
			}
		}
		return false;
	}
	
	public List<String> commonStrings(String[] array1, String[] array2) {
		List<String> ans = new ArrayList<String>();
		for (int i = 0; i < array1.length; i++) {
			if (stringArrayContains(array2, array1[i])) {
				ans.add(array1[i]);
			}
		}
		return ans;
	}
	
	public List<String> commonStrings(ArrayList<String> array1, ArrayList<String> array2) {
		List<String> ans = new ArrayList<String>();
		for (String s1 : array1) {
			if (array2.contains(s1)) {
				ans.add(s1);
			}
		}
		return ans;
	}
	
	public float jaccardIndex(String[] array1, String[] array2) {
		int common = commonStrings(array1, array2).size();
		return ((float) common) / (array1.length + array2.length - common);
	}
	
	public float jaccardIndex(ArrayList<String> array1, ArrayList<String> array2) {
		int common = commonStrings(array1, array2).size();
		return ((float) common) / (array1.size() + array2.size() - common);
	}
	
	public float nGramIndex(String[] array1, String[] array2) {
		List<String[]> pairs1 = new ArrayList<String[]>();
		List<String[]> pairs2 = new ArrayList<String[]>();
		for (int i = 0; i < array1.length - 1; i++) {
			String[] pair = new String[2];
			pair[0] = array1[i];
			pair[1] = array1[i + 1];
			pairs1.add(pair);
		}
		for (int i = 0; i < array2.length - 1; i++) {
			String[] pair = new String[2];
			pair[0] = array2[i];
			pair[1] = array2[i + 1];
			pairs2.add(pair);
		}
		int common = 0;
		// not the most effective way, but the code is simpler
		for (int i = 0; i < pairs1.size(); i++) {
			for (int j = 0; j < pairs2.size(); j++) {
				if (pairs1.get(i).equals(pairs2.get(j))) {
					common++;
				}
			}
		}
		common = common / 2;
		return ((float) common) / (pairs1.size() + pairs2.size() - common);
	}
	
	public double minOfTwo(double one, double two) {
		if (one >= two) {
			return two;
		} else {
			return one;
		}
	}
	
	public double maxOfTwo(double one, double two) {
		if (one >= two) {
			return one;
		} else {
			return two;
		}
	}

	public boolean branchContainsLink(Branch branch, String relName, boolean positive) {
		boolean ans = false;
		for (Link l : branch.containedLinkList) {
			if (l.getRelationName().equals(relName)) {
				boolean p = l.getGenerality() > 0;
				if (positive == p) {
					ans = true;
					break;
				}
			}
		}
		return ans;
	}
	
	// Branch with 3 elements: A --l--> B
	public Branch constructSimpleBranch(String logA, String link, String logB) {
		Branch descr = emptyNamedBranch("branch", -1);
		Logos A = emptyNamedLogos(logA, -1);
		Logos B = emptyNamedLogos(logB, -1);
		Link l = emptyNamedLink(link, -1);
		l.source = A;
		l.target = B;
		l.generality = generality(1, MainClass.belief);
		A.outwardLinks.add(l);
		B.inwardLinks.add(l);
		ArrayList<Logos> cll = new ArrayList<Logos>();
		cll.add(A);
		cll.add(B);
		ArrayList<Link> clkl = new ArrayList<Link>();
		clkl.add(l);
		descr.setContainedLogosList(cll);
		descr.setContainedLinkList(clkl);
		return descr;
	}
	
	// Returns the newest Branch, in which this Logos appears on the top level.
	// If there is no Branch containing this Logos, returns null.
	// Equality of Logos is decided by deep comparison.
	public Branch directShellBranch(Logos l, DatabaseInterface db) {
		ArrayList<Branch> candidates = new ArrayList<Branch>();
		for (Branch b : db.branchList) {
			for (Logos l_cont : b.containedLogosList) {
				// Check equality by deep comparison (field equality)
				if (l_cont.getName().equals(l.name)
						&& l_cont.getId() == l.id
						&& l_cont.getOutwardLinks().equals(l.outwardLinks)
						&& l_cont.getInwardLinks().equals(l.inwardLinks)) {
					candidates.add(b);
				}
			}
		}
		if (candidates.isEmpty()) {
			return null;
		} else {
			return candidates.get(candidates.size() - 1);
		}
	}
	
	public ArrayList<ArrayList<String>> chainsAsStringArraysFrom(Logos logos, DatabaseInterface db) {
		
		// dummy Logos object
		Logos l = new Logos(logos.name, -1, logos.outwardLinks, logos.inwardLinks);
		
		ArrayList<ArrayList<String>> chainSets = new ArrayList<ArrayList<String>>();
			
		for (Link outlink : l.outwardLinks) {

			// Create a chain for each outlink of source Logos
			ArrayList<String> ch = new ArrayList<String>();

			boolean terminal = false;

			while (!terminal) {

				if (directShellBranch(l, db) != null) {

					// The Logos is in a shell
					/*
					for (int i = 0; i < directShellBranch(l, db).containedLogosList.size(); i++) {

						if (i < directShellBranch(l, db).containedLogosList.size() - 1) {

							Logos l1 = directShellBranch(l, db).containedLogosList.get(i);
							Link lk1 = directShellBranch(l, db).containedLinkList.get(i);

							// If the Logos is a Branch, get its deepest representation recursively

							if (l1.getClass().getSimpleName().equals("Branch")) {
								ch.addAll(branchStringArray((Branch) l1));
							} else {
								ch.add(l1.getName());
							}

							ch.add(lk1.getRelationName());

						} else {	// last Logos in Branch
							
							Logos l1 = directShellBranch(l, db).containedLogosList.get(i);

							// If the Logos is a Branch, get its deepest representation recursively

							if (l1.getClass().getSimpleName().equals("Branch")) {
								ch.addAll(branchStringArray((Branch) l1));
							} else {
								ch.add(l1.getName());
							}
							
						}

					}*/
					
					// Does the shell Branch not have outlinks?
					if (directShellBranch(l, db).outwardLinks.isEmpty()) {
						chainSets.add(ch);
						terminal = true;
					} else {
						// Raise the level of Logos
						l = directShellBranch(l, db);
					}

				} else {
					// Top level Logos/Branch, not encapsuled
					
					if (l.getClass().getSimpleName().equals("Branch")) {
						ch.addAll(branchStringArray((Branch) l));
					} else {
						ch.add(l.getName());
					}

					// this creates an artifact: doubled outlink in the array, but for searches it's not very harmful
					ch.add(outlink.getRelationName());
					
					// Does the Logos not have outlinks?
					if (l.outwardLinks.isEmpty()) {
						chainSets.add(ch);
						terminal = true;
					} else {
						// In our Branch-Logos structure the outward link should actually be alone...
						ch.add(l.outwardLinks.get(0).getRelationName());
						l = l.outwardLinks.get(0).getTarget();
					}
					
				}

			}
		}

		return chainSets;
	}
	
	public ArrayList<String> branchStringArray(Branch b) {
		
		ArrayList<String> ans = new ArrayList<String>();
		
		for (int i = 0; i < b.containedLogosList.size(); i++) {
			
			Logos l1 = b.containedLogosList.get(i);
			
			if (l1.getClass().getSimpleName().equals("Branch")) {
				ans.addAll(branchStringArray((Branch) l1));
			} else {
				ans.add(l1.getName());
			}
			
			if (i < b.containedLogosList.size() - 1) {
				
				Link lk1 = b.containedLinkList.get(i);
			
				ans.add(lk1.getRelationName());
				
			}
			
		}
		
		return ans;
	}
	
	public ArrayList<String> beautifyChainStrings(ArrayList<String> listOfTokens) {
		
		ArrayList<String> cleanVersion = new ArrayList<String>();
		
		for (int i = 0; i < listOfTokens.size(); i++) {
			
			// every token but the last
			if (i < listOfTokens.size() - 1) {
				// remove doubled links
				if (listOfTokens.get(i).equals(listOfTokens.get(i + 1))) {
					continue;
				}
			}
			
			// don't translate such things as #ENTITY or #PROPERTY, and end there
			if (i < listOfTokens.size() - 2) {
				// remove doubled links
				if (listOfTokens.get(i + 2).equals("#ENTITY")
						|| listOfTokens.get(i + 2).equals("#PROPERTY")
						|| listOfTokens.get(i + 2).equals("#VERB")) {
					break;
				}
			}
			
			// remove underscores in Link names if contained
			if (listOfTokens.get(i).contains("_")) {
				String clean = listOfTokens.get(i).replaceAll("_", " ");
				cleanVersion.add(clean);
			} else {
				cleanVersion.add(listOfTokens.get(i));
			}

		}
		
		return cleanVersion;
	}
	
}