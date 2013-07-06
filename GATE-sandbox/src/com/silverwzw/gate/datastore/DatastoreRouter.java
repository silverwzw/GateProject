package com.silverwzw.gate.datastore;

import com.silverwzw.gate.manager.AnnotationIndex.IndexEntry;
import com.silvrewzw.gate.task.Task;

public interface DatastoreRouter {
	public void saveIndex(Task task, String url, Iterable<IndexEntry> iie);
	public void saveCache(String url, String content);
	public void saveTask(String name, String taskJson);
	public void saveDatastore(String name, String databaseJSON);
	public void reConnectCenter();
	public void resetCenter();
}