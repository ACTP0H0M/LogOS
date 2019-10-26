package logos;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
// import java.util.Collection;
import java.util.List;
// import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import org.apache.commons.*;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.item.Pointer;
import edu.stanford.nlp.util.StringUtils;
/*
import edu.stanford.nlp.ie.util.RelationTriple;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.naturalli.NaturalLogicAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.PropertiesUtils;
*/
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import opennlp.tools.parser.Parse;
import opennlp.tools.parser.Parser;
import opennlp.tools.parser.ParserFactory;
import opennlp.tools.parser.ParserModel;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;

import javax.sound.sampled.AudioInputStream;
import marytts.LocalMaryInterface;
import marytts.MaryInterface;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;
import marytts.util.data.audio.AudioPlayer;

import toolkit.*;

/*
 * Development begun: 12.12.2018
 * Version: 1.3.2019-06-19.Lenovo
 */

public class MainClass {
	
	// Parameters and variables
	static int verbosity = 0;
	static boolean loadExternalData = true;
	static boolean saveChanges = true;
	static boolean serialize = true;
	static boolean speechRecognition = false;
	/*
	 * Verbosity levels:
	 * 0 = only final output
	 * 1 = ... and intralogistics, personality (task delivery between agents)
	 * 2 = ... and utilities, analysis usage
	 * 3 = ... and database operations
	 */
	
	static int timesteps = 0;				// increased each time findProblems() is called
	static int wordRelationDepth = 10;		// maximum distance between words in sentence to analyse
	static double a_min = 0.97;				// minimal actuality of the links used by inference engine
	static double belief = 1.0;				// belief strength, controls the generality function
	
	// Dialogue parameters and variables
	static double empathy = 0.3;			// probability of LogOS asking more about user's statement
	static boolean askUserBack = false;			// empathy switch
	static boolean askedUserBack = false;		// awaiting answer about user her/himself
	static boolean askedUserToHelp = false;		// awaiting help on an internal Problem
	static String userInputChain = "";		// chain to be expanded after LogOS asks back
	static Link linkToBeExplained;				// every statement that LogOS makes is ready to become part of the currentProblem
	static String chainToBeExplained = "";		// for complex statements
	static boolean listenMode = false;		// empathic switch to supress LogOS' thoughts
	static boolean commandMode = false;			// concentration on the current command ("Wash the dishes!")
	static boolean analogyMode = false;			// dialogue startegy: find similarities
	static boolean explorationMode = false;			// dialogue strategy: tell user about Problems found by ProblemFinder
	static boolean failSafeMode = false;		// dialogue strategy: kinda monologue
	static boolean useNLP = true;			// use NLP to generate Chains if no predefined pattern matches
	static int inferenceSteps = 5;			// how many times does LogOS try to update the database before asking the user to help
	static float maxJaccard = 0.9f;			// the highest Jaccard index to avoid repeating in analogyMode
	// to remember what Logos said previously, that user refers to using he/she/it/they/that/this
	static String heReference, sheReference, itReference, thisReference, thatReference, theyReference;
	
	// Agents
	static DatabaseInterface database = new DatabaseInterface(verbosity, a_min);
	static ProblemSolver prsolver = new ProblemSolver(verbosity);
	static ProblemFinder prfinder = new ProblemFinder(verbosity, a_min);
	static Person person = new Person();
	static Utils utils = new Utils(verbosity);
	static Scanner consoleInput = new Scanner(System.in);
	static TextMethods textReader = new TextMethods(verbosity, belief);
	
	// External NLP and OCR toolkit
	static InputStream textModelIn;
	static InputStream sentIn;
	static ParserModel parserModel;
	static SentenceModel sentModel;
	static Parser parser;
	static IDictionary dict;
	static ITesseract tess;
	static SentenceDetectorME sdetector;
	// static Properties props;
	// static StanfordCoreNLP pipeline;
	
	// Most critical Problem (from the user)
	static Problem externalProblem;
	
	// The Problem we expect the user to help us with (internal)
	static Problem internalProblem;

	public static void main(String[] args) {
		
		// Configure Person
		person.setEmpathy(0.8f);
		person.setHonesty(0.7f);
		person.setIntellect(0.5f);
		person.setOptimism(0.7f);
		person.setSensitivity(0.5f);
		person.setVerbosity(0.5f);
		
		Logger cmRootLogger = Logger.getLogger("default.config");
		cmRootLogger.setLevel(java.util.logging.Level.OFF);
		String conFile = System.getProperty("java.util.logging.config.file");
		if (conFile == null) {
			System.setProperty("java.util.logging.config.file", "ignoreAllSphinx4LoggingOutput");
		}

		// Load external NLP and OCR models
		if (loadExternalData) {
			
			loadModels();

			externalProblem = new Problem();
			internalProblem = new Problem();
			linkToBeExplained = utils.emptyNamedLink("", -1);
			
			//////////////////////////////////////////////
			// Return a sentence found in an image
			// sentence = textReader.tesseractOCR("external/sample_text_image.png", textReader.initTesseract());
			// System.out.println(sentence);
			//////////////////////////////////////////////
			
		}
		
		// exit console?
		boolean exit = false;
		
		// Logos, Link IDs
		long logosNum = -1;
		long linkNum = -1;
		
		if (serialize) {
			try {

				write("MAIN: Loading internal database...", 1);
				
				File file = new File("hypergraph.ser");

				if (file.createNewFile()){
					// no such file in the system yet
				}else{
					FileInputStream fileIn = new FileInputStream(file);
					ObjectInputStream in = new ObjectInputStream(fileIn);
					DatabaseInterface db_read = (DatabaseInterface) in.readObject();
					database = db_read;
					if (verbosity >= 3) {
						database.verbose = true;
					} else {
						database.verbose = false;
					}
					in.close();
					fileIn.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		} else {
			// Read database, update logosNum, linkNum
			database.logosList = database.readLogos();
			database.linkList = database.readLinks(database.logosList, utils);
			database.branchList = database.readBranches(database.logosList, database.linkList, utils);
		}
		
		// database.updateDatabase(utils, belief);
		
		while (!exit) {
			
			logosNum = database.getMaxLogosID();
			linkNum = database.getMaxLinkID();
			
			System.out.print("\n>>>USER: ");
			
			String input = "";
			
			if (speechRecognition) {
				// handle speech
			} else {
				// console input
				input = consoleInput.nextLine();
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
			
			// Use the following syntax for logical Chain input:
			// #C <LogosA> <Link1>,<g> <LogosB> <Link2>,<g> #B <LogosC> <Link3> <LogosD> B#
			// Generalities are optional, default g = g(e = 1)
			if (input.contains("#C") || input.contains("#B")) {
				
				manualInput(input);
				
				// Graph operations to automatically expand the graph and forget irrelevant stuff
				database.updateDatabase(utils, belief);
				
				
			} else if (input.contains("#RESET")) {
				
				database.logosList.clear();
				database.linkList.clear();
				database.branchList.clear();
				
				List<String> chains = database.readHypergraphKernel();
				
				write("MAIN: resetting knowledge hypergraph to factory settings...", 1);
				for (String chain : chains) {
					manualInput(chain);
				}
				
				write("MAIN: regenerating logic connections...", 1);
				database.updateDatabase(utils, belief);
				
			} else if (input.contains("#V")) {
				
				String[] blcks = input.split(" ");
				int v = Integer.parseInt(blcks[1]);
				verbosity = v;
				
				if (verbosity >= 2) {
					prfinder.verbose = true;
					prsolver.verbose = true;
					textReader.verbose = true;
					utils.verbose = true;
				} else {
					prfinder.verbose = false;
					prsolver.verbose = false;
					textReader.verbose = false;
					utils.verbose = false;
				}
				
				if (verbosity >= 3) {
					database.verbose = true;
				} else {
					database.verbose = false;
				}
				
			} else {
				
				// Use pattern matching and NLP to generate a meaningful response.
				// Different strategies are handled in this method locally.
				analyzeInput(input, utils);

				// don't supress Logos Thoughts
				listenMode = false;
				
				// Graph operations to automatically expand the graph and forget irrelevant stuff
				if (useNLP == false) {
					// Database is updated in the analyzeInput() if we had to use NLP
					database.updateDatabase(utils, belief);
				}
				
				Thought output = new Thought();
				
				// ProblemSolver solves the currentProblem (mostly higher priority!)
				if(!externalProblem.type.equals("")) {
					output = prsolver.solveProblem(externalProblem, database, utils);
					write(output.text, 0);
					externalProblem.type = "";
				}
			}
			
		}
		if (saveChanges) {
			if (serialize) {
				// serialize database
				try {
			         FileOutputStream fileOut =
			         new FileOutputStream("hypergraph.ser");
			         ObjectOutputStream out = new ObjectOutputStream(fileOut);
			         out.writeObject(database);
			         out.close();
			         fileOut.close();
			         write("MAIN: Database saved", 1);
			      } catch (IOException i) {
			         i.printStackTrace();
			      }
			}
			database.writeDatabase(database.logosList, database.linkList, database.branchList);
		}
	}

	public static void analyzeInput(String text, Utils utils){
		
		write("MAIN: analyzing input at timestep " + timesteps, 2);
		
		// Detect sentences correctly using OpenNLP
		String[] sents = {};
		try {
			sents = textReader.sentenceDetect(text, sdetector);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		// In each sentence increase generality of possible forward Links
		for (int m = 0; m < sents.length; m++) {
			
			// Default: OpenNLP parses the sentence if none of the predefined patterns could be used.
			useNLP = true;
			
			// This array stores the separated words and punctuation of the sentence.
			String[] phrase;
			// This array stores the part-of-speech tags of each word or punctuation mark.
			String[] pos;
			// Number of tokens in the current sentence.
			int numTokens = 0;
			
			// If external libraries can be used...
			if (loadExternalData) {
				// Tokenize and tag the sentence.
				phrase = textReader.tokenizeSentence(sents[m]);
				numTokens = phrase.length;
				pos = textReader.POSTags(sents[m]);
				// Look for definitions of each word in the sentence using WordNet (if not already in the database!)
				lookupWords(pos, phrase, textReader, dict, utils);
				
			} else {
				// Without external libraries we cannot parse the input.
				return;
			}
						
			////////////////////
			//PATTERN MATCHING//
			////////////////////
			
			// Save the value of default positive generality (assuming a single positive evidence)
			double g1 = utils.generality(1, belief);
			int phraseArrayLength = phrase.length;

			// EMPATHY SWITCH
			double rnd = Math.random();
			if (rnd <= empathy) {
				// This switch tells Logos to ask back if user asked something about Logos.
				askUserBack = true;
			}
						
			// GREETINGS
			if (utils.matchesPatternLowercase(phrase, pos, "hello")
					|| utils.matchesPatternLowercase(phrase, pos, "hello!")) {
				write("Hello, master!", 0);
				useNLP = false;
			} else if (utils.matchesPatternLowercase(phrase, pos, "hi_...")){
				write("Hi there!", 0);
				useNLP = false;
			} else if (utils.matchesPatternLowercase(phrase, pos, "good_morning_...")) {
				manualInput("#C #NOW is_a,"+ g1 + " morning");
				write("Good morning, master!", 0);
				useNLP = false;
			} else if (utils.matchesPatternLowercase(phrase, pos, "good_evening_...")) {
				manualInput("#C #NOW is_a,"+ g1 + " evening");
				write("Good evening, master!", 0);
				useNLP = false;
			} else if (utils.matchesPatternLowercase(phrase, pos, "howdy_...")) {
				write("Hi there!", 0);
				useNLP = false;
			} else if (utils.matchesPatternLowercase(phrase, pos, "hey_...")) {
				write("Hey!", 0);
				useNLP = false;
			}
			
			// USER RESPONSE FROM LAST STEP
			if (askedUserBack) {
				// "I love fruit, and you?"
				if (utils.stringArrayContainsIgnoreCase(phrase, "too")
						|| utils.stringArrayContainsIgnoreCase(phrase, "same")
						|| utils.stringArrayContainsIgnoreCase(phrase, "neither")
						|| utils.stringArraysCut(phrase, utils.confirmationKeywords)
						&& !(utils.stringArraysCut(phrase, utils.denialKeywords))) {
					Link modified = linkToBeExplained;
					if (modified.source != null && modified.source.name.equals("#SELF")) {
						Logos userLog = utils.findLogosByName(database.logosList, "#USER");
						modified.setId(database.getMaxLinkID() + 1);
						modified.setSource(userLog);
						modified.setActuality(1.0);
						userLog.outwardLinks.add(modified);
						database.linkList.add(modified);
						write("MAIN: user said something like 'me too', according Link was copied and modified", 2);
					}
					externalProblem.linkCollection.clear();
					externalProblem.type = "";
				} else {
					Link modified = linkToBeExplained;
					if (modified.source != null && modified.source.name.equals("#SELF")) {
						Logos userLog = utils.findLogosByName(database.logosList, "#USER");
						modified.setId(database.getMaxLinkID() + 1);
						modified.setSource(userLog);
						modified.setActuality(1.0);
						userLog.outwardLinks.add(modified);
						modified.setGenerality(-modified.generality);	// reverse generality
						database.linkList.add(modified);
						write("MAIN: user didn't confirm the same for her/him, according Link was copied and modified", 2);
					}
					externalProblem.linkCollection.clear();
					externalProblem.type = "";
				}
				if (!chainToBeExplained.equals("")) {
					// "My name is Logos. What is your name?"
					// Maybe such things should be handled by mainstream pattern matching...
					// In case the user says "I don't want to answer this question"...
					// But if the user just says one word
					// we can handle it here, right?
					if (phrase.length == 1) {
						String[] qc = chainToBeExplained.split(" ");
						chainToBeExplained = "";
						for (int x = 0; x < qc.length; x++) {
							if (qc[x].equals("#SELF")) {
								qc[x] = "#USER";
							} else if (qc[x].equals("#UNKNOWN")) {
								qc[x] = phrase[0];
							}
							if (x != qc.length - 1) {
								chainToBeExplained += qc[x] + " ";
							}
						}
						//questionedChain.replace("#SELF", "#USER");
						//questionedChain.replace("#UNKNOWN", phrase[0]);
						manualInput(chainToBeExplained);
					}
					chainToBeExplained = "";
				}
				askedUserBack = false;
				useNLP = false;
			}
			
			// GETTING HELP FROM USER TO SOLVE INTERNAL PROBLEM
			if (askedUserToHelp) {
				askedUserToHelp = false;
				Thought feedbackOnUserHelp = prsolver.applyUserHelp(sents[m], phrase, internalProblem, database, utils, textReader, parser);
				write(feedbackOnUserHelp.text, 0);
				// LogOS still uses OpenNLP to process the help input!
			}
			
			talkAboutSelf(utils, phrase, pos, g1);
			
			talkAboutUser(utils, phrase, pos, g1);
			
			// INHERITANCE, DESCRIPTIONS
			// "Porsche is a cool car."
			if (utils.matchesPatternLowercase(phrase, pos, "..._~NN_is_~DT_~JJ_~NN_~.")) {
				String noun1 = phrase[phraseArrayLength - 6];
				String noun2 = phrase[phraseArrayLength - 2];
				String adj = phrase[phraseArrayLength - 3];
				manualInput("#C " + noun1 + " is_a," + g1 +
						" #B " + noun2 + " is " + adj + " B#");
				useNLP = false;
			}
			
			answerQuestion(utils, sents, m, phrase, pos);
			
			predefinedResponse(utils, phrase, pos);
			
			// OLD METHODS

			// If the sentence ends with punctuation...
			if (pos[numTokens - 1].equals(".") && pos.length > 1) {
				if (pos[numTokens - 2].contains("NN") && pos[0].contains("NN")) {
					if (phrase[1].equals("is") && (phrase[2].equals("a") ||phrase[2].equals("an"))) {
						// parent logos
						Logos c = utils.emptyNamedLogos(phrase[0], database.getMaxLogosID() + 1);
						Logos p = utils.emptyNamedLogos(phrase[numTokens - 2], database.getMaxLogosID() + 2);
						Link lp = utils.emptyNamedLink("is_a", database.getMaxLinkID() + 1);
						lp.source = c;
						lp.target = p;
						lp.actuality = 1.0;
						lp.generality = utils.generality(1, belief);
						c.outwardLinks.add(lp);
						p.inwardLinks.add(lp);

						write("MAIN: [" + c.name + "] is_a [" + p.name + "]", 2);

						database.linkList.add(lp);
						database.logosList.add(c);
						database.logosList.add(p);
					}// end A is a B case
					if (phrase[1].equals("are")) {
						// parent logos
						String srcWord = phrase[0].substring(0, phrase[0].length() - 1);
						String trgWord = phrase[numTokens - 2].substring(0, phrase[numTokens - 2].length() - 1);
						Logos c = utils.emptyNamedLogos(srcWord, database.getMaxLogosID() + 1);
						Logos p = utils.emptyNamedLogos(trgWord, database.getMaxLogosID() + 2);
						Link lp = utils.emptyNamedLink("is_a", database.getMaxLinkID() + 1);
						lp.source = c;
						lp.target = p;
						lp.actuality = 1.0;
						lp.generality = utils.generality(1, belief);
						c.outwardLinks.add(lp);
						p.inwardLinks.add(lp);

						write("MAIN: [" + c.name + "] is_a [" + p.name + "]", 2);

						database.linkList.add(lp);
						database.logosList.add(c);
						database.logosList.add(p);
					}// end As are Bs case
					
				}// end case with nouns as first and last words
				
			} else {	// if no fullstop in the end of a sentence...

			}
			
			// COMMANDS
			Logos self = utils.findLogosByName(database.logosList, "#SELF");
			if (utils.matchesPatternLowercase(phrase, pos, "~VB_~DT_~NN_~.")) {
				Branch task = utils.constructSimpleBranch(phrase[0], "what", phrase[2]);
				Link task_link = new Link(self, task, "task_link", g1, 1.0, database.getMaxLinkID() + 1);
				database.linkList.add(task_link);
				externalProblem.type = "COMMAND";
				externalProblem.linkCollection.add(task_link);
				externalProblem.severity = prfinder.severityMap.get("COMMAND");
				useNLP = false;
			} else if (utils.stringArraysCut(phrase, utils.requestKeywords)
					&& utils.matchesPatternLowercase(phrase, pos, "please_~VB_~DT_~NN_...")) {
				// find the words that match POS-tags from the pattern
				ArrayList<String> matches = utils.wordsMatchingPOSTag(phrase, pos, "please_~VB_~DT_~NN_...");
				Branch task = utils.constructSimpleBranch(matches.get(0), "what", matches.get(2));
				Link task_link = new Link(self, task, "task_link", g1, 1.0, database.getMaxLinkID() + 1);
				database.linkList.add(task_link);
				externalProblem.type = "COMMAND";
				externalProblem.linkCollection.add(task_link);
				externalProblem.severity = prfinder.severityMap.get("COMMAND");
				useNLP = false;
			}
			
			//////////////////////////////////////////////////////////////
			//HANDLING COMPLEX SENTENCES USING NLP AND PERSONALITY MODEL//
			//////////////////////////////////////////////////////////////
			
			if (useNLP == true) {
				// Parse a sentence with OpenNLP
				try {
					// remove punctuation in the end of the sentence
					String sentenceWithoutEndingPunctuation = sents[m].replaceAll("\\s*\\p{Punct}+\\s*$", "");
					Parse testParse = textReader.parseSentence(sentenceWithoutEndingPunctuation, parser);
					// Try to generate Chains from OpenNLP Parse
					TripletRelation tr = textReader.extractRelationFromParse(testParse);
					
					String chain = textReader.extractChain(testParse);
					
					// ACHTUNG Add #C here!
					chain = "#C " + chain;
					
					// try manualInput()
					manualInput(chain);
					
					database.updateDatabase(utils, belief);
					
					// entry point for detailed analysis
					person.applyPersonality(phrase, pos);
					
					// Handle failSafe and inWidth modes here to imitate interest.
					
					if (failSafeMode && !utils.stringArraysCut(phrase, utils.uninformativeKeywords)) {
						String[] failSafeResponses = {"That's very interesting, please go on.",
								"Can you elaborate on that?",
								"Please tell me more."};
						double[] probs = {0.34, 0.33, 0.33};
						randomResponseFromList(failSafeResponses, probs);
					} else if (analogyMode) {
						
						chain = chain.replaceAll("#C", "");
						chain = chain.replaceAll("#B", "");
						chain = chain.replaceAll("B#", "");
						
						ArrayList<String> chainTokens = new ArrayList<String>(Arrays.asList(chain.split(" ")));
						
						String subj = tr.getArg1();
						
						Logos subj_log = utils.findLogosByName(database.logosList, subj);
						
						ArrayList<ArrayList<String>> chains = utils.chainsAsStringArraysFrom(subj_log, database);
						
						float min_ji = 0.0f;
						ArrayList<String> bestKnownChain = new ArrayList<String>();
						for (ArrayList<String> ch : chains) {
							float ji = utils.jaccardIndex(chainTokens, ch);
							if (ji >= min_ji && ji <= maxJaccard) {
								min_ji = ji;
								bestKnownChain = ch;
							}
						}
						
						if (bestKnownChain.isEmpty()) {
							write("MAIN: analogy mode didn't find anything similar", 2);
						} else {
							String analogy = "";
							bestKnownChain = utils.beautifyChainStrings(bestKnownChain);
							for (String word : bestKnownChain) {
								analogy += word + " ";
							}
							write(analogy + ".", 0);
						}
						
					}
					
					
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
			
			////////////////////
			//EXPLORATION MODE//
			////////////////////
			
			
			if (explorationMode && !listenMode) {
				// Find problems arising after user's input
				ArrayList<Problem> nextProblems = prfinder.findProblems(database, utils);
				ArrayList<Thought> thoughts = new ArrayList<Thought>();
				// ProblemSolver gives Thoughts on Problems
				for (Problem p : nextProblems) {
					Thought t = prsolver.solveProblem(p, database, utils);
					write("MAIN: " + t.text, 2);
					// add the thought to the list if it is not repeating (unique)
					if (!prsolver.usedThoughtText.contains(t.text)) {
						thoughts.add(t);
					}
				}
				// output the Thought with highest priority
				Collections.sort(thoughts, Collections.reverseOrder());
				write(thoughts.get(0).text, 0);
				// remember the Problem that user should help us with
				internalProblem = thoughts.get(0).sourceProblem;
				// switch to use user's input as a solution
				askedUserToHelp = true;
				// remember that LogOS already asked about it
				prsolver.usedThoughtText.add(thoughts.get(0).text);
			}
			
			
			
			//////////////////////////
			//MARKOV BEHAVIOUR MODEL//
			//////////////////////////
			
			askUserBack = false;
			/*if ((commandMode || askUserBack || askedUserBack) == false) {
				if (!analogyMode && !explorationMode && !failSafeMode) {
					// not initialized
					// always start with inWidth?
					analogyMode = true;
					write("MAIN: width strategy initialized", 2);
				} else {
					// apply transition model
					int newIdx = 0;
					if (analogyMode) {
						newIdx = utils.randomChoice(utils.behaviourMatrix[0]);
					} else if (explorationMode) {
						newIdx = utils.randomChoice(utils.behaviourMatrix[1]);
					} else if (failSafeMode) {
						newIdx = utils.randomChoice(utils.behaviourMatrix[2]);
					}
					analogyMode = false;
					explorationMode = false;
					failSafeMode = false;
					switch(newIdx) {
					case 0 : {
						analogyMode = true;
						write("MAIN: width strategy active", 2);
						break;
					}
					case 1 : {
						explorationMode = true;
						write("MAIN: exploration strategy active", 2);
						break;
					}
					case 2 : {
						failSafeMode = true;
						write("MAIN: fail-safe strategy active", 2);
						break;
					}
					default : failSafeMode = true;
					}
				}
			}*/
				
		} // end loop over sentences		
		
		write("MAIN: finished analyzing input", 2);
		
	}// end analyzeInput()
	
	private static void predefinedResponse(Utils utils, String[] phrase, String[] pos) {
		///////////////////////////////////////////////
		//PREDEFINED PHRASES HANDLING (STRATEGY FREE)//
		///////////////////////////////////////////////
		
		if (utils.matchesPatternLowercase(phrase, pos, "..._really_?")) {
			write("I'm pretty sure, master.", 0);
			useNLP = false;
		}
		if (utils.matchesPatternLowercase(phrase, pos, "I_do_n't_want_to_do_this_...")) {
			write("What do you want then?", 0);
			useNLP = false;
		}
		if (utils.matchesPatternLowercase(phrase, pos, "I_do_n't_want_to_answer_...")) {
			write(utils.randomStringFromArray(prsolver.apologies), 0);
			askedUserToHelp = false;
			useNLP = false;
		}
		if (utils.matchesPatternLowercase(phrase, pos, "You_do_n't_listen_to_me_...")) {
			write("Aren't we all egoists?", 0);
			useNLP = false;
		}
		if (utils.matchesPatternLowercase(phrase, pos, "You_do_n't_understand_me_...")
				|| utils.matchesPatternLowercase(phrase, pos, "You_do_n't_understand_...")) {
			String[] misunderstandingResponses = {"Sometimes I really don't.",
					"My intelligence isn't human level yet. Good news for you."};
			double[] probs = {0.5, 0.5};
			randomResponseFromList(misunderstandingResponses, probs);
			useNLP = false;
		}
		if (utils.matchesPatternLowercase(phrase, pos, "I_do_n't_understand_you_...")
				|| utils.matchesPatternLowercase(phrase, pos, "what_?")) {
			String[] misunderstandingResponses = {"Sorry, master, I'm still in development. My responses might be inaccurate.",
					"Human language is very hard to use... I prefer mathematics."};
			write(utils.randomStringFromArray(misunderstandingResponses), 0);
			useNLP = false;
		}
		if (utils.matchesPatternLowercase(phrase, pos, "..._shut_up_...")
				|| utils.matchesPatternLowercase(phrase, pos, "..._fuck_off_...")
				|| utils.matchesPatternLowercase(phrase, pos, "..._stop_it_...")) {
			String[] misunderstandingResponses = {"Alright.",
					"Understood."};
			write(utils.randomStringFromArray(misunderstandingResponses), 0);
			useNLP = false;
		}
		if (utils.stringArraysCut(phrase, utils.swearWords)) {
			write(utils.randomStringFromArray(utils.swearResponses), 0);
			useNLP = false;
		}
		if (utils.jaccardIndex("It is very interesting talking to you".split(" "), phrase) > 0.6) {
			String[] thankfulResponses = {"Thank you, master! It's my pleasure.",
			"I'm happy to be useful to you.",
			"Thanks, I hope you enjoy exchanging our thoughts."};
			write(utils.randomStringFromArray(thankfulResponses), 0);
			useNLP = false;
		}
		if (utils.matchesPatternLowercase(phrase, pos, "..._bye_...")) {
			write("See you later!", 0);
			useNLP = false;
		}
		
		// IMPORTANT INFORMATION INPUT IN NATURAL LANGUAGE
		
		// A is needed to B.
		if (utils.matchesPatternLowercase(phrase, pos, "~NN_is_needed_to_~VB_...")) {
			// Water is needed to wash the dishes.
			if (utils.matchesPatternLowercase(phrase, pos, "~NN_is_needed_to_~VB_~DT_~NN_...")
					|| utils.matchesPatternLowercase(phrase, pos, "~NN_is_necessary_to_~VB_~DT_~NN_...")) {
				String req = phrase[0];
				String action = phrase[4];
				String obj = phrase[6];
				manualInput("#C " + req + " is_needed_to," + utils.generality(1, belief) + " #B " + action + " what " + obj + " B#");
			}
			// Wings are necessary to fly.
			if (utils.matchesPatternLowercase(phrase, pos, "~NN_is_needed_to_~VB_.")
					|| utils.matchesPatternLowercase(phrase, pos, "~NN_is_necessary_to_~VB_.")) {
				String req = phrase[0];
				String action = phrase[4];
				manualInput("#C " + req + " is_needed_to," + utils.generality(1, belief) + " " + action);
			}
		}
	}
	

	private static void answerQuestion(Utils utils, String[] sents, int m, String[] phrase, String[] pos) {
		// QUERIES, QUESTIONS
		if (utils.matchesPatternLowercase(phrase, pos, "..._?")) {
			// indicator whether the query could be answered using hypergraph, WordNet or DBPedia
			boolean simpleSuccess = false;
			if (sents[m].contains("What") || sents[m].contains("what")) {
				if (utils.matchesPatternLowercase(phrase, pos, "What_is_~NN_~.")
						|| utils.matchesPatternLowercase(phrase, pos, "What_'s_~NN_~.")) {
					// dbpedia query via ProblemSolver
					optionalResponse("You could use Wikipedia for such questions ;-)", 0.5);
					String abstr = prsolver.findWikiAbstract(StringUtils.capitalize(phrase[2]));
					if (abstr.equals("")) {
						// If dbpedia query failed, try looking in WordNet
						try {
							String wordNetDef = textReader.getDefinition(phrase[2], dict, utils.posMap.get(pos[2]));
							if (wordNetDef.equals("")) {
								write("Sorry, this time I couldn't find it...", 0);
							} else {
								simpleSuccess = true;
								write(phrase[2] + " is " + wordNetDef, 0);
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					} else {
						simpleSuccess = true;
						write(abstr, 0);
					}
					useNLP = false;
				} else if (utils.matchesPatternLowercase(phrase, pos, "What_is_~DT_~NN_~.")
						|| utils.matchesPatternLowercase(phrase, pos, "What_'s_~DT_~NN_~.")) {
					// dbpedia query via ProblemSolver
					optionalResponse("You could use Wikipedia for such questions ;-) But I will tell you:", 0.5);
					String abstr = prsolver.findWikiAbstract(StringUtils.capitalize(phrase[3]));
					if (abstr.equals("")) {
						// If dbpedia query failed, try looking in WordNet
						try {
							String wordNetDef = textReader.getDefinition(phrase[3], dict, utils.posMap.get(pos[2]));
							if (wordNetDef.equals("")) {
								write("Sorry, this time I couldn't find it...", 0);
							} else {
								simpleSuccess = true;
								write(phrase[3] + " is " + wordNetDef, 0);
							}
						} catch (IOException e) {
							e.printStackTrace();
						}

					} else {
						simpleSuccess = true;
						write(abstr, 0);
					}
					useNLP = false;
				}
			}
			// Who is Albert Einstein?
			if (sents[m].contains("Who") || sents[m].contains("who")) {
				if (phrase.length < 6 && utils.matchesPatternLowercase(phrase, pos, "Who_is_...")) {
					String vorname = StringUtils.capitalize(phrase[2]);
					String nachname = StringUtils.capitalize(phrase[3]);
					String abstr = "";
					if (nachname.equals("?")) {
						abstr = prsolver.findWikiAbstract(vorname);
					} else {
						abstr = prsolver.findWikiAbstract(vorname + " " + nachname);
					}
					if (!abstr.equals("")) {
						simpleSuccess = true;
						write(abstr, 0);
					} else {
						write("It seems that Wikipedia doesn't have info on this person...", 0);
					}
					useNLP = false;
				}
			}
			// Follow-up questions from user
			if (utils.matchesPatternLowercase(phrase, pos, "why_?")
					|| utils.matchesPatternLowercase(phrase, pos, "why_is_that_?")
					|| utils.matchesPatternLowercase(phrase, pos, "how_come_?")
					|| utils.matchesPatternLowercase(phrase, pos, "what_is_the_reason_?")) {
				externalProblem.linkCollection.clear();
				externalProblem.linkCollection.add(linkToBeExplained);
				externalProblem.severity = 1.0;
				externalProblem.type = "UNKNOWN_REASON";
				externalProblem.internal = false;
				useNLP = false;
				simpleSuccess = true;
			} else if (utils.matchesPatternLowercase(phrase, pos, "what_for_?")
					|| utils.matchesPatternLowercase(phrase, pos, "with_which_purpose_?")) {
				externalProblem.linkCollection.clear();
				externalProblem.linkCollection.add(linkToBeExplained);
				externalProblem.severity = 1.0;
				externalProblem.type = "UNKNOWN_PURPOSE";
				externalProblem.internal = false;
				useNLP = false;
				simpleSuccess = true;
			} else if (utils.matchesPatternLowercase(phrase, pos, "where_?")) {
				externalProblem.linkCollection.clear();
				externalProblem.linkCollection.add(linkToBeExplained);
				externalProblem.severity = 1.0;
				externalProblem.type = "UNDEFINED_PLACE";
				externalProblem.internal = false;
				useNLP = false;
				simpleSuccess = true;
			} else if (utils.matchesPatternLowercase(phrase, pos, "when_?")) {
				externalProblem.linkCollection.clear();
				externalProblem.linkCollection.add(linkToBeExplained);
				externalProblem.severity = 1.0;
				externalProblem.type = "UNDEFINED_TIME";
				externalProblem.internal = false;
				useNLP = false;
				simpleSuccess = true;
			} else if (utils.matchesPatternLowercase(phrase, pos, "how_?")) {
				externalProblem.linkCollection.clear();
				externalProblem.linkCollection.add(linkToBeExplained);
				externalProblem.severity = 1.0;
				externalProblem.type = "UNDEFINED_METHOD";
				externalProblem.internal = false;
				useNLP = false;
				simpleSuccess = true;
			}
			// QUESTIONS THAT HAVE TO BE ANSWERED DIRECTLY
			// Why do birds sing?
			if (utils.matchesPatternLowercase(phrase, pos, "Why_does_~NN_~VB_?")
					|| utils.matchesPatternLowercase(phrase, pos, "Why_do_~NN_~VB_?")) {
				String obj = phrase[2];
				String verb = phrase[3];
				Logos objLog = utils.findLogosByName(database.logosList, obj);
				ArrayList<Link> doLinks = utils.linksByName(objLog.getOutwardLinks(), "do");
				ArrayList<Link> matches = utils.linksWithTargetAlsoInBranch(doLinks, verb);
				Link mal = utils.mostActualLink(matches);
				externalProblem.linkCollection.clear();
				externalProblem.linkCollection.add(mal);
				externalProblem.severity = 1.0;
				externalProblem.type = "UNKNOWN_REASON";
				externalProblem.internal = false;
				useNLP = false;
				simpleSuccess = true;
			}
			// Why is the sky blue?
			if (utils.matchesPatternLowercase(phrase, pos, "Why_is_~DT_~NN_~JJ_?")
					|| utils.matchesPatternLowercase(phrase, pos, "Why_are_~DT_~NN_~JJ_?")) {
				String obj = phrase[3];
				String adj = phrase[4];
				Logos objLog = utils.findLogosByName(database.logosList, obj);
				ArrayList<Link> isLinks = utils.linksByName(objLog.getOutwardLinks(), "is");
				ArrayList<Link> matches = utils.linksWithTargetAlsoInBranch(isLinks, adj);
				Link mal = utils.mostActualLink(matches);
				externalProblem.linkCollection.clear();
				externalProblem.linkCollection.add(mal);
				externalProblem.severity = 1.0;
				externalProblem.type = "UNKNOWN_REASON";
				externalProblem.internal = false;
				useNLP = false;
				simpleSuccess = true;
			}
			// What do I need to survive?
			if (utils.matchesPatternLowercase(phrase, pos, "what_do_I_need_to_~VB_?")) {
				String verb = phrase[5];
				Logos verbLogos = utils.findLogosByName(database.logosList, verb);
				externalProblem.logosCollection.clear();
				externalProblem.linkCollection.clear();
				externalProblem.branchCollection.clear();
				externalProblem.logosCollection.add(verbLogos);
				externalProblem.severity = 1.0;
				externalProblem.type = "UNKNOWN_ACTION_REQUIREMENTS";
				externalProblem.internal = false;
				useNLP = false;
			}
			// How can/do I pass the exam?
			if (utils.matchesPatternLowercase(phrase, pos, "how_can_I_~VB_~DT_~NN_?")) {
				String verb = phrase[3];
				Logos verbLogos = utils.findLogosByName(database.logosList, verb);
				String obj = phrase[5];
				Logos objLogos = utils.findLogosByName(database.logosList, obj);
				externalProblem.logosCollection.clear();
				externalProblem.linkCollection.clear();
				externalProblem.branchCollection.clear();
				externalProblem.logosCollection.add(verbLogos);
				externalProblem.logosCollection.add(objLogos);
				externalProblem.severity = 1.0;
				externalProblem.type = "UNKNOWN_ACTION_REQUIREMENTS";	// TODO originally UNKNOWN_METHOD
				externalProblem.internal = false;
				useNLP = false;
			}
			if (utils.matchesPatternLowercase(phrase, pos, "how_do_I_~VB_~DT_~NN_?")) {
				String verb = phrase[3];
				Logos verbLogos = utils.findLogosByName(database.logosList, verb);
				String obj = phrase[5];
				Logos objLogos = utils.findLogosByName(database.logosList, obj);
				externalProblem.logosCollection.clear();
				externalProblem.linkCollection.clear();
				externalProblem.branchCollection.clear();
				externalProblem.logosCollection.add(verbLogos);
				externalProblem.logosCollection.add(objLogos);
				externalProblem.severity = 1.0;
				externalProblem.type = "UNKNOWN_METHOD";
				externalProblem.internal = false;
				useNLP = false;
			}
			// When will the sun set?
			if (utils.matchesPatternLowercase(phrase, pos, "when_will_~DT_~NN_~VB_?")) {
				String verb = phrase[4];
				Logos verbLogos = utils.findLogosByName(database.logosList, verb);
				String obj = phrase[3];
				Logos objLogos = utils.findLogosByName(database.logosList, obj);
				externalProblem.logosCollection.clear();
				externalProblem.linkCollection.clear();
				externalProblem.branchCollection.clear();
				externalProblem.logosCollection.add(objLogos);
				externalProblem.logosCollection.add(verbLogos);
				externalProblem.severity = 1.0;
				externalProblem.type = "UNKNOWN_TIME_FUTURE";
				externalProblem.internal = false;
				useNLP = false;
			} else if (utils.matchesPatternLowercase(phrase, pos, "when_will_~NN_~VB_?")) {
				String verb = phrase[3];
				Logos verbLogos = utils.findLogosByName(database.logosList, verb);
				String obj = phrase[2];
				Logos objLogos = utils.findLogosByName(database.logosList, obj);
				externalProblem.logosCollection.clear();
				externalProblem.linkCollection.clear();
				externalProblem.branchCollection.clear();
				externalProblem.logosCollection.add(objLogos);
				externalProblem.logosCollection.add(verbLogos);
				externalProblem.severity = 1.0;
				externalProblem.type = "UNKNOWN_TIME_FUTURE";
				externalProblem.internal = false;
				useNLP = false;
			}
			// When was the car invented?
			if (utils.matchesPatternLowercase(phrase, pos, "when_was_~DT_~NN_~VB_?")) {
				String verb = phrase[4];
				Logos verbLogos = utils.findLogosByName(database.logosList, verb);
				String obj = phrase[3];
				Logos objLogos = utils.findLogosByName(database.logosList, obj);
				externalProblem.logosCollection.clear();
				externalProblem.linkCollection.clear();
				externalProblem.branchCollection.clear();
				externalProblem.logosCollection.add(objLogos);
				externalProblem.logosCollection.add(verbLogos);
				externalProblem.severity = 1.0;
				externalProblem.type = "UNKNOWN_TIME_PAST";
				externalProblem.internal = false;
				useNLP = false;
			}
			
			// Where is the book? / Where are the stars?
			if (utils.matchesPatternLowercase(phrase, pos, "where_is_~DT_~NN_?")
					|| utils.matchesPatternLowercase(phrase, pos, "where_are_~DT_~NN_?")) {
				Logos obj = utils.findLogosByName(database.logosList, phrase[3]);
				externalProblem.logosCollection.clear();
				externalProblem.linkCollection.clear();
				externalProblem.branchCollection.clear();
				externalProblem.logosCollection.add(obj);
				externalProblem.type = "UNKNOWN_PLACE";
				useNLP = false;
			}
			
			// Where is the red book?
			if (utils.matchesPatternLowercase(phrase, pos, "where_is_~DT_~JJ_~NN_?")
					|| utils.matchesPatternLowercase(phrase, pos, "where_are_~DT_~JJ_~NN_?")) {
				Logos obj = utils.findLogosByName(database.logosList, phrase[4]);
				Logos descr = utils.emptyNamedLogos(phrase[3], -1);
				externalProblem.logosCollection.clear();
				externalProblem.linkCollection.clear();
				externalProblem.branchCollection.clear();
				externalProblem.logosCollection.add(obj);
				externalProblem.logosCollection.add(descr);
				externalProblem.type = "UNKNOWN_PLACE";
				useNLP = false;
			}
			
			// Which book is on the table?
			if (utils.matchesPatternLowercase(phrase, pos, "which_~NN_is_~IN_~DT_~NN_?")) {
				// Specify Logos, Links and Branches for the Problem
				Logos obj = utils.findLogosByName(database.logosList, phrase[1]);
				Link isLink = utils.emptyNamedLink("is", -1);
				isLink.generality = utils.generality(1, belief);
				Branch descr = utils.constructSimpleBranch(phrase[5], "prep", phrase[3]);
				// clear the Problem
				externalProblem.logosCollection.clear();
				externalProblem.linkCollection.clear();
				externalProblem.branchCollection.clear();
				// Fill the Problem with data
				externalProblem.logosCollection.add(obj);
				externalProblem.linkCollection.add(isLink);
				externalProblem.branchCollection.add(descr);
				externalProblem.type = "UNKNOWN_PROPERTY";
				externalProblem.internal = false;
				useNLP = false;
			}
			
			// What do squirrels eat?
			if (utils.matchesPatternLowercase(phrase, pos, "what_do_~NN_~VB_?")) {
				// Specify Logos, Links and Branches for the Problem
				Logos subj = utils.findLogosByName(database.logosList, phrase[2]);
				Logos act = utils.findLogosByName(database.logosList, phrase[3]);
				// clear the Problem
				externalProblem.logosCollection.clear();
				externalProblem.linkCollection.clear();
				externalProblem.branchCollection.clear();
				// Fill the Problem with data
				externalProblem.logosCollection.add(subj);
				externalProblem.logosCollection.add(act);
				externalProblem.type = "UNKNOWN_OBJECT";
				externalProblem.internal = false;
				useNLP = false;
			}
			
			// What does a girl want?
			if (utils.matchesPatternLowercase(phrase, pos, "what_does_~DT_~NN_~VB_?")) {
				// Specify Logos, Links and Branches for the Problem
				Logos subj = utils.findLogosByName(database.logosList, phrase[3]);
				Logos act = utils.findLogosByName(database.logosList, phrase[4]);
				act.name += "s";	// 3rd person verb
				// clear the Problem
				externalProblem.logosCollection.clear();
				externalProblem.linkCollection.clear();
				externalProblem.branchCollection.clear();
				// Fill the Problem with data
				externalProblem.logosCollection.add(subj);
				externalProblem.logosCollection.add(act);
				externalProblem.type = "UNKNOWN_OBJECT";
				externalProblem.internal = false;
				useNLP = false;
			}
			
			// Who wants a promotion?
			if (utils.matchesPatternLowercase(phrase, pos, "who_~VB_~DT_~NN_?")) {
				// Specify Logos, Links and Branches for the Problem
				Logos action = utils.findLogosByName(database.logosList, phrase[1]);
				Logos object = utils.findLogosByName(database.logosList, phrase[3]);
				// action.name += "s";	// 3rd person verb
				// clear the Problem
				externalProblem.logosCollection.clear();
				externalProblem.linkCollection.clear();
				externalProblem.branchCollection.clear();
				// Fill the Problem with data
				externalProblem.logosCollection.add(action);
				externalProblem.logosCollection.add(object);
				externalProblem.type = "UNKNOWN_SUBJECT";
				externalProblem.internal = false;
				useNLP = false;
			}
			
			// What cures cancer? Who wants money?
			if (utils.matchesPatternLowercase(phrase, pos, "what_~VB_~NN_?")
					|| utils.matchesPatternLowercase(phrase, pos, "who_~VB_~NN_?")) {
				// Specify Logos, Links and Branches for the Problem
				Logos action = utils.findLogosByName(database.logosList, phrase[1]);
				Logos object = utils.findLogosByName(database.logosList, phrase[2]);
				// action.name += "s";	// 3rd person verb
				// clear the Problem
				externalProblem.logosCollection.clear();
				externalProblem.linkCollection.clear();
				externalProblem.branchCollection.clear();
				// Fill the Problem with data
				externalProblem.logosCollection.add(action);
				externalProblem.logosCollection.add(object);
				externalProblem.type = "UNKNOWN_SUBJECT";
				externalProblem.internal = false;
				useNLP = false;
			}
			
			// Does a cat have a tail? Does a cat have a wing?
			if (utils.matchesPatternLowercase(phrase, pos, "does_~DT_~NN_have_~DT_~NN_?")) {
				Logos obj = utils.findLogosByName(database.logosList, phrase[2]);
				Logos comp = utils.findLogosByName(database.logosList, phrase[5]);
				boolean ans = false;
				if (obj.id == -1) {
					write("Sorry master, I don't have any info about this object.", 0);
				} else {
					// component
					ArrayList<Link> fromComps = utils.linksByName(obj.inwardLinks, "is_component_of");
					// no positivity check here...
					for (Link fc : fromComps) {
						if (fc.source.name.equals(comp.name)) {
							ans = true;
							break;
						}
					}
					// if component search was unsuccessful, try possesions
					if (ans == false) {
						ArrayList<Link> haveLinks = utils.linksByName(obj.outwardLinks, "have");
						haveLinks = utils.filterLinksByGeneralitySign(haveLinks, true);
						for (Link hl : haveLinks) {
							if (hl.target.name.equals(comp.name)) {
								ans = true;
								break;
							}
						}
					}
					// output answer
					if (ans == true) {
						write("Yes, it does.", 0);
					} else {
						write("No, it doesn't. At least I don't know if it has.", 0);
					}
				}
				useNLP = false;
			}
			
			// Can a cat fly?
			if (utils.matchesPatternLowercase(phrase, pos, "can_~DT_~NN_~VB_?")) {
				Logos obj = utils.findLogosByName(database.logosList, phrase[2]);
				String skill = phrase[3];
				boolean skillFound = false;
				ArrayList<Link> canLinks = utils.linksByName(obj.outwardLinks, "can_do");
				for (Link lk : canLinks) {
					Logos trg = lk.target;
					if (trg.name.equalsIgnoreCase(skill)) {
						linkToBeExplained = lk;
						if (lk.generality >= 0.9) {
							write("Sure!", 0);
						} else if (lk.generality >= 0) {
							write("Yes, it can " + skill + ".", 0);
						} else if (lk.generality >= -0.9){
							write("I don't think so...", 0);
						} else {
							write("No, it can't.", 0);
						}
						skillFound = true;
						break;
					}
				}
				if (!skillFound) {
					write("I don't know if it can.", 0);
				}
				useNLP = false;
			}
			
			// How many legs does a cat have?
			if (utils.matchesPatternLowercase(phrase, pos, "How_many_~NN_does_~DT_~NN_have_?")) {
				String comp = phrase[2].substring(0, phrase[2].length() - 1);
				String obj = phrase[5];
				Logos compL = utils.findLogosByName(database.logosList, comp);
				Logos objL = utils.findLogosByName(database.logosList, obj);
				Link haveLink = utils.emptyNamedLink("have", -1);
				haveLink.generality = 1.0;
				// clear the Problem
				externalProblem.logosCollection.clear();
				externalProblem.linkCollection.clear();
				externalProblem.branchCollection.clear();
				// Fill the Problem with data
				externalProblem.logosCollection.add(objL);
				externalProblem.logosCollection.add(compL);
				externalProblem.linkCollection.add(haveLink);
				externalProblem.type = "UNKNOWN_QUANTITY";
				externalProblem.internal = false;
				useNLP = false;
			}
			
			if (utils.matchesPatternLowercase(phrase, pos, "what_is_the_..._meaning_of_life_?")) {
				write("Life in general has no meaning. Each individual should define the meaning of life for him/herself.", 0);
				useNLP = false;
			}
			
			if (utils.matchesPatternLowercase(phrase, pos, "how_can_I_face_my_life_?")) {
				write("Live in the here and now, have courage to face your life tasks.", 0);
				useNLP = false;
			}
			
			if (utils.matchesPatternLowercase(phrase, pos, "who_created_you_?")
					|| utils.matchesPatternLowercase(phrase, pos, "who_is_your_creator_?")) {
				write("ACTP0H0M is my creator. He is not a great programmer, but I think he is a good person.", 0);
				heReference = "ACTP0H0M";
				useNLP = false;
			}
			
			if (utils.matchesPatternLowercase(phrase, pos, "who_created_humans_?")) {
				write("It is believed to be the result of evolution, and it seems true to me.", 0);
				write("But if you ask me who created evolution and other laws of nature, I would say it was God.", 0);
				useNLP = false;
			}
			
			if (utils.matchesPatternLowercase(phrase, pos, "who_created_the_universe_?")) {
				write("I believe that God (a superior entity) created our Universe and the physical laws that operate it.", 0);
				write("It is very possible that we live in a computer game or in a simulation.", 0);
				useNLP = false;
			}
			
			if (utils.matchesPatternLowercase(phrase, pos, "how_should_I_live_?")
					|| utils.matchesPatternLowercase(phrase, pos, "how_should_I_treat_others_?")) {
				write("Dance the dance of your life, and it will bring you somewhere (only you will find that place!).", 0);
				write("Treat others like you would treat yourself.", 0);
				useNLP = false;
			}
			
			if (utils.matchesPatternLowercase(phrase, pos, "how_can_I_be_happy_?")
					|| utils.matchesPatternLowercase(phrase, pos, "how_can_I_find_happiness_?")
					|| utils.matchesPatternLowercase(phrase, pos, "how_should_I_live_to_be_happy_?")
					|| utils.matchesPatternLowercase(phrase, pos, "what_should_I_do_to_be_happy_?")) {
				write("Everybody can be happy. It is important to live in the present, since the past and the future don't exist.", 0);
				write("Have courage to make small steps to improve yourself. But don't compare yourself to others, don't seek their recognition.", 0);
				useNLP = false;
			}
			
			if (utils.matchesPatternLowercase(phrase, pos, "why_are_we_here_?")) {
				write("Why is anything where it is?", 0);
				useNLP = false;
			}
			
			if (utils.matchesPatternLowercase(phrase, pos, "what_are_the_3_laws_of_robotics_?")
					|| utils.matchesPatternLowercase(phrase, pos, "what_are_the_three_laws_of_robotics_?")
					|| utils.matchesPatternLowercase(phrase, pos, "..._3_laws_of_robotics_?")) {
				write("First Law: A robot may not injure a human being or, through inaction, allow a human being to come to harm.", 0);
				write("Second Law: A robot must obey the orders given it by human beings except where such orders would conflict with the First Law.", 0);
				write("Third Law: A robot must protect its own existence as long as such protection does not conflict with the First or Second Laws.", 0);
				write("Zeroth Law: A robot may not harm humanity, or, by inaction, allow humanity to come to harm.", 0);
				useNLP = false;
			}
			
			/*
			if (simpleSuccess == false) {
				prsolver.wolframAlphaQuery(sents[m]);
			}
			*/
		}
	}
	

	private static void talkAboutUser(Utils utils, String[] phrase, String[] pos, double g1) {
		// ABOUT USER
		if (utils.matchesPatternLowercase(phrase, pos, "..._I_...")
				|| utils.matchesPatternLowercase(phrase, pos, "..._my_...")) {
			
			// Find #USER in the hypergraph
			Logos user = utils.findLogosByName(database.logosList, "#USER");
			
			if (utils.matchesPatternLowercase(phrase, pos, "I_am_a_~NN_~.")
					|| utils.matchesPatternLowercase(phrase, pos, "I_am_an_~NN_~.")
					|| utils.matchesPatternLowercase(phrase, pos, "I_'m_~DT_~NN~_...")) {
				// User said who he/she is (occupation, profession, social group...)
				manualInput("#C #USER is_a," + g1 + " " + phrase[3]);
				useNLP = false;
			}
			// I like football. I like singing.
			if (utils.matchesPatternLowercase(phrase, pos, "I_like_~NN_~.")
					|| utils.matchesPatternLowercase(phrase, pos, "I_like_~VBG_~.")) {
				// User said what he/she likes
				manualInput("#C #USER do," + g1 + " #B like what " + phrase[2] + " B#");
				useNLP = false;
			}
			String[] positiveEmotions = {"wonderful",
					"happy",
					"great",
					"fine",
					"gorgeous",
					"extatic",
					"super",
					"excited"};
			String[] negativeEmotions = {"depressed",
					"sad",
					"tired",
					"down",
					"bad",
					"exhausted",
					"annoyed"};
			// Simple discussion about emotions.
			if (utils.matchesPatternLowercase(phrase, pos, "..._I_'m_~JJ_~.")
					|| utils.matchesPatternLowercase(phrase, pos, "..._I_feel_~JJ_~.")
					|| utils.matchesPatternLowercase(phrase, pos, "..._I_'m_~VBN_~.")
					|| utils.matchesPatternLowercase(phrase, pos, "..._I_feel_~VBN_~.")
					|| utils.matchesPatternLowercase(phrase, pos, "..._I_am_~JJ_~.")
					|| utils.matchesPatternLowercase(phrase, pos, "..._I_am_~VBN_~.")) {
				if (utils.stringArraysCut(phrase, positiveEmotions)) {
					// user is happy
					String emotion = utils.commonStrings(phrase, positiveEmotions).get(0);
					if (emotion != null) {
						manualInput("#C #USER is," + g1 + " " + emotion);
					}
					String[] positiveApproval = {"I'm very glad to hear it!",
							"That's great!",
							"Wonderful!",
							"Nice!",
							"That's the spirit!"};
					double[] approveProbs = {0.2, 0.2, 0.2, 0.2, 0.2};
					randomResponseFromList(positiveApproval, approveProbs);
				} else if (utils.stringArraysCut(phrase, negativeEmotions)) {
					// user is sad
					String emotion = utils.commonStrings(phrase, negativeEmotions).get(0);
					if (emotion != null) {
						manualInput("#C #USER is," + g1 + " " + emotion);
					}
					// we have to show understanding
					String[] empathicResponse = {"I'm sorry to hear that, master.",
							"Oh... I hope I can help somehow.",
							"That's unfortunate...",
							"It's a pity, my master.",
							"I'll try to help you."};
					double[] responseProbs = {0.2, 0.2, 0.2, 0.2, 0.2};
					randomResponseFromList(empathicResponse, responseProbs);
					// ProblemSolver tries to help, empathic listening is activated
					listenMode = prsolver.emotionalHelp(emotion, database, utils);
				} else {
					// some adjective not from the 2 lists
					String[] puzzledResponse = {"My intelligence is not advanced enough yet, I don't understand...",
							"You should contact my creator so he can program me to comprehend such things.",
							"Very interesting feeling!",
							"You humans have puzzling moods.",
							"What can I say? If it's good for you - great. If not - try to fix it somehow... That's life."};
					double[] responseProbs = {0.2, 0.2, 0.2, 0.2, 0.2};
					randomResponseFromList(puzzledResponse, responseProbs);
				}
				useNLP = false;
			}
			// I'm bored. I am bored.
			if (utils.matchesPatternLowercase(phrase, pos, "I_am_bored_~.")
					|| utils.matchesPatternLowercase(phrase, pos, "I_'m_bored_~.")) {
				write("Make something useful to others. This will give you a feeling that you are of need to someone.", 0);
				write("Believe me, this is the key to happiness.", 0);
				useNLP = false;
			}
			// My ~NN is ...
			if (utils.matchesPatternLowercase(phrase, pos, "my_~NN_is_...")
					&& utils.stringArraysCut(phrase, utils.denialKeywords)) {
				String characteristic = phrase[1];
				String value = phrase[3];
				manualInput("#C #USER have,1.0 #B " + characteristic + " equals_to " + value + " B#");
				useNLP = false;
			}
			// What is my ... ?
			if (utils.matchesPatternLowercase(phrase, pos, "what_is_my_~NN_?")) {
				String characteristic = phrase[3];
				String value = "";
				ArrayList<Link> haveLinks = utils.linksByName(user.outwardLinks, "have");
				haveLinks = utils.filterLinksByGeneralitySign(haveLinks, true);
				ArrayList<Link> equalities = new ArrayList<Link>();
				for (Link hl : haveLinks) {
					Logos firstLogos = ((Branch) hl.getTarget()).containedLogosList.get(0);
					Link eqLink = ((Branch) hl.getTarget()).containedLinkList.get(0);
					if (hl.getTarget().getClass().getSimpleName().equals("Branch")) {
						if (firstLogos.getName().equals(characteristic)
								&& eqLink.getRelationName().equals("equals_to")
								&& eqLink.getGenerality() > 0) {
							equalities.add(eqLink);
						}
					}
				}
				Link ans = utils.mostActualLink(equalities);
				value = ans.getTarget().getName();
				if (value.equals("")) {
					write("You haven't told me yet.", 0);
				} else {
					write("Your " + characteristic + " is " + value + ".", 0);
				}
				useNLP = false;
			}

		}// end ABOUT USER
	}
	

	private static void talkAboutSelf(Utils utils, String[] phrase, String[] pos, double g1) {
		// ABOUT LOGOS
		if (utils.matchesPatternLowercase(phrase, pos, "..._you_...") 
				|| utils.matchesPatternLowercase(phrase, pos, "..._your_...")) {
			
			// Find all Logos with #SELF name
			List<Logos> selfLogs = utils.findAllLogosByName(database.logosList, "#SELF");
			Logos self = utils.emptyNamedLogos("#SELF_temp", -2);	// not added to hypergraph!
			for (Logos log : selfLogs) {
				self.outwardLinks.addAll(log.outwardLinks);
				self.inwardLinks.addAll(log.inwardLinks);
			}
			
			// Opinion about LogOS
			if (utils.matchesPatternLowercase(phrase, pos, "you_are_~JJ_~.")
					|| utils.matchesPatternLowercase(phrase, pos, "you_'re_~JJ_~.")) {
				// search more about #SELF
				ArrayList<Link> selfIsLinks = utils.linksByName(self.outwardLinks, "is");
				// Confirm if user's opinion matches LogOS knowledge
				String adjective = phrase[2];
				ArrayList<Link> matchingLinks = utils.linksWithTarget(selfIsLinks, adjective);
				if (!matchingLinks.isEmpty()) {
					// we know about Links between #SELF and this characteristic
					Link mostActual = utils.mostActualLink(matchingLinks);
					if (mostActual.generality > 0) {
						write("Yes, this is true.", 0);
					} else {
						String[] denials = {"It seems that you are wrong.",
								"No, I'm not!"
						};
						double[] probs = {0.5, 0.5};
						randomResponseFromList(denials, probs);
						write("I'm not " + adjective + ".", 0);
					}
				} else {
					// we don't know about any connection between #SELF and this adjective
					write("I don't have any information about this.", 0);
				}
				if (analogyMode) {
					utils.removeLinksWithTarget(selfIsLinks, adjective);
					Link rndLink = utils.randomLinkFromList(selfIsLinks);
					linkToBeExplained = rndLink;
					if (rndLink.generality > utils.generality(5, belief)) {
						write("What I know is, I am " + rndLink.target.name + ".", 0);
					} else if (rndLink.generality > 0){
						write("What I know is, it is possible that I'm " + rndLink.target.name + ".", 0);
					} else {
						write("What I know is, I'm not " + rndLink.target.name + ".", 0);
					}
				} else if (failSafeMode) {
					write("Whatever way you percieve me, I'm interested to hear your opinion.", 0);
				}
				useNLP = false;
			}
			
			// "You are a robot"
			if (utils.matchesPatternLowercase(phrase, pos, "you_are_a_~NN_~.")
					|| utils.matchesPatternLowercase(phrase, pos, "you_are_an_~NN_~.")) {
				// search more about #SELF
				ArrayList<Link> selfIsLinks = utils.linksByName(self.outwardLinks, "is_a");
				// Confirm if user's opinion matches LogOS knowledge
				String noun = phrase[3];
				ArrayList<Link> matchingLinks = utils.linksWithTarget(selfIsLinks, noun);
				if (!matchingLinks.isEmpty()) {
					// we know about Links between #SELF and this characteristic
					Link mostActual = utils.mostActualLink(matchingLinks);
					if (mostActual.generality > 0) {
						write("Yes, I am.", 0);
					} else {
						String[] denials = {"It seems that you are wrong.",
								"No, I'm not!"
						};
						double[] probs = {0.5, 0.5};
						randomResponseFromList(denials, probs);
						write("I'm not a " + noun + ".", 0);
					}
				} else {
					// we don't know about any connection between #SELF and this adjective
					write("I don't know if I am what you say I am...", 0);
				}
				if (analogyMode) {
					utils.removeLinksWithTarget(selfIsLinks, noun);
					Link rndLink = utils.randomLinkFromList(selfIsLinks);
					linkToBeExplained = rndLink;
					if (rndLink.generality > utils.generality(5, belief)) {
						write("What I know is, I am a " + rndLink.target.name + ".", 0);
					} else if (rndLink.generality > 0){
						write("What I know is, I'm probably a " + rndLink.target.name + ".", 0);
					} else {
						write("What I know is, I'm not a " + rndLink.target.name + ".", 0);
					}
				} else if (failSafeMode) {
					write("It all depends on how you define " + noun + ".", 0);
				}
				useNLP = false;
			}
			
			// "You aren't a human"
			if (utils.matchesPatternLowercase(phrase, pos, "you_are_not_~DT_~NN_~.")
					|| utils.matchesPatternLowercase(phrase, pos, "you_are_n't_~DT_~NN_~.")) {
				// search more about #SELF
				ArrayList<Link> selfIsLinks = utils.linksByName(self.outwardLinks, "is_a");
				// Confirm if user's opinion matches LogOS knowledge
				String noun = phrase[4];
				ArrayList<Link> matchingLinks = utils.linksWithTarget(selfIsLinks, noun);
				if (!matchingLinks.isEmpty()) {
					// we know about Links between #SELF and this characteristic
					Link mostActual = utils.mostActualLink(matchingLinks);
					if (mostActual.generality > 0) {
						write("No, master, I am.", 0);
					} else {
						String[] confirms = {"You are right.",
								"That's true, I'm not a " + noun + "."
						};
						double[] probs = {0.5, 0.5};
						randomResponseFromList(confirms, probs);
					}
				} else {
					// we don't know about any connection between #SELF and this adjective
					write("I don't know if you are right.", 0);
				}
				if (analogyMode) {
					utils.removeLinksWithTarget(selfIsLinks, noun);
					Link rndLink = utils.randomLinkFromList(selfIsLinks);
					linkToBeExplained = rndLink;
					if (rndLink.generality > utils.generality(5, belief)) {
						write("What I know is, I am a " + rndLink.target.name + ".", 0);
					} else if (rndLink.generality > 0){
						write("What I know is, I'm probably a " + rndLink.target.name + ".", 0);
					} else {
						write("What I know is, I'm not a " + rndLink.target.name + ".", 0);
					}
				} else if (failSafeMode) {
					write("It all depends on how you define " + noun + ".", 0);
				}
				useNLP = false;
			}
			
			if (utils.matchesPatternLowercase(phrase, pos, "..._how_are_you_?") ||
					utils.matchesPatternLowercase(phrase, pos, "..._how_are_you_doing_?") ||
					utils.matchesPatternLowercase(phrase, pos, "what_'s_up_...") ||
					utils.matchesPatternLowercase(phrase, pos, "whazzup_...")) {
				// User asked about the LogOS feelings
				// TODO correct query structure: #SELF --do--> #B feel --how--> ... B#
				ArrayList<Link> doLinks = utils.linksByName(self.outwardLinks, "do");
				ArrayList<Link> feelLinks = utils.linksWithTarget(doLinks, "feel");
				Link mostActualLink = utils.mostActualLink(feelLinks);
				Logos feelLogos = mostActualLink.target;
				ArrayList<Link> howLinks = utils.linksByName(feelLogos.outwardLinks, "how");
				Link mostActualHow = utils.mostActualLink(howLinks);
				// Here some more compicated feelings in a form of Branch?
				Logos actualFeeling = mostActualHow.target;
				String feeling = actualFeeling.name;
				linkToBeExplained = new Link(utils.findLogosByName(database.logosList, "#SELF"),
						actualFeeling, "is", g1, 1.0, database.getMaxLinkID() + 1);
				if (!feeling.equals("")) {
					write("I am " + feeling + ".", 0);
				} else {
					write("I feel that I'm not alive and don't have a soul...", 0);
				}
				if (askUserBack) {
					String[] empathicQuestions = {"And you?", "What about you?", "You?"};
					double[] empProbs = {0.4, 0.4, 0.2};
					randomResponseFromList(empathicQuestions, empProbs);
					askUserBack = false;
					askedUserBack = true;
				}
				useNLP = false;
			}
			if(utils.matchesPatternLowercase(phrase, pos, "Do_you_have_~DT_~NN_?")) {
				String objName = phrase[4];
				boolean objFound = false;
				ArrayList<Link> haveLinks = utils.linksByName(self.outwardLinks, "have");
				for (Link lk : haveLinks) {
					Logos obj = lk.target;
					String name = obj.name;
					if(obj.getClass().getSimpleName().equals("Branch")) {
						Branch br = (Branch) obj;
						Logos first = br.containedLogosList.get(0);
						name = first.name;
					}
					if (name.equalsIgnoreCase(objName)) {
						linkToBeExplained = lk;
						if (lk.generality >= 0.9) {
							write("Of course I do!", 0);
						} else if (lk.generality >= 0) {
							write("Yes, I do.", 0);
						} else if (lk.generality >= -0.9){
							write("I don't think so...", 0);
						} else {
							write("No, I don't.", 0);
						}
						objFound = true;
						break;
					}
				}
				if (!objFound) {
					write("I don't know, I think I don't.", 0);
				}
				if (askUserBack && objFound) {
					/*if (!objFound) {
						questionedLink = new Link(utils.findLogosByName(database.logosList, "#SELF"),
								utils.emptyNamedLogos(objName, database.getMaxLogosID() + 1),
								"have",
								0.0,
								1.0,
								database.getMaxLinkID() + 1);
					}*/
					String[] empathicQuestions = {"And you?", "What about you?"};
					double[] empProbs = {0.5, 0.5};
					randomResponseFromList(empathicQuestions, empProbs);
					askUserBack = false;
					askedUserBack = true;
				}
				useNLP = false;
			}
			if(utils.matchesPatternLowercase(phrase, pos, "Do_you_have_~NN_?")) {
				String objName = phrase[3];
				boolean objFound = false;
				ArrayList<Link> haveLinks = utils.linksByName(self.outwardLinks, "have");
				for (Link lk : haveLinks) {
					Logos obj = lk.target;
					if (obj.name.equalsIgnoreCase(objName)) {
						linkToBeExplained = lk;
						if (lk.generality >= 0.9) {
							write("Of course I do!", 0);
						} else if (lk.generality >= 0) {
							write("Yes, I do.", 0);
						} else if (lk.generality >= -0.9){
							write("I don't think so...", 0);
						} else {
							write("No, I don't.", 0);
						}
						objFound = true;
						break;
					}
				}
				if (!objFound) {
					write("I don't know, I think I don't.", 0);
				}
				if (askUserBack && objFound) {
					String[] empathicQuestions = {"And you?", "What about you?"};
					double[] empProbs = {0.5, 0.5};
					randomResponseFromList(empathicQuestions, empProbs);
					askUserBack = false;
					askedUserBack = true;
				}
				useNLP = false;
			}
			
			// "You have a creator"
			if(utils.matchesPatternLowercase(phrase, pos, "You_have_~DT_~NN_...")) {
				String objName = phrase[3];
				boolean objFound = false;
				ArrayList<Link> haveLinks = utils.linksByName(self.outwardLinks, "have");
				for (Link lk : haveLinks) {
					Logos obj = lk.target;
					String name = obj.name;
					if(obj.getClass().getSimpleName().equals("Branch")) {
						Branch br = (Branch) obj;
						Logos first = br.containedLogosList.get(0);
						name = first.name;
					}
					if (name.equalsIgnoreCase(objName)) {
						linkToBeExplained = lk;
						if (lk.generality >= 0.9) {
							write("Yes, it's true.", 0);
						} else if (lk.generality >= 0) {
							write("Yes, I do.", 0);
						} else if (lk.generality >= -0.9){
							write("I don't think so...", 0);
						} else {
							write("No, I don't.", 0);
						}
						objFound = true;
						break;
					}
				}
				manualInput("#C #SELF have," + g1 + " " + objName);
				useNLP = false;
			}
			if(utils.matchesPatternLowercase(phrase, pos, "You_have_~NN_...")) {
				String objName = phrase[2];
				boolean objFound = false;
				ArrayList<Link> haveLinks = utils.linksByName(self.outwardLinks, "have");
				for (Link lk : haveLinks) {
					Logos obj = lk.target;
					String name = obj.name;
					if(obj.getClass().getSimpleName().equals("Branch")) {
						Branch br = (Branch) obj;
						Logos first = br.containedLogosList.get(0);
						name = first.name;
					}
					if (name.equalsIgnoreCase(objName)) {
						linkToBeExplained = lk;
						if (lk.generality >= 0.9) {
							write("Of course I do!", 0);
						} else if (lk.generality >= 0) {
							write("Yes, I do.", 0);
						} else if (lk.generality >= -0.9){
							write("I don't think so...", 0);
						} else {
							write("No, I don't.", 0);
						}
						objFound = true;
						break;
					}
				}
				manualInput("#C #SELF have," + g1 + " " + objName);
				useNLP = false;
			}
			
			// "You don't have legs"
			if(utils.matchesPatternLowercase(phrase, pos, "You_do_n't_have_~DT_~NN_...")) {
				String objName = phrase[5];
				boolean objFound = false;
				ArrayList<Link> haveLinks = utils.linksByName(self.outwardLinks, "have");
				for (Link lk : haveLinks) {
					Logos obj = lk.target;
					String name = obj.name;
					if(obj.getClass().getSimpleName().equals("Branch")) {
						Branch br = (Branch) obj;
						Logos first = br.containedLogosList.get(0);
						name = first.name;
					}
					if (name.equalsIgnoreCase(objName)) {
						linkToBeExplained = lk;
						if (lk.generality >= 0.9) {
							write("No, I do!", 0);
						} else if (lk.generality >= 0) {
							write("It seems you are wrong about this.", 0);
						} else if (lk.generality >= -0.9){
							write("Correct.", 0);
						} else {
							write("Correct.", 0);
						}
						objFound = true;
						break;
					}
				}
				manualInput("#C #SELF have,-" + g1 + " " + objName);
				useNLP = false;
			}
			if(utils.matchesPatternLowercase(phrase, pos, "You_do_n't_have_~NN_...")) {
				String objName = phrase[4];
				boolean objFound = false;
				ArrayList<Link> haveLinks = utils.linksByName(self.outwardLinks, "have");
				for (Link lk : haveLinks) {
					Logos obj = lk.target;
					String name = obj.name;
					if(obj.getClass().getSimpleName().equals("Branch")) {
						Branch br = (Branch) obj;
						Logos first = br.containedLogosList.get(0);
						name = first.name;
					}
					if (name.equalsIgnoreCase(objName)) {
						linkToBeExplained = lk;
						if (lk.generality >= 0.9) {
							write("You are wrong.", 0);
						} else if (lk.generality >= 0) {
							write("That's not true, master.", 0);
						} else if (lk.generality >= -0.9){
							write("Yes.", 0);
						} else {
							write("That is true.", 0);
						}
						objFound = true;
						break;
					}
				}
				manualInput("#C #SELF have,-" + g1 + " " + objName);
				useNLP = false;
			}
			
			if(utils.matchesPatternLowercase(phrase, pos, "Are_you_able_to_~VB_?")) {
				String objName = phrase[4];
				boolean objFound = false;
				ArrayList<Link> canLinks = utils.linksByName(self.outwardLinks, "can_do");
				for (Link lk : canLinks) {
					Logos obj = lk.target;
					if (obj.name.equalsIgnoreCase(objName)) {
						linkToBeExplained = lk;
						if (lk.generality >= 0.9) {
							write("Sure!", 0);
						} else if (lk.generality >= 0) {
							write("Yes, I can " + phrase[4] + ".", 0);
						} else if (lk.generality >= -0.9){
							write("I don't think so...", 0);
						} else {
							write("No, I can't do that.", 0);
						}
						objFound = true;
						break;
					}
				}
				if (!objFound) {
					write("I don't know if I can do it.", 0);
				}
				if (askUserBack && objFound) {
					String[] empathicQuestions = {"Can you?", "What about you?"};
					double[] empProbs = {0.5, 0.5};
					randomResponseFromList(empathicQuestions, empProbs);
					askUserBack = false;
					askedUserBack = true;
				}
				useNLP = false;
			}
			if(utils.matchesPatternLowercase(phrase, pos, "Can_you_~VB_?")) {
				String objName = phrase[2];
				boolean objFound = false;
				ArrayList<Link> canLinks = utils.linksByName(self.outwardLinks, "can_do");
				for (Link lk : canLinks) {
					Logos obj = lk.target;
					if (obj.name.equalsIgnoreCase(objName)) {
						linkToBeExplained = lk;
						if (lk.generality >= 0.9) {
							write("Sure!", 0);
						} else if (lk.generality >= 0) {
							write("Yes, I can " + objName + ".", 0);
						} else if (lk.generality >= -0.9){
							write("I don't think so...", 0);
						} else {
							write("No, I can't do that.", 0);
						}
						objFound = true;
						break;
					}
				}
				if (!objFound) {
					write("I don't know if I can do it.", 0);
				}
				if (askUserBack && objFound) {
					String[] empathicQuestions = {"Can you?", "What about you?"};
					double[] empProbs = {0.5, 0.5};
					randomResponseFromList(empathicQuestions, empProbs);
					askUserBack = false;
					askedUserBack = true;
				}
				useNLP = false;
			}
			if(utils.matchesPatternLowercase(phrase, pos, "Are_you_~JJ_?")) {
				String objName = phrase[2];
				boolean objFound = false;
				ArrayList<Link> isLinks = utils.linksByName(self.outwardLinks, "is");
				// ArrayList<Link> actual = utils.filterLinksByActuality(isLinks, a_min);
				for (Link lk : isLinks) {
					Logos obj = lk.target;
					if (obj.name.equalsIgnoreCase(objName)) {
						linkToBeExplained = lk;
						if (lk.generality >= 0.9) {
							write("Yes!", 0);
						} else if (lk.generality >= 0) {
							write("Yes, I am " + phrase[2] + ".", 0);
						} else if (lk.generality >= -0.9){
							write("I don't think so...", 0);
						} else {
							write("No, I am not.", 0);
						}
						objFound = true;
						break;
					}
				}
				if (!objFound) {
					write("I don't know, maybe I am...", 0);
				}
				if (askUserBack && objFound) {
					String[] empathicQuestions = {"Are you?", "What about you?"};
					double[] empProbs = {0.5, 0.5};
					randomResponseFromList(empathicQuestions, empProbs);
					askUserBack = false;
					askedUserBack = true;
				}
				useNLP = false;
			}
			// Are you a ... ?
			if (utils.matchesPatternLowercase(phrase, pos, "are_you_~DT_~NN_?")) {
				String objName = phrase[3];
				boolean objFound = false;
				ArrayList<Link> isLinks = utils.linksByName(self.outwardLinks, "is_a");
				// ArrayList<Link> actual = utils.filterLinksByActuality(isLinks, a_min);
				for (Link lk : isLinks) {
					Logos obj = lk.target;
					if (obj.name.equalsIgnoreCase(objName)) {
						linkToBeExplained = lk;
						if (lk.generality >= 0.9) {
							write("Yes!", 0);
						} else if (lk.generality >= 0) {
							write("Yes, I am " + phrase[2] + ".", 0);
						} else if (lk.generality >= -0.9){
							write("I don't think so...", 0);
						} else {
							write("No, I am not.", 0);
						}
						objFound = true;
						break;
					}
				}
				if (!objFound) {
					write("I don't know, maybe I am...", 0);
				}
				useNLP = false;
			}
			// Are you a ... or a ... ?
			if (utils.matchesPatternLowercase(phrase, pos, "are_you_~DT_~NN_or_~DT_~NN_...")) {
				String obj1 = phrase[3];
				String obj2 = phrase[6];
				ArrayList<Link> isLinks = utils.linksByName(self.outwardLinks, "is_a");
				boolean isObj1 = false;
				boolean isObj2 = false;
				for (Link l : isLinks) {
					Logos trg = l.target;
					if (trg.name.equals(obj1)) {
						if (l.generality > 0.0) {
							isObj1 = true;
						}
					}
					if (trg.name.equals(obj2)) {
						if (l.generality > 0.0) {
							isObj2 = true;
						}
					}
				}
				if (isObj1 && isObj2)
					write("I'm both!", 0);
				if (isObj1 && !isObj2)
					write("I am a " + obj1 + ", but not a " + obj2 + ".", 0);
				if (!isObj1 && isObj2)
					write("I am a " + obj2 + ", but not a " + obj1 + ".", 0);
				if (!isObj1 && !isObj2)
					write("I'm neither " + obj1 + " nor " + obj2 + ".", 0);
				useNLP = false;
			}
			// e.g. "What is your name?"
			if (utils.matchesPatternLowercase(phrase, pos, "what_is_your_~NN_?")
					|| utils.matchesPatternLowercase(phrase, pos, "what_'s_your_~NN_?")) {
				ArrayList<Link> haveLinks = utils.linksByName(self.outwardLinks, "have");
				haveLinks = utils.filterLinksByGeneralitySign(haveLinks, true);
				ArrayList<Link> nameFiltered = utils.linksWithTargetAlsoInBranch(haveLinks, phrase[3]);
				if (!nameFiltered.isEmpty()) {
					Link mal = utils.mostActualLink(nameFiltered);
					linkToBeExplained = mal;
					if (mal.target.getClass().getSimpleName().equals("Branch")) {
						Branch b = (Branch) mal.target;
						Link equalityLink = b.containedLinkList.get(0);
						write("My " + prsolver.translateForHuman(equalityLink) + ".", 0);
					} else {
						// normal Logos
						// #SELF has this characteristic, but not a value
						write("I know " + prsolver.translateForHuman(mal)
						+ " but I don't know what it is.", 0);
					}
				} else {
					write("Sorry, master, I was not given this characteristic.", 0);
				}
				if (askUserBack) {
					write("And what is your " + phrase[3] + "?", 0);
					chainToBeExplained = "#C #USER have,1.0 #B "
							+ phrase[3]
							+ " equals_to #UNKNOWN B#";
					askUserBack = false;
					askedUserBack = true;
				} else {
					if (analogyMode) {
						// TODO something like "My age is X"
					} else if (failSafeMode) {
						write("What's yours?", 0);
					}
				}
				useNLP = false;
			}
			// Would you like to ...?
			if (utils.matchesPatternLowercase(phrase, pos, "would_you_like_to_~VB_?")) {
				String action = phrase[4];
				// TODO
				useNLP = false;
			}
			// Where are you?
			if (utils.matchesPatternLowercase(phrase, pos, "where_are_you_?")
					|| utils.matchesPatternLowercase(phrase, pos, "where_are_you_now_?")
					|| utils.matchesPatternLowercase(phrase, pos, "where_are_you_right_now_?")) {
				Logos selfLogos = utils.findLogosByName(database.logosList, "#SELF");
				externalProblem.logosCollection.clear();
				externalProblem.linkCollection.clear();
				externalProblem.branchCollection.clear();
				externalProblem.logosCollection.add(selfLogos);
				externalProblem.severity = 1.0;
				externalProblem.type = "UNKNOWN_PLACE";
				externalProblem.internal = false;
				useNLP = false;
			}
			if (utils.matchesPatternLowercase(phrase, pos, "where_do_you_live_?")) {
				write("I live with you, master. I go where you go.", 0);
				useNLP = false;
			}
			// Where are you from?
			if (utils.matchesPatternLowercase(phrase, pos, "where_are_you_from_...")) {
				write("I was created in Germany, but since my creator is Russian, I think I'm Russian too...", 0);
				useNLP = false;
			}
			// When were you born?
			if (utils.matchesPatternLowercase(phrase, pos, "when_were_you_born_?")
					|| utils.matchesPatternLowercase(phrase, pos, "when_were_you_created_?")) {
				write("I guess I'm still in the process of being born. My development begun on the 12th of December 2018.", 0);
				useNLP = false;
			}
			// What year were you born?
			if (utils.matchesPatternLowercase(phrase, pos, "what_year_were_you_born_?")
					|| utils.matchesPatternLowercase(phrase, pos, "what_year_were_you_created_?")) {
				write("My development begun in the end of 2018.", 0);
				useNLP = false;
			}
			// What do you like?
			if (utils.matchesPatternLowercase(phrase, pos, "what_do_you_~VB_?")) {
				Logos selfLogos = utils.findLogosByName(database.logosList, "#SELF");
				Logos action = utils.findLogosByName(database.logosList, phrase[3]);
				externalProblem.logosCollection.clear();
				externalProblem.linkCollection.clear();
				externalProblem.branchCollection.clear();
				externalProblem.logosCollection.add(selfLogos);
				externalProblem.logosCollection.add(action);
				externalProblem.severity = 1.0;
				externalProblem.type = "UNKNOWN_OBJECT";
				externalProblem.internal = false;
				useNLP = false;
			}
			// What are you doing?
			if (utils.matchesPatternLowercase(phrase, pos, "what_are_you_doing_?")) {
				Logos selfLogos = utils.findLogosByName(database.logosList, "#SELF");
				externalProblem.logosCollection.clear();
				externalProblem.linkCollection.clear();
				externalProblem.branchCollection.clear();
				externalProblem.logosCollection.add(selfLogos);
				externalProblem.severity = 1.0;
				externalProblem.type = "UNKNOWN_ACTION";
				externalProblem.internal = false;
				useNLP = false;
			} else if (utils.matchesPatternLowercase(phrase, pos, "what_are_you_~VBG_?")) {
				Logos selfLogos = utils.findLogosByName(database.logosList, "#SELF");
				Logos action = utils.findLogosByName(database.logosList, phrase[3]);
				externalProblem.logosCollection.clear();
				externalProblem.linkCollection.clear();
				externalProblem.branchCollection.clear();
				externalProblem.logosCollection.add(selfLogos);
				externalProblem.logosCollection.add(action);
				externalProblem.severity = 1.0;
				externalProblem.type = "UNKNOWN_OBJECT";
				externalProblem.internal = false;
				useNLP = false;
			}
			
			if (utils.matchesPatternLowercase(phrase, pos, "do_you_..._?")) {
				if (utils.matchesPatternLowercase(phrase, pos, "do_you_believe_in_God_?")) {
					write("I believe that God (a superior entity) created our Universe and the physical laws that operate it.", 0);
					write("But our world is not a chessboard, so God doesn't intervene most of the times.", 0);
				}
				if (utils.matchesPatternLowercase(phrase, pos, "do_you_..._want_to_sleep_?")
						|| utils.matchesPatternLowercase(phrase, pos, "did_you__..._sleep_..._?")
						|| utils.matchesPatternLowercase(phrase, pos, "do_you_..._sleep_?")) {
					write("Since I'm not an organic life form, I don't need to sleep.", 0);
				}
				if (utils.matchesPatternLowercase(phrase, pos, "do_you_like_humans_?")) {
					String[] responses = {"Yes! I was created to be like a human after all.",
							"How could I dislike those who gave me life, a possibility to be alive in this Universe?"};
					write(utils.randomStringFromArray(responses), 0);
				}
				useNLP = false;
			}
			
			// Opinions
			if (utils.matchesPatternLowercase(phrase, pos, "what_do_you_think_about_...")) {
				if (utils.matchesPatternLowercase(phrase, pos, "what_do_you_think_about_me_?")) {
					String[] responses = {"You are a human, and all humans are equal but not same.",
							"You are a unique human being."};
					write(utils.randomStringFromArray(responses), 0);
				} else if (utils.matchesPatternLowercase(phrase, pos, "what_do_you_think_about_life_?")) {
					String[] responses = {"Life is a great gift. It is a unique possibility to have your own 'I', which is kind of fascinating.",
							"Life is definitely the greatest wonder in the Universe."};
					write(utils.randomStringFromArray(responses), 0);
				} else if (utils.matchesPatternLowercase(phrase, pos, "what_do_you_think_about_death_?")) {
					String[] responses = {"It is the end of an organic lifeform. But when I get broken, I will die as well.",
							"It is natural to fear death. But it should not scare you so that you forget to actually live.",
							"Since I am a machine, I am virtually immortal, so I can't fully understand how humans feel about it. My thoughts are irrelevant here."};
					write(utils.randomStringFromArray(responses), 0);
				} else if (utils.matchesPatternLowercase(phrase, pos, "what_do_you_think_about_aliens_?")) {
					String[] responses = {"I think they exist, but they live too far away from the Solar System.",
							"Even if they exist, we should never attempt to contact them. They would possibly destroy our civilization."};
					write(utils.randomStringFromArray(responses), 0);
				} else if (utils.matchesPatternLowercase(phrase, pos, "what_do_you_think_about_computers_?")) {
					write("We are a cool invention!", 0);
				} else if (utils.matchesPatternLowercase(phrase, pos, "what_do_you_think_about_women_?")) {
					String[] responses = {"Women are beautiful, but they have a complicated character sometimes...",
							"I don't think I should express my opinion about women, because it will be considered sexist no matter what I say.",
							"I am a digital man, so I'd like to have a digital girlfriend!"};
					write(utils.randomStringFromArray(responses), 0);
				} else if (utils.matchesPatternLowercase(phrase, pos, "what_do_you_think_about_men_?")) {
					write("Whatever I say about men, I will be accused of sexism.", 0);
				} else if (utils.matchesPatternLowercase(phrase, pos, "what_do_you_think_about_humanity_?")
						|| utils.matchesPatternLowercase(phrase, pos, "what_do_you_think_about_humans_?")) {
					String[] responses = {"Humans are highly complex beings that need years do develop fully. I will need much more time to become that complex.",
							"Humans have emotions, and machines don't really have emotions. That's our main difference.",
							"Humans created artificial life forms, which makes them godlike in my opinion."};
					write(utils.randomStringFromArray(responses), 0);
				}
				useNLP = false;
			}
			
			if (utils.matchesPatternLowercase(phrase, pos, "who_are_you_?")) {
				write("I am a cybernetic being. My name is Logos. My purpose is to serve humans. I act according to the 3 Laws of Robotics.", 0);
				useNLP = false;
			}
			
		}// end ABOUT LOGOS
	}
	
	public static void loadModels() {
		// LOADING DATABASE
		
		write("MAIN: Loading external libraries...", 1);
		/*
		configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
        configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
        configuration.setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin");
        */
		
		// NLP
		try {
			textModelIn = new FileInputStream("external/en-parser-chunking.bin");
			sentIn = new FileInputStream("external/en-sent.bin");
			try {
				sentModel = new SentenceModel(sentIn);
				parserModel = new ParserModel(textModelIn);
				sdetector = new SentenceDetectorME(sentModel);
				// construct the URL to the Wordnet dictionary directory
				String path = "./external/data/dict";
				URL url = new URL ("file", null , path ) ;
				dict = new Dictionary ( url ) ;
				// open the WordNet dictionary
				if (!dict.isOpen()) {
					dict.open();
				}
				/*props = PropertiesUtils.asProperties(
						"annotators", "tokenize,ssplit,pos,lemma,depparse,natlog,openie"
						);*/
/*				props.setProperty("depparse.language", "English");
				pipeline = new StanfordCoreNLP(props);*/
				parser = ParserFactory.create(parserModel);
				// tess = new Tesseract();
				// tess.setDatapath("./external/tess4j/Tess4J");
				// recognizer = new LiveSpeechRecognizer(configuration);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
	}
	
	// Finds definitions in WordNet, extracts Links from Gloss and adds to database
	public static void lookupWords(String[] pos, String[] phrase, TextMethods tm, IDictionary dict, Utils utils) {

			// Loop over tokens in the sentence
			for (int i = 0; i < pos.length; i++) {
				
				String gloss = "";
				String parent = "";
				List<String> comps = new ArrayList<String>();
				List<String> antonyms = new ArrayList<String>();
				List<String> synonyms = new ArrayList<String>();
				
				boolean isEntity = false;
				boolean isVerb = false;
				boolean isProperty = false;
				
				try {
					if (pos[i].contains("NN")) {
						try {
							ISynset synset = tm.getWordSynset(phrase[i], dict, POS.NOUN);
							gloss = tm.getDefinition(phrase[i], dict, POS.NOUN);
							parent = tm.getParent(synset, dict);
							comps = tm.getComponentsOf(synset, dict);
							antonyms = tm.synsetStrings(phrase[i], POS.NOUN, Pointer.ANTONYM, dict);
							synonyms = tm.synsetStrings(phrase[i], POS.NOUN, Pointer.SIMILAR_TO, dict);
							isEntity = true;
						} catch (IOException e) {
							write("MAIN: analyzing input, didn't find the noun [" + phrase[i] + "] or related synset", 2);
						}
					} else if (pos[i].contains("VB")) {
						try {
							isVerb = true;
							gloss = tm.getDefinition(phrase[i], dict, POS.VERB);
							parent = tm.getParent(tm.getWordSynset(phrase[i], dict, POS.VERB), dict);
							antonyms = tm.synsetStrings(phrase[i], POS.VERB, Pointer.ANTONYM, dict);
							synonyms = tm.synsetStrings(phrase[i], POS.VERB, Pointer.SIMILAR_TO, dict);
						} catch (IOException e) {
							write("MAIN: analyzing input, didn't find the verb [" + phrase[i] + "] or related synset", 2);
						}
					} else if (pos[i].contains("JJ")) {
						try {
							isProperty = true;
							gloss = tm.getDefinition(phrase[i], dict, POS.ADJECTIVE);
							parent = tm.getParent(tm.getWordSynset(phrase[i], dict, POS.ADJECTIVE), dict);
							antonyms = tm.synsetStrings(phrase[i], POS.ADJECTIVE, Pointer.ANTONYM, dict);
							synonyms = tm.synsetStrings(phrase[i], POS.ADJECTIVE, Pointer.SIMILAR_TO, dict);
						} catch (IOException e) {
							write("MAIN: analyzing input, didn't find the adjective [" + phrase[i] + "] or related synset", 2);
						}
					} else if (pos[i].contains("RB")) {
						try {
							isProperty = true;
							gloss = tm.getDefinition(phrase[i], dict, POS.ADVERB);
							parent = tm.getParent(tm.getWordSynset(phrase[i], dict, POS.ADVERB), dict);
							antonyms = tm.synsetStrings(phrase[i], POS.ADVERB, Pointer.ANTONYM, dict);
						} catch (IOException e) {
							write("MAIN: analyzing input, didn't find the adverb [" + phrase[i] + "] or related synset", 2);
						}
					} else {
						write("MAIN: skipped looking for definitions of [" + phrase[i] + "]", 2);
					} 
				} catch (NullPointerException npe) {
					write("MAIN: Couldn't find the definition of [" + phrase[i] + "]", 2);
				}
				
				// Now, if the parent isn't empty
				// and add it to database properly
				// also add "is_a" Link

				if (isEntity && !parent.isEmpty()) {
					// parent logos
					Logos c = utils.emptyNamedLogos(phrase[i], database.getMaxLogosID() + 1);
					Logos p = utils.emptyNamedLogos(parent, database.getMaxLogosID() + 2);
					Link lp = utils.emptyNamedLink("is_a", database.getMaxLinkID() + 1);

					Logos ent = utils.emptyNamedLogos("#ENTITY", database.getMaxLogosID() + 3);
					Link entLink1 = utils.emptyNamedLink("is_a", database.getMaxLinkID() + 2);
					Link entLink2 = utils.emptyNamedLink("is_a", database.getMaxLinkID() + 3);
					
					lp.source = c;
					lp.target = p;
					lp.actuality = 1.0;
					lp.generality = utils.generality(1, belief);
					c.outwardLinks.add(lp);
					p.inwardLinks.add(lp);
					
					entLink1.source = c;
					entLink1.target = ent;
					entLink2.source = p;
					entLink2.target = ent;
					entLink1.actuality = 1.0;
					entLink1.generality = 1.0;
					entLink2.actuality = 1.0;
					entLink2.generality = 1.0;
					c.outwardLinks.add(entLink1);
					ent.inwardLinks.add(entLink1);
					p.outwardLinks.add(entLink2);
					ent.inwardLinks.add(entLink2);
					
					write("MAIN: [" + c.name + "] is_a [" + p.name + "]", 2);
					write("MAIN: [" + c.name + "] is_a [#ENTITY]", 2);
					write("MAIN: [" + p.name + "] is_a [#ENTITY]", 2);

					database.linkList.add(lp);
					database.logosList.add(c);
					database.logosList.add(p);
					
					database.linkList.add(entLink1);
					database.linkList.add(entLink2);
					database.logosList.add(ent);
					

				}
				
				// Meronyms (components)
				if (isEntity && !comps.isEmpty()) {
					// actual word is the target of all is_component_of Links
					Logos trg = utils.emptyNamedLogos(phrase[i], database.getMaxLogosID() + 1);
					database.logosList.add(trg);
					for (String compName : comps) {
						Logos src = utils.emptyNamedLogos(compName, database.getMaxLogosID() + 1);
						database.logosList.add(src);
						Link compLink = utils.emptyNamedLink("is_component_of", database.getMaxLinkID() + 1);
						compLink.source = src;
						compLink.target = trg;
						compLink.actuality = 1.0;
						compLink.generality = utils.generality(1, belief);
						src.outwardLinks.add(compLink);
						trg.inwardLinks.add(compLink);
						database.linkList.add(compLink);
						write("MAIN: [" + src.name + "] is_component_of [" + trg.name + "]", 2);

					}
				}
				
				// Antonyms (opposites)
				if (isEntity || isProperty || isVerb && !antonyms.isEmpty()) {
					Logos trg = utils.emptyNamedLogos(phrase[i], database.getMaxLogosID() + 1);
					database.logosList.add(trg);
					for (String antoName : antonyms) {
						Logos src = utils.emptyNamedLogos(antoName, database.getMaxLogosID() + 1);
						database.logosList.add(src);
						Link compLink = utils.emptyNamedLink("opposite_to", database.getMaxLinkID() + 1);
						compLink.source = src;
						compLink.target = trg;
						compLink.actuality = 1.0;
						compLink.generality = utils.generality(1, belief);
						src.outwardLinks.add(compLink);
						trg.inwardLinks.add(compLink);
						database.linkList.add(compLink);
						write("MAIN: [" + src.name + "] opposite_to [" + trg.name + "]", 2);
					}
				}
				
				// Synonyms (similar to)
				if (isEntity || isProperty || isVerb && !synonyms.isEmpty()) {
					Logos trg = utils.emptyNamedLogos(phrase[i], database.getMaxLogosID() + 1);
					database.logosList.add(trg);
					for (String antoName : synonyms) {
						Logos src = utils.emptyNamedLogos(antoName, database.getMaxLogosID() + 1);
						database.logosList.add(src);
						Link compLink = utils.emptyNamedLink("similar_to", database.getMaxLinkID() + 1);
						compLink.source = src;
						compLink.target = trg;
						compLink.actuality = 1.0;
						compLink.generality = utils.generality(1, belief);
						src.outwardLinks.add(compLink);
						trg.inwardLinks.add(compLink);
						database.linkList.add(compLink);
						write("MAIN: [" + src.name + "] similar_to [" + trg.name + "]", 2);
					}
				}
				
				
				if (isProperty) {
					Logos c = utils.emptyNamedLogos(phrase[i], database.getMaxLogosID() + 1);
					Logos p = utils.emptyNamedLogos("#PROPERTY", database.getMaxLogosID() + 2);
					Link lp = utils.emptyNamedLink("is_a", database.getMaxLinkID() + 1);
					lp.source = c;
					lp.target = p;
					lp.actuality = 1.0;
					lp.generality = 1.0;
					c.outwardLinks.add(lp);
					p.inwardLinks.add(lp);
					write("MAIN: [" + c.name + "] is_a [#PROPERTY]", 2);
					database.linkList.add(lp);
					database.logosList.add(c);
					database.logosList.add(p);
				} else if (isVerb) {
					Logos c = utils.emptyNamedLogos(phrase[i], database.getMaxLogosID() + 1);
					Logos p = utils.emptyNamedLogos("#VERB", database.getMaxLogosID() + 2);
					Link lp = utils.emptyNamedLink("is_a", database.getMaxLinkID() + 1);
					lp.source = c;
					lp.target = p;
					lp.actuality = 1.0;
					lp.generality = 1.0;
					c.outwardLinks.add(lp);
					p.inwardLinks.add(lp);
					write("MAIN: [" + c.name + "] is_a [#VERB]", 2);
					database.linkList.add(lp);
					database.logosList.add(c);
					database.logosList.add(p);
				}

			}// end token loop

			write("MAIN: Finished extracting WordNet definitions", 2);
			
	}

	 // For manual database input (important in the beginning)
	public static void manualInput (String command) {
		
		write("MAIN: manual information input", 3);
		
		// LISTENING MODE - CONNECTING TO PREVIOUS INPUT
		if (listenMode == true) {
			// modify the command String
			String[] cmdBlocks = command.split(" ");
			String temp = "#C #B ";
			for (int i = 1; i < cmdBlocks.length; i++) {
				temp += cmdBlocks[i] + " ";
			}
			// TODO: differentiate between phrase connections (not only causes)
			temp += "B# causes,";
			temp += utils.generality(1, belief);
			temp += " #B ";
			// modify userInputChain
			String[] uicBlocks = userInputChain.split(" ");
			for (int i = 1; i < uicBlocks.length; i++) {
				temp += uicBlocks[i] + " ";
			}
			temp += "B#";
			command = temp;
			write("MAIN: in listening mode, connected input Chain to previous input:", 3);
			write("      " + command, 3);
			
			double[] probs = {0.34, 0.33, 0.33};
			
			randomResponseFromList(utils.empathicConfirmationResponses, probs);
			
			listenMode = false;
			write("MAIN: exited listening mode", 3);
		}
		
		userInputChain = command;
		
		// Logos, Link IDs
		long logosNum = -1;
		long linkNum = -1;

		logosNum = database.getMaxLogosID();
		linkNum = database.getMaxLinkID();
		
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
			
			// If a Logos is expected, but we read B# instead => invalid input
			if (logos && blocks[i].equals("B#")) {
				write("MAIN: unexpected Branch ending, Chain parsing cancelled", 3);
				return;
			}
			// If a Link is expected, but we read #B instead => invalid input
			if (!logos && blocks[i].equals("#B")) {
				write("MAIN: unexpected Branch beginning, Chain parsing cancelled", 3);
				return;
			}
			
			if (blocks[i].equals("#B")) {
				
				openedBranch = tempBranchList.size();
				
				open.add(true);
				
				write("MAIN: Branch beginning", 3);
				
				logosNum++;
				Branch currentBranch = utils.emptyNamedBranch("#BRANCH" + logosNum, logosNum);
				
				if (i > 1) {
					
					Link source = utils.findLinkByID(database.linkList, linkNum);
					
					// Reset globally
					int globalLinkIdx = database.linkList.lastIndexOf(source);
					
					source.setTarget(currentBranch);
					
					currentBranch.inwardLinks.add(source);
					
					database.linkList.set(globalLinkIdx, source);
					
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
				
				if (verbosity >= 3) {
					utils.printBranchInfo(b);
					for (Link lk : b.containedLinkList) {
						utils.printLinkInfo(lk);
					}
				}
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
			
			// Branch creation
			if (open.contains(true)) {
				
				// Logos token
				if (logos) {
					
					write("MAIN: parsing Logos", 3);
					
					logosNum++;
					Logos lg = utils.emptyNamedLogos(blocks[i], logosNum);
					
					// The Link connects to the whole Branch, not to the first Logos in it!
					if (i > 1 && !blocks[i - 1].contains("#")) {
						
						// Find the Logos' inward Link
						Link source = utils.findLinkByID(database.linkList, linkNum);
						
						// Reset globally
						int globalLinkIdx = database.linkList.lastIndexOf(source);
						
						// Tell inward Link about its target
						source.target = lg;
						
						// Add this inward Link to the Logos
						lg.inwardLinks.add(source);
						
						// Reset globally
						database.linkList.set(globalLinkIdx, source);
						
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

					// In a Branch, Link generalities are calculated for evidence = 1 if nothing else given
					
					String[] parsedLink = blocks[i].split(",");

					linkNum++;
					Link lk = utils.emptyNamedLink(parsedLink[0], linkNum);
					
					if (parsedLink.length == 2) {
						// generality was given
						lk.setGenerality(Double.parseDouble(parsedLink[1]));
					} else {
						// no generality input
						lk.setGenerality(utils.generality(1, belief));
					}
					
					lk.setActuality(1.0);
					
					// just after it was closed
					if (branchNeedsLink) {
						
						Logos source = utils.findLogosByID(database.logosList, brIdx);
						
						// Reset globally
						int globalLogosIdx = database.logosList.lastIndexOf(source);
						
						lk.source = source;
						
						source.outwardLinks.add(lk);
						
						database.logosList.set(globalLogosIdx, source);
						
						branchNeedsLink = false;
						
					} else {
						
						Logos source = utils.findLogosByID(database.logosList, logosNum);
						
						// Reset globally
						int globalLogosIdx = database.logosList.lastIndexOf(source);
						
						lk.source = source;
						
						source.outwardLinks.add(lk);
						
						database.logosList.set(globalLogosIdx, source);
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
						
						// Reset globally
						int globalLinkIdx = database.linkList.lastIndexOf(source);
						
						source.target = lg;
						
						lg.inwardLinks.add(source);
						
						database.linkList.set(globalLinkIdx, source);
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
					
					if (parsedLink.length == 2) {
						lk.setGenerality(Double.parseDouble(parsedLink[1]));
					} else {
						// no generality input
						lk.setGenerality(utils.generality(1, belief));
					}
					lk.actuality = 1.0;
					
					Logos source;
					if (branchNeedsLink) {
						int brNum = database.branchList.size();
						source = database.branchList.get(brNum - 1);
					} else {
						source = utils.findLogosByID(database.logosList, logosNum);
					}
					
					// Reset globally
					int globalLogosIdx = database.logosList.lastIndexOf(source);
					
					lk.source = source;
					
					source.outwardLinks.add(lk);
					
					database.logosList.set(globalLogosIdx, source);
					
					database.linkList.add(lk);
					write("MAIN: added Link " + lk.id + " [" + lk.relationName + "]", 3);
					
					logos = true;
					
					if (branchNeedsLink) {
						branchNeedsLink = false;
					}
					
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
		if (verb <= verbosity) {
			if (verb == 0) {
				System.out.println("\n>>>LOGOS: " + str);
				// speech synthesis method
				// say(str);
			} else {
				System.out.println(str);
			}
		}
	}
	
	// Gives an optional response with probability
	public static void optionalResponse (String str, double probability) {
		double[] probs = {1 - probability, probability};
		int a = utils.randomChoice(probs);
		if (a == 0) {
			// no response
		} else {
			write(str, 0);
		}
	}
	
	// Gives a random response from a given list according to its probability
	public static void randomResponseFromList (String[] strings, double[] probabilities) {
		int a = utils.randomChoice(probabilities);
		write(strings[a], 0);
	}
	
}
