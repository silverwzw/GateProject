package com.silverwzw.gate.datastore;

import com.silverwzw.gate.manager.AnnotationIndex.IndexEntry;

public interface IndexDatastore extends Datastore {
	public boolean indexTableExists(String taskName);
	public void newIndexTable(String taskName);
	public void updateIndex(String taskName, String url, Iterable<IndexEntry> ii);
}