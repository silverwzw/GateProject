package com.silverwzw.gate.datastore;

public interface CenterDatastore extends Datastore {
	public void setDocumentCache(String url, String content);
	public void setTask(String name, String taskJson, String DatastoreName);
	public String getTask(String name);
	public String taskRouteDatastore(String taskName);
	public void setDatastore(String name, String datastoreJson);
	public String getDatastore(String name);
	public void initCenterDatastore();
	public boolean centerDatastoreNeedInit();
}