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
import java.util.List;

public class DatabaseInterface implements java.io.Serializable{
	
	boolean verbose;
	
	public ArrayList<Logos> logosList;
	public ArrayList<Link> linkList;
	public ArrayList<Branch> branchList;
	
	double forgetRate = 0.01;
	double rememberRate = 0.1;
	
	double mergeThresh = 0.6;	// two Links can be merged, if g1*g2 > mergeThresh
	double mergeLogosActualitySpan = 0.2;	// two Logos can be merged, if their maxLinkA are less different
	double linkRedundancySpan = 0.05;	// if two links contain same info and have close actualitites - redundant
	
	double a_min;
	
	double ownBelief = 1.0;	// how much does LogOS believe in its own conclusions
	
	int[] tempElementSize = {0, 0, 0};	// Logos, Links, Branches
	int[] tempStats = new int[6];
	
	ArrayList<String> inheritedLinks = new ArrayList<String>();	// links that can be inherited
	ArrayList<String> mergeableLogos = new ArrayList<String>();
	
	public DatabaseInterface(int verbosity, double a_min) {
		
		logosList = new ArrayList<Logos>();
		linkList = new ArrayList<Link>();
		branchList = new ArrayList<Branch>();
		this.a_min = a_min;
		
		inheritedLinks.add("can_do");
		inheritedLinks.add("can_be");
		inheritedLinks.add("is_a");
		inheritedLinks.add("is_component_of");
		
		mergeableLogos.add("#SELF");
		mergeableLogos.add("#ENTITY");
		mergeableLogos.add("#PROPERTY");
		mergeableLogos.add("#VERB");
		mergeableLogos.add("#USER");
		mergeableLogos.add("#NOW");
		
		if (verbosity >= 3) {
			verbose = true;
		}
	}
	
	/*
	 * This method should contain all relevant rules for knowledge graph
	 * compression, expansion and merging. This is where things get forgotten
	 * to save some space. Actuality and generality throughout the graph
	 * are updated.
	 */
	public void updateDatabase(Utils utils, double belief) {
		
		write("DATABASE: UPDATE STEP");
		
		decreaseAllActualities(utils);
		
		ArrayList<Logos> actualLogosList = utils.filterByActuality(logosList, a_min);
		
		// Apply logic rules to the database hypergraph
		mergeSameLogos(utils);
		cleanLogosLinks(utils, actualLogosList);
		applyInheritance(utils);
		applySavedRules(utils, belief);
		applyResourceRule(utils, actualLogosList);
		applyReverseResourceRule(utils, actualLogosList);
		mergeRedundantChains(utils, belief);
		
		// ACHTUNG!!! Don't generalize the rules too much. They are definitely Link-specific.
		
		
		// After applying logic rules, delete Links with zero evidence and a = 0
		
		// Just to be sure, delete all Links with empty targets and sources
		
		// See if some chains with no Links are left. Generalize them with can_do, can_be etc.
		
		// In the end, update tempElementSize[]
		tempElementSize[0] = logosList.size();
		tempElementSize[1] = linkList.size();
		tempElementSize[2] = branchList.size();
		
		write("\nDATABASE: TIMESTEP SUMMARY");
		write("          Logos merged: " + tempStats[0]);
		write("          Links inherited: " + tempStats[1]);
		write("          Links from custom rules: " + tempStats[2]);
		write("          resource rule: " + tempStats[3]);
		write("          inverse resource rule: " + tempStats[4]);
		write("          Links merged: " + tempStats[5]);
		write("          Logos number: " + tempElementSize[0]);
		write("          Link number: " + tempElementSize[1]);
		write("          Branch number: " + tempElementSize[2]);
	}
	
	public void decreaseAllActualities(Utils utils) {
		// Decrease link actualities
		write("\nDATABASE: decreasing all Link actualities by " + forgetRate);
		for (Link link : linkList) {
			link.actuality = utils.updateActuality(link.actuality, true, forgetRate, rememberRate);
		}
	}
	
	public void cleanLogosLinks(Utils utils, ArrayList<Logos> actList) {
		write("\nDATABASE: cleaning Link lists of all Logos with actuality > " + a_min);
		for (Logos logos : actList) {
			int inLinks = logos.inwardLinks.size();
			int i = 0;
			while (i < inLinks) {
				Link l = logos.inwardLinks.get(i);
				long current_id = l.id;
				if (i != 0) {
					long last_id = logos.inwardLinks.get(i - 1).id;
					if (current_id == last_id) {
						logos.inwardLinks.remove(l);
						inLinks--;
					}
				}
				i++;
			}
			int outLinks = logos.outwardLinks.size();
			int j = 0;
			while (j < outLinks) {
				Link l = logos.outwardLinks.get(j);
				long current_id = l.id;
				if (j != 0) {
					long last_id = logos.outwardLinks.get(j - 1).id;
					if (current_id == last_id) {
						logos.outwardLinks.remove(l);
						outLinks--;
					}
				}
				j++;
			}
		}
		
	}
	
	public void applyReverseResourceRule(Utils utils, ArrayList<Logos> actualLogosList) {
		/*
		 * If A do B and C is_needed_to B, then A has C
		 */
		int applied = 0;
		for (Logos a : actualLogosList) {
			ArrayList<Link> out_do_links = utils.linksByName(a.outwardLinks, "do");
			ArrayList<Link> out_can_do_links = utils.linksByName(a.outwardLinks, "can_do");
			out_do_links.addAll(out_can_do_links);
			for (Link lk1 : out_do_links) {
				double g_do = lk1.generality;
				Logos b = lk1.target;
				ArrayList<Link> in_resource_links = utils.linksByName(b.inwardLinks, "is_needed_to");
				for (Link res_link : in_resource_links) {
					if (g_do > 0) {
						if (res_link.generality > 0) {
							// A has C
							Logos c = res_link.source;
							double g_new = g_do * res_link.generality;
							Link have_link = new Link(a, c, "have", g_new, 1.0, getMaxLinkID() + 1);
							have_link.setArg1(lk1);
							have_link.setArg2(res_link);
							if (!isRedundantLink(have_link, utils)) {
								a.outwardLinks.add(have_link);
								c.inwardLinks.add(have_link);
								linkList.add(have_link);
								write("\nDATABASE: applied reverse resource rule");
								applied++;
								if (verbose) {
									utils.printLogosInfo(a);
									utils.printLinkInfo(have_link);
									utils.printLogosInfo(c);
								}
							}
						}
					}
				}
			}
		}
		if (verbose) {
			System.out.println("\nDATABASE: reverse resource rule generated " + applied + " new Links");
		}
		tempStats[4] = applied;
	}
	
	public void applyResourceRule(Utils utils, ArrayList<Logos> actualLogosList) {
		/*
		 * If C has A and A is needed to B, then C can B
		 * If C has no A and A is needed to B, then C can't B 
		 * g(new) = g(have) * g(is_needed_to)
		 */
		int applied = 0;
		for (Logos c : actualLogosList) {
			ArrayList<Link> out_have_links = utils.linksByName(c.outwardLinks, "have");
			for (Link lk1 : out_have_links) {
				double g = lk1.generality;
				Logos a = lk1.target;
				ArrayList<Link> out_resource_links = utils.linksByName(a.outwardLinks, "is_needed_to");
				for (Link res_link : out_resource_links) {
					if (g > 0) {
						if (res_link.generality > 0) {
							// C can B
							Logos b = res_link.target;
							double g_new = g * res_link.generality;
							Link can_link = new Link(c, b, "can_do", g_new, 1.0, getMaxLinkID() + 1);
							can_link.setArg1(lk1);
							can_link.setArg2(res_link);
							if (!isRedundantLink(can_link, utils)) {
								c.outwardLinks.add(can_link);
								b.inwardLinks.add(can_link);
								linkList.add(can_link);
								write("\nDATABASE: applied resource rule");
								applied++;
								if (verbose) {
									utils.printLogosInfo(c);
									utils.printLinkInfo(can_link);
									utils.printLogosInfo(b);
								}
							}
						}
					} else {
						if (res_link.generality > 0) {
							// C can't B
							Logos b = res_link.target;
							double g_new = g * res_link.generality;
							Link can_link = new Link(c, b, "can_do", g_new, 1.0, getMaxLinkID() + 1);
							can_link.setArg1(lk1);
							can_link.setArg2(res_link);
							if (!isRedundantLink(can_link, utils)) {
								c.outwardLinks.add(can_link);
								b.inwardLinks.add(can_link);
								linkList.add(can_link);
								write("\nDATABASE: applied resource rule");
								applied++;
								if (verbose) {
									utils.printLogosInfo(c);
									utils.printLinkInfo(can_link);
									utils.printLogosInfo(b);
								}
							}
						}
					}
				}
			}
		}
		if (verbose) {
			System.out.println("\nDATABASE: resource rule generated " + applied + " new Links");
		}
		tempStats[3] = applied;
	}
	
	// Merges hypergraph nodes that encode the same thing.
	// It means "car" in "red car" and "green car" shouldn't be merged.
	public void mergeSameLogos(Utils utils) {
		
		int n_merged = 0;
		
		int logListSize = logosList.size();
		
		for (int i = 0; i < logListSize; i++) {
			
			for (int j = 0; j < logListSize; j++) {
				
				if (i < j && i < logosList.size() && j < logosList.size()) {
					
					Logos l1 = logosList.get(i);
					Logos l2 = logosList.get(j);
					
					// are Logos mergeable?
					int checksum = 0;
					if (l1.name.toLowerCase().equals(l2.name.toLowerCase())) {
						checksum++;
					}
					if (Math.abs(utils.maxLinkActuality(l1) - utils.maxLinkActuality(l2)) < mergeLogosActualitySpan) {
						checksum++;
					}
					// check whether they have different targets of "is" Links
					ArrayList<Link> descriptions1 = utils.linksByName(l1.outwardLinks, "is");
					descriptions1 = utils.filterLinksByGeneralitySign(descriptions1, true);
					ArrayList<Link> descriptions2 = utils.linksByName(l2.outwardLinks, "is");
					descriptions2 = utils.filterLinksByGeneralitySign(descriptions2, true);
					for (Link dl1 : descriptions1) {
						for (Link dl2 : descriptions2) {
							if (dl1.target.name.equals(dl2.target.name)) {
								checksum++;
								break;
							}
						}
					}
					
					if (checksum == 3) {
						
						write("DATABASE: merging Logos " + l1.id + " [" + l1.name + "] with Logos " + l2.id);
						
						n_merged++;
						
						for (Link inlink : (ArrayList<Link>) l2.inwardLinks.clone()) {
							inlink.target = l1;
							l1.inwardLinks.add(inlink);
							
						}
						for (Link outlink : (ArrayList<Link>) l2.outwardLinks.clone()) {
							outlink.source = l1;
							l1.outwardLinks.add(outlink);
						}
						
						// Delete all Links that were connected to redundant L2 Logos from
						// linkList
						deleteAllConnectedLinks(l2);
						
						logosList.remove(l2);
						
						write("          Removed Logos " + l2.id + " [" + l2.name + "]");
						
						/*for (Logos log : logosList) {
							// Search for deleted Logos in Links' sources and targets
							for (Link lk : log.outwardLinks) {

								if (lk.target.equals(l2)) {
									lk.target.id = -1;
									lk.target.name = "";
								}

							} 
							
							for (Link lk : log.inwardLinks) {

								if (lk.source.equals(l2)) {
									lk.source.id = -1;
									lk.source.name = "";
								}

							} 
							
						}*/
						
						// Search for deleted Logos in Branches and reset
						for (Branch br : branchList) {
							
							ArrayList<Logos> inLogos = br.containedLogosList;
							
							if (inLogos.contains(l2)) {
								
								write("DATABASE: correcting Branch reference");
								write("          Branch " + br.id + " contains deleted Logos " +
										l2.id + " [" + l2.name + "]");
								if (verbose) {
									utils.printBranchInfo(br);
									
									//test
									utils.printLinkInfo(br.containedLinkList.get(0));
								}
								
								int logIdx = inLogos.indexOf(l2);
								
								inLogos.set(logIdx, l1);
								write("          Changed this Logos to Logos " + 
										l1.id + " [" + l1.name + "]");
								
								if (verbose) {
									utils.printBranchInfo(br);
								}
								
								for (Link lk : (ArrayList<Link>) br.containedLinkList.clone()) {
									
									if (verbose) {
										utils.printLinkInfo(lk);
										System.out.println("l2.id = " + l2.id);
									}
									
									if (lk.target.id == l2.id) {
										
										Link newLink = utils.findLinkByID(linkList, lk.id);
										
										br.containedLinkList.set(logIdx - 1, newLink);
										
										write("          Reset Link " + lk.id + " [" + lk.relationName + "] to Link " +
												newLink.id + " [" + newLink.relationName + "]");
										
									}
									
									if (lk.source.id == l2.id) {
										
										Link newLink = utils.findLinkByID(linkList, lk.id);
										
										br.containedLinkList.set(logIdx, newLink);
										
										write("          Reset Link " + lk.id + " [" + lk.relationName + "] to Link " +
												newLink.id + " [" + newLink.relationName + "]");
										
									}
									
								}
								
							}
							
						}
						
						
					}
				}
			}
		}
		
		if (verbose) {
			System.out.println("\nDATABASE: " + n_merged + " Logos merged");
		}
		tempStats[0] = n_merged;
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
			System.out.println("\nDATABASE: merging redundancies...");
		}
		
		// can easily merge, if both links have high generality (absolute value)
		for (Link one : (ArrayList<Link>) linkList.clone()) {
			
			for (Link two : (ArrayList<Link>) linkList.clone()) {
				
				// Finde alle Paare von Links, die gleichnamige Quellen und Senken haben
				if (one.source.name.equals(two.source.name) && one.target.name.equals(two.target.name)) {
					
					// Mache jedes Paar nur einmal, und prüfe, ob die beiden Links ein hohes Produkt von Generalitäten haben.
					// Das setzt voraus, dass die beiden Links gleiche Vorzeichen haben.
					// Dadurch wird sichergestellt, dass keine widersprüchliche Links zusammengefügt werden.
					if (one.id < two.id && one.generality * two.generality > mergeThresh) {
						
						if (verbose) {
							System.out.println("\nDATABASE: Found Links with same relation names, sources and targets");
							utils.printLinkInfo(one);
							utils.printLinkInfo(two);
							System.out.println("          " + one.source.name + " [" + one.relationName + "] " + one.target.name);
							//System.out.println("          g = " + one.generality);
							//System.out.println("          a = " + one.actuality);
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
										
										// Generalität des zusammengefügten Links wird aus der Summe der Erfahrung berechnet.
										int sum_evidence = utils.evidenceFromGenerality(one.generality, belief) +
												utils.evidenceFromGenerality(two.generality, belief);
										double g = utils.generality(sum_evidence, belief);
										//double a = (one.actuality + two.actuality) / 2;	// actuality of freshly merged link is the average
										double a = utils.minOfTwo(one.actuality, two.actuality);
										
										Link merged = new Link(one.source, one.target, one.relationName, g, a, lk_id);
										
										write("\n          Merged Link:");
										if (verbose) {
											utils.printLinkInfo(merged);
										}
										
										if (one.arg1 != null) {
											merged.arg1 = one.arg1;
										}
										if (one.arg2 != null) {
											merged.arg2 = one.arg2;
										}
										
										one.source.outwardLinks.add(merged);
										one.target.inwardLinks.add(merged);
										
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
			System.out.println("\nDATABASE: " + chains_merged + " redundant chains eliminated");
		}
		tempStats[5] = chains_merged;
	}
	
	
	/*
	 * An object A can do what its parent object B can do.
	 * Inherited Links list gives the relations which can be
	 * directly inherited.
	 */
	private void applyInheritance(Utils utils) {
		
		int n_new_links = 0;
		
		if (verbose) {
			System.out.println("\nDATABASE: applying inheritance...");
		}
		
		for (Logos logosA : logosList) {
			
			if (utils.maxLinkActuality(logosA) >= a_min) {
				for (Link outlink : (ArrayList<Link>) logosA.outwardLinks.clone()) {

					if (outlink.relationName.equals("is_a") && outlink.generality >= 0.0) {

						Logos logosB = outlink.target;

						if (verbose) {
							System.out.println("\n          Found [is_a] Link with high generality");
							System.out.println(
									"          Its target Logos has " + logosB.outwardLinks.size() + " outward Links");
						}

						for (Link outlinkB : logosB.outwardLinks) {

							if (verbose) {
								System.out.println("\n          Checking outward Link [" + outlinkB.relationName + "]");
							}

							// Check whether this Link can be inherited
							// Don't create Links that are already there
							if (inheritedLinks.contains(outlinkB.relationName)) {

								if (verbose) {
									System.out.println(
											"          Link [" + outlinkB.relationName + "-->" + outlinkB.target.name
													+ "] can be inherited by Logos [" + logosA.name + "]");
								}

								// Make a new direct link

								Logos logosC = outlinkB.target;

								// This Link was already inherited...
								if (utils.haveSameTarget(logosB, logosA, outlinkB.relationName)) {
									write("          However, this Link is already in the hypergraph!");
								} else {
									long lk_idx = getMaxLinkID() + 1;
									double g_new = outlink.generality * outlinkB.generality;
									Link newLink = new Link(logosA, logosC, outlinkB.relationName, g_new, 1.0, lk_idx);
									newLink.arg1 = outlink;
									newLink.arg2 = outlinkB;
									linkList.add(newLink);
									logosA.outwardLinks.add(newLink);
									logosC.inwardLinks.add(newLink);

									n_new_links++;
								}

							}

						}

					}

				} 
			}
			
		}// end logosList loop
		
		if (verbose) {
			System.out.println("\nDATABASE: " + n_new_links + " new Links inherited");
		}
		tempStats[1] = n_new_links;
	}
	
	/*
	 * For the rules saved in hypergraph itself
	 */
	public void applySavedRules(Utils utils, double belief) {
		write("\nDATABASE: applying custom rules...");
		int applied = 0;
		for (Link link : (ArrayList<Link>) linkList.clone()) {
			if (link.relationName.startsWith("if")) {
				write("DATABASE: checking Link applicability...");
				String[] parsedLink = link.relationName.split(":");
				Branch cause = (Branch) link.target;
				Branch effect = (Branch) link.source;
				String requiredLink = parsedLink[1];
				String linkToCreate = parsedLink[2];
				ArrayList<Link> linksToCause = utils.linksWithTargetBranch(
						utils.filterLinksByActuality(linkList, 1.0 - forgetRate),
						cause);
				if (linksToCause.isEmpty()) {
					continue;
				} else {
					write("\nDATABASE: applying rule:");
					utils.printBranchInfo(effect);
					write("\n" + "LINK: " + link.relationName);
					utils.printBranchInfo(cause);
					List<Link> matchingLinks = utils.linksByName(linksToCause, requiredLink);
					List<Logos> actors = new ArrayList<Logos>();
					for (Link lk : matchingLinks) {
						actors.add(lk.source);
					}
					// for each actor create the effect branch
					for (Logos actor : actors) {
						Link generatedLink = utils.emptyNamedLink(linkToCreate, getMaxLinkID() + 1);
						// "room sp_prep in" is a sibling of "location sp_prep out_of" effect
						Branch sibling = utils.findBranchByID(branchList,
								utils.mostSimilarTargetBranch(actor.outwardLinks, effect));
						// write("found sibling");
						// utils.printBranchInfo(sibling);
						if (!sibling.name.equals("none")) {
							// replace all parents with children
							utils.specifyBranch(effect, sibling);
						}
						generatedLink.arg1 = link;
						// here only one link needed for deduction
						generatedLink.source = actor;
						generatedLink.actuality = 1.0;
						generatedLink.generality = utils.generality(1, belief);
						generatedLink.target = effect;						
						if (!isRedundantLink(generatedLink, utils)) {
							applied++;
							actor.outwardLinks.add(generatedLink);
							effect.inwardLinks.add(generatedLink);
							write("DATABASE: successful inference ["
									+ actor.name
									+ "] " + generatedLink.relationName
									+ " [" + effect.name
									+ "]");
									utils.printBranchInfo(effect);
							linkList.add(generatedLink);
						}
					}
				}
			}
		}
		if (verbose) {
			System.out.println("\nDATABASE: custom rules generated " + applied + " new Links");
		}
		tempStats[2] = applied;
	}
	
	// Deletes all Links connected to a given Logos in the linkList
	public void deleteAllConnectedLinks(Logos logos) {
		long logId = logos.id;
		for (Link link : (ArrayList<Link>) linkList.clone()) {
			if (link.source.id == logId || link.target.id == logId) {
				linkList.remove(link);
			}
		}
	}
	
	public boolean isRedundantLink(Link link, Utils utils) {
		List<Link> closeActualLinks = utils.filterLinksByActuality(linkList, link.actuality - linkRedundancySpan);
		closeActualLinks.remove(link);
		for (Link lk : closeActualLinks) {
			if (lk.source.name.equals(link.source.name)
					&& lk.target.name.equals(link.target.name)
					&& lk.generality * link.generality > 0) {
				return true;
			}
		}
		return false;
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
				
				tempElementSize[0]++;
				
				if (verbose)
					System.out.println("DATABASE: added Logos " + l.id + " [" + l.name + "]");
			}
			
			if (verbose)
				System.out.println("DATABASE: Logos number = " + tempElementSize[0]);
			
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
				
				tempElementSize[1]++;
				
				if (verbose)
					System.out.println("DATABASE: added Link " + lk.id + " [" + lk.relationName + "]");
				
			}
			
			if (verbose)
				System.out.println("DATABASE: Link number = " + tempElementSize[1]);
			
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
				
				tempElementSize[2]++;
				
				if (verbose)
					System.out.println("DATABASE: added Branch " + b.id + " [" + b.name + "]");
			}
			
			if (verbose)
				System.out.println("DATABASE: Branch number = " + tempElementSize[2]);
			
		} catch (IOException e) {
			System.err.println(e);
		}	
		return brlist;
	}
	
	public ArrayList<String> readHypergraphKernel() {
		write("DATABASE: reading hypergraph kernel...");
		ArrayList<String> chains = new ArrayList<String>();
		Charset charset = Charset.forName("UTF-8");
		Path source = Paths.get("kernel_backup/kernel.txt");
		try (BufferedReader wlreader = Files.newBufferedReader(source, charset)){
			String line = null;
			while((line = wlreader.readLine()) != null) {	//what should be done with every line
				chains.add(line);
			}
		} catch (IOException e) {
			System.err.println(e);
		}	
		return chains;
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
				int linkIdx = 0;
				for (Logos l_in : br.containedLogosList) {
					// for the last Logos, outward Link id = -1
					if (!br.containedLogosList.get(br.containedLogosList.size() - 1).equals(l_in)) {
						line += l_in.id + "/" + br.containedLinkList.get(linkIdx).id + "/";
						linkIdx++;
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
	
	// For manual database input (important in the beginning)
	public void manualInput (String command, Utils utils) {

		write("MAIN: parsing internal Chainese input");

		// Logos, Link IDs
		long logosNum = -1;
		long linkNum = -1;

		logosNum = getMaxLogosID();
		linkNum = getMaxLinkID();

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

			write("MAIN: at index " + i);

			// Better: recursive Branch completion

			if (blocks[i].equals("#B")) {

				openedBranch = tempBranchList.size();

				open.add(true);

				write("MAIN: Branch beginning");

				logosNum++;
				Branch currentBranch = utils.emptyNamedBranch("#BRANCH" + logosNum, logosNum);

				if (i > 1) {

					Link source = utils.findLinkByID(linkList, linkNum);

					// Reset globally
					int globalLinkIdx = linkList.lastIndexOf(source);

					source.target = currentBranch;

					currentBranch.inwardLinks.add(source);

					linkList.set(globalLinkIdx, source);

				}

				tempBranchList.add(currentBranch);

				continue;

			}

			if (blocks[i].equals("B#")) {

				write("MAIN: Branch ending");

				Branch b = tempBranchList.get(openedBranch);

				logosList.add(b);

				branchList.add(b);

				write("MAIN: added Branch and Logos " + b.id + " [" + b.name + "]");

				if (verbose) {
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

					write("MAIN: parsing Logos");

					logosNum++;
					Logos lg = utils.emptyNamedLogos(blocks[i], logosNum);

					// The Link connects to the whole Branch, not to the first Logos in it!
					if (i > 1 && !blocks[i - 1].contains("#")) {

						// Find the Logos' inward Link
						Link source = utils.findLinkByID(linkList, linkNum);

						// Reset globally
						int globalLinkIdx = linkList.lastIndexOf(source);

						// Tell inward Link about its target
						source.target = lg;

						// Add this inward Link to the Logos
						lg.inwardLinks.add(source);

						// Reset globally
						linkList.set(globalLinkIdx, source);

					}

					logosList.add(lg);
					write("MAIN: added Logos " + lg.id + " [" + lg.name + "]");

					// In a Branch: add Logos in list
					tempBranchList.get(openedBranch).containedLogosList.add(lg);

					logos = false;

					continue;

				}

				// Non-Logos token
				if (!logos) {

					write("MAIN: parsing non-Logos");

					linkNum++;
					Link lk = utils.emptyNamedLink(blocks[i], linkNum);

					// In a Branch, Link generalities are calculated for evidence = 1
					lk.generality = utils.generality(1, ownBelief);
					lk.actuality = 1.0;

					// just after it was closed
					if (branchNeedsLink) {

						Logos source = utils.findLogosByID(logosList, brIdx);

						// Reset globally
						int globalLogosIdx = logosList.lastIndexOf(source);

						lk.source = source;

						source.outwardLinks.add(lk);

						logosList.set(globalLogosIdx, source);

						branchNeedsLink = false;

					} else {

						Logos source = utils.findLogosByID(logosList, logosNum);

						// Reset globally
						int globalLogosIdx = logosList.lastIndexOf(source);

						lk.source = source;

						source.outwardLinks.add(lk);

						logosList.set(globalLogosIdx, source);
					}

					tempBranchList.get(openedBranch).containedLinkList.add(lk);

					linkList.add(lk);
					write("MAIN: added Link " + lk.id + " [" + lk.relationName + "]");

					logos = true;

				}

				// Normal chain, no Branch
			} else {

				// Logos token
				if (logos) {

					write("MAIN: parsing Logos");

					logosNum++;
					Logos lg = utils.emptyNamedLogos(blocks[i], logosNum);

					if (i > 1) {

						Link source = utils.findLinkByID(linkList, linkNum);

						// Reset globally
						int globalLinkIdx = linkList.lastIndexOf(source);

						source.target = lg;

						lg.inwardLinks.add(source);

						linkList.set(globalLinkIdx, source);
					}

					logosList.add(lg);
					write("MAIN: added Logos " + lg.id + " [" + lg.name + "]");

					logos = false;

					continue;

				}

				// Non-Logos token
				if (!logos) {

					write("MAIN: parsing non-Logos");

					String[] parsedLink = blocks[i].split(",");

					linkNum++;
					Link lk = utils.emptyNamedLink(parsedLink[0], linkNum);

					lk.generality = Double.parseDouble(parsedLink[1]);
					lk.actuality = 1.0;

					Logos source;
					if (branchNeedsLink) {
						int brNum = branchList.size();
						source = branchList.get(brNum - 1);
					} else {
						source = utils.findLogosByID(logosList, logosNum);
					}

					// Reset globally
					int globalLogosIdx = logosList.lastIndexOf(source);

					lk.source = source;

					source.outwardLinks.add(lk);

					logosList.set(globalLogosIdx, source);

					linkList.add(lk);
					write("MAIN: added Link " + lk.id + " [" + lk.relationName + "]");

					logos = true;

					if (branchNeedsLink) {
						branchNeedsLink = false;
					}

				}

			}

		}// end for

	}

}
