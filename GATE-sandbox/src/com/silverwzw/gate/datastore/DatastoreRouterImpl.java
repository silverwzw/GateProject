package com.silverwzw.gate.datastore;

import com.silverwzw.Debug;
import com.silverwzw.JSON.JSON;
import com.silverwzw.JSON.JSON.JsonStringFormatException;
import com.silverwzw.gate.manager.AnnotationIndex.IndexEntry;
import com.silvrewzw.gate.task.Task;

public class DatastoreRouterImpl implements DatastoreRouter {

	@Override
	public void saveIndex(Task task, String url, Iterable<IndexEntry> iie) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void saveCache(String url, String content) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void saveTask(String name, String taskJson) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void saveDatastore(String name, String databaseJSON) {
		// TODO Auto-generated method stub
		
	}

	private IndexDatastore DatastoreBuilder(String dbJsonStr) {
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
}
