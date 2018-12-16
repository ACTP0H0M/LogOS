package logos;

import java.util.ArrayList;

public class ProblemFinder {
	
	boolean verbose;
	
	public ProblemFinder(int verbosity) {
		if (verbosity >= 3) {
			verbose = true;
		}
	}
	
	/*
	 * Finds Problems after analyzing input text in accordance to actual database.
	 */
	public ArrayList<Problem> findProblems(String text, ArrayList<Logos> loglist, ArrayList<Link> linklist, Utils utils) {
		
		String[] tokens = utils.tokensFromText(text);
			
		// TODO Generate an "unknown" Link between each two Logos (?) if no evidence
		// whatsoever is available in the database about this connection.
		
		for (int i = 0; i < tokens.length; i++) {
			
			
			
		}
		
		
		
	}
	
	

}
