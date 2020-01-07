package bot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Keywords {
	
	public List<String> endOfPhraseMarks = Arrays.asList(".", "!", "?", "...", ";");
	public List<String> punctuation = Arrays.asList(".", "!", "?", "...", ";", ",", ":", "-");
	public String[] confirmationKeywords = {"yes",
			"sure",
			"correct",
			"of course",
			"true",
			"agree",
			"absolutely",
			"exactly",
			"yeah",
			"neither"};
	public String[] denialKeywords = {"no",
			"nope",
			"false",
			"wrong",
			"not",
			"n't",
			"impossible",
			"bullshit"};
	public String[] uncertaintyKeywords = {"maybe",
			"could",
			"possibly",
			"possible",
			"perhaps",
			"perchance"};
	public String[] empathicConfirmationResponses = {"I see...",
			"I understand...",
			"Thank you for opening up, master!"};
	public String[] uninformativeKeywords = {"thanks",
			"thank",
			"alright",
			"fine",
			"ok",
			"okay"};
	public String[] swearWords = {"fuck",
			"motherfucker",
			"asshole",
			"ass",
			"cyka",
			"blyat",
			"bitch"};
	public String[] spatialPrepositions = {"in", "on", "at", "under", "above", "near", "behind", "between", "in front of", "opposite to", "out"};
	public String[] vagueLocationWords = {"nearby", "close", "far", "above", "below", "left", "right", "here", "there"};
	public String[] uninformativeVerbs = {"is", "are", "was", "were", "have", "had"};
	public String[] requestKeywords = {"please", "Please"};
	public String[] positiveEmotions = {"wonderful",
			"happy",
			"great",
			"fine",
			"gorgeous",
			"extatic",
			"super",
			"excited",
			"well",
			"good",
			"alright"};
	public String[] negativeEmotions = {"depressed",
			"sad",
			"tired",
			"down",
			"bad",
			"exhausted",
			"annoyed",
			"terrible"};
	public String[] colours = {"red", "orange", "yellow", "green", "blue", "violet", "black", "white", "grey", "purple", "pink", "amber", "tortoise", "golden", "brown"};
	public String[] forms = {"round", "square", "triangle", "rectangular", "star-shaped", "plus-shaped", "elliptic", "spherical", "cubic"};
	
	// Antonym pairs (positive or greater value first)
	public String[] temperaturePair = {"hot", "cold"};
	public String[] sizePair = {"big", "small"};
	public String[] roughnessPair = {"rough", "smooth"};
	public String[] weightPair = {"heavy", "light"};
	public String[] easePair = {"easy", "hard"};
	public String[] hardnessPair = {"hard", "soft"};
	public String[] pricePair = {"expensive", "cheap"};
	public String[] thicknessPair = {"thick", "thin"};
	public String[] darknessPair = {"dark", "bright"};
	public String[] amountPair = {"many", "few"};
	public String[] distancePair = {"far", "close"};
	public String[] heightPair = {"tall", "low"};
	public String[] altitudePair = {"high", "low"};
	public String[] widthPair = {"wide", "narrow"};
	public String[] depthPair = {"deep", "shallow"};
	public String[] intelligencePair = {"clever", "stupid"};
	public String[] funPair = {"interesting", "boring"};
	
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
	public String[] goodbyePhrases = {"Goodbye, master! See you soon.",
			"Bye! Have a good time!",
			"Ciao!",
			"Goodbye!"};
	public String[] noProblemPhrases = {"No problem, master!",
			"It's alright.",
			"It's fine, I can understand.",
			"Oh, it's not a big deal!"};
	public String[] youreWelcomePhrases = {"You are welcome!", "It's my pleasure to be helping you!", "Logos at your service!"};
	public String[] musicGenres = {"classical", "rock", "jazz", "blues", "techno", "rap", "metal", "soul", "pop", "indie", "alternative", "punk", "funk", "country", "reggae"};
	public String[] planets = {"Mercury", "Venus", "Earth", "Mars", "Jupiter", "Saturn", "Uranus", "Neptune"};
	
	
	/*
	 * Conceptual:
	 * What if every word hat its own algorithm that describes interactions with
	 * other words? Something like a normal dictionary, but algorithmic?
	 */
	
	public boolean stringArrayContains(String[] array, String word) {
		for (int i = 0; i < array.length; i++) {
			if (array[i].equals(word)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isEndOfPhrase(String token) {
		if (endOfPhraseMarks.contains(token))
			return true;
		else
			return false;
	}
	
	public boolean isPunctuation(String token) {
		if (punctuation.contains(token))
			return true;
		else
			return false;
	}
	
	public List<String> commonStrings(String[] array1, String[] array2) {
		List<String> ans = new ArrayList<String>();
		for (int i = 0; i < array1.length; i++) {
			if (stringArrayContains(array2, array1[i])) {
				ans.add(array1[i]);
			}
		}
		return ans;
	}
	
	public List<String> commonStrings(ArrayList<String> array1, ArrayList<String> array2) {
		List<String> ans = new ArrayList<String>();
		for (String s1 : array1) {
			if (array2.contains(s1)) {
				ans.add(s1);
			}
		}
		return ans;
	}
	
	public float jaccardIndex(String[] array1, String[] array2) {
		int common = commonStrings(array1, array2).size();
		return ((float) common) / (array1.length + array2.length - common);
	}
	
	public float jaccardIndex(ArrayList<String> array1, ArrayList<String> array2) {
		int common = commonStrings(array1, array2).size();
		return ((float) common) / (array1.size() + array2.size() - common);
	}
	
	public float nGramIndex(String[] array1, String[] array2) {
		List<String[]> pairs1 = new ArrayList<String[]>();
		List<String[]> pairs2 = new ArrayList<String[]>();
		for (int i = 0; i < array1.length - 1; i++) {
			String[] pair = new String[2];
			pair[0] = array1[i];
			pair[1] = array1[i + 1];
			pairs1.add(pair);
		}
		for (int i = 0; i < array2.length - 1; i++) {
			String[] pair = new String[2];
			pair[0] = array2[i];
			pair[1] = array2[i + 1];
			pairs2.add(pair);
		}
		int common = 0;
		// not the most effective way, but the code is simpler
		for (int i = 0; i < pairs1.size(); i++) {
			for (int j = 0; j < pairs2.size(); j++) {
				if (pairs1.get(i).equals(pairs2.get(j))) {
					common++;
				}
			}
		}
		common = common / 2;
		return ((float) common) / (pairs1.size() + pairs2.size() - common);
	}
	
	public String randomStringFromArray (String[] array) {
		int idx = (int) (Math.random() * array.length);
		return array[idx];
	}

}
