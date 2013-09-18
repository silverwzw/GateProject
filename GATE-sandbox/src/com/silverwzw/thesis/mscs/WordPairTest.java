package com.silverwzw.thesis.mscs;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.silverwzw.api.google.Search;
import com.silverwzw.cmdapp.Executable;

public class WordPairTest {
	
	private String table;
	
	WordPairTest(String table) {
		this.table = table;
	}

	public class Analysis implements Executable {
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
		public Analysis(java.sql.Connection conn, String[][] kw, int listSize) {
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
							PreparedStatement ps= conn.prepareStatement("Select url from " + table + " where wordA = ? AND wordB = ?;");
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
	

	public class Dump2db implements Executable  {
		String[][] kw;
		int listSize;
		java.sql.Connection conn;
		String time;
		public Dump2db(java.sql.Connection conn, String[][] kw, int listSize, String time) {
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
			conn.createStatement().execute("CREATE TABLE " + table + " (wordA VARCHAR(32) NOT NULL, wordB VARCHAR(32) NOT NULL, url VARCHAR(2047) NOT NULL);");
			for (int i = 0; i < kw.length; i++) {
				for (int j = i + 1; j < kw.length; j++) {
					for (String wordA : kw[i]) {
						for (String wordB : kw[j]) {
							Search gq;
							gq = new Search(wordA.replaceAll(" ", "%20") + "%20" + wordB.replaceAll(" ", "%20"), time);
							gq.useGoogleApi(false);
							gq.setXGoogleApiEscapeTime(5000);
							List<String> urls = gq.asUrlStringList(listSize);
							for (String url : urls) {
								System.out.println("[saving] " + url);
								PreparedStatement ps = conn.prepareStatement("INSERT INTO " + table + " (wordA, wordB, url) VALUES (?, ?, ?);");
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
}
