package com.silverwzw.gate.datastore;

public interface Datastore {
	public Iterable<Result> execute(String sql);
	public Iterable<Result> executeQuery(String sql);
	public void executeUpdate(String sql);
	public boolean isClosed();
	public void close();
	public static interface Result {
		public Object get(String field);
	}
}