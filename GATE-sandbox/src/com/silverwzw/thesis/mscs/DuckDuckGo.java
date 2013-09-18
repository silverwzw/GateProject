package com.silverwzw.thesis.mscs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.silverwzw.Debug;
import com.silverwzw.JSON.JSON;
import com.silverwzw.JSON.JSON.JsonStringFormatException;
import com.silverwzw.api.AbstractSearch;

public class DuckDuckGo extends AbstractSearch {
	
	protected String ua = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/29.0.1516.3 Safari/537.36";
	
	public DuckDuckGo(String queryString) {
		Debug.into(this, "<Constructor>");
		setSearchTerm(queryString);
		Debug.out(this, "<Constructor>");
	}
	
	public void setUserAgent(String ua) {
		this.ua = ua;
	}
	
	public static void main(String arg[]) {
		Debug.set(3);
		new DuckDuckGo("Steven").asUrlStringList(200);
	}
	
	public List<String> asUrlStringList(int i) {
		List<String> urlStringList;
		URLConnection conn;
		int rcount;
		String s, nextQueryURL;
		
		Debug.into(this, "asUrlStringList");
		rcount = 0;
		nextQueryURL = "d.js?q=" + q + "&t=D&l=us-en&p=1&s=0";
		urlStringList = new ArrayList<String>(i);
		
		try {
			Debug.println(3,"sending impersonate inital search request.");
			conn = new URL("https://duckduckgo.com/?q=" + q).openConnection();
			conn.setRequestProperty("User-Agent", ua);
			conn.setRequestProperty("Referer", "https://duckduckgo.com");
			new BufferedReader(new InputStreamReader(conn.getInputStream())).readLine();
			
			while (nextQueryURL != null) {
				Debug.println(3,"sending actual request.");
				conn = new URL("https://duckduckgo.com/" + nextQueryURL).openConnection();
				conn.setRequestProperty("User-Agent", ua);
				conn.setRequestProperty("Referer", "https://duckduckgo.com");
				conn.connect();
				nextQueryURL = null;
				Debug.println(3, "reading query result from stream");
				BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				StringBuilder sb = new StringBuilder();
				
				while ((s = r.readLine()) != null) {
					sb.append(s);
					s = r.readLine();
				}
				s = sb.toString();
				Debug.println(3, "parsing result string");
				for (Entry<String, JSON> el : JSON.parse(s.substring(17,s.length()-2))) {
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
			System.err.println("JSON parser error when parsing result string from DuckDuckGo");
		}
		Debug.out(this, "asUrlStringList");
		
		return urlStringList;
	}

}
