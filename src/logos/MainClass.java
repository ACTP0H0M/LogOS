package logos;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/*
 * Development begun: 12.12.2018
 */

public class MainClass {
	
	// Parameters
	static int verbosity = 3;
	/*
	 * Verbosity levels:
	 * 0 = only final output
	 * 1 = ... and intralogistics (task delivery between agents)
	 * 2 = ... and utilities, analysis usage
	 * 3 = ... and database operations
	 */
	static int k = 10;	// coefficient for confidence calculation
	
	static int timesteps = 0;	// increased each time findProblems() is called
	static int wordRelationDepth = 10;	// maximum distance between words in sentence to analyse
	static double a_min = 0.1;	// minimal actuality of the links in the search
	static double belief = 1.0;	// belief strength, controls the generality function
	
	
	// Agents
	static DatabaseInterface database = new DatabaseInterface(verbosity);
	static ProblemSolver prsolver = new ProblemSolver(verbosity);
	static ProblemFinder prfinder = new ProblemFinder(verbosity);
	static Utils utils = new Utils(verbosity);
	static Scanner consoleInput = new Scanner(System.in);

	public static void main(String[] args) {
		
		// exit console?
		boolean exit = false;
		
		// Logos, Link IDs
		long logosNum = -1;
		long linkNum = -1;
		
		// Read database, update logosNum, linkNum
		database.logosList = database.readLogos();
		database.linkList = database.readLinks(database.logosList, utils);
		database.branchList = database.readBranches(database.logosList, database.linkList, utils);
		
		while (!exit) {
			
			logosNum = database.getMaxLogosID();
			linkNum = database.getMaxLinkID();
			
			String input = consoleInput.nextLine();
			
			// create Logos "input"
			logosNum++;
			Logos inputLogos = utils.emptyNamedLogos("input", logosNum);
			
			// Link from "SELF" to "input" : "receive"
			Logos selfLogos = utils.findLogosByID(database.logosList, 0);
			linkNum++;
			Link receiveLink = utils.freshLink(selfLogos, inputLogos, "receive", linkNum);
			
			// Logos with the message String
			logosNum++;
			Logos inputStringLogos = utils.emptyNamedLogos(input, logosNum);
			
			// Link from "input" to "inputStringLogos" : "equals to"
			linkNum++;
			Link equalsLink = utils.freshLink(inputLogos, inputStringLogos, "equals_to", linkNum);
			
			// add these technical Logos and Links to database
			database.linkList.add(receiveLink);
			database.linkList.add(equalsLink);
			database.logosList.add(inputLogos);
			database.logosList.add(inputStringLogos);
			
			if (verbosity >= 3) {
				System.out.print("DATABASE: new ");
				utils.printLogosInfo(inputLogos);
				System.out.print("DATABASE: new ");
				utils.printLogosInfo(inputStringLogos);
			}
			
			// Place for forced exit
			if (input.contains("#EXIT")) {
				exit = true;
				break;
			}
			
			////////////////////////////
			// MAIN ANALYSIS PIPELINE //
			////////////////////////////
			
			timesteps++;
			
			// Graph operations to automatically expand the graph and forget irrelevant stuff
			database.updateDatabase(utils, belief);
			
			// Place for manual input overrides...
			if (input.contains("#CHAIN") || input.contains("#BRANCH")) {
				
				manualInput(input);
				
			} else {
				
				// Check input for compatibility, try to understand the sentence
				analyzeInput(input, utils);
				
				// Find problems arising after user's input
				
				
				// Solve each Problem consequently, producing Thoughts
				/*ArrayList<Thought> thoughtQueue = new ArrayList<Thought>();
				for (Problem p : nextProblems) {
					thoughtQueue.add(prsolver.solveProblem(p));
				}*/
				
				// Decide whether to approve solutions (thoughts)
				// processThoughts(thoughtQueue);
				
			}
			
		}
		
		database.writeDatabase(database.logosList, database.linkList, database.branchList);
	}
	
	/*
	 * Analyzing input text in accordance to actual database.
	 * It makes modifications to database, so it's here.
	 */
	public static void analyzeInput(String text, Utils utils) {
		
		boolean verbose = false;
		
		if (verbosity >= 3) {
			verbose = true;
		}
		
		write("MAIN: analyzing input at timestep " + timesteps, 2);
		
		// Working with input sentences
		String[] tokens = utils.tokensFromText(text);
		List<List<String>> sentences = new ArrayList<List<String>>();
		List<String> sentence = new ArrayList<String>();
		
		// iterate over tokens (words and punctuation)
		for (int i = 0; i < tokens.length; i++) {
			// add token to the sentence
			sentence.add(tokens[i]);
			// if punctuation token, save the sentence and clear it
			if (utils.isEndOfPhrase(tokens[i])) {
				sentences.add(sentence);
				sentence = new ArrayList<String>();
			}
		}
		
		// in each sentence perform relation analysis
		
		for (List<String> phrase : sentences) {
			
			// Iterate over words to increase link actualities of mentioned words
			for (int i = 0; i < phrase.size(); i++) {
				for (int j = 0; j < phrase.size(); j++) {
					
					String word1 = phrase.get(i);
				    String word2 = phrase.get(j);
					
					// skip punctuation, look only forward to minimize unlikely connections
					if (!utils.isPunctuation(word1) && !utils.isPunctuation(word2) &&
							j > i) {
						int abs_dist = Math.abs(utils.distanceWithoutPunctuation(word1, word2, phrase));
						// only for different words inside relation horizon
						if (abs_dist <= wordRelationDepth && abs_dist != 0) {
							
							// TODO: operations on each word combination
							
							Logos first = utils.emptyNamedLogos("", -1);
							Logos second = utils.emptyNamedLogos("", -1);
							
							boolean firstFound = false;
							boolean secondFound = false;
							
							for (Logos l1 : database.logosList) {
								if (l1.name.equals(word1)) {
									firstFound = true;
									first = l1;
									l1.actualizeAllLinks(database.rememberRate, verbose);
									break;
								}
							}
							
							for (Logos l2 : database.logosList) {
								if (l2.name.equals(word2)) {
									secondFound = true;
									second = l2;
									l2.actualizeAllLinks(database.rememberRate, verbose);
									break;
								}
							}
							
							// If one of the words wasn't found
							if (!firstFound || !secondFound) {
								
								write("      Analyzing input, an unknown word occured", 2);
								
								/* 
								* With a high probability, the token is representing a Link
								*  in the human language: is, has, what, that, it, to...
								*  In this case we could increase the actuality of all similar
								*  Links next to analyzed words in order to increase the
								*  chances of getting the meaning right.
								*/
								
							} else {
								
								write("      Analyzing input, found both words", 2);
								
								ArrayList<Link> possibleLinks = new ArrayList<Link>();
								
								// do l1 and l2 have source-target relation?
								// search through all l1-links
								
								for (Link lk : first.inwardLinks) {
									if (lk.source.id == second.id) {
										possibleLinks.add(lk);
									}
								}
								for (Link lk : first.outwardLinks) {
									if (lk.target.id == second.id) {
										possibleLinks.add(lk);
									}
									// l2 can be the first in the Branch, don't forget that!
									if (lk.target.getClass().getSimpleName().equals("Branch")) {
										if (((Branch) (lk.target)).containedLogosList.get(0).name.equals(second.name)) {
											possibleLinks.add(lk);
										}
									}
								}
								
								// EXPERIMENTAL: choose the Link by maximum |g|*a
								double ga = -2.0;
								Link bestLink = utils.emptyNamedLink("", -1);
								for (Link lk : possibleLinks) {
									double link_ga = Math.abs(lk.generality) * lk.actuality;
									if (link_ga > ga) {
										ga = link_ga;
										bestLink = lk;
									}
								}
								
								long bestLinkID = bestLink.id;
								
								write("      Analyzing input, found best Link:", 2);
								write("      [" + bestLink.relationName + "] <" + bestLink.generality + ", " + bestLink.actuality + ">", 2);
								
								for (Link l : database.linkList) {
									if (l.id == bestLinkID) {
										
										// Operations on the best Link candidate
										
										write("      Updating actuality and generality of apparent Link", 2);
										
										l.actuality = utils.
												updateActuality(l.actuality, false, database.forgetRate, database.rememberRate);
										l.generality = utils.
												generality(utils.evidenceFromGenerality(l.generality, belief) + 1, belief);
										
										write("      [" + l.relationName + "] <" + l.generality + ", " + l.actuality + ">", 2);
										
										break;
									}
								}
								
							}
						} // end of single combination processing
					}
				}
			} // end of all combination analysis in a sentence
			
			/*
			 * Here can be other pattern matching routines to extract Links from
			 * a natural language sentence.
			 */
			
			
		} // end loop over sentences
		
		write("MAIN: extracting links from input finished", 2);
		
	}// end analyzeInput()
	
	/*
	 * This method can be used to prove the value of Thoughts
	 * and to engage an action according to the most valuable Thoughts.
	 */
	public static void processThoughts (ArrayList<Thought> thoughtQueue) {
		
	}
	
	/*
	 * For manual database input (important in the beginning)
	 */
	public static void manualInput (String command) {
		
		write("MAIN: manual information input", 3);
		
		// Logos, Link IDs
		long logosNum = -1;
		long linkNum = -1;

		// TODO: wrong, get max IDs!
		logosNum = database.getMaxLogosID();
		linkNum = database.getMaxLinkID();
		
		//logosNum = database.logosList.size() - 1;
		//linkNum = database.linkList.size() - 1;
		
		String[] blocks = command.split(" ");
		
		boolean logos = true;
		// boolean addToBranch = false;
		
		int openedBranch = -1;
		
		// empty working objects
		// Branch actualBranch = new Branch("", -1, new ArrayList<Link>(), new ArrayList<Link>());
		ArrayList<Branch> tempBranchList = new ArrayList<Branch>();
		ArrayList<Boolean> open = new ArrayList<Boolean>();
		
		boolean branchNeedsLink = false;
		int brIdx = -1;
		
		for (int i = 1; i < blocks.length; i++) {
			
			write("MAIN: at index " + i, 3);
			
			// Better: recursive Branch completion
			
			if (blocks[i].equals("#B")) {
				
				openedBranch = tempBranchList.size();
				
				open.add(true);
				
				write("MAIN: Branch beginning", 3);
				
				logosNum++;
				Branch currentBranch = utils.emptyNamedBranch("#BRANCH" + logosNum, logosNum);
				
				if (i > 1) {
					
					Link source = utils.findLinkByID(database.linkList, linkNum);
					
					currentBranch.inwardLinks.add(source);
					
					source.target = currentBranch;
					
				}
				
				tempBranchList.add(currentBranch);
								
				continue;
				
			}
			
			if (blocks[i].equals("B#")) {
				
				write("MAIN: Branch ending", 3);
				
				Branch b = tempBranchList.get(openedBranch);
				
				database.logosList.add(b);
				
				database.branchList.add(b);
				
				write("MAIN: added Branch and Logos " + b.id + " [" + b.name + "]", 3);
				
				open.set(openedBranch, false);
				
				openedBranch = open.lastIndexOf(true);
				
				if (openedBranch != -1) {
					
					tempBranchList.get(openedBranch).containedLogosList.add(b);
					
				}
				
				// switch to "link needed" mode
				branchNeedsLink = true;
				brIdx = (int) b.id;
				
				continue;
			}
			
			//////////////////////////////////////////////////////
			
			// Branch indicator
			/*if (blocks[i].contains("#")) {
				
				// TODO what to do in the beginning and in the end
				if (addToBranch == false) {
					
					write("MAIN: Branch beginning", 3);
					
					logosNum++;
					actualBranch = utils.emptyNamedBranch("#BRANCH" + logosNum, logosNum);
					
					if (i > 1) {
						
						Link source = utils.findLinkByID(database.linkList, linkNum);
						
						actualBranch.inwardLinks.add(source);
						
						source.target = actualBranch;
						
					}
					
				} else {
					
					write("MAIN: Branch ending", 3);
					
					database.logosList.add(actualBranch);
					write("MAIN: added Branch/Logos " + actualBranch.id + "[" + actualBranch.name + "]", 3);
					
					database.branchList.add(actualBranch);
					
				}
				
				addToBranch = !addToBranch;
				continue;
				
			}*/
			
			///////////////////////////////////////
			
			// Branch creation
			if (open.contains(true)) {
				
				// Logos token
				if (logos) {
					
					write("MAIN: parsing Logos", 3);
					
					logosNum++;
					Logos lg = utils.emptyNamedLogos(blocks[i], logosNum);
					
					// The Link connects to the whole Branch, not to the first Logos in it!
					if (i > 1 && !blocks[i - 1].contains("#")) {
						
						Link source = utils.findLinkByID(database.linkList, linkNum);
						
						lg.inwardLinks.add(source);
						
						source.target = lg;
						
					}
					
					database.logosList.add(lg);
					write("MAIN: added Logos " + lg.id + " [" + lg.name + "]", 3);
					
					// In a Branch: add Logos in list
					tempBranchList.get(openedBranch).containedLogosList.add(lg);
					
					logos = false;
					
					continue;
					
				}
				
				// Non-Logos token
				if (!logos) {
					
					write("MAIN: parsing non-Logos", 3);

					linkNum++;
					Link lk = utils.emptyNamedLink(blocks[i], linkNum);

					lk.generality = 0.0;
					lk.actuality = 1.0;
					
					// just after it was closed
					if (branchNeedsLink) {
						
						Logos source = utils.findLogosByID(database.logosList, brIdx);
						
						source.outwardLinks.add(lk);
						
						lk.source = source;
						
						branchNeedsLink = false;
						
					} else {
						
						Logos source = utils.findLogosByID(database.logosList, logosNum);
						
						source.outwardLinks.add(lk);
						
						lk.source = source;
					}
					
					tempBranchList.get(openedBranch).containedLinkList.add(lk);
					
					database.linkList.add(lk);
					write("MAIN: added Link " + lk.id + " [" + lk.relationName + "]", 3);
					
					logos = true;
					
				}
			
			// Normal chain, no Branch
			} else {
				
				// Logos token
				if (logos) {
					
					write("MAIN: parsing Logos", 3);
					
					logosNum++;
					Logos lg = utils.emptyNamedLogos(blocks[i], logosNum);
					
					if (i > 1) {
						
						Link source = utils.findLinkByID(database.linkList, linkNum);
						
						lg.inwardLinks.add(source);
						
						source.target = lg;
						
					}
					
					database.logosList.add(lg);
					write("MAIN: added Logos " + lg.id + " [" + lg.name + "]", 3);
					
					logos = false;
					
					continue;
					
				}
				
				// Non-Logos token
				if (!logos) {
					
					write("MAIN: parsing non-Logos", 3);

					String[] parsedLink = blocks[i].split(",");

					linkNum++;
					Link lk = utils.emptyNamedLink(parsedLink[0], linkNum);

					lk.generality = Double.parseDouble(parsedLink[1]);
					lk.actuality = 1.0;
					
					Logos source = utils.findLogosByID(database.logosList, logosNum);
					
					source.outwardLinks.add(lk);
					
					lk.source = source;
					
					database.linkList.add(lk);
					write("MAIN: added Link " + lk.id + " [" + lk.relationName + "]", 3);
					
					logos = true;
					
				}
				
			}
			
		}// end for
		
//		for (Branch br : tempBranchList) {
//			
//			database.branchList.add(br);
//			
//		}
		
	}
	
	public static void write (String str, int verb) {
		if (verb <= verbosity)
			System.out.println(str);
	}

}
