package com.silverwzw.gate.datastore;

import java.sql.ResultSet;

import com.silverwzw.gate.index.AnnotationIndex.IndexEntry;

public interface Datastore {
	public ResultSet execute(String sql);
	public ResultSet executeQuery(String sql);
	public void executeUpdate(String sql);
	public boolean isClosed();
	public void close();
	public boolean indexTableExists(String taskName);
	public void newIndexTable(String taskName);
	public void updateIndex(String taskName, String url, Iterable<IndexEntry> ii);
}