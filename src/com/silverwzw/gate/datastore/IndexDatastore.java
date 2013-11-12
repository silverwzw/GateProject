package com.silverwzw.gate.datastore;

import java.util.Set;

import gate.Annotation;

public interface IndexDatastore extends Datastore {
	public void updateIndex(String taskName, String url, Set<Annotation> annotSet);
	public void initIndexDatastore();
	public boolean indexDatastoreNeedInit();
}