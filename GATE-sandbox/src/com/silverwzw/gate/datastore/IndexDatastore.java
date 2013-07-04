package com.silverwzw.gate.datastore;

import com.silverwzw.gate.manager.AnnotationIndex.IndexEntry;

public interface IndexDatastore extends Datastore {
	public void updateIndex(String taskName, String url, Iterable<IndexEntry> ii);
	public void initIndexDatastore();
	public boolean indexDatastoreNeedInit();
}