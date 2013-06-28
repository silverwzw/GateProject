package com.silverwzw.gate.datastore;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


public class MySQLimpl implements Datastore {
	public Boolean debug = false;
	private Connection conn;
	public static class MySQLConnectionException extends RuntimeException {
		MySQLConnectionException() {super();};
		MySQLConnectionException(Exception e) {super(e);};
		MySQLConnectionException(String s) {super(s);};
		MySQLConnectionException(String s, Exception e) {super(s,e);};
	}
	public static class MySQLImplSQLException extends RuntimeException {
		MySQLImplSQLException() {super();};
		MySQLImplSQLException(Exception e) {super(e);};
		MySQLImplSQLException(String s) {super(s);};
		MySQLImplSQLException(String s, Exception e) {super(s,e);};
	}
	public MySQLimpl (String connStr, String user, String passwd) {
		try {
			 conn = DriverManager.getConnection(connStr,user,passwd);
			 if (conn == null) {
				 throw new MySQLConnectionException("conn = null");
			 }
			 if (conn.isClosed()) {
				 throw new MySQLConnectionException("conn.isClosed() == true");
			 }
		} catch (SQLException e) {
			throw new MySQLConnectionException("Conn String = " + connStr, e);
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
			throw new MySQLImplSQLException("Error creating Statement: " + sql, e);
		}
		try {
			b = stm.execute(sql);
		} catch (SQLException e) {
			throw new MySQLImplSQLException("Error Executing: " + sql, e);
		}
		try {
			return b ? new ResultSetContainer(stm.getResultSet()) : null;
		} catch (SQLException e) {
			throw new MySQLImplSQLException("Error retriving result set: " + sql, e);
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
			throw new MySQLImplSQLException(e);
		}
	}
	public void close(){
		try {
			conn.close();
		} catch (Exception e) {
			throw new MySQLImplSQLException(e);
		}
		conn = null;
	}
	public Iterable<Result> executeQuery(String sql) {
		try {
			return new ResultSetContainer(conn.createStatement().executeQuery(sql));
		} catch (SQLException e) {
			throw new MySQLImplSQLException("Error Executing: " + sql, e);
		}
	}
	public void executeUpdate(String sql) {
		try {
			conn.createStatement().executeUpdate(sql);
		} catch (SQLException e) {
			throw new MySQLImplSQLException("Error Executing: " + sql, e);
		}
	}
}