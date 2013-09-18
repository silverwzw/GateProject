package com.silverwzw.thesis.mscs;


import com.silverwzw.Debug;
import com.silverwzw.cmdapp.SimpleCommandlineApplication.ActionHandler;
import com.silverwzw.thesis.mscs.SingleWordTest.Analysis;


public class Experiment extends ActionHandler {
	
	public Experiment() {

	}
	protected void registry() {
		String[][] kw = {
			{"menu", "start", "button", "icon", "click"},
			{"geek","nerd", "hacker", "cracker", "genius"},
			{"sed", "grep", "awk", "cat", "cut"}
		};
		java.sql.Connection conn;
		int listSize = 100;
		
		try {
			conn = java.sql.DriverManager.getConnection("jdbc:mysql://localhost:3306/thesis_data","thesis","thesis");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		Debug.set(3);
		register('d', new WordPairTest("exp6").new Dump2db(conn, kw, listSize, "h"));
		register('a', new WordPairTest("exp6").new Analysis(conn, kw, listSize));
	}
}
