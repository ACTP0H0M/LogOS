package logos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProblemFinder {
	
	boolean verbose;
	double a_min;
	
	public HashMap<String, Float> severityMap;
	public Set<Map.Entry<String, Float>> severityKeyset;
	
	public ProblemFinder(int verbosity, double a_min) {
		if (verbosity >= 2) {
			verbose = true;
		}
		this.a_min = a_min;
		severityMap = new HashMap<String, Float>();
		severityMap.put("CONTRADICTION", 0.9f);
		severityMap.put("MISSING_RESOURCE", 0.8f);
		severityMap.put("UNKNOWN_REASON", 0.5f);
		severityMap.put("UNKNOWN_PURPOSE", 0.4f);
		severityMap.put("NO_INHERITANCE", 0.7f);
		severityMap.put("NO_DESCRIPTIONS", 0.4f);
		severityMap.put("UNKNOWN_ACTION_REQUIREMENTS", 0.9f);
		severityMap.put("UNKNOWN_PLACE", 0.8f);
		severityMap.put("UNKNOWN_TIME_FUTURE", 0.5f);
		severityMap.put("UNKNOWN_TIME_PAST", 0.5f);
		severityMap.put("UNKNOWN_METHOD", 0.8f);
		severityMap.put("UNKNOWN_PROPERTY", 0.6f);
		severityMap.put("UNKNOWN_OBJECT", 0.7f);
		severityMap.put("UNKNOWN_ACTION", 0.7f);
		severityMap.put("UNKNOWN_SUBJECT", 0.7f);
		severityMap.put("PHILOSOPHICAL_QUESTION", 0.5f);
		severityMap.put("COMMAND", 1.0f);
		severityKeyset = severityMap.entrySet();
	}
	
	/*
	 *  This method doesn't modify the hypergraph, it only searches for problems
	 *  using diverse strategies.
	 */
	public ArrayList<Problem> findProblems(DatabaseInterface database, Utils utils) {
		
		ArrayList<Problem> problems = new ArrayList<Problem>();
		
		List<Logos> actualLogosList = utils.filterByActuality(database.logosList, a_min);
		
		// Iterate only over actual logos
		for (Logos logos : actualLogosList) {
			
			ArrayList<Link> outlinks = logos.outwardLinks;
			
			for (Link link : outlinks) {
				
				// some cases when no Problems should be generated
				if (link.getTarget().getName().equals("#ENTITY")
						|| link.getTarget().getName().equals("#VERB")
						|| link.getTarget().getName().equals("#PROPERTY")) {
					continue;
				}
				
				// Find CONTRADICTION
				// The simplest form of a contradiction: two Links with same names, same targets and sources
				// but different generality signs.
				ArrayList<Link> identicalLinks = utils.linksByName(utils.linksWithTarget(outlinks, link.target.name), link.relationName);
				
				if (link.target.getClass().getSimpleName().equals("Branch")) {
					identicalLinks.addAll(utils.linksWithTargetBranch(outlinks, (Branch) link.target));
				}
				
				identicalLinks.remove(link);
				// Now, identicalLinks doesn't contain the argument Link itself
				// Contradictions are the Links with opposite generality sign
				ArrayList<Link> contradictions = utils.filterLinksByGeneralitySign(identicalLinks, link.generality < 0);
				
				if (!contradictions.isEmpty()) {
					Link contra = contradictions.get(0);
					Problem p = new Problem();
					p.linkCollection.add(link);
					p.linkCollection.add(contra);
					p.internal = true;
					p.severity = severityMap.get("CONTRADICTION");
					p.type = "CONTRADICTION";
					problems.add(p);
					write("found a CONTRADICTION between following Links:");
					if (verbose) {
						utils.printLinkInfo(link);
						utils.printLinkInfo(contra);
					}
				}
				
				// Find UNKNOWN_REASON
				// Links with both empty logic arguments (arg1 and arg2), meaning that Logos doesn't know
				// why this Link exists.
				if (link.arg1 == null && link.arg2 == null) {
					Problem p = new Problem();
					p.linkCollection.add(link);
					p.internal = true;
					p.severity = severityMap.get("UNKNOWN_REASON");
					p.type = "UNKNOWN_REASON";
					problems.add(p);
					write("UNKNOWN_REASON detected for the Link:");
					if (verbose) {
						utils.printLinkInfo(link);
					}
				}
				
				// Find MISSING_RESOURCE
				if (link.getRelationName().equals("task_link")) {
					Logos task = link.target;
					Logos actor = link.source;
					// If the actor doesn't have something that is needed for the task,
					// MISSING_RESOURCE problem is raised.
					ArrayList<String> reqs = new ArrayList<String>();
					for (Link reqLink : utils.filterLinksByGeneralitySign(utils.linksByName(task.inwardLinks, "is_needed_to"), true)) {
						String reqName = reqLink.getSource().getName();
						reqs.add(reqName);
					}
					ArrayList<String> actorHas = new ArrayList<String>();
					for (Link haveLink : utils.filterLinksByGeneralitySign(utils.linksByName(actor.outwardLinks, "have"), true)) {
						String objName = haveLink.getTarget().getName();
						actorHas.add(objName);
					}
					int checksum = 0;
					for (String req : reqs) {
						for (String obj : actorHas) {
							if (req.equals(obj)) {
								checksum++;
							}
						}
					}
					if (checksum < reqs.size()) {
						Problem p = new Problem();
						p.linkCollection.add(link);
						p.internal = true;
						p.severity = severityMap.get("MISSING_RESOURCE");
						p.type = "MISSING_RESOURCE";
						problems.add(p);
						write("MISSING_RESOURCE detected for the task Link:");
						if (verbose) {
							utils.printLinkInfo(link);
						}
					}
				}
				
				// Find UNKNOWN_METHOD
				if (link.getRelationName().equals("task_link")) {
					Logos task = link.target;
					ArrayList<Link> methodLinks = utils.linksByName(task.outwardLinks, "method_link");
					if (methodLinks.isEmpty()) {
						Problem p = new Problem();
						p.logosCollection.add(task);
						p.internal = true;
						p.severity = severityMap.get("UNKNOWN_METHOD");
						p.type = "UNKNOWN_METHOD";
						problems.add(p);
						write("UNKNOWN_METHOD detected for the task Logos:");
						if (verbose) {
							utils.printLogosInfo(logos);
						}
					}
				}
				
				
				
				
				
			}// end outlink loop
			
			// Problems connected to Logos object rather than a Link
			
			// Find UNKNOWN_PURPOSE
			// TODO Ideally, check whether the Branch has do/did Link inside
			if (logos.getClass().getSimpleName().equals("Branch") || utils.hasLinkWithTarget(logos, "is_a", "#VERB")) {
				// check if there is an outward "in_order_to" link
				boolean hasPurpose = false;
				for (Link ol : logos.outwardLinks) {
					if (ol.getRelationName().equals("in_order_to")) {
						hasPurpose = true;
						break;
					}
				}
				if (hasPurpose == false) {
					Problem p = new Problem();
					p.logosCollection.add(logos);
					p.internal = true;
					p.severity = severityMap.get("UNKNOWN_PURPOSE");
					p.type = "UNKNOWN_PURPOSE";
					problems.add(p);
					write("UNKNOWN_PURPOSE detected for the Logos/Branch:");
					if (verbose) {
						switch(logos.getClass().getSimpleName()) {
						case "Logos": utils.printLogosInfo(logos); break;
						case "Branch" : utils.printBranchInfo((Branch) logos);
						}
					}
				}
			}
			
			// Find NO_INHERITANCE
			// Only positive definitions are seen as inheritance.
			if (utils.hasLinkWithTarget(logos, "is_a", "#ENTITY")) {
				ArrayList<String> parents = new ArrayList<String>();
				for (Link inheritance : utils.filterLinksByGeneralitySign(utils.linksByName(logos.outwardLinks, "is_a"), true)) {
					if (!inheritance.getTarget().getName().equals("#ENTITY")) {
						parents.add(inheritance.getTarget().getName());
					}
				}
				if (parents.isEmpty()) {
					Problem p = new Problem();
					p.logosCollection.add(logos);
					p.internal = true;
					p.severity = severityMap.get("NO_INHERITANCE");
					p.type = "NO_INHERITANCE";
					problems.add(p);
					write("NO_INHERITANCE detected for the Logos:");
					if (verbose) {
						utils.printLogosInfo(logos);
					}
				}
			}
			
			// Find NO_DESCRIPTIONS
			if (utils.hasLinkWithTarget(logos, "is_a", "#ENTITY")) {
				if (utils.linksByName(logos.outwardLinks, "is").isEmpty()) {
					Problem p = new Problem();
					p.logosCollection.add(logos);
					p.internal = true;
					p.severity = severityMap.get("NO_DESCRIPTIONS");
					p.type = "NO_DESCRIPTIONS";
					problems.add(p);
					write("NO_DESCRIPTIONS detected for the Logos:");
					if (verbose) {
						utils.printLogosInfo(logos);
					}
				}
			}
			
			// Find UNKNOWN_ACTION_REQUIREMENTS
			// Isolated action Logos
			if (utils.hasLinkWithTarget(logos, "is_a", "#VERB")
					&& !utils.stringArrayContains(utils.uninformativeVerbs, logos.getName())) {
				if (utils.linksByName(logos.inwardLinks, "is_needed_to").isEmpty()) {
					Problem p = new Problem();
					p.logosCollection.add(logos);
					p.internal = true;
					p.severity = severityMap.get("UNKNOWN_ACTION_REQUIREMENTS");
					p.type = "UNKNOWN_ACTION_REQUIREMENTS";
					problems.add(p);
					write("UNKNOWN_ACTION_REQUIREMENTS detected for the Logos:");
					if (verbose) {
						utils.printLogosInfo(logos);
					}
				}
			}
			// action Branch
			if (logos.getClass().getSimpleName().equals("Branch") &&
					(utils.branchContainsLink((Branch) logos, "do", true) ||
							utils.branchContainsLink((Branch) logos, "did", true))) {
				if (utils.linksByName(logos.inwardLinks, "is_needed_to").isEmpty()) {
					Problem p = new Problem();
					p.logosCollection.add(logos);
					p.internal = true;
					p.severity = severityMap.get("UNKNOWN_ACTION_REQUIREMENTS");
					p.type = "UNKNOWN_ACTION_REQUIREMENTS";
					problems.add(p);
					write("UNKNOWN_ACTION_REQUIREMENTS detected for the Branch:");
					if (verbose) {
						utils.printBranchInfo((Branch) logos);
					}
				}
			}
			
			// Find UNKNOWN_PLACE
			// for entities and Branches
			ArrayList<Link> isLinks = utils.linksByName(outlinks, "is");
			isLinks = utils.filterLinksByGeneralitySign(isLinks, true);
			ArrayList<Link> locationLinks = new ArrayList<Link>();
			if (utils.hasLinkWithTarget(logos, "is_a", "#ENTITY")
					|| logos.getClass().getSimpleName().equals("Branch")) {
				for (Link il : isLinks) {
					if (il.target.getClass().getSimpleName().equals("Branch")) {
						for (Link cl : ((Branch) il.target).containedLinkList) {
							if (cl.getRelationName().equals("prep")
									&& utils.stringArrayContains(utils.spatialPrepositions, cl.getTarget().getName())) {
								locationLinks.add(il);
							}
						}
					}
				} 
			}
			// If it's not a verb and location is unknown, raise the problem
			if (!utils.hasLinkWithTarget(logos, "is_a", "#VERB")
					&& locationLinks.isEmpty()) {
				Problem p = new Problem();
				p.logosCollection.add(logos);
				p.internal = true;
				p.severity = severityMap.get("UNKNOWN_PLACE");
				p.type = "UNKNOWN_PLACE";
				problems.add(p);
				write("UNKNOWN_PLACE detected for the Logos/Branch:");
				if (verbose) {
					utils.printLogosInfo(logos);
				}
			}
			// for verbs
			if (utils.hasLinkWithTarget(logos, "is_a", "#VERB")) {
				ArrayList<Link> howLinks = utils.linksByName(outlinks, "how");
				howLinks = utils.filterLinksByGeneralitySign(howLinks, true);
				ArrayList<Link> locLinks = new ArrayList<Link>();
				for (Link il : howLinks) {
					if (il.getTarget().getClass().getSimpleName().equals("Branch")) {
						for (Link cl : ((Branch) il.getTarget()).containedLinkList) {
							if (cl.getRelationName().equals("prep")
									&& utils.stringArrayContains(utils.spatialPrepositions,
											cl.getTarget().getName())) {
								locLinks.add(il);
							}
						}
					}
				}
				if (locLinks.isEmpty()) {
					Problem p = new Problem();
					p.logosCollection.add(logos);
					p.internal = true;
					p.severity = severityMap.get("UNKNOWN_PLACE");
					p.type = "UNKNOWN_PLACE";
					problems.add(p);
					write("UNKNOWN_PLACE detected for the Logos:");
					if (verbose) {
						utils.printLogosInfo(logos);
					}
				}
			}
			
			// Find UNKNOWN_PROPERTY
			if (utils.hasLinkWithTarget(logos, "is_a", "#ENTITY")
					|| logos.getClass().getSimpleName().equals("Branch")) {
				// use isLinks from above
				boolean hasProperty = false;
				for (Link is_link : isLinks) {
					if (utils.hasLinkWithTarget(is_link.target, "is_a", "#PROPERTY")) {
						hasProperty = true;
						break;
					}
				}
				if (!hasProperty) {
					Problem p = new Problem();
					p.logosCollection.add(logos);
					p.internal = true;
					p.severity = severityMap.get("UNKNOWN_PROPERTY");
					p.type = "UNKNOWN_PROPERTY";
					problems.add(p);
					write("UNKNOWN_PROPERTY detected for the Logos/Branch:");
					if (verbose) {
						utils.printLogosInfo(logos);
					}
				}
			}
			
			// Find UNKNOWN_OBJECT
			if (utils.hasLinkWithTarget(logos, "is_a", "#VERB")) {
				boolean hasDobj = false;
				for (Link out_link: outlinks) {
					if (out_link.getRelationName().equals("what")) {
						hasDobj = true;
						break;
					}
				}
				if (!hasDobj) {
					Problem p = new Problem();
					p.logosCollection.add(logos);
					p.internal = true;
					p.severity = severityMap.get("UNKNOWN_OBJECT");
					p.type = "UNKNOWN_OBJECT";
					problems.add(p);
					write("UNKNOWN_OBJECT detected for the Logos:");
					if (verbose) {
						utils.printLogosInfo(logos);
					}
				}
			}
			
			// Find UNKNOWN_ACTION
			if (utils.hasLinkWithTarget(logos, "is_a", "#ENTITY")
					|| logos.getClass().getSimpleName().equals("Branch")) {
				boolean hasAction = false;
				for (Link out_link: outlinks) {
					if (out_link.getRelationName().equals("do")
							|| out_link.getRelationName().equals("did")) {
						hasAction = true;
						break;
					}
				}
				if (!hasAction) {
					Problem p = new Problem();
					p.logosCollection.add(logos);
					p.internal = true;
					p.severity = severityMap.get("UNKNOWN_ACTION");
					p.type = "UNKNOWN_ACTION";
					problems.add(p);
					write("UNKNOWN_ACTION detected for the Logos/Branch:");
					if (verbose) {
						utils.printLogosInfo(logos);
					}
				}
			}
			
			// P.S.
			// UNKNOWN_SUBJECT, PHILOSOPHICAL_QUESTION and COMMAND are never internal Problems
			
		}// end actual logos loop
		
		return problems;
		
	}
	
	void write(String str) {
		if (verbose) {
			System.out.println("PROBLEM_FINDER: " + str);
		}
	}
	
}