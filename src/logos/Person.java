package logos;

import opennlp.tools.parser.Parse;
import logos.Utils;

/*
 * This class will contain an extension of what was done in the MainClass
 * (methods talkAboutSelf, predefinedResponse), thus allowing even more
 * human-like interaction. While most simple questions are handled in the 
 * MainClass, the Person class deals with personal traits and character.
 * Especially important is the interaction strategy when the user doesn't
 * ask direct questions but rather makes statements that LogOS has to react
 * on.
 * The Person class will also implement attention algorithms that will help
 * LogOS concentrate on the most important information in the current context.
 * Short and long term planning must be also implemented.
 */
public class Person {
	
	private double empathy, sensitivity, optimism, honesty, verbosity, intellect;
	Emotion emotion;
	Keywords keywords = new Keywords();
	
	String heReference, sheReference, itReference, thisReference, thatReference, theyReference;

	// Working with sentences and database to generate answers
	public void applyPersonality(String sentence) {
		
		write("exited MainClass an entered Person");
		
		// At first, the right emotion must be chosen according to the input
		chooseEmotion(sentence);
		
		switch (emotion) {
		
		case HAPPINESS:
			happyAnswer(sentence);
			break;
			
		case ANGER:
			angryAnswer(sentence);
			break;
			
		case FEAR:
			fearfulAnswer(sentence);
			break;
			
		case SURPRISE:
			surprisedAnswer(sentence);
			break;
			
		case DISGUST:
			disgustedAnswer(sentence);
			break;
			
		case SADNESS:
			sadAnswer(sentence);
			break;
			
		case INTEREST:
			interestedAnswer(sentence);
			break;
			
		case EMBARASSEMENT:
			embarassedAnswer(sentence);
			break;
			
		case REGRET:
			regretfulAnswer(sentence);
			break;
			
		default:
			interestedAnswer(sentence);
			break;
		}
	}
	
	// Modifies the Emotion field of the class Person given the input sentence
	private void chooseEmotion(String sentence) {
		
	}
	
	private void happyAnswer(String sentence) {
		
	}
	
	private void angryAnswer(String sentence) {
		
	}
	
	private void fearfulAnswer(String sentence) {
		
	}
	
	private void surprisedAnswer(String sentence) {
		
	}
	
	private void disgustedAnswer(String sentence) {
		
	}
	
	private void sadAnswer(String sentence) {
		
	}
	
	private void interestedAnswer(String sentence) {
		
	}
	
	private void embarassedAnswer(String sentence) {
		
	}
	
	private void regretfulAnswer(String sentence) {
		
	}
	
	// for debugging purposes
	private void write(String phrase) {
		if (MainClass.verbosity >= 2)
			System.out.println("PERSON: " + phrase);
	}
	
	public double getEmpathy() {
		return empathy;
	}

	public void setEmpathy(double empathy) {
		this.empathy = empathy;
	}

	public double getSensitivity() {
		return sensitivity;
	}

	public void setSensitivity(double sensitivity) {
		this.sensitivity = sensitivity;
	}

	public double getOptimism() {
		return optimism;
	}

	public void setOptimism(double optimism) {
		this.optimism = optimism;
	}

	public double getHonesty() {
		return honesty;
	}

	public void setHonesty(double honesty) {
		this.honesty = honesty;
	}

	public double getVerbosity() {
		return verbosity;
	}

	public void setVerbosity(double verbosity) {
		this.verbosity = verbosity;
	}

	public double getIntellect() {
		return intellect;
	}

	public void setIntellect(double intellect) {
		this.intellect = intellect;
	}

}
