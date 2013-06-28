package com.silverwzw.gate.datastore;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.silverwzw.gate.datastore.Datastore.Result;


public class ResultSetContainer implements Iterable<Datastore.Result> {
	final public static class RecordNotRemovableException extends RuntimeException {
		RecordNotRemovableException() {super();};
		RecordNotRemovableException(Exception e) {super(e);};
		RecordNotRemovableException(String s) {super(s);};
		RecordNotRemovableException(String s, Exception e) {super(s,e);};
	}
	final public static class multiIteratorException extends RuntimeException {
		multiIteratorException() {super();};
		multiIteratorException(Exception e) {super(e);};
		multiIteratorException(String s) {super(s);};
		multiIteratorException(String s, Exception e) {super(s,e);};
	}
	final public static class RecordSetCloseException extends RuntimeException {
		RecordSetCloseException() {super();};
		RecordSetCloseException(Exception e) {super(e);};
		RecordSetCloseException(String s) {super(s);};
		RecordSetCloseException(String s, Exception e) {super(s,e);};
	}
	final public static class ResultSetContainerGeneralException extends RuntimeException {
		ResultSetContainerGeneralException(Exception e) {super(e);};
		ResultSetContainerGeneralException(String s) {super(s);};
		ResultSetContainerGeneralException(String s, Exception e) {super(s,e);};
	}
	final public static class ResultImpl implements Datastore.Result {
		Map<String, Object> m;
		ResultImpl(Map<String, Object> m) {
			this.m = m;
		}
		public Object get(String field) {
			return m.get(field);
		}
	}
	final public class ResultItorImpl implements Iterator<Datastore.Result> {
		ResultSetMetaData rsmd;
		ResultItorImpl() {
			try {
				rsmd = rs.getMetaData();
			} catch (SQLException e) {
				throw new ResultSetContainerGeneralException(e);
			}
		}
		public boolean hasNext() {
			try {
				return rs.isLast();
			} catch (SQLException e) {
				throw new ResultSetContainerGeneralException(e);
			}
		}
		public Result next() {
			HashMap<String, Object> hm;
			hm = new HashMap<String, Object>();
			try {
				for (int i = 0; i < rsmd.getColumnCount(); i++) {
					hm.put(rsmd.getColumnName(i), rs.getObject(i));
				}
				rs.next();
			} catch (SQLException e) {
				throw new ResultSetContainerGeneralException(e);
			}
			return new ResultImpl(hm);
		}
		public void remove(){
			throw new RecordNotRemovableException();
		}
	}
	private ResultSet rs;
	Iterator<Result> iter;
	ResultSetContainer(ResultSet rs) {
		this.rs = rs;
		iter = null;
	}
	public ResultSet getResultSet() {
		iter = new ResultItorImpl();
		return rs;
	}
	public Iterator<Datastore.Result> iterator() {
		if (iter != null) {
			throw new multiIteratorException();
		}
		iter = new ResultItorImpl();
		return iter;
	}
	public void close() {
		if (rs == null) {
			throw new RecordSetCloseException("RecordSet is null");
		}
		try {
			if (rs.isClosed()) {
				throw new RecordSetCloseException("RecordSet is null");
			}
			rs.close();
		} catch (SQLException e) {
			throw new RecordSetCloseException(e);
		}
		rs = null;
	}
	protected void finalize() throws Throwable {
		try {
			if (rs != null || !rs.isClosed()) {
				rs.close();
			}
		} finally {
			super.finalize();
		}
	}
}