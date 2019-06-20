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

}
