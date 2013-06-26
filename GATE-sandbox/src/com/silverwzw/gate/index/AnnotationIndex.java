package com.silverwzw.gate.index;

import gate.Annotation;
import gate.Controller;
import gate.Corpus;
import gate.CorpusController;
import gate.Document;
import gate.creole.ExecutionException;

import java.io.Serializable;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import com.silverwzw.gate.filter.AnnotationFilter;

@SuppressWarnings("serial")
public class AnnotationIndex implements Serializable {
	public static class IndexEntry implements Serializable {
		private long start, end;
		private String thumb;
		private IndexEntry(long start, long end) {
			this.start = start;
			this.end = end;
		}
		private IndexEntry(long start, long end, String abs) {
			this.start = start;
			this.end = end;
			this.thumb = abs;
		}
		public String getAbstract() {
			return thumb;
		}
		public long getStart() {
			return start;
		}
		public long getEnd() {
			return end;
		}
		public boolean equals(Object o) {
			if (o.getClass().equals(this.getClass())) {
				return start == ((IndexEntry) o).start && end == ((IndexEntry) o).end; 
			} else {
				return false;
			}
		}
	}
	
	HashMap<String,Set<IndexEntry>> index;
	
	public AnnotationIndex() {
		index = new HashMap<String,Set<IndexEntry>>();
	}
	
	public Collection<IndexEntry> getAll(String url) {
		return index.get(url);
	}
	
	public void add(String url, Collection<Annotation> ac, String content) {
		Set<IndexEntry> is;
		
		if ((is = index.get(url)) == null) {
			is = new HashSet<IndexEntry>();
		}
		
		for (Annotation a : ac) {
			long start,end;
			start = a.getStartNode().getOffset();
			end = a.getEndNode().getOffset();
			if (content == null) {
				is.add(new IndexEntry(start,end));
			} else {
				is.add(new IndexEntry(start, end, content.substring((int)start,(int)end)));
			}
		}
		index.put(url, is);
	}
	public void add(String url, Collection<Annotation> ac) {
		add(url, ac, null);
	}
	public void buildIndex(CorpusController c, Corpus corpus, AnnotationFilter filter) throws ExecutionException {
		c.setCorpus(corpus);
		c.execute();
		for (Document doc : corpus) {
			String url, content;
			filter.resetScenario();
			filter.setScenario(doc.getAnnotations());
			url = doc.getSourceUrl().toString();
			content = doc.getContent().toString();
			add(url, filter.findAll(), content);
		}
	}
	public String toString() {
		String r = "";
		for (Entry<String, Set<IndexEntry>> ise : index.entrySet()) {
			r += "[Doc]\t" + ise.getKey() + "\n";
			for (IndexEntry i : ise.getValue()) {
				r += "[Annot]\t    <" + i.getEnd() + ',' + i.getStart() + '>';
				if (i.getAbstract() != null) {
					r += "\n[Thumb]\t\t" + i.getAbstract();
				}
				r += '\n';
			}
		}
		return r;
	}
}
