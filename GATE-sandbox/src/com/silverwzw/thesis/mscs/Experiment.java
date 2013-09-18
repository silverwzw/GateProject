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
		int listSize = 115;
		Search searchEngine;
		/*
		 * searchEngine = new com.silverwzw.api.google.Search(null, "h");
		 * searchEngine.useGoogleApi(false);
		 * searchEngine.setResultPerPage(50);
		 * searchEngine.setXGoogleApiEscapeTime(5000);
		 */
		searchEngine = new DuckDuckGo(null);
		try {
			conn = java.sql.DriverManager.getConnection("jdbc:mysql://localhost:3306/thesis_data","thesis","thesis");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		Debug.set(3);
		register('d', new WordPairTest("exp9").new Dump2db(conn, kw, listSize, searchEngine));
		register('a', new WordPairTest("exp9").new Analysis(conn, kw, listSize));
	}
}
