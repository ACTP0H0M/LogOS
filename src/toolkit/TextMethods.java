package toolkit;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.io.File;
import net.sourceforge.tess4j.*;
 
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import edu.mit.jwi.*;
import edu.mit.jwi.item.*;
import edu.stanford.nlp.ie.util.RelationTriple;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.naturalli.NaturalLogicAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.util.CoreMap;
import logos.Branch;
import logos.DatabaseInterface;
import logos.Link;
import logos.Logos;
import logos.Utils;
import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.cmdline.parser.ParserTool;
import opennlp.tools.parser.Parse;
import opennlp.tools.parser.Parser;
import opennlp.tools.parser.ParserFactory;
import opennlp.tools.parser.ParserModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;

/*
 * Methods for text parsing and dictionary queries
 */
public class TextMethods {
	
	public boolean verbose;
	private double belief;
	
	private static final String LABEL_TOP = "TOP";
	private static final String LABEL_SENTENCE = "S";
	private static final String LABEL_NOUN_PHRASE = "NP";
	private static final String LABEL_VERBAL_PHRASE = "VP";

	private static final String LABEL_NAME_PREFIX = "NN";
	private static final String LABEL_VERB_PREFIX = "VB";
	
	private boolean existentialThere = false;
	
	public TextMethods(int verbosity, double belief) {
		this.belief = belief;
		if (verbosity >= 2) {
			verbose = true;
		}
	}
	
//	public Thing lookupThing(String name, IDictionary dict) {
//		Thing t = new Thing();
//		t.setName(name);
//		ArrayList<String> hypernyms = new ArrayList<String>();
//		ArrayList<String> hyponyms = new ArrayList<String>();
//		try {
//			hypernyms = synsetStrings(name, POS.NOUN, Pointer.HYPERNYM, dict);
//			hyponyms = synsetStrings(name, POS.NOUN, Pointer.HYPONYM, dict);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		for (String str : hypernyms) {
//			Thing hyper = new Thing();
//			hyper.setName(str);
//			t.addHigherLevel(hyper);
//		}
//		for (String str : hyponyms) {
//			Thing hypo = new Thing();
//			hypo.setName(str);
//			t.addLowerLevel(hypo);
//		}
//		return t;
//	}
	
	// OpenNLP, use it to get an array of Strings for a given word, POS and synset Pointer
	public ArrayList<String> synsetStrings(String name, POS pos, Pointer synsetPtr, IDictionary dict) throws IOException {
		// look up first sense of the word "dog "
				IIndexWord idxWord = dict . getIndexWord (name, pos ) ;
				IWordID wordID = idxWord . getWordIDs () . get (0) ;
				IWord word = dict . getWord ( wordID ) ;
				ISynset synset = word.getSynset();
				if (verbose) {
					System . out . println ("Id = " + wordID ) ;
					System . out . println (" Lemma = " + word . getLemma () ) ;
					System . out . println (" Gloss = " + word . getSynset () . getGloss () ) ;
				}
				
				// get the (hypernyms)
				List < ISynsetID > xnyms = synset . getRelatedSynsets ( synsetPtr) ;
				
				ArrayList<String> ans = new ArrayList<String>();

				// print out each h y p e r n y m s id and synonyms
				List < IWord > words ;
				for( ISynsetID sid : xnyms ) {
					words = dict . getSynset ( sid ) . getWords () ;
					
					for( Iterator < IWord > i = words . iterator () ; i . hasNext () ;) {
						ans.add(i.next().getLemma());

					}

				}
				return ans;
	}
	
	// OpenNLP
	public Parse parseSentence(String sentence, Parser parser) throws FileNotFoundException {
		// System.out.println("Parsing...");
		Parse topParses[] = ParserTool.parseLine(sentence, parser, 1);
		if (verbose) {
			for (Parse p : topParses)
				p.show();
		}
		if (topParses.length == 0)
			return null;
		else 
			return topParses[0];
	}
	
	// without #C for easier SBAR reading
	public String extractChain(Parse p) {
		String result = "";
		if (p.getType().equals(LABEL_TOP)) {
			return extractChain(p.getChildren()[0]);
		}
		if (p.getType().equals(LABEL_SENTENCE)) {
			Parse[] phrases = p.getChildren();
			for (int i = 0; i < phrases.length; i++) {
				// phrase type specific methods
				if (phrases[i].getType().equals(LABEL_NOUN_PHRASE)) {
					result += branchFromNP(phrases[i]);
				} else if (phrases[i].getType().equals(LABEL_VERBAL_PHRASE)) {
					result += branchFromVP(phrases[i]);
				}
			}
		}
		if (verbose) {
			System.out.println("TEXT_METHODS: extracted Chain from user input:");
			System.out.println(result);
		}
		// reset switches
		existentialThere = false;
		return result;
	}
	
	public String branchFromNP(Parse np) {
		String branch = "";
		String noun = "";
		String adj = "";
		String rb = "";
		String adjp_branch = "";
		String whnp_branch = "";
		String pp_branch = "";
		String np_branch = "";
		String sbar_branch = "";
		String prp = "";
		if (np.getChildCount() == 1) {
			String childType = np.getChildren()[0].getType();
			Span span = np.getChildren()[0].getSpan();
			if (childType.equals("PRP")) {
				String text = np.getChildren()[0].getText().substring(span.getStart(),
						span.getEnd());
				if (text.equals("I")) {
					text = "#USER";
				} else if (text.toLowerCase().equals("you")) {
					text = "#SELF";
				}
				branch += text + " ";
			}
			if (childType.equals("EX")) {
				existentialThere = true;
			}
			if (childType.equals("NN") || childType.equals("NNS")
					|| childType.equals("VBG") || childType.equals("NNP")) {
				noun = np.getChildren()[0].getText().substring(span.getStart(), span.getEnd());
				branch = noun + " ";
			}
		} else {
			// NP has many components
			Parse[] np_children = np.getChildren();
			for (Parse child : np_children) {
				if (child.getType().equals("NN") || child.getType().equals("NNS")
						|| child.getType().equals("VBG") || child.getType().equals("NNP")) {
					Span span = child.getSpan();
					noun = child.getText().substring(span.getStart(),
							span.getEnd());
				}
				if (child.getType().equals("JJ") || child.getType().equals("JJS")) {
					Span span = child.getSpan();
					adj = child.getText().substring(span.getStart(),
							span.getEnd());
				}
				// other cases (phrases), PRP
				if (child.getType().equals("PRP")) {
					Span span = np.getChildren()[0].getSpan();
					String text = np.getChildren()[0].getText().substring(span.getStart(),
							span.getEnd());
					if (text.equals("I")) {
						text = "#USER";
					} else if (text.toLowerCase().equals("you")) {
						text = "#SELF";
					}
					branch += text + " ";
				}
				if (child.getType().equals("PRP$")) {
					Span span = np.getChildren()[0].getSpan();
					String text = np.getChildren()[0].getText().substring(span.getStart(),
							span.getEnd());
					if (text.equals("my") || text.equals("My")) {
						text = "#B #USER have";
						prp = "my";
					} else if (text.toLowerCase().equals("your")) {
						text = "#B #SELF have";
						prp = "your";
					}
					branch += text + " ";
				}
				if (child.getType().equals("RB") && existentialThere == true) {
					Span span = child.getSpan();
					rb = child.getText().substring(span.getStart(), span.getEnd());
				}
				if (child.getType().equals("NP")) {
					np_branch = branchFromNP(child);
				}
				if (child.getType().equals("PP")) {
					pp_branch = branchFromPP(child);
				}
				if (child.getType().equals("ADJP")) {
					adjp_branch = branchFromADJP(child);
				}
				// handle subordinate phrase
				if (child.getType().equals("SBAR")) {
					sbar_branch = branchFromSBAR(child);
					break;
				}
			}
			
			if (!noun.equals("") && adj.equals("") && prp.equals("")) {
				// just a noun
				branch = noun + " ";
			}
			if (!noun.equals("") && adj.equals("") && !prp.equals("")) {
				// noun with preposition ("my car", "your thoughts")
				branch += noun + " B# ";
			}
			if (!noun.equals("") && !adj.equals("") && !prp.equals("")) {
				// noun with preposition and adjective ("your bad code")
				branch += "#B " + noun + " is " + adj + " B# ";
			}
			if (!noun.equals("") && !adj.equals("") && prp.equals("")) {
				// nice adjective-noun pair
				branch = "#B " + noun + " is " + adj + " B# ";
			}
			if (!noun.equals("") && !adj.equals("") && !rb.equals("")) {
				// e.g. "nice restaurant nearby" => #B restaurant is nice B# is nearby
				branch = "#B " + noun + " is " + adj + " B# is " + rb;
			}
			// other cases...
			if (!np_branch.equals("") && pp_branch.equals("") && adjp_branch.equals("")) {
				branch += np_branch + sbar_branch;
			}
			if (!np_branch.equals("") && !pp_branch.equals("")) {
				branch = "#B " + np_branch + "is " + pp_branch + "B# " + sbar_branch;
			}
			if (!np_branch.equals("") && !adjp_branch.equals("")) {
				branch = "#B " + np_branch + "is " + adjp_branch + "B# "+ sbar_branch;
			}
			
		}
		return branch;
	}
	
	public String branchFromVP(Parse vp) {
		String branch = "";
		String verb = "";
		String verb_type = "";
		boolean negation = false;
		int encapsule = 0;	// if this gets over 1, make #B ... B# around the Branch
		// handle verbs (usually in the beginning)
		for (Parse child : vp.getChildren()) {
			if (child.getType().startsWith("VB")) {
				verb_type = child.getType();
				Span span = child.getSpan();
				if (!existentialThere) {
					verb = child.getText().substring(span.getStart(), span.getEnd());
				}
				if (verb.equals("don't") || verb.equals("didn't")) {
					negation = true;
					continue;
				}
				break;
			}
			if (child.getType().equals("RB")) {
				Span span = child.getSpan();
				String adverb = child.getText().substring(span.getStart(),
						span.getEnd());
				if (adverb.equals("n't")) {
					negation = true;
				}
			}
			
		}
		// check for Link keywords like "is", "was", "causes", "have been" etc.
		if (!verb.isEmpty()) {
			if (verb.equals("is") || verb.equals("are")) {
				branch = "is ";
				if (negation) {
					branch = "is,-0.5 ";
				}
			} else if (verb.equals("was") || verb.equals("were")) {
				branch = "was ";
			} else {
				// normal verbs, no Link keywords
				encapsule++;
				if (verb_type.equals("VBD")) {
					// past tense
					branch = "did #B " + verb + " ";
					if (negation) {
						branch = "did,-0.5 #B ";
					}
				} else if (verb_type.equals("VBG")) {
					// present continuous
					branch = "#B " + verb + " ";
				} else {
					// present
					branch = "do #B " + verb + " ";
					if (negation) {
						branch = "do,-0.5 #B ";
					}
				}
			}
		}
		// handle verbal phrase
		for (Parse child : vp.getChildren()) {
			if (child.getType().equals("VP")) {
				branch += branchFromVP(child);
				encapsule++;
				break;
			}
		}
		// handle prepositional phrase
		for (Parse child : vp.getChildren()) {
			if (child.getType().equals("PP")) {
				if (branch.endsWith("is ") || branch.endsWith("was ")) {
					branch += branchFromPP(child);
				} else {
					encapsule++;
					branch += "how " + branchFromPP(child);
				}
				break;
			}
		}
		// handle adjective phrase
		for (Parse child : vp.getChildren()) {
			if (child.getType().equals("ADJP")) {
				if (!(verb.equals("is") || verb.equals("are") || verb.equals("was") || verb.equals("were"))) {
					encapsule++;
					branch += "how " + branchFromADJP(child);
					break;
				} else {
					encapsule++;
					branch += branchFromADJP(child);
					break;
				}
			}
		}
		// handle noun phrase
		for (Parse child : vp.getChildren()) {
			if (child.getType().equals("NP")) {
				if (branch.endsWith("is ") || branch.endsWith("was ") || existentialThere) {
					branch += branchFromNP(child);
				} else {
					encapsule++;
					branch += "what " + branchFromNP(child);
				}
				break;
			}
		}
		// format chain if needed
		if (encapsule > 1) {
			branch = branch + "B# ";
		}
		// handle subordinate phrase
		for (Parse child : vp.getChildren()) {
			if (child.getType().equals("SBAR")) {
				encapsule++;
				// branch = branch + "B# ";
				branch += branchFromSBAR(child);
				break;
			}
		}
		return branch;
	}
	
	// subordinate verbal phrase (without subject)
	public String branchFromSubVP(Parse vp) {
		String branch = "";
		String verb = "";
		String verb_type = "";
		boolean negation = false;
		int encapsule = 0;	// if this gets over 1, make #B ... B# around the Branch
		// handle verbs (usually in the beginning)
		for (Parse child : vp.getChildren()) {
			if (child.getType().startsWith("VB")) {
				verb_type = child.getType();
				Span span = child.getSpan();
				verb = child.getText().substring(span.getStart(),
						span.getEnd());
				if (verb.equals("don't") || verb.equals("didn't")) {
					negation = true;
					continue;
				}
				break;
			}
			if (child.getType().equals("RB")) {
				Span span = child.getSpan();
				String adverb = child.getText().substring(span.getStart(),
						span.getEnd());
				if (adverb.equals("n't")) {
					negation = true;
				}
			}
			
		}
		// check for Link keywords like "is", "was", "causes", "have been" etc.
		if (!verb.isEmpty()) {
			if (verb.equals("is") || verb.equals("are")) {
				/*branch = "is ";	// no generality input, correct this in the manualInput()
				if (negation) {
					branch = "isn't ";
				}*/
			} else if (verb.equals("was") || verb.equals("were")) {
				/*branch = "was ";	*/
				// no generality input, correct this in the manualInput()
			} else {
				// normal verbs, no Link keywords
				encapsule++;
				if (verb_type.equals("VBD")) {
					// past tense
					branch = verb + " ";
					if (negation) {
						branch = "-" + verb + " ";
					}
				} else {
					// present
					branch = verb + " ";
					if (negation) {
						branch = "-" + verb + " ";
					}
				}
			}
		}
		// handle verbal phrase
		for (Parse child : vp.getChildren()) {
			if (child.getType().equals("VP")) {
				branch += branchFromVP(child);
				encapsule++;
				break;
			}
		}
		// handle prepositional phrase
		for (Parse child : vp.getChildren()) {
			if (child.getType().equals("PP")) {
				if (branch.endsWith("is ") || branch.endsWith("was ")) {
					branch += branchFromPP(child);
				} else {
					encapsule++;
					branch += "how " + branchFromPP(child);
				}
				break;
			}
		}
		// handle adjective phrase
		for (Parse child : vp.getChildren()) {
			if (child.getType().equals("ADJP")) {
				encapsule++;
				branch += "how " + branchFromADJP(child);
				break;
			}
		}
		// handle noun phrase
		for (Parse child : vp.getChildren()) {
			if (child.getType().equals("NP")) {
				if (branch.endsWith("is ") || branch.endsWith("was ")) {
					branch += branchFromNP(child);
				} else {
					encapsule++;
					branch += "what " + branchFromNP(child);
				}
				break;
			}
		}
		// format chain if needed
		if (encapsule > 1) {
			branch = "#B " + branch + "B# ";
		}
		// handle subordinate phrase
		for (Parse child : vp.getChildren()) {
			if (child.getType().equals("SBAR")) {
				encapsule++;
				// branch = branch + "B# ";
				branch += branchFromSBAR(child);
				break;
			}
		}
		return branch;
	}
	
	public String branchFromPP(Parse pp) {
		String branch = "#B ";
		// first read the NP child
		if (pp.getChildCount() == 2) {
			if (pp.getChildren()[0].getType().equals("IN")) {
				// handle things before "prep" Link
				if (pp.getChildren()[1].getType().equals("NP")) {
					branch += branchFromNP(pp.getChildren()[1]);
				}
				// make the "prep" Link
				Span span = pp.getChildren()[0].getSpan();
				String prep = pp.getChildren()[0].getText().substring(span.getStart(),
						span.getEnd());
				branch += "prep " + prep + " B# ";
			}
		}
		return branch;
	}
	
	public String branchFromADJP(Parse adjp) {
		String branch = "";
		if (adjp.getChildCount() == 1) {
			if (adjp.getChildren()[0].getType().equals("JJ")) {
				Span span = adjp.getChildren()[0].getSpan();
				String adj = adjp.getChildren()[0].getText().substring(span.getStart(),
						span.getEnd());
				return adj + " ";
			}
		} else if (adjp.getChildCount() == 2) {
			String firstTag = adjp.getChildren()[0].getType();
			Span firstSpan = adjp.getChildren()[0].getSpan();
			String firstWord = adjp.getChildren()[0].getText().substring(firstSpan.getStart(),firstSpan.getEnd());
			String secondTag = adjp.getChildren()[1].getType();
			Span secondSpan = adjp.getChildren()[1].getSpan();
			String secondWord = adjp.getChildren()[1].getText().substring(secondSpan.getStart(),secondSpan.getEnd());
			if (firstTag.equals("JJR") && secondTag.equals("JJ")) {
				return "#B " + secondWord + " is " + firstWord + " B# ";
			} else if (firstTag.equals("RBR") && secondTag.equals("JJ")) {
				return "#B " + secondWord + " is " + firstWord + " B# ";
			}
		}
		return branch;
	}
	
	public String branchFromADVP(Parse advp) {
		// TODO adverbial phrase
		return "";
	}
	
	public String branchFromSBAR(Parse sbar) {
		String branch = "";
		// first child: WHNP or IN - translate into Link
		if (sbar.getChildCount() > 1) {
			if (sbar.getChildren()[0].getType().equals("WHNP")) {
				//if VP is the only child of SBAR-S
				if (sbar.getChildren()[1].getType().equals("S")
						&& sbar.getChildren()[1].getChildCount() == 1) {
					if (sbar.getChildren()[1].getChildren()[0].getType().equals("VP")) {
						branch += "which " + branchFromSubVP(sbar.getChildren()[1].getChildren()[0]);
					}
				} else {
					branch += "which #B " + extractChain(sbar.getChildren()[1]) + "B# ";
				}
			}
			if (sbar.getChildren()[0].getType().equals("IN")) {
				// subordinate conjunctions (because, since, before, if) - just use them!
				Span span = sbar.getChildren()[0].getSpan();
				String conj = sbar.getChildren()[0].getText().substring(span.getStart(),
						span.getEnd());
				branch += conj + " #B " + extractChain(sbar.getChildren()[1]) + "B# ";
			}
		}		
		return branch;
	}
	
	public TripletRelation extractRelationFromParse(Parse p){
		
		TripletRelation rel = new TripletRelation();
		if (p != null){
			rel = new TripletRelation(getSubject(p),
					getPredicate(p),
					getObject(p) );
			if (verbose) {
				System.out.println("TEXT_METHODS: extracted relation triple:\n" + rel.toString());
			}
		}
		else {
			System.out.println("TEXT_METHODS-extractRelationFromSentence: no valid parse from parseSentence");
		}
		
		
		return rel;
	}
	
	// TODO add possibility of multiple Ss and PP
	public static String getSubject(final Parse parse) {
		if (parse.getType().equals(LABEL_TOP)) {
			return getSubject(parse.getChildren()[0]);
		}

		if (parse.getType().equals(LABEL_SENTENCE)) {
			for (Parse child : parse.getChildren()) {
				if (child.getType().equals(LABEL_NOUN_PHRASE)) {
					return getSubject(child);
				}
			}
		}
		if (parse.getType().equals(LABEL_NOUN_PHRASE)) {
			return getFirstOccurenceForType(parse, LABEL_NAME_PREFIX);
		}

		return "";
	}

	public static String getPredicate(final Parse parse) {
		if (parse.getType().equals(LABEL_TOP)) {
			return getPredicate(parse.getChildren()[0]);
		}

		if (parse.getType().equals(LABEL_SENTENCE)) {
			for (Parse child : parse.getChildren()) {
				if (child.getType().equals(LABEL_VERBAL_PHRASE)) {
					return getPredicate(child);
				}
			}
			return "";
		}
		if (parse.getType().equals(LABEL_VERBAL_PHRASE)) {
			return getFirstOccurenceForType(parse, LABEL_VERB_PREFIX);
		}

		return "";
	}

	public static String getObject(final Parse parse) {
		String object = "";
		if (parse.getType().equals(LABEL_TOP)) {
			return getObject(parse.getChildren()[0]);
		}

		if (parse.getType().equals(LABEL_SENTENCE)) {
			for (Parse child : parse.getChildren()) {
				if (child.getType().equals(LABEL_VERBAL_PHRASE)) {
					object = getObject(child); 
					if (!object.isEmpty()){
						return object;
					}
				}
			}
			return object;
		}
		if (parse.getType().equals(LABEL_VERBAL_PHRASE)) {
			return getFirstOccurenceForType(parse, LABEL_NAME_PREFIX);
		}

		return object;
	}
	
	public static String getConstituent(final Parse parse, final String syntactic_cat,
			String lexical_cat) {
		String object = "";
		if (parse.getType().equals(LABEL_TOP)) {
			return getConstituent(parse.getChildren()[0], syntactic_cat, lexical_cat);
		}

		if (parse.getType().equals(LABEL_SENTENCE)) {
			for (Parse child : parse.getChildren()) {
				if (child.getType().equals(syntactic_cat)) {
					object = getConstituent(child, syntactic_cat, lexical_cat); 
					if (!object.isEmpty()){
						return object;
					}
				}
			}
			return object;
		}
		if (parse.getType().equals(syntactic_cat)) {
			return getFirstOccurenceForType(parse, lexical_cat);
		}

		return object;
	}

	// public static String getObject(Parse parse){}

	private static String getFirstOccurenceForType(final Parse parse,
			final String typePrefix) {
		
		//TODO ADD PRP 
		// For now we are only checking the prefix

		// check current
		if (parse.getType().length() > 1
				&& parse.getType().startsWith(typePrefix)) {
			Span span = parse.getSpan();
			String text = parse.getText().substring(span.getStart(),
					span.getEnd());
			return text;
		}

		// check children (breadth)
		for (Parse child : parse.getChildren()) {
			if (child.getType().length() > 1
					&& child.getType().startsWith(typePrefix)) {
				Span span = child.getSpan();
				String text = child.getText().substring(span.getStart(),
						span.getEnd());
				if (!text.isEmpty())
					return text;
			}
		}

		// recursively check for children (deep)
		for (Parse child : parse.getChildren()) {
			String text = getFirstOccurenceForType(child, typePrefix);
			if (!text.isEmpty())
				return text;
		}
		
		return "";
	}
	
	public String getDefinition (String token, IDictionary dict, POS pos) throws IOException{
		
		if (verbose)
			System.out.println("TEXT_METHODS: Getting definition of [" + token + "]");

		try {
			// look up first sense of the word
			IIndexWord idxWord = dict.getIndexWord(token, pos);
			IWordID wordID = idxWord.getWordIDs().get(0);
			IWord word = dict.getWord(wordID);
			ISynset synset = word.getSynset();
			if (verbose) {
				System.out.println("Id = " + wordID);
				System.out.println(" Lemma = " + word.getLemma());
				System.out.println(" Gloss = " + word.getSynset().getGloss());

				// get the (hypernyms)
				List<ISynsetID> hypernyms = synset.getRelatedSynsets(Pointer.HYPERNYM);
				// print out each h y p e r n y m s id and synonyms
				List<IWord> words;
				for (ISynsetID sid : hypernyms) {
					words = dict.getSynset(sid).getWords();

					System.out.print(sid + " {");
					for (Iterator<IWord> i = words.iterator(); i.hasNext();) {

						System.out.print(i.next().getLemma());
						if (i.hasNext()) {

							System.out.print(", ");

						}
					}

					System.out.println("}");
				}
			}
			String glossWithExample = word.getSynset().getGloss();
			// Don't need the example
			return glossWithExample.split(";")[0];
		} catch (NullPointerException e) {
			return "";
		}
		
		
	}
	
	public String getParent (ISynset synset, IDictionary dict) {
		try {
			List<ISynsetID> hypernyms = synset.getRelatedSynsets(Pointer.HYPERNYM);
			ISynsetID first_hypernym = hypernyms.get(0);
			ISynset first_hyper_synset = dict.getSynset(first_hypernym);
			String parent = first_hyper_synset.getWords().get(0).getLemma();
			return parent;
		} catch (IndexOutOfBoundsException e) {
			// TODO: handle exception
			if (verbose)
				System.out.println("TEXT_METHODS: WordNet doesn't contain any hypernyms for this word");
			return "";
		}
	}
	
	public List<String> getComponentsOf (ISynset synset, IDictionary dict) {
		List<String> ans = new ArrayList<String>();
		try {
			List<ISynsetID> meronyms = synset.getRelatedSynsets(Pointer.MERONYM_PART);
			for (ISynsetID isid : meronyms) {
				ISynset mero = dict.getSynset(isid);
				String component = mero.getWords().get(0).getLemma();
				ans.add(component);
			}
			return ans;
		} catch (IndexOutOfBoundsException e) {
			// TODO: handle exception
			if (verbose)
				System.out.println("TEXT_METHODS: WordNet doesn't contain any meronyms for this word");
			return ans;
		}
	}
	
	public ISynset getWordSynset (String token, IDictionary dict, POS pos) {
		// look up first sense of the word
		IIndexWord idxWord = dict . getIndexWord (token, pos ) ;
		IWordID wordID = idxWord . getWordIDs () . get (0) ;
		IWord word = dict . getWord ( wordID ) ;
		ISynset synset = word.getSynset();
		return synset;

	}
	
	public ITesseract initTesseract(){
		ITesseract instance = new Tesseract();
		instance.setDatapath("./external/tess4j/Tess4J");
		return instance;
	}
	
	public String tesseractOCR(String filename, ITesseract instance){
		File imageFile = new File(filename);
		try{
			String result = instance.doOCR(imageFile);
			return result;
		} catch (TesseractException e) {
			System.err.println(e.getMessage());
			return null;
		}
	}
	
	public String[] sentenceDetect(String paragraph, SentenceDetectorME sdetector) throws InvalidFormatException, IOException { 
        // detect sentences in the paragraph
        String sentences[] = sdetector.sentDetect(paragraph);
        return sentences;
    }
	
	public String[] tokenizeSentence(String sentence){
		InputStream modelIn = null;
		String[] tokens;
		 
        try {
            modelIn = new FileInputStream("external/en-token.bin");
            TokenizerModel model = new TokenizerModel(modelIn);
            TokenizerME tokenizer = new TokenizerME(model);
            tokens = tokenizer.tokenize(sentence);
            double tokenProbs[] = tokenizer.getTokenProbabilities();
            
            // console
            /*System.out.println("Token\t: Probability\n-------------------------------");
            for(int i=0;i<tokens.length;i++){
                System.out.println(tokens[i]+"\t: "+tokenProbs[i]);
            }*/
            
            
            return tokens;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        finally {
            if (modelIn != null) {
                try {
                    modelIn.close();
                }
                catch (IOException e) {
                }
            }
        }

	}

	public String[] POSTags(String sentence){
		String[] tags;
		InputStream tokenModelIn = null;
        InputStream posModelIn = null;
        
        try {
            
            // tokenize the sentence
            tokenModelIn = new FileInputStream("external/en-token.bin");
            TokenizerModel tokenModel = new TokenizerModel(tokenModelIn);
            Tokenizer tokenizer = new TokenizerME(tokenModel);
            String tokens[] = tokenizer.tokenize(sentence);
 
            // Parts-Of-Speech Tagging
            // reading parts-of-speech model to a stream 
            posModelIn = new FileInputStream("external/en-pos-maxent.bin");
            // loading the parts-of-speech model from stream
            POSModel posModel = new POSModel(posModelIn);
            // initializing the parts-of-speech tagger with model 
            POSTaggerME posTagger = new POSTaggerME(posModel);
            // Tagger tagging the tokens
            tags = posTagger.tag(tokens);
            // Getting the probabilities of the tags given to the tokens
            double probs[] = posTagger.probs();
            
            if (verbose) {
				System.out.println("Token\t:\tTag\t:\tProbability\n---------------------------------------------");
				for (int i = 0; i < tokens.length; i++) {
					System.out.println(tokens[i] + "\t:\t" + tags[i] + "\t:\t" + probs[i]);
				} 
			}
			return tags;
            
        }
        catch (IOException e) {
            // Model loading failed, handle the error
            e.printStackTrace();
            return null;
        }
        finally {
            if (tokenModelIn != null) {
                try {
                    tokenModelIn.close();
                }
                catch (IOException e) {
                }
            }
            if (posModelIn != null) {
                try {
                    posModelIn.close();
                }
                catch (IOException e) {
                }
            }
        }
		
	}

	public String[] sentToChunks(String[] tokens){
		
		String[] chunks;
		
		try{
           
            // Parts-Of-Speech Tagging
            // reading parts-of-speech model to a stream
            InputStream posModelIn = new FileInputStream("en-pos-maxent.bin");
            // loading the parts-of-speech model from stream
            POSModel posModel = new POSModel(posModelIn);
            // initializing the parts-of-speech tagger with model
            POSTaggerME posTagger = new POSTaggerME(posModel);
            // Tagger tagging the tokens
            String tags[] = posTagger.tag(tokens);
 
            // reading the chunker model
            InputStream ins = new FileInputStream("en-chunker.bin");
            // loading the chunker model
            ChunkerModel chunkerModel = new ChunkerModel(ins);
            // initializing chunker(maximum entropy) with chunker model
            ChunkerME chunker = new ChunkerME(chunkerModel);
            // chunking the given sentence : chunking requires sentence to be tokenized and pos tagged
            chunks = chunker.chunk(tokens,tags);
 
            // printing the results
            System.out.println("Printing chunks for the given sentence...");
            System.out.println("\nTOKEN - POS_TAG - CHUNK_ID\n-------------------------");
            for(int i=0;i< chunks.length;i++){
                System.out.println(tokens[i]+" - "+tags[i]+" - "+chunks[i]);
            }
            
            return chunks;
            
        } catch (FileNotFoundException e){
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
		
	}
	
	// CoreNLP - don't use it on small computers
	public void testDependencies(String text, Properties props, StanfordCoreNLP pipeline) {
		// Annotate an example document.

				Annotation doc = new Annotation(text);
				pipeline.annotate(doc);

				// Loop over sentences in the document
				int sentNo = 0;
				for (CoreMap sentence : doc.get(CoreAnnotations.SentencesAnnotation.class)) {
					System.out.println("Sentence #" + ++sentNo + ": " + sentence.get(CoreAnnotations.TextAnnotation.class));

					// Print SemanticGraph
					System.out.println(sentence.get(SemanticGraphCoreAnnotations.EnhancedDependenciesAnnotation.class).toString(SemanticGraph.OutputFormat.LIST));
					
					// Get the OpenIE triples for the sentence
					Collection<RelationTriple> triples = sentence.get(NaturalLogicAnnotations.RelationTriplesAnnotation.class);

					// Print the triples
					for (RelationTriple triple : triples) {
						System.out.println(triple.confidence + "\t" +
								triple.subjectLemmaGloss() + "\t" +
								triple.relationLemmaGloss() + "\t" +
								triple.objectLemmaGloss());
					}
					
					/*

					// Alternately, to only run e.g., the clause splitter:
					List<SentenceFragment> clauses = new OpenIE(props).clausesInSentence(sentence);
					for (SentenceFragment clause : clauses) {
						System.out.println(clause.parseTree.toString(SemanticGraph.OutputFormat.LIST));
					}
					System.out.println();*/
				}
		
	}
	
	public void write (String str) {
		if (verbose)
			System.out.println(str);
	}
	
}
