package com.silverwzw.gate.datastore;

import gate.Annotation;

import java.util.HashMap;
import java.util.Set;

import com.silverwzw.Debug;
import com.silverwzw.JSON.JSON;
import com.silverwzw.JSON.JSON.JsonStringFormatException;
import com.silverwzw.gate.manager.AnnotationIndex.IndexEntry;
import com.silvrewzw.gate.task.Task;

public class DatastoreRouterImpl implements DatastoreRouter {
	
	private CenterDatastore cds = null;
	private HashMap<String, IndexDatastore> idsm;
	private String datastoreJsonStr;
	
	public DatastoreRouterImpl(String datastoreJsonStr) {
		Debug.into(this, "<Constructor>");
		Debug.println(3, "Connecting center datastore");
		this.datastoreJsonStr = datastoreJsonStr;
		cds = center();
		if (cds.centerDatastoreNeedInit()) {
			cds.initCenterDatastore();
		}
		idsm = new HashMap<String, IndexDatastore>();
		Debug.out(this, "<Constructor>");
	}

	public void saveIndex(Task task, String url, Set<Annotation> annotSet) {
		Debug.into(this, "saveIndex");
		
		String taskName;
		String dbName;
		
		taskName = task.getName();
		dbName = center().taskRouteDatastore(taskName);
		
		index(dbName).updateIndex(taskName, url, annotSet);
		
		Debug.out(this, "saveIndex");
	}

	final public void saveCache(String url, String content) {
		Debug.into(this, "saveCache");
		center().setDocumentCache(url, content);
		Debug.out(this, "saveCache");
	}

	public void saveTask(String name, String taskJson) {
		Debug.into(this, "saveTask");
		JSON json;
		try {
			json = JSON.parse(taskJson);
		} catch (JsonStringFormatException e) {
			throw new RuntimeException(e);
		}
		center().setTask(name, json.toString(), (String) json.get("datastore").toObject());
		Debug.out(this, "saveTask");
	}

	final public void saveDatastore(String name, String databaseJSON) {
		Debug.into(this, "saveDatastore");
		center().setDatastore(name, databaseJSON);
		Debug.out(this, "saveDatastore");
	}
	
	private Datastore datastoreBuilder(String dbJsonStr) {
		Debug.into(this, "DatastoreBuilder");
		
		JSON json;
		IndexDatastore retVal = null;
		
		try {
			json = JSON.parse(dbJsonStr);
		} catch (JsonStringFormatException e) {
			throw new RuntimeException(e);
		}
		
		Debug.println(3, "Building Datastore Object : " + json.toString());
		if (json.get("jdbc") != null) {
			String jdbcConnStr;
			jdbcConnStr = (String) json.get("jdbc").toObject();
			if (jdbcConnStr.startsWith("jdbc:mysql:")) {
				retVal = new JDBC_MySQL_impl(dbJsonStr);
			}
		}
		
		Debug.out(this, "DatastoreBuilder");
		return retVal;
	}
	
	final private CenterDatastore center() {
		return (cds != null && !cds.isClosed()) ? cds : (CenterDatastore) datastoreBuilder(datastoreJsonStr);
	}
	
	final private IndexDatastore index(String indexDatastoreName) {
		IndexDatastore retVal;
		
		retVal = idsm.get(indexDatastoreName);
		if (retVal == null || retVal.isClosed()) {
			Debug.println(3, "lazy load - datastore '" + indexDatastoreName + '\'');
			retVal = (IndexDatastore) datastoreBuilder(center().getDatastore(indexDatastoreName));
			if (retVal.indexDatastoreNeedInit()) {
				retVal.initIndexDatastore();
			}
			idsm.put(indexDatastoreName, retVal);
		}
		
		return retVal;
	}

	final public void reConnectCenter() {
		if (cds.isClosed()) {
			cds.reConnect();
		}
	}

	final public void resetCenter() {
		cds.initCenterDatastore();
	}

	public String getTask(String taskName) {
		return cds.getTask(taskName);
	}
}
