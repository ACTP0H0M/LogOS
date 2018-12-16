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
	
	// TODO read write Branches
	
	public DatabaseInterface(int verbosity) {
		logosList = new ArrayList<Logos>();
		linkList = new ArrayList<Link>();
		branchList = new ArrayList<Branch>();
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

}
