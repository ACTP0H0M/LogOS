package logos;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class DatabaseInterface {
	
	boolean verbose;
	
	ArrayList<Logos> logosList;
	ArrayList<Link> linkList;
	ArrayList<Branch> branchList;
	
	double forgetRate = 0.00;
	double rememberRate = 0.1;
	
	double mergeThresh = 0.6;	// two Links can be merged, if g1*g2 > mergeThresh
	double mergeLogosActualitySpan = 0.2;	// two Logos can be merged, if their maxLinkA are less different
	
	ArrayList<String> inheritedLinks = new ArrayList<String>();	// links that can be inherited
	
	public DatabaseInterface(int verbosity) {
		
		logosList = new ArrayList<Logos>();
		linkList = new ArrayList<Link>();
		branchList = new ArrayList<Branch>();
		
		inheritedLinks.add("can_do");
		inheritedLinks.add("can_be");
		inheritedLinks.add("is_a");
		inheritedLinks.add("is_component_of");
		
		if (verbosity >= 3) {
			verbose = true;
		}
	}
	
	// read and write database
	
	public ArrayList<Logos> readLogos() {
		ArrayList<Logos> loglist = new ArrayList<Logos>();
		Charset charset = Charset.forName("UTF-8");
		Path source = Paths.get("logos.txt");
		try (BufferedReader wlreader = Files.newBufferedReader(source, charset)){
			String line = null;
			
			while((line = wlreader.readLine()) != null) {	//what should be done with every line
				String[] blocks = line.split("/");
				
				String name = blocks[0];
				long id = Long.parseLong(blocks[1]);
				
				Logos l = new Logos(name, id, new ArrayList<Link>(), new ArrayList<Link>());
				
				loglist.add(l);
				
				if (verbose)
					System.out.println("DATABASE: added Logos " + l.id + " [" + l.name + "]");
			}
		} catch (IOException e) {
			System.err.println(e);
		}	
		return loglist;
	}
	
	public ArrayList<Link> readLinks(ArrayList<Logos> logoslist, Utils utils) {
		ArrayList<Link> linklist = new ArrayList<Link>();
		Charset charset = Charset.forName("UTF-8");
		Path source = Paths.get("links.txt");
		try (BufferedReader wlreader = Files.newBufferedReader(source, charset)){
			String line = null;
			
			while((line = wlreader.readLine()) != null) {	//what should be done with every line
				String[] blocks = line.split("/");
				
				long sourceid = Long.parseLong(blocks[0]);
				long targetid = Long.parseLong(blocks[1]);
				String relname = blocks[2];
				double freq = Double.parseDouble(blocks[3]);
				double conf = Double.parseDouble(blocks[4]);
				long id = Long.parseLong(blocks[5]);
				
				// find Logos by id
				Logos sourceLogos = utils.findLogosByID(logoslist, sourceid);
				Logos targetLogos = utils.findLogosByID(logoslist, targetid);
				
				Link lk = new Link(sourceLogos, targetLogos, relname, freq, conf, id);
				
				sourceLogos.outwardLinks.add(lk);
				targetLogos.inwardLinks.add(lk);
				
				linklist.add(lk);
				
				if (verbose)
					System.out.println("DATABASE: added Link " + lk.id + " [" + lk.relationName + "]");
				
			}
		} catch (IOException e) {
			System.err.println(e);
		}	
		return linklist;
	}
	
	// Read Branches
	public ArrayList<Branch> readBranches(ArrayList<Logos> loglist, ArrayList<Link> linklist, Utils utils) {
		ArrayList<Branch> brlist = new ArrayList<Branch>();
		Charset charset = Charset.forName("UTF-8");
		Path source = Paths.get("branches.txt");
		try (BufferedReader wlreader = Files.newBufferedReader(source, charset)){
			String line = null;
			
			while((line = wlreader.readLine()) != null) {	//what should be done with every line
				String[] blocks = line.split("/");
				
				// First token like #BRANCH16 --> id = 16
				String name = blocks[0];
				long id = Long.parseLong(name.replaceAll("#BRANCH", ""));
				
				Branch b = new Branch(name, id, new ArrayList<Link>(), new ArrayList<Link>());
				
				// logos1 / link1 / ..... / last logos
				ArrayList<Logos> insideLogos = new ArrayList<Logos>();
				ArrayList<Link> insideLinks = new ArrayList<Link>();
				
				for (int i = 1; i < blocks.length; i++) {
					if (i % 2 == 1) {
						insideLogos.add(utils.findLogosByID(loglist, Long.parseLong(blocks[i])));
					} else {
						insideLinks.add(utils.findLinkByID(linklist, Long.parseLong(blocks[i])));
					}
				}
				
				// in and out links?
				Logos foundLogos = utils.findLogosByName(loglist, name);
				
				for (Link l : foundLogos.inwardLinks) {
					b.inwardLinks.add(l);
				}
				
				for (Link l : foundLogos.outwardLinks) {
					b.outwardLinks.add(l);
				}
				
				b.setContainedLogosList(insideLogos);
				b.setContainedLinkList(insideLinks);
				
				brlist.add(b);
				
				if (verbose)
					System.out.println("DATABASE: added Branch " + b.id + " [" + b.name + "]");
			}
		} catch (IOException e) {
			System.err.println(e);
		}	
		return brlist;
	}
	
	public void writeDatabase(ArrayList<Logos> loglist, ArrayList<Link> linklist, ArrayList<Branch> brlist) {
		
		// Write Logos
		try {
			new PrintWriter("logos.txt").close();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("logos.txt", true)))) {
			String line = "";
			for (Logos lg : loglist) {
				line = lg.name + "/" + lg.id + "/";
				out.println(line);
			}
		} catch (IOException e) {
			System.out.println("DATABASE: Couldn't rewrite logos.txt");
		}
		
		// Write Links
		try {
			new PrintWriter("links.txt").close();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("links.txt", true)))) {
			String line = "";
			for (Link lk : linklist) {
				line = lk.source.id + "/" + lk.target.id + "/" + lk.relationName + "/" + lk.generality + "/"
						+ lk.actuality + "/" + lk.id + "/";
				out.println(line);
			}
		} catch (IOException e) {
			System.out.println("DATABASE: Couldn't rewrite linkss.txt");
		}
		
		// Write Branches
		try {
			new PrintWriter("branches.txt").close();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("branches.txt", true)))) {
			
			for (Branch br : brlist) {
				String line = "";
				line += br.name + "/";
				for (Logos l_in : br.containedLogosList) {
					// for the last Logos, outward Link id = -1
					if (!br.containedLogosList.get(br.containedLogosList.size() - 1).equals(l_in)) {
						line += l_in.id + "/" + l_in.outwardLinks.get(0).id + "/";
					} else {
						line += l_in.id + "/";
					}
				}
				out.println(line);
			}
		} catch (IOException e) {
			System.out.println("DATABASE: Couldn't rewrite branches.txt");
		}
		
	}
	
	/*
	 * This method should contain all relevant rules for knowledge graph
	 * compression, expansion and merging. This is where things get forgotten
	 * to save some space. Actuality and generality throughout the graph
	 * are updated.
	 */
	public void updateDatabase(Utils utils, double belief) {
		
		// Decrease link actualities
		for (Link link : linkList) {
			link.actuality = utils.updateActuality(link.actuality, true, forgetRate, rememberRate);
		}
		
		// Apply logic rules to the database hypergraph
		applyInheritance();
		mergeRedundantChains(utils, belief);
		mergeSameLogos(utils);
		// ACHTUNG!!! Don't generalize the rules too much. They are definitely Link-specific.
		
		
		// After applying logic rules, delete Links with zero evidence and a = 0
		
		// Just to be sure, delete all Links with empty targets and sources
		
		// See if some chains with no Links are left. Generalize them with can_do, can_be etc.
		
	}
	
	/*
	 * If their actualities are not very different
	 */
	public void mergeSameLogos(Utils utils) {
		
		int logListSize = logosList.size();
		
		for (int i = 0; i < logListSize; i++) {
			for (int j = 0; j < logListSize; j++) {
				if (i < j && i < logosList.size() && j < logosList.size()) {
					Logos l1 = logosList.get(i);
					Logos l2 = logosList.get(j);
					if (!l1.name.equals("input") && l1.name.equals(l2.name) && Math.abs(utils.maxLinkActuality(l1) - 
							utils.maxLinkActuality(l2)) < mergeLogosActualitySpan) {
						write("DATABASE: merging Logos " + l1.id + " [" + l1.name + "] with Logos " + l2.id);
						for (Link inlink : (ArrayList<Link>) l2.inwardLinks.clone()) {
							inlink.target = l1;
							l1.inwardLinks.add(inlink);
						}
						for (Link outlink : (ArrayList<Link>) l2.outwardLinks.clone()) {
							outlink.source = l1;
							l1.outwardLinks.add(outlink);
						}
						logosList.remove(l2);
						write("          Removed Logos " + l2.id + " [" + l2.name + "]");
					}
				}
			}
		}

		
	}
	
	/*
	 * If you find two chains with same names (g and a don't matter)
	 * merge those chains together.
	 * Includes the case of same targets and reltions for a given logos...
	 * Maybe some other special cases come here.
	 * It just has to make sense if called "merge redundancies" ;-)
	 */
	public void mergeRedundantChains(Utils utils, double belief) {
		
		int chains_merged = 0;
		
		if (verbose) {
			System.out.println("DATABASE: merging redundancies...");
		}
		
		// can easily merge, if both links have high generality (absolute value)
		for (Link one : (ArrayList<Link>) linkList.clone()) {
			
			for (Link two : (ArrayList<Link>) linkList.clone()) {
				
				if (one.id < two.id && one.generality * two.generality > mergeThresh) {
					
					if (one.source.name.equals(two.source.name) && one.target.name.equals(two.target.name)) {
						
						if (verbose) {
							System.out.println("          Found Links with same relation names, sources and targets");
							System.out.println("          " + one.source.name + " [" + one.relationName + "] " + one.target.name);
							System.out.println("          g = " + one.generality);
							System.out.println("          a = " + one.actuality);
						}
						
						int a1 = one.source.inwardLinks.size() + one.source.outwardLinks.size();
						int a2 = two.source.inwardLinks.size() + two.source.outwardLinks.size();
						
						if (a1 > a2) {
							
							for (Link lk : (ArrayList<Link>) two.source.inwardLinks.clone()) {
								lk.target = one.source;
								one.source.inwardLinks.add(lk);
							}
							for (Link lk : (ArrayList<Link>) two.source.outwardLinks.clone()) {
								lk.source = one.source;
								one.source.outwardLinks.add(lk);
							}
							
							
							
						} else if (a1 == a2) {
							// check if it's actually the same Logos
							if (one.source.id == two.source.id) {
								
								if (verbose) {
									System.out.println("          Logos A1 and A2 are the same");
								}
								
								// Do the same Logos merging for B
								
								int b1 = one.target.inwardLinks.size() + one.target.outwardLinks.size();
								int b2 = two.target.inwardLinks.size() + two.target.outwardLinks.size();
								
								if (b1 > b2) {
									
									for (Link lk : (ArrayList<Link>) two.target.inwardLinks.clone()) {
										lk.target = one.target;
										one.target.inwardLinks.add(lk);
									}
									for (Link lk : (ArrayList<Link>) two.target.outwardLinks.clone()) {
										lk.source = one.target;
										one.target.outwardLinks.add(lk);
									}
									
									
									
								} else if (b1 == b2) {
									// check if it's actually the same Logos
									if (one.target.id == two.target.id) {
										
										// That would actually mean we have a double Link...
										long lk_id = getMaxLinkID() + 1;
										int sum_evidence = utils.evidenceFromGenerality(one.generality, belief) +
												utils.evidenceFromGenerality(two.generality, belief);
										double g = utils.generality(sum_evidence, belief);
										double a = 1.0;	// actuality of freshly merged link is high
										
										Link merged = new Link(one.source, one.target, one.relationName, g, a, lk_id);
										linkList.add(merged);
										
										chains_merged++;
										
										// remove 2 links and all their connections
										one.source.outwardLinks.remove(one);
										one.target.inwardLinks.remove(one);
										two.source.outwardLinks.remove(two);
										two.target.inwardLinks.remove(two);
										linkList.remove(one);
										linkList.remove(two);
										
									} else {
										
										// just move to B1
										for (Link lk : (ArrayList<Link>) two.target.inwardLinks.clone()) {
											lk.target = one.target;
											one.target.inwardLinks.add(lk);
										}
										for (Link lk : (ArrayList<Link>) two.target.outwardLinks.clone()) {
											lk.source = one.target;
											one.target.outwardLinks.add(lk);
										}
										
									}
									
									
								} else {
									
									for (Link lk : (ArrayList<Link>) one.target.inwardLinks.clone()) {
										lk.target = two.target;
										two.target.inwardLinks.add(lk);
									}
									for (Link lk : (ArrayList<Link>) one.target.outwardLinks.clone()) {
										lk.source = two.target;
										two.target.outwardLinks.add(lk);
									}
									
								}
								
								// OLD: After B wass merged...
								// OLD: But now find the doubled Link and merge it as well! Increase g and a!
								
								
								
							} else {
								
								// TODO: What to do if A1 and A2 have same link number, but are not the same?
								
								// use the first case, move to the first Logos
								
								for (Link lk : (ArrayList<Link>) two.source.inwardLinks.clone()) {
									lk.target = one.source;
									one.source.inwardLinks.add(lk);
								}
								for (Link lk : (ArrayList<Link>) two.source.outwardLinks.clone()) {
									lk.source = one.source;
									one.source.outwardLinks.add(lk);
								}
								
							}
							
							
							
							
							
						} else {
							
							for (Link lk : (ArrayList<Link>) one.source.inwardLinks.clone()) {
								lk.target = two.source;
								two.source.inwardLinks.add(lk);
							}
							for (Link lk : (ArrayList<Link>) one.source.outwardLinks.clone()) {
								lk.source = two.source;
								two.source.outwardLinks.add(lk);
							}
							
						}
						
					}
					
				}
				
			}
			
		}
		
		if (verbose) {
			System.out.println("          " + chains_merged + " redundant chains eliminated");
		}
		
	}
	
	
	/*
	 * An object A can do what its parent object B can do.
	 * Inherited Links list gives the relations which can be
	 * directly inherited.
	 */
	private void applyInheritance() {
		
		int n_new_links = 0;
		
		if (verbose) {
			System.out.println("DATABASE: applying inheritance...");
		}
		
		for (Logos logosA : logosList) {
			
			for (Link outlink : (ArrayList<Link>) logosA.outwardLinks.clone()) {
				
				if (outlink.relationName.equals("is_a") && outlink.generality >= 0.0) {
					
					Logos logosB = outlink.target;
					
					if (verbose) {
						System.out.println("          Found [is_a] Link with high generality");
						System.out.println("          Its target Logos has " + logosB.outwardLinks.size() + " outward Links");
					}
					
					for (Link outlinkB : logosB.outwardLinks) {
						
						if (verbose) {
							System.out.println("          Checking outward Link [" + outlinkB.relationName + "]");
						}
						
						if (inheritedLinks.contains(outlinkB.relationName)) {
							
							if (verbose) {
								System.out.println("          This Link can be inherited by Logos [" + logosA.name + "]");
							}
							
							// Make a new direct link
							n_new_links++;
							Logos logosC = outlinkB.target;
							long lk_idx = getMaxLinkID();
							double g_new = outlink.generality * outlinkB.generality;
							Link newLink = new Link(logosA, logosC, outlinkB.relationName, g_new, 1.0, lk_idx);
							linkList.add(newLink);
							logosA.outwardLinks.add(newLink);
							logosC.inwardLinks.add(newLink);
							
						}
						
					}
					
				}
				
			}
			
		}// end logosList loop
		
		if (verbose) {
			System.out.println("          " + n_new_links + " new Links inherited");
		}
		
	}
	
	public long getMaxLinkID() {
		long id = -1;
		for (Link lk : linkList) {
			if (lk.id > id) {
				id = lk.id;
			}
		}
		return id;
	}
	
	public long getMaxLogosID() {
		long id = -1;
		for (Logos l : logosList) {
			if (l.id > id) {
				id = l.id;
			}
		}
		return id;
	}
	
	public void write (String str) {
		if (verbose)
			System.out.println(str);
	}

}
