package logos;

import java.util.ArrayList;

/*
 * Assumption generated by ProblemSolver.
 * Is to be sent to MainClass for final descision.
 */
public class Thought implements Comparable<Thought>{
	
	public String text;		// written by ProblemSolver
	public int timestep;	// written by MainClass
	public float priority;	// by ProblemSolver
	public Problem sourceProblem;
	// Solution data
	public ArrayList<Link> solutionLinkCollection = new ArrayList<Link>();
	public ArrayList<Logos> solutionLogosCollection = new ArrayList<Logos>();
	public ArrayList<Branch> solutionBranchCollection = new ArrayList<Branch>();
	
	public int compareTo(Thought other) {
        return ((Float) priority).compareTo(other.priority);
    }

}
