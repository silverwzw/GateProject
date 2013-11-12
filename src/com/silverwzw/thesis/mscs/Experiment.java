package com.silverwzw.thesis.mscs;


import com.silverwzw.Debug;
import com.silverwzw.cmdapp.SimpleCommandlineApplication.ActionHandler;
import com.silverwzw.api.Search;


public class Experiment extends ActionHandler {
	
	public Experiment() {

	}
	protected void registry() {
		String[][] kw = {
			/*{"menu", "start", "button", "icon", "click"},
			{"geek","nerd", "hacker", "cracker", "genius"},
			{"sed", "grep", "awk", "cat", "cut"}*/
			{"Windows", "menu", "start", "word", "explorer"},
			{"Microsoft", "\"Windows XP\"", "ipconfig", "\"Program Files\"","\"task bar\""},
			{"Linux", "iptables", "awk", "sudo", "Debian"},
		};
		java.sql.Connection conn;
		int listSize = 100;
		Search searchEngine;
		
		com.silverwzw.api.google.Search gse = new com.silverwzw.api.google.Search(null, "h");
		gse.useGoogleApi(true);
		gse.setResultPerPage(10);
		gse.setXGoogleApiEscapeTime(5000);
		searchEngine = gse;
		/*
		searchEngine = new DuckDuckGo(null);
		*/
		try {
			conn = java.sql.DriverManager.getConnection("jdbc:mysql://localhost:3306/thesis_data","thesis","thesis");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		Debug.set(3);
		register('d', new WordPairTest("Google_Pair_listA").new Dump2db(conn, kw, listSize, searchEngine));
		register('a', new WordPairTest("Google_Pair_listA").new Analysis(conn, kw, listSize));
	}
}
