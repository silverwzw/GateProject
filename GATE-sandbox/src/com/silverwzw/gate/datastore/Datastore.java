package com.silverwzw.gate.datastore;

import java.sql.ResultSet;
import java.util.Collection;

public interface Datastore {
	public ResultSet execute(String sql);
	public ResultSet executeQuery(String sql);
	public void executeUpdate(String sql);
	public Collection<String> listAllTable();
	public boolean isClosed();
	public void close();
	public void reConnect();
}