package logos;

import java.util.ArrayList;
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
	 * 2 = ... and utilities usage
	 * 3 = ... and database operations
	 */
	static int k = 10;	// coefficient for confidence calculation
	
	// Agents
	static DatabaseInterface database = new DatabaseInterface(verbosity);
	static ProblemSolver prsolver = new ProblemSolver(verbosity);
	static ProblemFinder prfinder = new ProblemFinder(verbosity);
	static Utils utils = new Utils(verbosity, k);
	static Scanner consoleInput = new Scanner(System.in);

	public static void main(String[] args) {
		
		// exit console?
		boolean exit = false;
		
		// Logos, Link IDs
		int logosNum = -1;
		int linkNum = -1;
		
		// Read database, update logosNum, linkNum
		database.logosList = database.readLogos();
		database.linkList = database.readLinks(database.logosList, utils);
		database.branchList = database.readBranches(database.logosList, database.linkList, utils);
		
		while (!exit) {
			
			logosNum = database.logosList.size() - 1;
			linkNum = database.linkList.size() - 1;
			
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
			
			// Place for manual input overrides...
			if (input.contains("#CHAIN") || input.contains("#BRANCH")) {
				
				manualInput(input);
				
			} else {
				
				// TODO main intelligent pipeline
				
				// Find problems arising after user's input
				// ArrayList<Problem> nextProblems = prfinder.findProblems(input, database.logosList, database.linkList, utils);
				
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
		int logosNum = -1;
		int linkNum = -1;

		logosNum = database.logosList.size() - 1;
		linkNum = database.linkList.size() - 1;
		
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
