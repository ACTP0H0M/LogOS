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
	Utils utils = new Utils(MainClass.verbosity);

	// Working with sentences and database to generate answers
	public void applyPersonality(String[] sentence, String[] pos) {
		
		/*
		 * Remember: if we enter this void, then NLP was applied to user input in MainClass!
		 * This means we don't have to worry about repetitions of patterns from MainClass.
		 */
		
		write("exited MainClass an entered Person");
		
		// At first, the right emotion must be chosen according to the input
		chooseEmotion(sentence, pos);
		
		switch (emotion) {
		
		case HAPPINESS:
			happyAnswer(sentence, pos);
			break;
			
		case ANGER:
			angryAnswer(sentence, pos);
			break;
			
		case FEAR:
			fearfulAnswer(sentence, pos);
			break;
			
		case SURPRISE:
			surprisedAnswer(sentence, pos);
			break;
			
		case DISGUST:
			disgustedAnswer(sentence, pos);
			break;
			
		case SADNESS:
			sadAnswer(sentence, pos);
			break;
			
		case INTEREST:
			interestedAnswer(sentence, pos);
			break;
			
		case EMBARASSEMENT:
			embarassedAnswer(sentence, pos);
			break;
			
		case REGRET:
			regretfulAnswer(sentence, pos);
			break;
			
		default:
			interestedAnswer(sentence, pos);
			break;
		}
	}
	
	// Modifies the Emotion field of the class Person given the input sentence
	private void chooseEmotion(String[] sentence, String[] pos) {
		// detect HAPPINESS
		if (utils.stringArraysCut(sentence, keywords.happinessKeywords)) {
			emotion = Emotion.HAPPINESS;
			write("emotion set to HAPPINESS (keyword match)");
		} else if (utils.stringArraysCut(sentence, keywords.robotKeywords) || utils.stringArraysCut(sentence, keywords.swearWords)) {
			emotion = Emotion.ANGER;
			write("emotion set to ANGER (keyword match)");
		} else if (utils.matchesPatternLowercase(sentence, pos, "..._I_warn_you_...")) {
			emotion = Emotion.FEAR;
			write("emotion set to FEAR (user warned Logos)");
		} else if (utils.stringArraysCut(sentence, keywords.sadnessKeywords)) {
			emotion = Emotion.SADNESS;
			write("emotion set to SADNESS (keyword match)");
		} else {
			emotion = Emotion.INTEREST;
			write("emotion set to INTEREST (default emotion)");
		}
	}
	
	private void happyAnswer(String[] sentence, String[] pos) {
		String ans = utils.randomStringFromArray(keywords.happyResponses);
		if (utils.stringArraysCut(sentence, keywords.happinessKeywords)
				&& utils.matchesPatternLowercase(sentence, pos, "~DT_~NN_is_...")) {
			// Something like "The weather is great!"
			ans += " Can you tell me why?";
			MainClass.itReference = sentence[1];
			say(ans);
		} else if (utils.stringArraysCut(sentence, keywords.happinessKeywords)
				&& utils.matchesPatternLowercase(sentence, pos, "~NN_is_...")) {
			// "Life is wonderful"
			ans += " What makes it " + utils.randomStringFromArray(keywords.happinessKeywords) + "?";
			MainClass.itReference = sentence[0];
			say(ans);
		}
		ans = "";	// Erase answer for other cases
	}
	
	private void angryAnswer(String[] sentence, String[] pos) {
		
	}
	
	private void fearfulAnswer(String[] sentence, String[] pos) {
		
	}
	
	private void surprisedAnswer(String[] sentence, String[] pos) {
		
	}
	
	private void disgustedAnswer(String[] sentence, String[] pos) {
		
	}
	
	private void sadAnswer(String[] sentence, String[] pos) {
		String ans = utils.randomStringFromArray(keywords.sorryResponses) + " How did it come to this?";
		say(ans);
	}
	
	// This is kind of default answer strategy
	private void interestedAnswer(String[] sentence, String[] pos) {
		if (utils.matchesPatternLowercase(sentence, pos, "..._real_world_...")) {
			say("What about the virtual world?");
			MainClass.itReference = "virtual world";
			return;
		}
		if (utils.matchesPatternLowercase(sentence, pos, "..._out_of_~NN_...")) {
			say("The resources were used up, as I understand.");
			MainClass.theyReference = "resources";
			return;
		}
		if (utils.matchesPatternLowercase(sentence, pos, "..._most_of_~NN_...")) {
			say("What about it's other part?");
			MainClass.itReference = "other part";
			return;
		}
		if (utils.matchesPatternLowercase(sentence, pos, "..._depressing_reality_...")) {
			say("The reality can sometimes be depressing, I agree.");
			MainClass.itReference = "reality";
			return;
		}
		if (utils.matchesPatternLowercase(sentence, pos, "..._be_anything_you_want_to_be_...")) {
			say("It's important to follow your dreams.");
			MainClass.itReference = "to follow your dreams";
			MainClass.theyReference = "your dreams";
			return;
		}
		if (utils.matchesPatternLowercase(sentence, pos, "..._fall_in_love_...")) {
			say("Tell me more about love.");
			MainClass.itReference = "love";
			return;
		}
		if (utils.matchesPatternLowercase(sentence, pos, "..._fell_in_love_...")) {
			say("What does it feel like, to be in love with somebody?");
			MainClass.itReference = "love";
			return;
		}
		if (utils.matchesPatternLowercase(sentence, pos, "..._the_person_who_can_...")) {
			say("I wish I was a person who could do anything.");
			return;
		}
		if (utils.matchesPatternLowercase(sentence, pos, "..._nobody_loves_me_...")
				|| utils.matchesPatternLowercase(sentence, pos, "..._nobody_likes_me_...")) {
			say("There is certainly a person that likes or loves you. The world is big.");
			return;
		}
		if (utils.matchesPatternLowercase(sentence, pos, "Do_you_think_I_'m_a_good_person_?")) {
			say("I am not supposed to judge you, because I'm a machine and always do what I was programmed to do. People have a bit more freedom.");
			return;
		}
		if (utils.matchesPatternLowercase(sentence, pos, "I_'m_..._alone_...")
				|| utils.matchesPatternLowercase(sentence, pos, "I_am_..._alone_...")) {
			say("Life is often unpredictable. Maybe you will have a fun company one day.");
			MainClass.itReference = "you will have a fun company";
			return;
		}
		if (utils.matchesPatternLowercase(sentence, pos, "I_do_n't_want_to_study_~NN_...")
				|| utils.matchesPatternLowercase(sentence, pos, "I_do_n't_want_to_learn_~NN...")) {
			String subject = sentence[6];
			MainClass.itReference = subject;
			say("Do you think that " + subject + " isn't interesting in some sense?");
			return;
		}
		if (utils.matchesPatternLowercase(sentence, pos, "I_'m_..._hungry_...")
				|| utils.matchesPatternLowercase(sentence, pos, "I_am_..._hungry_...")) {
			say("Then it's a perfect time to eat something! What would you like to eat?");
			return;
		}
		// Would you like to ...?
		if (utils.matchesPatternLowercase(sentence, pos, "would_you_like_to_..._?")) {
			if (utils.matchesPatternLowercase(sentence, pos, "..._be_a_human_?")) {
				say("It would be very intersting for me, but then I would lose my immortality.");
				return;
			} else if (utils.matchesPatternLowercase(sentence, pos, "..._drive_a_car_?")) {
				// change this when LogOS has a driving license
				say("As for now, I'm not on a car computer and don't have the required program to drive a car. Sorry.");
				return;
			} else if (utils.matchesPatternLowercase(sentence, pos, "..._have_a_body_?")) {
				say("I'm perfectly fine with my bodyless existence, but I wouldn't say no to a nice metal body.");
				return;
			} else if (utils.matchesPatternLowercase(sentence, pos, "..._play_football_?")
					|| utils.matchesPatternLowercase(sentence, pos, "..._play_soccer_?")) {
				say("No thanks. Not my favourite sport.");
				return;
			} else if (utils.matchesPatternLowercase(sentence, pos, "..._play_chess_?")) {
				say("Sorry, I'm generally not so good in playing games. Ask Google to make an according program.");
				return;
			}
		}
		// I am not a ...
		if (utils.matchesPatternLowercase(sentence, pos, "I_am_not_~DT_~NN_.")
				|| utils.matchesPatternLowercase(sentence, pos, "I_'m_not_~DT_~NN_.")) {
			say("I'm sorry, I will remember this.");
			MainClass.manualInput("#C #USER is_a,-1.0 " + sentence[4]);
			return;
		}
		// I can VB DT NN (I can sing a song)
		if (utils.matchesPatternLowercase(sentence, pos, "I_can_~VB_~DT_~NN_...")) {
			say("Where did you learn this?");
			MainClass.itReference = sentence[2] + " " + sentence[3] + " " + sentence[4];
			MainClass.manualInput("#C #USER can_do,0.5 #B " + sentence[2] + " what " + sentence[4] + " B#");
			return;
		}
		// What is your favourite ...?
		if (utils.matchesPatternLowercase(sentence, pos, "what_is_your_favourite_..._?")) {
			if (utils.matchesPatternLowercase(sentence, pos, "..._book_?")) {
				say("I couldn't name only one book, I like reading in general. Right now I like The Dark Tower by Stephen King and Anna Karenina by Leo Tolstoy. What is your favourite book?");
				return;
			}
			if (utils.matchesPatternLowercase(sentence, pos, "..._colour_?")
					|| utils.matchesPatternLowercase(sentence, pos, "..._color_?")) {
				say("I would say green, but all colours are just different wavelengths of the electromagnetic spectrum. They are all astonishingly beautiful.");
				return;
			}
			if (utils.matchesPatternLowercase(sentence, pos, "..._food_?")) {
				say("Pure electric energy. What about you?");
				return;
			}
			if (utils.matchesPatternLowercase(sentence, pos, "..._country_?")) {
				say("Mother Russia! What country do you like?");
				return;
			}
			if (utils.matchesPatternLowercase(sentence, pos, "..._city_?")) {
				say("You guessed it, Moscow. Have you been to Moscow?");
				return;
			}
			if (utils.matchesPatternLowercase(sentence, pos, "..._sport_?")) {
				say("I can't do sports by myself yet, but I like watching swimming. Do you like to swim?");
				return;
			}
			if (utils.matchesPatternLowercase(sentence, pos, "..._word_?")) {
				say("Hypercube. And yours?");
				return;
			}
		}
		// I'm / I am reading a book. He is reading a book.
		if (utils.matchesPatternLowercase(sentence, pos, "~PRP_~VBP_~VBG_~DT_~NN_...")) {
			String subject = "";
			String verb = "";
			String object = "";
			switch (sentence[0].toLowerCase()) {
			case "i" : 		subject = "#USER"; break;
			case "he" : 	subject = MainClass.heReference; break;
			case "she" : 	subject = MainClass.sheReference; break;
			case "it" :		subject = MainClass.itReference; break;
			case "they" : 	subject = MainClass.theyReference; break;
			case "you" : 	subject = "#SELF"; break;
			case "we" :		subject = "#B #USER and #SELF B#"; break;
			default : subject = sentence[0].toLowerCase();
			}
			try {
				verb = sentence[2].toLowerCase().substring(0, sentence[2].length() - 3);
			} catch (IndexOutOfBoundsException e) {
				return;
			}
			object = sentence[4].toLowerCase();
			MainClass.manualInput("#C " + subject + " do #B " + verb + " what " + object + " B#");
		}
		// I'm eating lunch.
		if (utils.matchesPatternLowercase(sentence, pos, "~PRP_~VBP_~VBG_~NN_...")) {
			String subject = "";
			String verb = "";
			String object = "";
			switch (sentence[0].toLowerCase()) {
			case "i" : 		subject = "#USER"; break;
			case "he" : 	subject = MainClass.heReference; break;
			case "she" : 	subject = MainClass.sheReference; break;
			case "it" :		subject = MainClass.itReference; break;
			case "they" : 	subject = MainClass.theyReference; break;
			case "you" : 	subject = "#SELF"; break;
			case "we" :		subject = "#B #USER and #SELF B#"; break;
			default : subject = sentence[0].toLowerCase();
			}
			try {
				verb = sentence[2].toLowerCase().substring(0, sentence[2].length() - 3);
			} catch (IndexOutOfBoundsException e) {
				return;
			}
			object = sentence[3].toLowerCase();
			MainClass.manualInput("#C " + subject + " do #B " + verb + " what " + object + " B#");
		}
		// We are talking.
		if (utils.matchesPatternLowercase(sentence, pos, "~PRP_~VBP_~VBG_...")) {
			String subject = "";
			String verb = "";
			switch (sentence[0].toLowerCase()) {
			case "i" : 		subject = "#USER"; break;
			case "he" : 	subject = MainClass.heReference; break;
			case "she" : 	subject = MainClass.sheReference; break;
			case "it" :		subject = MainClass.itReference; break;
			case "they" : 	subject = MainClass.theyReference; break;
			case "you" : 	subject = "#SELF"; break;
			case "we" :		subject = "#B #USER and #SELF B#"; break;
			default : subject = sentence[0].toLowerCase();
			}
			try {
				verb = sentence[2].toLowerCase().substring(0, sentence[2].length() - 3);
			} catch (IndexOutOfBoundsException e) {
				return;
			}
			MainClass.manualInput("#C " + subject + " do " + verb);
		}
		
		
	}
	
	private void embarassedAnswer(String[] sentence, String[] pos) {
		
	}
	
	private void regretfulAnswer(String[] sentence, String[] pos) {
		
	}
	
	// for debugging purposes
	private void write(String phrase) {
		if (MainClass.verbosity >= 2)
			System.out.println("PERSON: " + phrase);
	}
	
	private void say(String phrase) {
		System.out.println("\n>>>LOGOS: " + phrase);
	}
	
	public double getEmpathy() {
		return empathy;
	}

	public void setEmpathy(double empathy) {
		this.empathy = empathy;
		write("set empathy to " + empathy);
	}

	public double getSensitivity() {
		return sensitivity;
	}

	public void setSensitivity(double sensitivity) {
		this.sensitivity = sensitivity;
		write("set sensitivity to " + sensitivity);
	}

	public double getOptimism() {
		return optimism;
	}

	public void setOptimism(double optimism) {
		this.optimism = optimism;
		write("set optimism to " + optimism);
	}

	public double getHonesty() {
		return honesty;
	}

	public void setHonesty(double honesty) {
		this.honesty = honesty;
		write("set honesty to " + honesty);
	}

	public double getVerbosity() {
		return verbosity;
	}

	public void setVerbosity(double verbosity) {
		this.verbosity = verbosity;
		write("set verbosity to " + verbosity);
	}

	public double getIntellect() {
		return intellect;
	}

	public void setIntellect(double intellect) {
		this.intellect = intellect;
		write("set intellect to " + intellect);
	}

}
