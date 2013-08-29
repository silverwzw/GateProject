package com.silverwzw.gate;

import gate.Gate;

import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.silverwzw.Debug;
import com.silverwzw.JSON.JSON;
import com.silverwzw.cmdapp.Executable;
import com.silverwzw.cmdapp.SimpleCommandlineApplication;
import com.silverwzw.cmdapp.SimpleCommandlineApplication.ActionHandler;
import com.silverwzw.cmdapp.SimpleCommandlineApplication.CommandlineArgumentParseException;
import com.silverwzw.gate.datastore.DatastoreRouter;
import com.silverwzw.gate.datastore.DatastoreRouterImpl;
import com.silverwzw.gate.manager.GateProjectManager;
import com.silverwzw.google.api.GQuery;
import com.silverwzw.thesis.mscs.DataHelper;

public class GateProject extends SimpleCommandlineApplication {

	public static void main(String[] args) throws Exception {
		(new GateProject()).execute(args);
	}

	public static DatastoreRouter datastoreRouter = null; 
	public static List<String> taskName = new LinkedList<String>();
	public static String dcolistJsonStr = null;
	public static String gateHome = null;
	public static String gatePluginHome = null;
	public static List<PostponeExecutable> pexec = new LinkedList<PostponeExecutable>();
	public static Collection<URL> gateCreoleDir = new LinkedList<URL>();
	
	GateProject() {
		
	}
	
	final protected String helpMessage() {
		return 
				"Usage: load   -e <file> [-d <file ..>] [-t <file ..>] [-v [level]]\n" +
				"  or : run    -e <file> -t <taskName> -l <file> [-g <path>] [-p <path>] [-c <url ..>] [-v [level]]\n" +
				"  or : reset  -e <file>\n" +
				"  or : deamon -e <file> [-g <path>] [-p <path>] [-c <url ..>] [-v [level]]" +
				"  or : --help, help\n" +
				"\n" +
				"Action 'load': Load config file to datastore\n" +
				"    -e <file>     Required. Specify the center datastore.\n" +
				"                            <file> is a datastore config file.\n" +
				"    -d <file ..>  Optional. Define new index datastore and save to center datastore.\n" +
				"                            <file ..> is a list of datastore config file.\n" +
				"    -t <file ..>  Optional. Define new task(s) and save to center datastore.\n" +
				"                            <file ..> is a list of task config file.\n" +
				"    -v [level]    Optional. Enable debug mode.\n" +
				"                            [level] is the debug level (1-3), default is 1.\n" +
				"\n" +
				"Action 'run': run a Task\n" +
				"    -e <file>     Required. Specify the center datastore.\n" +
				"                            <file> is a datastore config file.\n" +
				"    -t <task ..>  Required. Specify the task(s) to be executed.\n" +
				"                            <task ..> is the name of the task.\n" +
				"    -l <file>     Required. Specify the local/web document(s) to be processed.\n" +
				"                            <file> is a json file that contains a list of document path/url.\n" +
				"    -g <path>     Optional. Set the path to GATE home\n"+
				"    -p <path>     Optional. Set the path to GATE plugin home\n" +
				"    -c <path ..>  Optional. Set the path(s) of Creole plugin directory(ies)\n" +
				"    -v [level]    Optional. Enable debug mode.\n" +
				"                            [level] is the debug level (1-3), default is 1.\n" +
				"\n" +
				"Action 'reset': reset datastore\n" +
				"    -e <file>     Required. Specify the center datastore.\n" +
				"                            <file> is a datastore config file.\n" +
				"\n" +
				"Action 'deamon': run as deamon\n" + 
				"    -e <file>     Required. Specify the center datastore.\n" +
				"    -g <path>     Optional. Set the path to GATE home\n"+
				"    -p <path>     Optional. Set the path to GATE plugin home\n" +
				"    -c <path ..>  Optional. Set the path(s) of Creole plugin directory(ies)\n" +
				"    -v [level]    Optional. Enable debug mode.\n" +
				"\n" +
				"Action 'help'\n" +
				"  or '--help':\n" +
				"                  show this help message\n";
	}
	
	final protected void post(String[] s) throws Exception {
		if (datastoreRouter == null && !s[0].equals("help") && !s[0].equals("--help") && !s[0].equals("thesis")) {
			throw new CommandlineArgumentParseException("option -e cannot be ommitted!");
		}
		for (PostponeExecutable pexe : pexec) {
			pexe.run();
		}
		pexec = null;
	}
	
	abstract static class PostponeExecutable implements Executable {
		String[] args;
		final public void execute(String[] args) throws Exception {
			this.args = args;
			prePostpone();
			pexec.add(this);
		}
		public void prePostpone() throws Exception {
			;
		}
		abstract public void run() throws Exception;
	}

	final protected void registry() {
		register("load", new ActionLoad());
		register("reset", new ActionReset());
		register("run", new ActionRun());
		register("thesis", new ActionThesis());
	}	
}


class ActionThesis extends ActionHandler {
	
	public ActionThesis() {

	}
	protected void registry() {
		String[][] kw = {
			{"Windows", "menu", "start", "word", "explorer"},
			{"Microsoft","\"Windows XP\"", "ipconfig", "\"Program Files\"", "taskbar"},
			{"Linux", "iptables", "awk", "sudo", "Debian"}
		};
		java.sql.Connection conn;
		int listSize = 80;
		
		try {
			conn = java.sql.DriverManager.getConnection("jdbc:mysql://localhost:3306/thesis_data","thesis","thesis");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		Debug.set(3);
		register('d', new Dump2db(conn, kw, listSize));
		register('a', new Anaylsis(conn, kw, listSize));
	}
}

class Anaylsis implements Executable {
	String[][] kw;
	java.sql.Connection conn;
	int listSize;
	private class Pair<T> {
		public T a,b;
		public Pair(T a, T b) {
			this.a = a;
			this.b = b;
		}
		public boolean equals(Object o) {
			if (!this.getClass().isInstance(o)) {
				return false;
			}
			return a.equals(this.getClass().cast(o).a) && b.equals(this.getClass().cast(o).b);
		}
	}
	private class WordPair extends Pair<String> {
		public WordPair(String a, String b) {
			super(a, b);
		}
		public String toString() {
			return "<" + a.replaceAll("\"", "") + ">-<" + b.replaceAll("\"", "") + ">";
		}
	}
	public Anaylsis(java.sql.Connection conn, String[][] kw, int listSize) {
		this.conn = conn;
		this.kw = kw;
		this.listSize = listSize;
	}
	public void execute(String[] args) throws Exception {
		List<List<WordPair>> combs = new ArrayList<List<WordPair>>(3);
		Map<WordPair, List<String>> word2url = new HashMap<WordPair, List<String>>(); 
		for (int i = 0; i < kw.length; i++) {
			for (int j = i + 1; j < kw.length; j++) {
				List<WordPair> comb = new ArrayList<WordPair>(25);
				for (String wordA : kw[i]) {
					for (String wordB : kw[j]) {
						WordPair wp = new WordPair(wordA, wordB); 
						comb.add(wp);
						PreparedStatement ps= conn.prepareStatement("Select url from word2url where wordA = ? AND wordB = ?;");
						ps.setString(1, wordA);
						ps.setString(2, wordB);
						ResultSet rs = ps.executeQuery();
						List<String> urls;
						urls = new LinkedList<String>();
						while (rs.next()) {
							urls.add(rs.getString("url"));
						}
						word2url.put(wp, urls);
					}
				}
				combs.add(comb);
			}
		}
		for (List<WordPair> comb : combs) {
			Set<String> ttl = new HashSet<String>();
			Map<Pair<WordPair>, Integer> overlapTable;
			int totalUrls;
			
			overlapTable = new HashMap<Pair<WordPair>, Integer>(); 
			for (WordPair wp : comb) {
				ttl.addAll(word2url.get(wp));
			}
			totalUrls = ttl.size();
			System.out.println("total number of urls: " + totalUrls);
			System.out.println("total overlap : " + (kw[0].length*kw[0].length * listSize - ttl.size()) + '\n');
			for (WordPair wp : comb) {
				System.out.print("," + wp.toString() );
			}
			
			for (WordPair wp1 : comb) {
				System.out.print("\n" + wp1.toString() + ',');
				for (WordPair wp2 : comb) {
					if (wp1.equals(wp2)) {
						System.out.print("N/A,");
					} else {
						ttl = new HashSet<String>();
						ttl.addAll(word2url.get(wp1));
						ttl.addAll(word2url.get(wp2));
						int overlap;
						overlap = 2 * listSize - ttl.size();
						overlapTable.put(new Pair<WordPair>(wp1,wp2), overlap);
						System.out.print(overlap + ",");
					}
				}
			}
			
			System.out.println("\n\nBest Seq:");
			List<WordPair> wps = new ArrayList<WordPair>(25);
			Pair<WordPair> initPwp = getInitPair(overlapTable);
			WordPair nWordPair;
			wps.add(initPwp.a);
			wps.add(initPwp.b);
			while ((nWordPair = getNextWp(overlapTable, wps, comb)) != null) {
				wps.add(nWordPair);
			}
			ttl = new HashSet<String>();
			for (WordPair wp : wps) {
				System.out.print(wp.toString() + ",");
				ttl.addAll(word2url.get(wp));
				System.out.println(ttl.size()/(float)totalUrls);
			}
			
			System.out.println("\n");
		}
	}
	
	private WordPair getNextWp(Map<Pair<WordPair>, Integer> overlapTable, List<WordPair> wps, List<WordPair> comb) {
		Map<WordPair,Integer> wp2eval;
		wp2eval = new HashMap<WordPair,Integer>(); 
		for (WordPair wp : comb) {
			if (wps.contains(wp)) {
				continue;
			}
			int eval = 0;
			for (Entry<Pair<WordPair>, Integer> en : overlapTable.entrySet()) {
				Pair<WordPair> pwp;
				pwp = en.getKey();
				if (pwp.a.equals(wp) && wps.contains(pwp.b)) {
					eval += en.getValue();
				}
			}
			wp2eval.put(wp, eval);
		}
		int currentBestEval = 9999;
		WordPair currentBest = null;
		for (Entry<WordPair,Integer> en : wp2eval.entrySet()) {
			if (currentBest == null || currentBestEval > en.getValue()) {
				currentBest = en.getKey();
				currentBestEval = en.getValue();
			}
		}
		return currentBest;
	}
	
	private Pair<WordPair> getInitPair(Map<Pair<WordPair>, Integer> overlapTable) {
		List<Pair<WordPair>> currentBestPairs;
		Integer currentBestOverlap = null;
		for (Entry<Pair<WordPair>, Integer> en : overlapTable.entrySet()) {
			if (currentBestOverlap == null || currentBestOverlap > en.getValue()) {
				currentBestOverlap = en.getValue();
			}
		}
		currentBestPairs = new LinkedList<Pair<WordPair>>();
		for (Entry<Pair<WordPair>, Integer> en : overlapTable.entrySet()) {
			if (en.getValue() == currentBestOverlap) {
				currentBestPairs.add(en.getKey());
			}
		}
		Integer currentBestEvalpwp = null;
		Pair<WordPair> currentBestPair = null;
		for (Pair<WordPair> pwp : currentBestPairs) {
			Integer i;
			i = evalpwp(overlapTable, pwp);
			if (currentBestEvalpwp == null || i < currentBestEvalpwp) {
				currentBestEvalpwp = i;
				currentBestPair = pwp;
			}
		}
		return currentBestPair;
	}
	
	private int getTotalOverlap(Map<Pair<WordPair>, Integer> overlapTable, WordPair wp) {
		int sum = 0;
		for (Entry<Pair<WordPair>, Integer> en : overlapTable.entrySet()) {
			Pair<WordPair> pwp;
			pwp = en.getKey(); 
			if (pwp.a.equals(wp) || pwp.b.equals(wp)) {
				sum += en.getValue();
			}
		}
		return sum;
	}
	
	private int evalpwp(Map<Pair<WordPair>, Integer> overlapTable, Pair<WordPair> pwp) {
		return getTotalOverlap(overlapTable, pwp.a) + getTotalOverlap(overlapTable, pwp.b) - 2 *  overlapTable.get(pwp);
	}
}

class Dump2db implements Executable  {
	String[][] kw;
	int listSize;
	java.sql.Connection conn;
	public Dump2db(java.sql.Connection conn, String[][] kw, int listSize) {
		this.conn = conn;
		this.kw = kw;
		this.listSize = listSize;
	}
	public void execute(String[] args) throws Exception {
		try {
			conn.createStatement().execute("DROP TABLE word2url;");
		} catch (Exception ex) {
			System.err.println("Table word2url not exist");
		}
		conn.createStatement().execute("CREATE TABLE word2url (wordA VARCHAR(32) NOT NULL, wordB VARCHAR(32) NOT NULL, url VARCHAR(2047) NOT NULL);");
		for (int i = 0; i < kw.length; i++) {
			for (int j = i + 1; j < kw.length; j++) {
				for (String wordA : kw[i]) {
					for (String wordB : kw[j]) {
						GQuery gq;
						gq = new GQuery(wordA.replaceAll(" ", "%20") + "%20" + wordB.replaceAll(" ", "%20"));
						List<String> urls = gq.asUrlStringList(listSize);
						for (String url : urls) {
							System.out.println("[saving] " + url);
							PreparedStatement ps = conn.prepareStatement("INSERT INTO word2url(wordA, wordB, url) VALUES (?, ?, ?);");
							ps.setString(1, wordA);
							ps.setString(2, wordB);
							ps.setString(3, url);
							ps.execute();
						}
					}
				}
			}
		}
	}
}

class ActionLoad extends ActionHandler {
	final protected void registry() {
		register('v', new ChangeDebugLvl());
		register('e', new SetCenterDatastore());
		register('d', new SaveDatastore());
		register('t', new SaveTask());
	}
}

class ActionReset extends ActionHandler {
	final protected void registry() {
		register('v', new ChangeDebugLvl());
		register('e', new SetCenterDatastore());
	}
	final protected void post() {
		GateProject.datastoreRouter.resetCenter();
	}
}

class ActionRun extends ActionHandler {
	final protected void registry() {
		register('e', new SetCenterDatastore());
		register('v', new ChangeDebugLvl());
		register('t', new SetTaskAndRun());
		register('l', new SetInitDocuments());
		register('g', new SetGateHome());
		register('p', new SetGatePluginHome());
		register('c', new SetCreoleDirectory());
	}
}

class ChangeDebugLvl implements Executable {
	public void execute(String[] args) throws Exception {
		if (args.length == 0) {
			Debug.set(1);
			return;
		} else if (args.length == 1){
			try {
				int i;
				i = Integer.parseInt(args[0]);
				if (i < 1) {
					throw new NumberFormatException();
				}
				Debug.set(i);
			} catch (NumberFormatException e) {
				throw new CommandlineArgumentParseException("Debug level should be an positive integer");
			}
		} else {
			throw new CommandlineArgumentParseException("Too many arguments for option -v");
		}
	}
}

class SetCenterDatastore implements Executable {
	public void execute(String[] args) throws Exception {
		if (args.length == 0) {
			throw new CommandlineArgumentParseException("Not enough argument for option -e");
		}
		if (args.length > 1) {
			throw new CommandlineArgumentParseException("Too many argument for option -e");
		}
		if (GateProject.datastoreRouter != null) {
			throw new CommandlineArgumentParseException("-e option can only appear once. ");
		}
		GateProject.datastoreRouter = new DatastoreRouterImpl(JSON.parse(new File(args[0])).toString());
	}
}

class SaveTask extends GateProject.PostponeExecutable {
	public void run() throws Exception {
		if (args.length == 0) {
			throw new CommandlineArgumentParseException("Not enough argument for option -t");
		}
		for (int i = 0; i < args.length; i++) {
			JSON json;
			json = JSON.parse(new File(args[i]));

			GateProject.datastoreRouter.saveTask((String) json.get("name").toObject(), json.toString());
		}
	}
}

class SaveDatastore extends GateProject.PostponeExecutable {
	public void run() throws Exception {
		if (args.length == 0) {
			throw new CommandlineArgumentParseException("Not enough argument for option -d");
		}
		for (int i = 0; i < args.length; i++) {
			JSON json;
			json = JSON.parse(new File(args[i]));
			GateProject.datastoreRouter.saveDatastore((String) json.get("name").toObject(), json.toString());
		}
	}
}

class SetGateHome implements Executable {
	public void execute(String[] args) throws Exception {
		if (args.length == 0) {
			throw new CommandlineArgumentParseException("Not enough argument for option -g");
		}
		if (args.length > 1) {
			throw new CommandlineArgumentParseException("Too many argument for option -g");
		}
		GateProject.gateHome = args[0];
	}
}

class SetGatePluginHome implements Executable {
	public void execute(String[] args) throws Exception {
		if (args.length == 0) {
			throw new CommandlineArgumentParseException("Not enough argument for option -p");
		}
		if (args.length > 1) {
			throw new CommandlineArgumentParseException("Too many argument for option -p");
		}
		GateProject.gatePluginHome = args[0];
	}
}

class SetCreoleDirectory implements Executable {
	public void execute(String[] args) throws Exception {
		if (args.length == 0) {
			throw new CommandlineArgumentParseException("Not enough argument for option -c");
		}
		for (int i = 0; i < args.length; i++) {
			GateProject.gateCreoleDir.add((new File(args[i])).toURI().toURL());
		}
	}
}

class SetTaskAndRun extends GateProject.PostponeExecutable {
	public void prePostpone() throws Exception {
		if (args.length == 0) {
			throw new CommandlineArgumentParseException("Not enough argument for option -t");
		}
		for (int i = 0; i < args.length; i++) {
			GateProject.taskName.add(args[i]);
		}
	}
	public void run() throws Exception {
		if (GateProject.dcolistJsonStr == null) {
			throw new CommandlineArgumentParseException("Option -l is required!");
		}
		GateProjectManager gpm;
		gpm = new GateProjectManager();
		gpm.setGate(GateProject.gateHome, GateProject.gatePluginHome, GateProject.gateCreoleDir);
		gpm.setDebug(Debug.level());
		gpm.addJob(GateProject.datastoreRouter, GateProject.taskName, GateProject.dcolistJsonStr);
		(new Thread(gpm)).start();
	}
}

class SetInitDocuments implements Executable {
	public void execute(String[] args) throws Exception {
		if (args.length == 0) {
			throw new CommandlineArgumentParseException("Not enough argument for option -l");
		}
		if (args.length > 1) {
			throw new CommandlineArgumentParseException("Too many argument for option -l");
		}
		GateProject.dcolistJsonStr = JSON.parse(new File(args[0])).toString();
	}
}