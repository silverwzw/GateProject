package com.silverwzw.gate.datastore;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Pattern;

import com.silverwzw.Debug;
import com.silverwzw.gate.manager.AnnotationIndex.IndexEntry;


final public class JDBCimpl implements IndexDatastore, CacheDatastore {
	
	private Connection conn;
	static Pattern identifierTest = Pattern.compile("^[a-zA-Z_][a-zA-Z_0-9]*$");
	
	@SuppressWarnings("serial")
	public static class ConnectionException extends RuntimeException {
		ConnectionException() {super();};
		ConnectionException(Exception e) {super(e);};
		ConnectionException(String s) {super(s);};
		ConnectionException(String s, Exception e) {super(s,e);};
	}
	@SuppressWarnings("serial")
	public static class ImplSQLException extends RuntimeException {
		ImplSQLException() {super();};
		ImplSQLException(Exception e) {super(e);};
		ImplSQLException(String s) {super(s);};
		ImplSQLException(String s, Exception e) {super(s,e);};
	}
	@SuppressWarnings("serial")
	public static class UnsafeRequestException extends RuntimeException {
		UnsafeRequestException() {super();};
		UnsafeRequestException(Exception e) {super(e);};
		UnsafeRequestException(String s) {super(s);};
		UnsafeRequestException(String s, Exception e) {super(s,e);};
	}
	
	public JDBCimpl (String connStr, String user, String passwd) {
		Debug.into(this, "<Constructor>");
		try {
			 conn = DriverManager.getConnection(connStr,user,passwd);
			 if (conn == null) {
				 throw new ConnectionException("conn = null");
			 }
			 if (conn.isClosed()) {
				 throw new ConnectionException("conn.isClosed() == true");
			 }
		} catch (SQLException e) {
			throw new ConnectionException("Conn String = " + connStr, e);
		}
		Debug.info("Successfully connected to MySQL.");
		Debug.out(this, "<Constructor>");
	}
	public ResultSet execute(String sql) {
		Debug.into(this, "execute");
		
		boolean b;
		Statement stm;
		ResultSet rs;
		
		try {
			stm = conn.createStatement();
		} catch (SQLException e) {
			throw new ImplSQLException("Error creating Statement: " + sql, e);
		}
		Debug.info("executeing SQL:" + sql);
		try {
			b = stm.execute(sql);
		} catch (SQLException e) {
			throw new ImplSQLException("Error Executing: " + sql, e);
		}
		try {
			rs = b ? stm.getResultSet() : null;
		} catch (SQLException e) {
			throw new ImplSQLException("Error retriving result set: " + sql, e);
		}
		Debug.out(this, "execute");
		return rs;
	}
	
	protected void finalize() throws Throwable {
		try {
			if (conn != null && !conn.isClosed()) {
				Debug.println(3, "JDBCimpl.finalize : releasing conn object.");
				conn.close();
				Debug.println(3, "JDBCimpl.finalize : released.");
			}
		} finally {
			super.finalize();
		}
	}
	
	final public boolean isClosed() {
		try {
			return conn == null || conn.isClosed();
		} catch (SQLException e) {
			throw new ImplSQLException(e);
		}
	}
	
	final public void close(){
		Debug.into(this, "close");
		try {
			if (conn != null && !conn.isClosed()) {
				Debug.println(3, "releasing conn object.");
				conn.close();
				Debug.println(3, "released.");
			}
		} catch (Exception e) {
			throw new ImplSQLException(e);
		}
		conn = null;
		Debug.into(this, "close");
	}
	
	public ResultSet executeQuery(String sql) {
		Debug.into(this, "executeQuery");
		
		ResultSet rs;
		try {
			Debug.info("executeing SQL Query:" + sql);
			rs = conn.createStatement().executeQuery(sql);
		} catch (SQLException e) {
			throw new ImplSQLException("Error Executing: " + sql, e);
		}
		
		Debug.out(this, "executeQuery");
		return rs;
	}
	
	public void executeUpdate(String sql) {
		Debug.into(this, "executeUpdate");
		try {
			Debug.info("executeing SQL Update:" + sql);
			conn.createStatement().executeUpdate(sql);
		} catch (SQLException e) {
			throw new ImplSQLException("Error Executing: " + sql, e);
		}
		Debug.out(this, "executeUpdate");
	}
	
	public boolean indexTableExists(String taskName) {
		Debug.into(this, "indexTableExists");
		
		String[] types = {"TABLE"};
		ResultSet rs;
		boolean r = false;
		
		try {
			rs = conn.getMetaData().getTables(null, null, "%", types);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		try {
			while (rs.next()) {
				if (rs.getString("TABLE_NAME").equals("index_" + taskName)) {
					r = true;
					break;
				}
			}
		} catch (SQLException e) {
			throw new ImplSQLException(e);
		}
		
		
		Debug.out(this, "indexTableExists");
		return r;
	}
	
	public void newIndexTable(String taskName) {
		Debug.into(this, "newIndexTable");
		
		// check taskName
		if (!identifierTest.matcher(taskName).matches()) {
			throw new UnsafeRequestException("unsafe identifier " + taskName);
		}
		Debug.println(3, "Deleting old index table");
		if (indexTableExists(taskName)) {
			executeUpdate("DROP TABLE index_" + taskName + " ;");
		}
		
		Debug.println(3, "creating new index table");
		executeUpdate("CREATE TABLE index_" + taskName + " (url varchar(1024), start int, end int);");
		Debug.out(this, "newIndexTable");
	}
	
	public void updateIndex(String taskName, String url, Iterable<IndexEntry> ii) {
		Debug.into(this, "updateIndex");
		
		// check taskName
		if (!identifierTest.matcher(taskName).matches()) {
			throw new UnsafeRequestException("unsafe identifier " + taskName);
		}
		
		Debug.println(2, "Deleting old index entry");
		
		try {
			PreparedStatement ps = conn.prepareStatement("DELETE FROM index_" + taskName + " WHERE url = ? ;");
			ps.setString(1, url);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new ImplSQLException(e);
		}
		
		Debug.println(2, "Inserting new index entry");
		
		for (IndexEntry ie : ii) {
			try {
				PreparedStatement ps = conn.prepareStatement("INSERT INTO index_" + taskName + " (url, start, end) VALUES (?, ?, ?) ;");
				ps.setString(1, url);
				ps.setInt(2, (int)ie.getStart());
				ps.setInt(3, (int)ie.getEnd());
				ps.executeUpdate();
			} catch (SQLException e) {
				throw new ImplSQLException(e);
			}
		}
		
		Debug.out(this, "updateIndex");
	}
	
	public void updateDocument(String url, String content) {
		;
	}
}