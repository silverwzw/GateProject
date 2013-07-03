package com.silverwzw.gate.datastore;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


public class JDBCimpl implements Datastore {
	public Boolean debug = false;
	private Connection conn;
	public static class ConnectionException extends RuntimeException {
		ConnectionException() {super();};
		ConnectionException(Exception e) {super(e);};
		ConnectionException(String s) {super(s);};
		ConnectionException(String s, Exception e) {super(s,e);};
	}
	public static class ImplSQLException extends RuntimeException {
		ImplSQLException() {super();};
		ImplSQLException(Exception e) {super(e);};
		ImplSQLException(String s) {super(s);};
		ImplSQLException(String s, Exception e) {super(s,e);};
	}
	public JDBCimpl (String connStr, String user, String passwd) {
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
		if (debug) {
			System.out.println("successfully connected to MySQL.");
		}
	}
	public ResultSetContainer execute(String sql) {
		boolean b;
		Statement stm;
		try {
			stm = conn.createStatement();
		} catch (SQLException e) {
			throw new ImplSQLException("Error creating Statement: " + sql, e);
		}
		try {
			b = stm.execute(sql);
		} catch (SQLException e) {
			throw new ImplSQLException("Error Executing: " + sql, e);
		}
		try {
			return b ? new ResultSetContainer(stm.getResultSet()) : null;
		} catch (SQLException e) {
			throw new ImplSQLException("Error retriving result set: " + sql, e);
		}
	}
	protected void finalize() throws Throwable {
		try {
			if (conn != null && !conn.isClosed()) {
				conn.close();
			}
		} finally {
			super.finalize();
		}
	}
	public boolean isClosed() {
		try {
			return conn == null || conn.isClosed();
		} catch (SQLException e) {
			throw new ImplSQLException(e);
		}
	}
	public void close(){
		try {
			conn.close();
		} catch (Exception e) {
			throw new ImplSQLException(e);
		}
		conn = null;
	}
	public Iterable<Result> executeQuery(String sql) {
		try {
			return new ResultSetContainer(conn.createStatement().executeQuery(sql));
		} catch (SQLException e) {
			throw new ImplSQLException("Error Executing: " + sql, e);
		}
	}
	public void executeUpdate(String sql) {
		try {
			conn.createStatement().executeUpdate(sql);
		} catch (SQLException e) {
			throw new ImplSQLException("Error Executing: " + sql, e);
		}
	}
}