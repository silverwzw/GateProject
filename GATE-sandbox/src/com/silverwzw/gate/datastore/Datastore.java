package com.silverwzw.gate.datastore;

import java.sql.ResultSet;

public interface Datastore {
	public ResultSet execute(String sql);
	public ResultSet executeQuery(String sql);
	public void executeUpdate(String sql);
	public boolean isClosed();
	public void close();
}