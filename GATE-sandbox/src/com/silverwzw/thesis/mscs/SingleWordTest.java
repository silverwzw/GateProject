package com.silverwzw.thesis.mscs;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.silverwzw.Debug;
import com.silverwzw.cmdapp.Executable;
import com.silverwzw.google.api.Search;

public class SingleWordTest {
	
	private String table;
	
	SingleWordTest(String table) {
		this.table = table;
	}

	public class Dump2db implements Executable  {
		String[] kw;
		int listSize;
		java.sql.Connection conn;
		public Dump2db(java.sql.Connection conn, String[] kw, int listSize) {
			this.conn = conn;
			this.kw = kw;
			this.listSize = listSize;
		}
		public void execute(String[] args) throws Exception {
			try {
				conn.createStatement().execute("DROP TABLE " + table + ";");
			} catch (Exception ex) {
				System.err.println("Table " + table + " not exist");
			}
			conn.createStatement().execute("CREATE TABLE " + table + " (word VARCHAR(32) NOT NULL, url VARCHAR(2047) NOT NULL);");
			for (String word : kw) {
				Search gq;
				gq = new Search(word.replaceAll(" ", "%20"));
				gq.useGoogleApi(false);
				gq.setResultPerPage(50);
				List<String> urls = gq.asUrlStringList(listSize);
				for (String url : urls) {
					Debug.println(3, "<saving> " + url);
					PreparedStatement ps = conn.prepareStatement("INSERT INTO " + table + " (word, url) VALUES (?, ?);");
					ps.setString(1, word);
					ps.setString(2, url);
					ps.execute();
				}
				
			}
		}
	}
	
	public class Anaylsis implements Executable {
		String[] kw;
		int listSize;
		java.sql.Connection conn;
		public Anaylsis(java.sql.Connection conn, String[] kw, int listSize) {
			this.conn = conn;
			this.kw = kw;
			this.listSize = listSize;
		}
		public void execute(String[] args) throws Exception {
			Map<String,List<String>> urls;
			urls = new HashMap<String, List<String>>();
			for (String word : kw) {
				PreparedStatement ps = conn.prepareStatement("SELECT * FROM " + table + " WHERE word = ? ;");
				ResultSet rs;
				List<String> l;
				l = new ArrayList<String>();
				ps.setString(1, word);
				rs = ps.executeQuery();
				while (rs.next()) {
					l.add(rs.getString("url"));
				}
				urls.put(word, l);
			}
			for (String w1 : kw) {
				for (String w2 : kw) {
					if (w1.equals(w2)) {
						continue;
					}
					Set<String> s = new HashSet<String>();
					s.addAll(urls.get(w1));
					s.addAll(urls.get(w2));
					System.out.println("<" + w1 +">" + "-<" + w2 + "> : " + (urls.get(w1).size() + urls.get(w2).size() - s.size()));
				}
			}
		}
		
	}
}
