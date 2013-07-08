package com.silverwzw.gate.datastore;

import java.util.Set;

import gate.Annotation;

import com.silvrewzw.gate.task.Task;

public interface DatastoreRouter {
	public void saveIndex(Task task, String url, Set<Annotation> annotSet);
	public void saveCache(String url, String content);
	public void saveTask(String name, String taskJson);
	public void saveDatastore(String name, String databaseJSON);
	public void reConnectCenter();
	public void resetCenter();
	public String getTask(String taskName);
}