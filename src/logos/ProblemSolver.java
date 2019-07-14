package logos;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.apache.jena.ontology.*;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;

import com.wolfram.alpha.WAEngine;
import com.wolfram.alpha.WAException;
import com.wolfram.alpha.WAPlainText;
import com.wolfram.alpha.WAPod;
import com.wolfram.alpha.WAQuery;
import com.wolfram.alpha.WAQueryResult;
import com.wolfram.alpha.WASubpod;

import opennlp.tools.parser.Parser;
import toolkit.TextMethods;

public class ProblemSolver extends Object{
	
	// PUT YOUR APPID HERE:
    private static String appid = "XXXXX";
	
	boolean verbose;
	
	// How many times does Logos try applying logic rules
	// on the database before giving up and asking user.
	private int inferenceSteps;
	
	// When a CONTRADICTION is solved, how much does LogOS trust the
	// more actual Link of 2 contradicting Links.
	// This should be very close to 0.5!
	private double trustInNew = 0.6;
	
	public HashMap<String, String> linkPlusMap;
	public Set<Map.Entry<String, String>> linkPlusKeyset;
	public HashMap<String, String> linkMinusMap;
	public Set<Map.Entry<String, String>> linkMinusKeyset;
	
	// Arrays to pick random phrases from
	public String[] okWords = {"OK",
			"Okay",
			"Alright",
			"I see"};
	public String[] avoidanceAdvicePhrases = {"I recommend you avoiding ",
			"You should avoid ",
			"Maybe it would help if you avoided "};
	public String[] curiousityPhrases = {"I'd like to know ",
			"I'm interested ",
			"I'm curious ",
			"I have a question: ",
			"I'm wondering "};
	public String[] contradictionPhrases = {"Sorry, I have some contradicting information. ",
			"I'm a bit confused, master. "};
	public String[] userHelpRequests = {"Could you help me with this, please? ",
			"I think I can't solve this problem without your help. ",
			"I would like you to help me with this question if you don't mind. ",
			"I have a question. "};
	public String[] answerRefusals = {"I don't want to answer this question.",
			"Unfortunately I can't answer this question.",
			"I can't help you with this, sorry."};
	public String[] dontKnowPhrases = {"I don't know.",
			"Who knows?",
			"idk",
			"I have no clue.",
			"No idea.",
			"I'm clueless about this."};
	public String[] apologies = {"I'm very sorry, master.",
			"Sorry, I won't do this again.",
			"My apologies, master."};
	public String[] thanks = {"Thank you, master!",
			"Thanks!",
			"I appreciate your help, thank you."};
	
	public ArrayList<String> usedThoughtText = new ArrayList<String>();
	
	public ProblemSolver(int verbosity) {
		if (verbosity >= 2) {
			verbose = true;
		}
		
		linkPlusMap = new HashMap<String, String>();
		linkPlusMap.put("is_a", " is a ");
		linkPlusMap.put("is_component_of", " is a part of ");
		linkPlusMap.put("is", " is ");
		linkPlusMap.put("can_be", " can be ");
		linkPlusMap.put("can_do", " can ");
		linkPlusMap.put("is_needed_to", " is necessary to ");
		linkPlusMap.put("do", " ");
		linkPlusMap.put("have", " have ");
		linkPlusMap.put("equals_to", " is ");
		linkPlusMap.put("whom", " ");
		linkPlusMap.put("what", " ");
		linkPlusMap.put("similar_to", " is similar to ");
		linkPlusMap.put("how", " ");
		linkPlusMap.put("prep", "");
		linkPlusKeyset = linkPlusMap.entrySet();
		
		linkMinusMap = new HashMap<String, String>();
		linkMinusMap.put("is_a", " isn't a ");
		linkMinusMap.put("is", " isn't ");
		linkMinusMap.put("is_needed_to", " isn't necessary to ");
		linkMinusMap.put("can_do", " can't ");
		linkMinusMap.put("can_be", " can't be ");
		linkMinusMap.put("have", " don't have ");
		linkMinusMap.put("equals_to", " isn't ");
		linkMinusMap.put("is_component_of", " isn't a part of ");
		linkMinusMap.put("similar_to", "isn't similar to");
		linkMinusMap.put("do", " don't ");
		linkMinusMap.put("how", " not ");
		linkMinusKeyset = linkMinusMap.entrySet();
		
		
	}
	
	// Solves given Problem using internal knowledge or by simply asking the user
	// TODO Try solving Problems by updating the database x times
	public Thought solveProblem(Problem problem, DatabaseInterface database, Utils utils) {
		Thought th = new Thought();
		th.sourceProblem = problem;
		if (problem.linkCollection == null) {
			return th;
		}
		switch(problem.type) {
		case "CONTRADICTION" :
		{
			if (problem.internal == true) {
				// Methods for solving internal CONTRADICTIONS
				Link l1 = problem.linkCollection.get(0);
				Link l2 = problem.linkCollection.get(1);
				// g1 is the generality of more actual Link
				double g1 = 0;
				double g2 = 0;
				if (l1.getActuality() >= l2.getActuality()) {
					g1 = l1.getGenerality();
					g2 = l2.getGenerality();
				} else {
					g1 = l2.getGenerality();
					g2 = l1.getGenerality();
				}
				
				// The merged Link has generality of the average evidence
				int weightedEvidence = (int) (trustInNew * utils.evidenceFromGenerality(g1, MainClass.belief)
						+ (1 - trustInNew) * utils.evidenceFromGenerality(g2, MainClass.belief));
				double gNew = utils.generality(weightedEvidence, MainClass.belief);
				// The second problem Link is deleted, the first is modified
				database.linkList.remove(l2);
				l2.source.outwardLinks.remove(l2);
				l2.target.inwardLinks.remove(l2);
				l1.actuality = utils.maxOfTwo(l1.actuality, l2.actuality);
				l1.generality = gNew;
				
				problem.solved = true;

				/*
				th.text = utils.randomStringFromArray(contradictionPhrases)
						+ " What seems true to you: "
						+ translateForHuman(problem.linkCollection.get(0))
						+ " or "
						+ translateForHuman(problem.linkCollection.get(1))
						+ "?";
						*/
			} else {
				// external CONTRADICTION
			}
		};
		break;
		case "MISSING_RESOURCE" :{
			if (problem.internal == true) {
				// For example, if the COMMAND was to wash the dishes, but LogOS doesn't have water.
				Logos req = problem.logosCollection.get(0);
				// Since LogOS is a chatbot for now, it cannot get physical objects.
				Thought solution = new Thought();
				solution.text = "I don't have " + req.getName() + ".";
				solution.sourceProblem = problem;
				solution.priority = (float) problem.severity;
				// TODO
				// If LogOS gets an embodiment, the algorithms to move the body are called here...
				problem.solved = false;	// for the time being
			} else {
				// external MISSING_RESOURCE Problem
				// actually not used
			}
		};
		break;
		case "UNKNOWN_REASON" :
		{
			// If LogOS knows how this link was generated...
			if (problem.linkCollection.get(0).arg1 != null) {
				// ...it just tells us which links were used by inference engine
				// No internal problems should be handled here!
				if (problem.linkCollection.get(0).arg2 != null) {
					// LogOS had 2 arguments to make this conclusion
					Link arg1 = problem.linkCollection.get(0).arg1;
					String translation1 = translateForHuman(arg1);
					Link arg2 = problem.linkCollection.get(0).arg2;
					String translation2 = translateForHuman(arg2);
					th.text = "Because " + translation1 + " and " + translation2 + ".";
				} else {
					// LogOS had 1 argument to make this conclusion
					Link arg = problem.linkCollection.get(0).arg1;
					String translation = translateForHuman(arg);
					th.text = "Because " + translation + ".";
				}
				problem.solved = true;
			} else {
				if (!problem.internal) {
					// LogOS has no information about the reason.
					// He could try to update database, check DBPedia, WordNet or 
					// make assumptions (induction/deduction/abduction).
					
					// Finally, he can just accept the reality without asking for reason.
					th.text = "It's just how it is.";
				} else {
					// Solve internal UNKNOWN_REASON problems here.
					// At this point, very soon LogOS will discover that after asking "Why?" iteratively
					// there comes a question that has no answer based on experience or data.
					// After this, only weak assumptions can be made (using induction or deduction).
					
					
					
					th.text = "Why " + translateForHuman(problem.linkCollection.get(0)) + "?";
				}
			}
		};
		break;
		case "UNKNOWN_PURPOSE" :{
			if (problem.internal == true) {
				if (problem.logosCollection.get(0).getClass().getSimpleName().equals("Branch")) {
					th.text = "What is the purpose of " + translateBranch((Branch) problem.logosCollection.get(0)) + "?";
				} else {
					th.text = "What is the purpose of " + problem.logosCollection.get(0).getName() + "ing?";
				}
			} else {
				for (Link ol : problem.logosCollection.get(0).outwardLinks) {
					if (ol.getRelationName().equals("in_order_to")) {
						if (problem.logosCollection.get(0).getClass().getSimpleName().equals("Branch")) {
							th.text = translateBranch((Branch) problem.logosCollection.get(0)) + " in order to " + translateForHuman(ol) + ".";
						} else {
							th.text = problem.logosCollection.get(0).getName() + " in order to " + translateForHuman(ol) + ".";
						}
						problem.solved = true;
						break;
					}
				}
			}
		};
		break;
		case "NO_INHERITANCE" :{
			if (problem.internal == true) {
				// Just search for WordNet definitions like in MainClass.lookupWords
				
			} else {
				// This kind of questions should be actually solved by DBPedia queries in MainClass
			}
		};
		break;
		case "NO_DESCRIPTIONS" :{	// this seems to be redundant to UNKNOWN_PROPERTY...
			if (problem.internal == true) {
				// Normally we aquire such knowledge by perception.
				// Example: What is the book X like?
				// We look at the book and SEE that it's big and red.
			} else {
				
				
			}
		};
		break;
		// "What do I need to change the world?"
		case "UNKNOWN_ACTION_REQUIREMENTS" :{
			if (problem.internal == true) {
				// LogOS has to research external sources to find out
				// what the subject needs to accomplish the action.
				if (problem.logosCollection.get(0).getClass().getSimpleName().equals("Branch")) {
					th.text = "What is required for " + translateBranch((Branch) problem.logosCollection.get(0)) + "?";
				} else {
					th.text = "What is required to " + problem.logosCollection.get(0).getName() + "?";
				}
			} else {
				// external problem
				// Logos containing the action
				Logos action = problem.logosCollection.get(0);
				if (problem.logosCollection.size() > 1) {
					// action and object are both relevant
					// Logos containing the direct object
					Logos object = problem.logosCollection.get(1);
					// List of Links that target the action Logos
					ArrayList<Link> linksToAction = utils.linksWithTargetAlsoInBranch(database.linkList, action.getName());
					// Find "what" links to the object Logos
					ArrayList<Link> whatLinks = utils.linksByName(action.outwardLinks, "what");
					// Check whether there is such a "what" link that targets the object
					if (utils.linksWithTargetAlsoInBranch(whatLinks, object.getName()).isEmpty()) {
						// LogOS knows what is needed for the action isolated, but not in connection with object
						// TODO
						
					} else {
						// Find "is_needed_to" Links with positive generality
						ArrayList<Link> linksFromRequirements = utils.linksByName(linksToAction, "is_needed_to");
						linksFromRequirements = utils.filterLinksByGeneralitySign(linksFromRequirements, true);
						ArrayList<Logos> reqs = new ArrayList<Logos>();
						for (Link l : linksFromRequirements) {
							reqs.add(l.source);
						}
						th.solutionLogosCollection.addAll(reqs);
					}
				} else {
					// only the isolated action is interesting
					// List of Links that target the action Logos
					ArrayList<Link> linksToAction = utils.linksWithTargetAlsoInBranch(database.linkList, action.getName());
					// Find "is_needed_to" Links with positive generality
					ArrayList<Link> linksFromRequirements = utils.linksByName(linksToAction, "is_needed_to");
					linksFromRequirements = utils.filterLinksByGeneralitySign(linksFromRequirements, true);
					ArrayList<Logos> reqs = new ArrayList<Logos>();
					for (Link l : linksFromRequirements) {
						reqs.add(l.source);
					}
					th.solutionLogosCollection.addAll(reqs);
				}
				if (th.solutionLogosCollection.isEmpty()) {
					th.text = "Unfortunately I don't know this.";
				} else {
					th.text = "The following is necessary: ";
					for (int i = 0; i < th.solutionLogosCollection.size(); i++) {
						Logos lg = th.solutionLogosCollection.get(i);
						if (lg.getClass().getSimpleName().equals("Branch")) {
							th.text += translateBranch((Branch) lg);
						} else {
							th.text += lg.getName();
						}
						if (i == th.solutionLogosCollection.size() - 1) {
							th.text += ".";
						} else {
							th.text += ";";
						}
					}
				}
			}
		};
		break;
		case "UNKNOWN_PLACE" :{
			if (problem.internal == true) {
				write("solving internal UNKNOWN_PLACE Problem");
				if (problem.logosCollection.get(0).getClass().getSimpleName().equals("Branch")) {
					th.text = "Where is the " + translateBranch((Branch) problem.logosCollection.get(0)) + "?";
				} else {
					th.text = "Where is the " + problem.logosCollection.get(0).getName() + "?";
				}
			} else if (problem.logosCollection.size() == 1){
				write("solving external UNKNOWN_PLACE Problem");
				ArrayList<Logos> objects = utils.findAllLogosByName(database.logosList, problem.logosCollection.get(0).getName());
				if (objects.size() != 0) {
					write("at least one object Logos was found");
				} else {
					write("the object with unknown location was not found");
					th.text = "I don't have any information about " + problem.logosCollection.get(0).getName() + ".";
				}
				ArrayList<Link> locations = new ArrayList<Link>();
				for (Logos obj : objects) {
					ArrayList<Link> isLinks = utils.linksByName(obj.outwardLinks, "is");
					ArrayList<Link> branchOutIs;
					try {
						branchOutIs = utils.linksByName(utils.directShellBranch(obj, database).outwardLinks, "is");
						isLinks.addAll(branchOutIs);
					} catch (NullPointerException e) {
						write("there is an object without a description");
					}
					isLinks = utils.filterLinksByGeneralitySign(isLinks, true);
					for (Link il : isLinks) {
						if (il.target.getClass().getSimpleName().equals("Branch")) {
							write("an 'is' Link has a Branch target " + il.target.name);
							for (Link cl : ((Branch) il.target).containedLinkList) {
								if (cl.getRelationName().equals("prep")
										&& utils.stringArrayContains(utils.spatialPrepositions, cl.getTarget().getName())) {
									locations.add(il);
									write("found a location of " + problem.logosCollection.get(0).getName());
								}
							}
						} else {
							// if the location is just an adverb ("nearby" etc.)
							if (utils.stringArrayContains(utils.vagueLocationWords, il.target.name)) {
								locations.add(il);
								write("found an approximate location of " + problem.logosCollection.get(0).getName());
							}
						}
					} 
				}
				Link mostActualLocation = utils.mostActualLink(locations);
				if (!mostActualLocation.getRelationName().equals("")) {
					th.text = translateForHuman(mostActualLocation) + ".";
					th.solutionLinkCollection.add(mostActualLocation);
					problem.solved = true;
				} else {
					th.text = "Sorry, I don't know the location of " + problem.logosCollection.get(0).getName() + ".";
				}
			} else if (problem.logosCollection.size() == 2) {
				
				// Object and description
				write("solving external UNKNOWN_PLACE Problem with specification");
				
				// Find all Logos matching specified object's name
				ArrayList<Logos> objects = utils.findAllLogosByName(database.logosList, problem.logosCollection.get(0).getName());
				
				if (objects.size() != 0) {
					write("at least one object Logos was found");
				} else {
					write("the object with unknown location was not found");
					th.text = "I don't have any information about " + problem.logosCollection.get(0).getName() + ".";
				}
				
				// Leave only the objects with matching property
				for (Logos obj : (ArrayList<Logos>) objects.clone()) {
					// Remove all Logos that have wrong descriptions
					write("checking Logos " + obj.id);
					try {
						// Problem here: if the object has 2 or more shell Branches...
						// This should be solved by improving merging algorithms.
						// Example: If two objects of same category but different properties are given,
						// don't merge them. Assume that red ball and blue ball is not the same ball!
						// For now it only remembers the first occurence of an object.
						Branch shell = utils.directShellBranch(obj, database);
						int logIdx = shell.containedLogosList.lastIndexOf(obj);
						if (!shell.containedLinkList.get(logIdx).getRelationName().equals("is")) {
							objects.remove(obj);
							write("removed Logos " + obj.id + " because the next Link in the Branch is not [is]");
						}
						if (shell.containedLinkList.get(logIdx).getGenerality() < 0) {
							objects.remove(obj);
							write("removed Logos " + obj.id + " because the next Link in the Branch has a negative generality");
						}
						if (!shell.containedLinkList.get(logIdx).getTarget().getName().equals(problem.logosCollection.get(1).getName())) {
							objects.remove(obj);
							write("removed Logos " + obj.id + " because its specification is wrong");
						}
					} catch (NullPointerException npe) {
						write("the object in database is not specified");
					}
				}
				// List of Links to possible locations
				ArrayList<Link> locations = new ArrayList<Link>();
				for (Logos obj : objects) {
					ArrayList<Link> isLinks = utils.linksByName(obj.outwardLinks, "is");
					ArrayList<Link> branchOutIs;
					try {
						Branch dsb = utils.directShellBranch(obj, database);
						branchOutIs = utils.linksByName(dsb.outwardLinks, "is");
						isLinks.addAll(branchOutIs);
						write("direct shell Branch " + dsb.id + " of Logos " + obj.id + " has " + branchOutIs.size() + " outward [is] Links");
					} catch (NullPointerException e) {
						write("Logos " + obj.id + " doesn't have a direct shell Branch");
					}
					// Find all positive Links from the object
					isLinks = utils.filterLinksByGeneralitySign(isLinks, true);
					for (Link il : isLinks) {
						if (il.target.getClass().getSimpleName().equals("Branch")) {
							write("an 'is' Link has a Branch target " + il.target.name);
							for (Link cl : ((Branch) il.target).containedLinkList) {
								// Add only if the target is a Branch containing "prep" Link and a spatial preposition
								if (cl.getRelationName().equals("prep")
										&& utils.stringArrayContains(utils.spatialPrepositions, cl.getTarget().getName())) {
									locations.add(il);
									write("found a location of " + problem.logosCollection.get(0).getName());
								}
							}
						} else {
							// if the location is just an adverb ("nearby" etc.)
							if (utils.stringArrayContains(utils.vagueLocationWords, il.target.name)) {
								locations.add(il);
								write("found an approximate location of " + problem.logosCollection.get(0).getName());
							}
						}
					} 
				}
				Link mostActualLocation = utils.mostActualLink(locations);
				if (!mostActualLocation.getRelationName().equals("")) {
					th.text = translateForHuman(mostActualLocation) + ".";
					th.solutionLinkCollection.add(mostActualLocation);
					problem.solved = true;
				} else {
					th.text = "Sorry, I don't know the location of the "
								+ problem.logosCollection.get(1).getName() + " "
								+ problem.logosCollection.get(0).getName() + ".";
				}
			} else if (problem.logosCollection.size() == 3) {
				// Object, description and action
				
			}
		};
		break;
		case "UNKNOWN_TIME_FUTURE" :{
			if (problem.internal == true) {
				
			} else {
				
			}
		};
		break;
		case "UNKNOWN_TIME_PAST" :{
			if (problem.internal == true) {
				
			} else {
				
			}
		};
		break;
		case "UNKNOWN_METHOD" :{
			if (problem.internal == true) {
				if (problem.logosCollection.get(0).getClass().getSimpleName().equals("Branch")) {
					th.text = "How can I " + translateBranch((Branch) problem.logosCollection.get(0)) + "?";
				} else {
					th.text = "How can I " + problem.logosCollection.get(0).getName() + "?";
				}
			} else {
				// How can/do I pass the exam?
				// Search for matching Branches and their outward "method_link"
				
				// Logos containing the action
				Logos action = problem.logosCollection.get(0);
				if (problem.logosCollection.size() > 1) {
					// action and object are both relevant
					// Logos containing the direct object
					Logos object = problem.logosCollection.get(1);
					// List of Links that target the action Logos
					ArrayList<Link> linksToAction = utils.linksWithTargetAlsoInBranch(database.linkList, action.getName());
					// Find "what" links to the object Logos
					ArrayList<Link> whatLinks = utils.linksByName(action.outwardLinks, "what");
					// Check whether there is such a "what" link that targets the object
					if (utils.linksWithTargetAlsoInBranch(whatLinks, object.getName()).isEmpty()) {
						// LogOS knows what is needed for the action isolated, but not in connection with object
						// TODO
						
					} else {
						// Find "method_link" Links with positive generality
						ArrayList<Link> linksFromRequirements = utils.linksByName(linksToAction, "method_link");
						linksFromRequirements = utils.filterLinksByGeneralitySign(linksFromRequirements, true);
						
						// Also sort linksFromRequirements by actuality (meaning the order of actions)
						Collections.sort(linksFromRequirements, Collections.reverseOrder());
						
						// The requirements can be both Branches and Logos, so use Logos here as a super class.
						ArrayList<Logos> reqs = new ArrayList<Logos>();
						for (Link l : linksFromRequirements) {
							reqs.add(l.source);
						}
						th.solutionLogosCollection.addAll(reqs);
					}
				} else {
					// only the isolated action is interesting
					// List of Links that target the action Logos
					ArrayList<Link> linksToAction = utils.linksWithTargetAlsoInBranch(database.linkList, action.getName());
					ArrayList<Link> linksFromRequirements = utils.linksByName(linksToAction, "method_link");
					linksFromRequirements = utils.filterLinksByGeneralitySign(linksFromRequirements, true);
					
					// Also sort linksFromRequirements by actuality (meaning the order of actions)
					Collections.sort(linksFromRequirements, Collections.reverseOrder());
					
					ArrayList<Logos> reqs = new ArrayList<Logos>();
					for (Link l : linksFromRequirements) {
						reqs.add(l.source);
					}
					th.solutionLogosCollection.addAll(reqs);
				}
				
				// Save the answer as a Thought
				if (th.solutionLogosCollection.isEmpty()) {
					th.text = "Unfortunately I don't know this.";
				} else {
					th.text = "The following is necessary: ";
					for (int i = 0; i < th.solutionLogosCollection.size(); i++) {
						Logos lg = th.solutionLogosCollection.get(i);
						if (lg.getClass().getSimpleName().equals("Branch")) {
							th.text += translateBranch((Branch) lg);
						} else {
							th.text += lg.getName();
						}
						if (i == th.solutionLogosCollection.size() - 1) {
							th.text += ".";
						} else {
							th.text += ";";
						}
					}
				}
				
				
			}// end external UNKNOWN_METHOD
		};
		break;
		case "UNKNOWN_PROPERTY" :{
			if (problem.internal == true) {
				// even if we find some information on this,
				// this is only a "can_be" Link!
				
			} else {
				write("solving external UNKNOWN_PROPERTY Problem");
				// Here we have to find the best matching part of the hypergraph...
				// Example: (LogOS knows that) Red car is in the garage, blue car is on the street.
				// User: Which car is in the garage?
				Logos obj = problem.logosCollection.get(0);
				// utils.printLogosInfo(obj);
				if (problem.logosCollection.size() == 1) {
					// No action verb specified.
					write("no action verb specified");
					if (problem.linkCollection.size() == 0) {
						// Return description of the object as it is in general.
						// TODO
					} else {
						write("the object has specifying descriptions");
						// Description is connected to object via "is" or "have" Link.
						Link connection = problem.linkCollection.get(0);
						// utils.printLinkInfo(connection);
						Branch descr = problem.branchCollection.get(0);
						// utils.printBranchInfo(descr);
						ArrayList<Logos> objects = utils.findAllLogosByName(database.logosList, obj.getName());
						if (objects.isEmpty()) {
							th.text = "I have no data about " + obj.getName() + ".";
							th.priority = (float) problem.severity;
							return th;
						}
						for (Logos o : objects) {
							// Are there direct connections to description on the same Branch level?
							ArrayList<Link> linksToDescr = utils.linksByName(o.outwardLinks, connection.getRelationName());
							linksToDescr = utils.linksWithTargetBranch(linksToDescr, descr);
							linksToDescr = utils.filterLinksByGeneralitySign(linksToDescr, true);
							try {
								if (linksToDescr.isEmpty()) {
									write("specification is not connected directly to the object");
									write("going one level up to the direct shell Branch");
									// The searched Property could be between description and object
									Branch direct = utils.directShellBranch(o, database);
									// utils.printBranchInfo(direct);
									linksToDescr = utils.linksWithTargetBranch(direct.outwardLinks, descr);
									linksToDescr = utils.filterLinksByGeneralitySign(linksToDescr, true);
									if (linksToDescr.isEmpty()) {
										// Nothing was found (the description was missing)
										th.text = "I don't know what the described " + obj.getName() + " is like.";
									} else {
										// Look in the direct shell Branch of o
										int logIdx = direct.containedLogosList.lastIndexOf(o);
										if (direct.containedLinkList.get(logIdx).getRelationName().equals("is")) {
											// The property could be a Branch as well, but it's handled by translateBranch
											th.text = translateBranch(direct) + ".";
											th.priority = (float) problem.severity;
											problem.solved = true;
											return th;
										} else {
											th.text = "I don't know the properties of the described " + obj.getName() + ".";
										}
									}
								} else {
									write("found an object specified via direct Link");
									write("going one level up to the direct shell Branch");
									// The searched Property should be in the end
									Branch direct = utils.directShellBranch(o, database);
									// utils.printBranchInfo(direct);
									ArrayList<Link> isLks = utils.linksByName(direct.outwardLinks, "is");
									// isLks = utils.filterLinksByGeneralitySign(isLks, true);
									if (isLks.isEmpty()) {
										// no info
										th.text = "I don't know which " + obj.getName() + ".";
										th.priority = (float) problem.severity;
										return th;
									} else {
										// found
										Link mal = utils.mostActualLink(isLks);
										th.text = translateForHuman(mal) + ".";
										th.solutionLinkCollection.add(mal);
										th.priority = (float) problem.severity;
										problem.solved = true;
										return th;
									}
								} 
							} catch (NullPointerException e) {
								continue;
							}
							
						}
					}
				} else {
					// Action verb specified.
					// TODO
				}
			}
		};
		break;
		case "UNKNOWN_OBJECT" :{
			// Questions like "What do you like?", "What does Jack build?"
			if (problem.internal == true) {
				
			} else {
				write("solving external UNKNOWN_OBJECT Problem");
				// Subject and action are given.
				Logos subject = problem.logosCollection.get(0);
				Logos action = problem.logosCollection.get(1);
				ArrayList<Logos> subjs = utils.findAllLogosByName(database.logosList, subject.getName());
				ArrayList<Link> ans = new ArrayList<Link>();
				if (subjs.isEmpty()) {
					th.text = "I don't know anything about " + subject.getName() + ".";
					th.priority = (float) problem.severity;
					return th;
				}
				for (Logos subj : subjs) {
					// Firstly, try to find a direct positive outward "do" Link on the same level
					ArrayList<Link> doPositive = utils.linksByName(subj.outwardLinks, "do");
					doPositive = utils.filterLinksByGeneralitySign(doPositive, true);
					// If none was found, try searching on higher levels
					if (doPositive.isEmpty()) {
						boolean foundDo = false;
						// This dummy Logos is valid for finding the direct shell Branch in Utils
						// since it has the same fields (name, id, out/inward Links).
						Logos dirShell = new Logos(subj.getName(), subj.getId(), subj.getOutwardLinks(), subj.getInwardLinks());
						while (!foundDo) {
							write("didn't find matching positive 'do' Links on the same level, scanning direct shell Branch");
							dirShell = utils.directShellBranch(dirShell, database);
							if (dirShell == null) {
								write("no direct shell Branch found");
								break;
							} else {
								doPositive = utils.filterLinksByGeneralitySign(utils.linksByName(dirShell.outwardLinks, "do"), true);
								if (doPositive.isEmpty()) {
									write("direct shell Branch doesn't have positive outward 'do' Links");
									continue;
								} else {
									doPositive = utils.linksWithTargetAlsoInBranch(doPositive, action.getName());
									if (doPositive.isEmpty()) {
										write("direct shell Branch has positive 'do' Links, but not with the right target");
										continue;
									} else {
										foundDo = true;
									}
								}
							}
						}
					}
					// TODO is this generality filtering redundant?
					// doPositive = utils.filterLinksByGeneralitySign(doPositive, true);
					if (doPositive.isEmpty()) {
						write("positive 'do' Links were not found");
						continue;
					}
					doPositive = utils.linksWithTargetAlsoInBranch(doPositive, action.getName());
					if (doPositive.isEmpty()) {
						write("outward 'do' Links don't target the specified action");
						continue;
					}
					for (Link doLink : doPositive) {
						if (doLink.getTarget().getClass().getSimpleName().equals("Branch")) {
							Logos act = ((Branch) doLink.getTarget()).containedLogosList.get(0);
							ArrayList<Link> whatLinks = new ArrayList<Link>();
							whatLinks = utils.linksByName(act.outwardLinks, "what");
							whatLinks = utils.filterLinksByGeneralitySign(whatLinks, true);
							if (whatLinks.isEmpty()) {
								write("positive 'what' Links were not found");
								continue;
							}
							write("found an object");
							ans.add(utils.mostGeneralLink(whatLinks));
						} else {
							ArrayList<Link> whatLinks = new ArrayList<Link>();
							whatLinks = utils.linksByName(doLink.getTarget().outwardLinks, "what");
							whatLinks = utils.filterLinksByGeneralitySign(whatLinks, true);
							if (whatLinks.isEmpty()) {
								write("positive 'what' Links were not found");
								continue;
							}
							write("found an object");
							ans.add(utils.mostGeneralLink(whatLinks));
						}
					}
				}
				String pronounTranslation = subject.getName();
				if (subject.getName().equals("#SELF")) {
					pronounTranslation = "I";
				}
				if (subject.getName().equals("#USER")) {
					pronounTranslation = "you";
				}
				if (ans.isEmpty()) {
					th.text = "I don't know what " + pronounTranslation + " " + action.getName() + ".";
				} else {
					th.solutionLinkCollection.add(utils.mostActualLink(ans));
					problem.solved = true;
					th.text = pronounTranslation + " " + translateForHuman(utils.mostActualLink(ans)) + ".";
				}
			}
		};
		break;
		case "UNKNOWN_ACTION" :{
			// The actor Logos is given.
			if (problem.internal == true) {
				
			} else {
				write("solving external UNKNOWN_ACTION Problem");
				Logos actor = problem.logosCollection.get(0);
				ArrayList<Link> doPositive = new ArrayList<Link>();
				doPositive = utils.linksByName(actor.outwardLinks, "do");
				doPositive = utils.filterLinksByGeneralitySign(doPositive, true);
				boolean foundDo = false;
				if (doPositive.isEmpty()) {
					write("positive 'do' Links were not found on the same level, searching on higher levels");
					// This dummy Logos is valid for finding the direct shell Branch in Utils
					// since it has the same fields (name, id, out/inward Links).
					Logos dirShell = new Logos(actor.getName(), actor.getId(), actor.getOutwardLinks(), actor.getInwardLinks());
					while (!foundDo) {
						write("didn't find positive 'do' Links on the same level, scanning direct shell Branch");
						dirShell = utils.directShellBranch(dirShell, database);
						if (dirShell == null) {
							write("no direct shell Branch found");
							break;
						} else {
							doPositive = utils.filterLinksByGeneralitySign(utils.linksByName(dirShell.outwardLinks, "do"), true);
							if (doPositive.isEmpty()) {
								write("direct shell Branch doesn't have positive outward 'do' Links");
								continue;
							} else {
								foundDo = true;
							}
						}
					}
				} else
					foundDo = true;
				if (!foundDo) {
					th.text = "I don't know what " + actor.getName() + " do.";
					th.priority = (float) problem.severity;
					return th;
				} else {
					// Translate you/I
					String subjectPr = actor.getName();
					if (subjectPr.equals("#SELF")) {
						subjectPr = "I";
					}
					if (subjectPr.equals("#USER")) {
						subjectPr = "you";
					}
					Link maDo = utils.mostActualLink(doPositive);
					th.solutionLinkCollection.add(maDo);
					problem.solved = true;
					th.text = subjectPr + " " + translateForHuman(maDo) + ".";
				}
				
			}
		};
		break;
		case "UNKNOWN_SUBJECT" :{
			if (problem.internal == true) {
				
			} else {
				write("solving external UNKNOWN_SUBJECT Problem");
				// Given action and object
				Logos action = problem.logosCollection.get(0);
				Logos object = problem.logosCollection.get(1);
				Branch br = utils.constructSimpleBranch(action.getName(), "what", object.getName());
				ArrayList<Link> totalDoLinks = utils.linksByName(database.linkList, "do");
				totalDoLinks = utils.filterLinksByGeneralitySign(totalDoLinks, true);
				Branch br_similar = utils.findBranchByID(database.branchList, utils.mostSimilarTargetBranch(totalDoLinks, br));
				if (br_similar.id == -1) {
					write("didn't find similar Branches in the hypergraph");
					th.text = "I don't have information about it.";
				}
				Logos subject = utils.mostActualLink(utils.linksByName(br_similar.inwardLinks, "do")).source;
				if (subject.name.equals("")) {
					write("didn't find the subject");
					th.text = "Sorry, I don't know that.";
				} else {
					th.solutionLogosCollection.add(subject);
					problem.solved = true;
					String subjectPr = subject.getName();
					if (subject.getClass().getSimpleName().equals("Branch")) {
						subjectPr = translateBranch((Branch) subject);
					}
					// Translate you/I
					if (subject.getName().equals("#SELF")) {
						subjectPr = "I";
					}
					if (subject.getName().equals("#USER")) {
						subjectPr = "you";
					}
					String objectPr = object.getName();
					if (subject.getName().equals("#SELF")) {
						objectPr = "me";
					}
					if (subject.getName().equals("#USER")) {
						objectPr = "you";
					}
					th.text = subjectPr + " " + action.getName() + " " + objectPr + ".";
				}
			}
		};
		break;
		case "PHILOSOPHICAL_QUESTION" :{
			if (problem.internal == true) {
				
			} else {
				
			}
		};
		break;
		case "COMMAND" :{
			boolean resourceUnavailable = false;
			boolean subtaskFailed = false;
			if (problem.internal == true) {
				write("received an internal COMMAND");
				// Generate task_links from #SELF to get all required resources
				Logos self = utils.findLogosByName(database.logosList, "#SELF");
				Link originalTaskLink = problem.linkCollection.get(0);
				Logos originalTask = originalTaskLink.target;
				// A list of all objects that LogOS can use
				ArrayList<Logos> actorHas = new ArrayList<Logos>();
				for (Link haveLink : utils.filterLinksByGeneralitySign(utils.linksByName(self.outwardLinks, "have"), true)) {
					actorHas.add(haveLink.getTarget());
				}
				// A list of all material requirements to accomplish the action
				ArrayList<Logos> reqs = new ArrayList<Logos>();
				for (Link reqLink : utils.filterLinksByGeneralitySign(utils.linksByName(originalTask.inwardLinks, "is_needed_to"), true)) {
					reqs.add(reqLink.getSource());
				}
				// If no requirements were found, throw an UNKNOWN_ACTION_REQUIREMENTS
				if (reqs.isEmpty()) {
					Problem noReqs = new Problem();
					noReqs.type = "UNKNOWN_ACTION_REQUIREMENTS";
					noReqs.logosCollection.add(originalTask);
					noReqs.severity = 1.0f;
					noReqs.internal = true;
					solveProblem(noReqs, database, utils);
					if (noReqs.solved == false) {
						write("didn't get any information about action requirements");
						th.text = "Sorry, without this information I cannot execute your command.";
					}
				}
				// A list of all objects that are in requirements but not in possession of LogOS
				ArrayList<Logos> missing = new ArrayList<Logos>();
				for (Logos req : reqs) {
					if (!actorHas.contains(req)) {
						missing.add(req);
					}
				}
				for (Logos m : missing) {
					write("creating a MISSING_RESOURCE Problem for the resource: " + m.getName());
					// for each missing resource create a MISSING_RESOURCE Problem and try to solve it somehow
					Problem missingResourceProblem = new Problem();
					missingResourceProblem.type = "MISSING_RESOURCE";
					missingResourceProblem.logosCollection.add(m);
					missingResourceProblem.severity = 1.0f;	// like COMMAND
					missingResourceProblem.internal = true;
					solveProblem(missingResourceProblem, database, utils);
					if (missingResourceProblem.solved == false) {
						write("couldn't get the resource [" + m.getName() + "], exit COMMAND execution");
						th.text = "I don't have " + m.getName() + ".";
						resourceUnavailable = true;
						break;
					}
				}
				if (resourceUnavailable) {
					break;
				}
				// After trying to get all required materials, LogOS follows the order of actions (method_links)
				ArrayList<Link> methodLinks = utils.linksByName(originalTask.inwardLinks, "method_link");
				methodLinks = utils.filterLinksByGeneralitySign(methodLinks, true);
				// Actualities of method_links encode the order: most actual go first.
				// Good to know: database doesn't update actualities of method_links (actualities stay unchanged to remember the order)
				Collections.sort(methodLinks, Collections.reverseOrder());
				// Now try to accomplish subtasks
				for (Link ml : methodLinks) {
					Problem subtask = new Problem();
					subtask.type = "COMMAND";
					subtask.logosCollection.add(ml.getSource());
					subtask.severity = 1.0f;
					subtask.internal = true;
					solveProblem(subtask, database, utils);
					if (subtask.solved == false) {
						write("couldn't execute a subtask, exit COMMAND execution");
						th.text = "I couldn't execute your command because of missing resources.";
						subtaskFailed = true;
						break;
					}
				}
				if (subtaskFailed) {
					break;
				}
				problem.solved = true;
			} else {
				write("received an external COMMAND");
				// Generate task_links from #SELF to get all required resources
				Logos self = utils.findLogosByName(database.logosList, "#SELF");
				Link originalTaskLink = problem.linkCollection.get(0);
				long knownID = utils.mostSimilarTargetBranch(database.linkList, (Branch) originalTaskLink.target);
				Logos originalTask = utils.findLogosByID(database.logosList, knownID);
				// A list of all objects that LogOS can use
				ArrayList<Logos> actorHas = new ArrayList<Logos>();
				for (Link haveLink : utils.filterLinksByGeneralitySign(utils.linksByName(self.outwardLinks, "have"), true)) {
					actorHas.add(haveLink.getTarget());
				}
				// A list of all material requirements to accomplish the action
				ArrayList<Logos> reqs = new ArrayList<Logos>();
				for (Link reqLink : utils.filterLinksByGeneralitySign(utils.linksByName(originalTask.inwardLinks, "is_needed_to"), true)) {
					reqs.add(reqLink.getSource());
				}
				// If no requirements were found, throw an UNKNOWN_ACTION_REQUIREMENTS
				if (reqs.isEmpty()) {
					Problem noReqs = new Problem();
					noReqs.type = "UNKNOWN_ACTION_REQUIREMENTS";
					noReqs.logosCollection.add(originalTask);
					noReqs.severity = 1.0f;
					noReqs.internal = true;
					solveProblem(noReqs, database, utils);
					if (noReqs.solved == false) {
						write("didn't get any information about action requirements");
						th.text = "Sorry, without this information I cannot execute your command.";
					}
				}
				// A list of all objects that are in requirements but not in possession of LogOS
				ArrayList<Logos> missing = new ArrayList<Logos>();
				for (Logos req : reqs) {
					if (!actorHas.contains(req)) {
						missing.add(req);
					}
				}
				for (Logos m : missing) {
					write("creating a MISSING_RESOURCE Problem for the resource: " + m.getName());
					// for each missing resource create a MISSING_RESOURCE Problem and try to solve it somehow
					Problem missingResourceProblem = new Problem();
					missingResourceProblem.type = "MISSING_RESOURCE";
					missingResourceProblem.logosCollection.add(m);
					missingResourceProblem.severity = 1.0f;	// like COMMAND
					missingResourceProblem.internal = true;
					solveProblem(missingResourceProblem, database, utils);
					if (missingResourceProblem.solved == false) {
						write("couldn't get the resource [" + m.getName() + "], exit COMMAND execution");
						th.text = "I don't have " + m.getName() + ".";
						resourceUnavailable = true;
						break;
					}
				}
				if (resourceUnavailable) {
					break;
				}
				// After trying to get all required materials, LogOS follows the order of actions (method_links)
				ArrayList<Link> methodLinks = utils.linksByName(originalTask.inwardLinks, "method_link");
				methodLinks = utils.filterLinksByGeneralitySign(methodLinks, true);
				// Actualities of method_links encode the order: most actual go first.
				// Good to know: database doesn't update actualities of method_links (actualities stay unchanged to remember the order)
				Collections.sort(methodLinks, Collections.reverseOrder());
				// Now try to accomplish subtasks
				for (Link ml : methodLinks) {
					Problem subtask = new Problem();
					subtask.type = "COMMAND";
					subtask.logosCollection.add(ml.getSource());
					subtask.severity = 1.0f;
					subtask.internal = true;
					solveProblem(subtask, database, utils);
					if (subtask.solved == false) {
						write("couldn't execute a subtask, exit COMMAND execution");
						th.text = "I couldn't execute your command because of missing resources.";
						subtaskFailed = true;
						break;
					}
				}
				if (subtaskFailed) {
					break;
				}
				problem.solved = true;
			}
		};
		break;
		
		case "UNKNOWN_QUANTITY": {
			// Example: How many legs does a cat have?
			Logos arg1 = problem.logosCollection.get(0);	// cat
			Logos arg2 = problem.logosCollection.get(1);	// leg
			Link arg3 = problem.linkCollection.get(0);		// have
			if (arg1 instanceof Branch) {
				Branch subjBranch = utils.findMatchingBranch(database.branchList, (Branch) arg1);
				if (subjBranch == null) {
					write("specified Branch was not found");
					th.text = "I didn't find the specified subject in the database.";
				} else {
					
				}
			} else {
				ArrayList<Logos> subjs = utils.findAllLogosByName(database.logosList, arg1.getName());
				for (Logos subj : subjs) {
					if (subj.getId() == -1) {
						write("specified subject Logos was not found");
						th.text = "I didn't find the specified subject in the database.";
					} else {
						ArrayList<Link> linksWithRightTarget = utils.linksWithTargetAlsoInBranch(subj.outwardLinks,
								arg2.getName());
						if (linksWithRightTarget.isEmpty()) {
							write("the subject doesn't have any Links to the object");
							th.text = "I don't know, master.";
						} else {
							ArrayList<Link> correctNamedLinks = utils.linksByName(linksWithRightTarget,
									arg3.getRelationName());
							correctNamedLinks = utils.filterLinksByGeneralitySign(correctNamedLinks,
									arg3.generality > 0);
							if (correctNamedLinks.isEmpty()) {
								write("the Links between subject and object don't contain correct Links");
								th.text = "I don't know, master.";
							} else {
								Link mal = utils.mostActualLink(correctNamedLinks);
								if (mal.target instanceof Branch) {
									Branch trg = (Branch) mal.target;
									// check if the contained Link is "unit"
									if (trg.containedLinkList.get(0).getRelationName().equals("unit")) {
										Logos quant = trg.containedLinkList.get(0).getTarget();
										th.text = arg1.getName() + " " + beautifyLink(arg3) + " " + quant.getName()
												+ " " + arg2.getName() + ".";
										problem.solved = true;
										break;
									} else {
										write("the subject has a correct Link to the object, but the object doesn't have a quantity");
										th.text = "I don't know the quantity.";
									}
								} else {
									write("the subject has a correct Link to the object, but the object is not in a Branch");
									th.text = arg1.getName() + " " + beautifyLink(arg3) + " " + arg2.getName()
											+ ", but I don't know the quantity.";
								}
							}
						}
					} 
				}
			}
		}
		break;
		
		default :
		{
			th.text = "I have no methods for solving this problem / answering this question.";
		};
		}
		
		th.priority = (float) problem.severity;
		
		return th;
	}

	// Apply user's response to Logos' question to find a solution
	public Thought applyUserHelp(String input, String[] inputTokens, Problem problem, DatabaseInterface database, Utils utils, TextMethods textReader, Parser parser) {
		Thought th = new Thought();
		th.sourceProblem = problem;
		if (problem.linkCollection == null) {
			return th;
		}
		// User may say "I don't want to answer that question" etc.
		// In this case LogOS apologizes.
		String[] keywords1 = {"answer", "want", "refuse", "help"};
		if (utils.stringArraysCut(inputTokens, utils.denialKeywords)
				&& utils.stringArraysCut(inputTokens, keywords1)) {
			th.text = utils.randomStringFromArray(apologies);
			return th;
		}
		// Another possible case: "I don't know" or similar.
		String[] keywords2 = {"know", "clue", "idea"};
		if (utils.stringArraysCut(inputTokens, utils.denialKeywords)
				&& utils.stringArraysCut(inputTokens, keywords2)) {
			th.text = utils.randomStringFromArray(okWords);
			return th;
		}
		
		// Problem type dependent routines to integrate user's answer
		switch(problem.type) {
		case "CONTRADICTION" :
		{
			// User will probably tell LogOS which statement seems correct.
			// Maybe he/she tells LogOS that both are true (depending on context).
			String[] keywords3 = {"first", "former"};
			String[] keywords4 = {"second", "latter"};
			String[] keywords5 = {"neither", "none"};
			if (utils.stringArraysCut(inputTokens, keywords3)) {
				// delete the second link
				database.linkList.remove(problem.linkCollection.get(1));
				write("deleted the second statement");
			} else if (utils.stringArraysCut(inputTokens, keywords4)) {
				// delete the first link
				database.linkList.remove(problem.linkCollection.get(0));
				write("deleted the second statement");
			} else if (utils.stringArraysCut(inputTokens, keywords5)){
				// both are wrong
				database.linkList.remove(problem.linkCollection.get(0));
				database.linkList.remove(problem.linkCollection.get(1));
				write("deleted both statements");
			} else {
				// both can be true
				write("both statements remain unchanged");
			}
			problem.solved = true;
			th.text = utils.randomStringFromArray(thanks);
		};
		break;
		case "MISSING_RESOURCE" :{
			
		};
		break;
		case "UNKNOWN_REASON" :
		{
			// User will try to explain the reasons behind "problematic" Link.
			// Typical "because" intros are excluded
			input = input.toLowerCase().replaceAll("because ", "");
			input = input.toLowerCase().replaceAll("it's ", "");
			input = input.toLowerCase().replaceAll("that's ", "");
			// The rest of the sentence is parsed by OpenNLP, followed by Chain extraction and Chainese input to the database
			String chain = "";
			try {
				// Connect problematic link with the reason
				Link problemLink = problem.linkCollection.get(0);
				chain = "#C #B "
						+ problemLink.source.name
						+ " "
						+ problemLink.relationName
						+ ","
						+ problemLink.generality
						+ " "
						+ problemLink.target.name
						+ " B# because #B ";
				chain += textReader.extractChain(textReader.parseSentence(input, parser)) + "#B";
				database.manualInput(chain, utils);
				write("connected user input to the Link in question");
				th.text = utils.randomStringFromArray(thanks);
				problem.solved = true;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		};
		break;
		case "UNKNOWN_ACTION_REQUIREMENTS" :
		{
			// "What is required to fly?"
			// "What is required to read a book?"
			Logos action = problem.logosCollection.get(0);
			if (problem.logosCollection.size() == 2) {
				// action and object are given
				// TODO
			} else {
				// only action is given
				String[] notImportant = {"what", "you", "need", "required", "are",
						"is", "all", "definitely", "surely", "would"};
				for (int i = 0; i < inputTokens.length; i++) {
					if (!utils.stringArrayContainsIgnoreCase(notImportant, inputTokens[i])) {
						database.manualInput("#C " + inputTokens[i] + " is_needed_to,"
								+ utils.generality(1, MainClass.belief) + " "
								+ action.getName(),
								utils);
					}
				}
				th.text = utils.randomStringFromArray(thanks);
				problem.solved = true;
			}
			
		};
		break;
		case "UNKNOWN_PLACE" : {
			// "Where is the car?"
			Logos object = problem.logosCollection.get(0);
			if (problem.logosCollection.size() == 2) {
				// object and description are given
				// TODO
			} else {
				// only object is given
				String[] notImportant = {"it", "it's", "they", "he", "she", "he's", "she's"};
				String[] dienst = {"'s", "is", "are", "'re", "the", "a", "an"};
				String transform = "";	// input with replaced pronouns
				for (int i = 0; i < inputTokens.length; i++) {
					// Replace unimportant pronouns by the object asked
					if (utils.stringArrayContainsIgnoreCase(notImportant, inputTokens[i])
							&& !utils.stringArrayContainsIgnoreCase(dienst, inputTokens[i])) {
						inputTokens[i] = object.getName();
						transform += inputTokens[i] + " ";
					}
				}
				String chain = "#C ";
				try {
					chain += textReader.extractChain(textReader.parseSentence(transform, parser));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				database.manualInput(chain, utils);
				th.text = utils.randomStringFromArray(thanks);
				problem.solved = true;
			}
		}
		break;
		default :
		{
			th.text = utils.randomStringFromArray(okWords);
		};
		}
		
		th.priority = (float) problem.severity;
		
		return th;
	}
	
	// subroutine to handle negative emotional conditions
	public boolean emotionalHelp(String emotion, DatabaseInterface database, Utils utils) {
		boolean listenMode = false;
		// separate Scanner
		Scanner psychologist = new Scanner(System.in);
		// search in the hypergraph, what could cause such condition
		Logos emoLogos = utils.findLogosByName(database.logosList, emotion);
		List<Logos> homonyms = utils.sameNamedLogosList(database.logosList, emoLogos);
		homonyms.add(emoLogos);
		List<Logos> causes = new ArrayList<Logos>();
		for (Logos logos : homonyms) {
			for (Link lk_in : logos.inwardLinks) {
				if (lk_in.relationName.equals("causes")) {
					causes.add(lk_in.source);
				}
			}
		}
		int numCauses = causes.size();
		//boolean solved = false;
		if (numCauses != 0) {
			boolean tryKnownCause = true;
			//while (!solved) {
				if (tryKnownCause) {
					Logos trial = utils.randomLogosFromList(causes);
					if (trial.getClass().getSimpleName().equals("Branch")) {
						// Branch as a cause
						System.out.println(">>>LOGOS: Is it because " + translateBranch((Branch) trial) + "?");
					} else {
						// normal Logos
						System.out.println(">>>LOGOS: Is it because of " + trial.name + "?");
					}
					tryKnownCause = false;
					System.out.print(">>>USER: ");
					String userInput = psychologist.nextLine();
					String[] parse = utils.tokensFromText(userInput);
					if (utils.stringArraysCut(utils.confirmationKeywords, parse) &&
							!(utils.stringArraysCut(utils.denialKeywords, parse))) {
						// confirmed, LogOS good boy :-)
						// we have to recommend the user to eliminate/avoid the cause
						double[] probs = {0.25, 0.25, 0.25, 0.25};
						randomResponseFromList(okWords, probs, utils);
						System.out.print(".\n");
						double[] probs1 = {0.34, 0.33, 0.33};
						randomResponseFromList(avoidanceAdvicePhrases, probs1, utils);
						if (trial.getClass().getSimpleName().equals("Branch")) {
							// Branch as a cause
							System.out.print("such situation when " + translateBranch((Branch) trial) + ".\n");
						} else {
							// normal Logos
							System.out.print(trial.name + ".\n");
						}
						//solved = true;
						//break;
					} else if (utils.stringArraysCut(utils.denialKeywords, parse)) {
						// denied
						// alright, what could cause this condition?
						System.out.println(">>>LOGOS: What do you think makes you feel " + emotion + "?");
						/*
						 * Possible inputs:
						 * "I think I work too much."
						 * "Maybe it's the bad weather."
						 * "I haven't eaten much today."
						 * "My crush doesn't like me."
						 */
						// actually, go back to the main class to talk further
						listenMode = true;
						write("empathic listening mode activated");
						
					} else if (utils.stringArraysCut(utils.uncertaintyKeywords, parse)) {
						// maybe LogOS was right
						// for now: the same avoidance strategy as in confirmation case
						double[] probs = {0.25, 0.25, 0.25, 0.25};
						randomResponseFromList(okWords, probs, utils);
						System.out.print(". ");
						double[] probs1 = {0.34, 0.33, 0.33};
						randomResponseFromList(avoidanceAdvicePhrases, probs1, utils);
						if (trial.getClass().getSimpleName().equals("Branch")) {
							// Branch as a cause
							System.out.print("such situation when " + translateBranch((Branch) trial) + ".\n");
						} else {
							// normal Logos
							System.out.print(trial.name + ".\n");
						}
						//solved = true;
						//break;
					} else {
						// TODO what to do here?
						tryKnownCause = true;
					}
				}// end trying with known causes
				
				// user's own version must be known here
				// ask if the problem is solved (to exit)
				
				
			//}// end of solution finding loop
		} else {
			// no known causes in the hypergraph
			System.out.println(">>>LOGOS: I don't know what can cause such condition yet.");
			System.out.println(">>>LOGOS: What do you think makes you feel " + emotion + "?");

			// go to main class for further talking...
			listenMode = true;
			write("empathic listening mode activated");
		}
		//psychologist.close();
		write("exited emotionalHelp() method");
		return listenMode;
	}
	
	public String translateForHuman(Link link) {
		String tr = "";
		Logos src = link.getSource();
		Logos trg = link.getTarget();
		// Reverse order for prepositions and adjectives
		if (link.getRelationName().equals("prep")) {
			src = link.getTarget();
			trg = link.getSource();
		}
		if (src.getClass().getSimpleName().equals("Branch")) {
			tr += translateBranch((Branch) src) + " ";
		} else {
			String name = src.name;
			if (name.equals("#SELF")) {
				name = "I";
			}
			if (name.equals("#USER")) {
				name = "you";
			}
			tr += name;
		}
		
		tr += beautifyLink(link);
		
		if (trg.getClass().getSimpleName().equals("Branch")) {
			tr += translateBranch((Branch) trg) + " ";
		} else {
			String name = trg.name;
			if (name.equals("#SELF")) {
				name = "I";
			}
			if (name.equals("#USER")) {
				name = "you";
			}
			tr += name;
		}
		return tr;
	}
	
	public String translateBranch(Branch br) {
		String tr = "";
		List<Logos> contLog = new ArrayList<Logos>();
		contLog = (List<Logos>) br.getContainedLogosList().clone();
		List<Link> contLk = new ArrayList<Link>();
		contLk = (List<Link>) br.getContainedLinkList().clone();
		for (int i = 0; i < contLog.size(); i++) {
			// Reverse order for prepositions
			if (i < contLog.size() - 1) {
				Link lk = contLk.get(i);
				if (lk.getRelationName().equals("prep") ||
						(lk.getRelationName().equals("is") &&
								!lk.target.getClass().getSimpleName().equals("Branch"))) {
					Logos log = contLog.get(i);
					Logos log2 = contLog.get(i + 1);
					contLog.set(i, log2);
					contLog.set(i + 1, log);
				}
			}
			Logos log = contLog.get(i);
			if (log.getClass().getSimpleName().equals("Branch")) {
				tr += translateBranch((Branch) log) + " ";
			} else {
				String name = log.name;
				if (name.equals("#SELF")) {
					name = "I";
				}
				if (name.equals("#USER")) {
					name = "you";
				}
				tr += name + " ";
			}
			if (i < contLog.size() - 1) {
				// get the link with same index
				Link lk = contLk.get(i);
				tr += beautifyLink(lk);
			}
		}
		return tr;
	}
	
	// translates link to human-like text
	public String beautifyLink(Link link) {
		String tr = "";
		double gen = link.generality;
		// Exception: don't write "is" if it targets an adjective
		if (!(link.getRelationName().equals("is") && !link.target.getClass().getSimpleName().equals("Branch"))) {
			if (gen > 0) {
				tr += linkPlusMap.get(link.relationName);
			} else {
				tr += linkMinusMap.get(link.relationName);
			} 
		}
		return tr;
	}
	
	public String findWikiAbstract(String word) {
		
		String ans = "";
		
		ParameterizedSparqlString qs = new ParameterizedSparqlString(""
                + "PREFIX rdfs:    <http://www.w3.org/2000/01/rdf-schema#>\n"
                + "PREFIX dbo:     <http://dbpedia.org/ontology/>\n"
                + "SELECT DISTINCT ?resource ?abstract\n"
                + "WHERE {\n"
                + "  ?resource rdfs:label ?label.\n"
                + "  ?resource dbo:abstract ?abstract.\n"
                + "  FILTER (lang(?abstract) = 'en')}");

        Literal literal = ResourceFactory.createLangLiteral(word, "en");
        qs.setParam("label", literal);

        //System.out.println(qs);
        
        QueryExecution exec = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", qs.asQuery());

        ResultSet results = ResultSetFactory.copyResults(exec.execSelect());

        while (results.hasNext()) {

        	ans = results.next().get("abstract").toString();

        }

        //ResultSetFormatter.out(results);
		return ans;
	}
	
	// Gives a random response from a given list according to its probability
	// ACHTUNG: without new line
	public void randomResponseFromList (String[] strings, double[] probabilities, Utils utils) {
		int a = utils.randomChoice(probabilities);
		System.out.print(">>>LOGOS: " + strings[a]);
	}

	public void wolframAlphaQuery(String input) {

        // The WAEngine is a factory for creating WAQuery objects,
        // and it also used to perform those queries. You can set properties of
        // the WAEngine (such as the desired API output format types) that will
        // be inherited by all WAQuery objects created from it. Most applications
        // will only need to crete one WAEngine object, which is used throughout
        // the life of the application.
        WAEngine engine = new WAEngine();
        
        // These properties will be set in all the WAQuery objects created from this WAEngine.
        engine.setAppID(appid);
        engine.addFormat("plaintext");

        // Create the query.
        WAQuery query = engine.createQuery();
        
        // Set properties of the query.
        query.setInput(input);
        
        try {
            // For educational purposes, print out the URL we are about to send:
            write("Query URL:");
            write(engine.toURL(query));
            write("");
            
            // This sends the URL to the Wolfram|Alpha server, gets the XML result
            // and parses it into an object hierarchy held by the WAQueryResult object.
            WAQueryResult queryResult = engine.performQuery(query);
            
            if (queryResult.isError()) {
                write("Query error");
                write("  error code: " + queryResult.getErrorCode());
                write("  error message: " + queryResult.getErrorMessage());
            } else if (!queryResult.isSuccess()) {
                write("Query was not understood; no results available.");
            } else {
                // Got a result.
                write("Successful query. Pods follow:\n");
                for (WAPod pod : queryResult.getPods()) {
                    if (!pod.isError()) {
                        System.out.println(pod.getTitle());
                        System.out.println("------------");
                        for (WASubpod subpod : pod.getSubpods()) {
                            for (Object element : subpod.getContents()) {
                                if (element instanceof WAPlainText) {
                                    System.out.println(((WAPlainText) element).getText());
                                    System.out.println("");
                                }
                            }
                        }
                        System.out.println("");
                    }
                }
                // We ignored many other types of Wolfram|Alpha output, such as warnings, assumptions, etc.
                // These can be obtained by methods of WAQueryResult or objects deeper in the hierarchy.
            }
        } catch (WAException e) {
            e.printStackTrace();
        }
	}

	void write(String str) {
		if (verbose) {
			System.out.println("PROBLEM_SOLVER: " + str);
		}
	}

	public int getInferenceSteps() {
		return inferenceSteps;
	}

	public void setInferenceSteps(int inferenceSteps) {
		this.inferenceSteps = inferenceSteps;
	}

	public double getTrustInNew() {
		return trustInNew;
	}

	public void setTrustInNew(double trustInNew) {
		this.trustInNew = trustInNew;
	}
	
}