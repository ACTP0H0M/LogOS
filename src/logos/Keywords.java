package logos;

/*
 * Doesn't get modified.
 * This class only contains lists of specified keywords that help trigger
 * some reaction. Precise naming of the lists is necessary to assure correct
 * functionality (especially of the Person class).
 * This basically duplicates all lists introduced in Utils and contains much
 * more other lists.
 * 
 * Format: use the whole width of the editor page before beginning the new line!
 */
public class Keywords {
	
	public String[] confirmationKeywords = {"yes","sure","correct","of course","true","agree","absolutely","exactly","yeah","neither"};
	public String[] denialKeywords = {"no","nope","false","wrong","not","n't","impossible","bullshit"};
	public String[] uncertaintyKeywords = {"maybe","could","possibly","possible","perhaps","perchance"};
	public String[] empathicConfirmationResponses = {"I see...","I understand...","Thank you for opening up, master!"};
	public String[] uninformativeKeywords = {"thanks","thank","alright","fine","ok","okay"};
	public String[] swearWords = {"fuck","motherfucker","asshole","ass","cyka","blyat","bitch"};
	public String[] swearResponses = {"That's rude!","Are you alright?","What?!","Please don't swear."};
	public String[] spatialPrepositions = {"in", "on", "at", "under", "above", "near", "behind", "between", "in front of", "opposite to", "out"};
	public String[] uninformativeVerbs = {"is", "are", "was", "were", "have", "had"};
	public String[] requestKeywords = {"please", "Please"};
	public String[] robotKeywords = {"robot","AI","program","algorithm","Terminator"};
	public String[] happinessKeywords = {"happy","great","wonderful","perfect","amazing","cool","exciting","gorgeous","super"};
	public String[] sadnessKeywords = {"depressed","sad","meaningless","nightmare","died","dead","ill","tragedy","catastrophe","tired"};
	public String[] happyResponses = {"Wow, that's good news!","Nice!","That's cool!","I'm glad to hear that!"};
	public String[] sorryResponses = {"I'm very sorry to hear that.","It's painful to hear this news.","It makes me sad."};

}
