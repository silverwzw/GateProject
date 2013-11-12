package com.silverwzw.thesis.mscs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.silverwzw.Debug;
import com.silverwzw.JSON.JSON;
import com.silverwzw.JSON.JSON.JsonStringFormatException;
import com.silverwzw.api.AbstractSearch;

public class DuckDuckGo extends AbstractSearch {
	
	protected String ua = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/29.0.1516.3 Safari/537.36";
	protected String impQ;
	
	public DuckDuckGo(String queryString) {
		Debug.into(this, "<Constructor>");
		setSearchTerm(queryString);
		Debug.out(this, "<Constructor>");
	}
	
	public void setSearchTerm(String searchTerm) {
		if (searchTerm == null) {
			return;
		}
		impQ = "";
		q = "";
		for (String term : searchTerm.trim().split(" ")) {
			if (!impQ.equals("")) {
				impQ += "+";
				q += "%20";
			}
			try {
				String t;
				t = java.net.URLEncoder.encode(term, "UTF-8");
				impQ += t;
				q += t;
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	public void setUserAgent(String ua) {
		this.ua = ua;
	}
	
	public static void main(String arg[]) {
		Debug.set(3);
		new DuckDuckGo("Steve Jobs").asUrlStringList(200);
	}
	
	public List<String> asUrlStringList(int i) {
		List<String> urlStringList;
		URLConnection conn;
		int rcount;
		String s = "", nextQueryURL;
		
		Debug.into(this, "asUrlStringList");
		rcount = 0;
		nextQueryURL = "d.js?q=" + q + "&l=us-en&p=1&s=0";
		urlStringList = new ArrayList<String>(i);
		
		try {
			Debug.println(3,"sending impersonate inital search request.");
			conn = new URL("https://duckduckgo.com/?q=" + impQ).openConnection();
			conn.setRequestProperty("User-Agent", ua);
			conn.setRequestProperty("Referer", "https://duckduckgo.com");
			
			BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuilder sb = new StringBuilder();

			Debug.println(3,"extracting the initial entry.");
			
			while ((s = r.readLine()) != null) {
				sb.append(s);
				s = r.readLine();
			}
			s = sb.toString();
			
			Matcher m = Pattern.compile("\\(\\'/(d\\.js\\?q=" + q + ".*&s=0)\\'\\);").matcher(s);
			if (m.find()) {
				nextQueryURL = m.group(1);
				Debug.println(3,"The initial entry found is : " + nextQueryURL);
			} else {
				System.err.println("Initial Entry not found.");
				return urlStringList;
			}
			
			while (nextQueryURL != null) {
				Debug.println(3,"sending actual request.");
				conn = new URL("https://duckduckgo.com/" + nextQueryURL).openConnection();
				conn.setRequestProperty("User-Agent", ua);
				conn.setRequestProperty("Referer", "https://duckduckgo.com");
				conn.connect();
				nextQueryURL = null;
				Debug.println(3, "reading query result from stream");
				r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				sb = new StringBuilder();
				
				while ((s = r.readLine()) != null) {
					sb.append(s);
					s = r.readLine();
				}
				s = sb.toString();
				Debug.println(3, "parsing result string");
				Matcher findStartIndex = Pattern.compile("if\\s+\\(\\s*nrn\\s*\\)\\s+nrn\\('.',\\[\\{").matcher(s);
				if (findStartIndex.find()) {
					Debug.println(3, "Successfully extract json from script");
				} else {
					throw new RuntimeException(s);
				}
				for (Entry<String, JSON> el : JSON.parse(s.substring(findStartIndex.end() - 2, s.length() -2))) {
					String u = null, c = null, n = null;
					
					for (Entry<String, JSON> el2 : el.getValue()) {
						if (el2.getKey().equals("c")) {
							c = (String)el2.getValue().toObject();
						}
						if (el2.getKey().equals("u")) {
							u = (String)el2.getValue().toObject();
						}
						if (el2.getKey().equals("n")) {
							n = (String)el2.getValue().toObject();
						}
					}
					
					if (n != null) {
						Debug.println(3, "Next query page entry found");
						nextQueryURL = n;
					}
					
					if (c != null && !c.equals(u)) {
						System.err.println("Warning: DuckDuckGo query result u != c : " + el.getValue().toString());
					} else if (c == null && u == null && n== null) {
						System.err.println("Warning: DuckDuckGo query result u, c, n all null");
					}
					String url = u == null ? c : u;
					if (url != null) {
						urlStringList.add(url);
						rcount++;
						Debug.println(3, "url(" + rcount + ") found : " + url);
						if (rcount == i) {
							Debug.println(3, "get enough url, returning.");
							Debug.out(this, "asUrlStringList");
							return urlStringList;
						}
					}
				}
			}
			Debug.println(3, "didn't get enough url, only got " + urlStringList.size());
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (JsonStringFormatException e) {
			System.err.println("JSON parser error when parsing result string from DuckDuckGo : " + s);
		}
		Debug.out(this, "asUrlStringList");
		
		return urlStringList;
	}

}
