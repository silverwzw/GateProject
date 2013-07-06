package com.silverwzw.gate.datastore;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.LinkedList;
import java.util.regex.Pattern;

import com.silverwzw.Debug;
import com.silverwzw.JSON.JSON;
import com.silverwzw.JSON.JSON.JsonStringFormatException;
import com.silverwzw.gate.manager.AnnotationIndex.IndexEntry;


final public class JDBC_MySQL_impl implements IndexDatastore, CenterDatastore {
	
	private Connection conn;
	String connStr,u,p;
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
	
	public JDBC_MySQL_impl(String dbJson) {
		Debug.into(this, "<Constructor>");
		
		JSON db;
		String connStr,u,p;
		
		try {
			db = JSON.parse(dbJson);
		} catch (JsonStringFormatException e) {
			throw new RuntimeException();
		}
		
		connStr = (String) db.get("jdbc").toObject();
		u = (String) db.get("user").toObject();
		p = (String) db.get("passwd").toObject();
		
		ConstructorImpl(connStr, u, p);
		Debug.out(this, "<Constructor>");
	}
	
	public JDBC_MySQL_impl (String connStr, String user, String passwd) {
		Debug.into(this, "<Constructor>");
		this.connStr = connStr;
		u = user;
		p = passwd;
		ConstructorImpl(connStr, user, passwd);
		Debug.out(this, "<Constructor>");
	}
	
	private void ConstructorImpl(String connStr, String user, String passwd) {
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
		Debug.info("Successfully connected to Database via JDBC.");
	}
	
	//implementing Datastore interface
	
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
	
	final public boolean tableExists(String tableName) {
		Debug.into(this, "tableExists");
		
		String[] types = {"TABLE"};
		ResultSet rs = null;
		boolean r = false;
		
		try {
			rs = conn.getMetaData().getTables(null, null, "%", types);

			while (rs.next()) {
				if (rs.getString("TABLE_NAME").equals(tableName)) {
					r = true;
					break;
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				if (rs != null && !rs.isClosed()) {
					rs.close();
				}
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
		
		Debug.out(this, "tableExists");
		return r;
	}
	
	final public Collection<String> listAllTable() {
		Debug.into(this, "listAllTable");
		
		LinkedList<String> tl;
		String[] types = {"TABLE"};
		ResultSet rs = null;
		
		tl = new LinkedList<String>();
		
		try {
			rs = conn.getMetaData().getTables(null, null, "%", types);

			while (rs.next()) {
				tl.add(rs.getString("TABLE_NAME"));
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				if (rs != null && !rs.isClosed()) {
					rs.close();
				}
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
		
		Debug.out(this, "listAllTable");
		return tl;
	}
	
	//implementing IndexDatastore interface
	public void initIndexDatastore() {
		Debug.into(this, "initIndexDatastore");
		
		Collection<String> allTable;
		
		allTable = listAllTable();
		
		if (allTable.contains("gate_task_table")) {
			Debug.println(3, "old data found. cleaning old data.");
			
			ResultSet rs;
			Debug.println(3, "Querying all old index table.");
			rs = executeQuery("SELECT table_name FROM gate_task_table;");
			
			try {
				while(rs.next()) {
					String table_name;
					
					table_name = rs.getString("table_name");
					Debug.println(3, "Dropping old index table :'" + table_name + "'");
					executeUpdate("DELETE * FROM gate_task_table WHERE table_name = '" + table_name + "';") ;
					if (allTable.contains(table_name)) {
						executeUpdate("DROP TABLE " + table_name + ";");
					}
				}
			} catch (SQLException e) {
				throw new ImplSQLException(e);
			}
			Debug.println(3, "Dropping old meta table :'gate_task_table'");
			executeUpdate("DROP TABLE gate_task_table;");
			Debug.println(3, "Dropping old meta table :'gate_url_list'");
			executeUpdate("DROP TABLE gate_url_list;");
		}

		
		Debug.println(3, "Creating new meta table :'gate_task_table'");
		executeUpdate("CREATE TABLE gate_task_table (task_name VARCHAR(127) NOT NULL, table_name VARCHAR(127) NOT NULL, UNIQUE (table_name), PRIMARY KEY (task_name));");
		Debug.println(3, "Creating new meta table :'gate_url_list'");
		executeUpdate("CREATE TABLE gate_url_list (id INT NOT NULL AUTO_INCREMENT, url VARCHAR(2047) NOT NULL, UNIQUE (url), PRIMARY KEY (id));");
		Debug.out(this, "initIndexDatastore");
	}
	
	final public boolean indexDatastoreNeedInit() {
		Debug.into(this, "indexDatastoreNeedInit");
		
		Collection<String> lt;
		lt = listAllTable();
		
		Debug.out(this, "indexDatastoreNeedInit");
		return (lt.contains("gate_task_table") && lt.contains("gate_url_list"));
	}
	
	final private String getIndexTable(String taskName) {
		Debug.into(this, "getIndexTable");
		
		// check taskName
		if (!identifierTest.matcher(taskName).matches()) {
			throw new UnsafeRequestException("unsafe identifier " + taskName);
		}
		
		ResultSet rs = null;
		Debug.println(3, "Querying table name for task '" + taskName + '\'');
		rs = executeQuery("SELECT table_name FROM gate_task_table WHERE task_name = '" + taskName + "';");

		String tn;
		try {
			if (rs.next()) {
				tn = rs.getString("table_name");
				Debug.println(3, "table found : '" + tn + '\'');
			} else {
				Debug.println(3, "Table not found. Creating new index table.");
				executeUpdate("CREATE TABLE index_" + taskName + " (url_id INT NOT NULL, start INT NOT NULL, end INT NOT NULL, FOREIGN KEY (url_id) REFERENCES gate_url_list(id));");
				executeUpdate("INSERT INTO gate_task_table (task_name, table_name) VALUES ('" + taskName +"', 'index_" + taskName + "');");
				tn = "index_" + taskName;
			}
		} catch (SQLException e) {
			throw new ImplSQLException(e);
		} finally {
			try {
				if (rs != null && !rs.isClosed()) {
					rs.close();
				}
			} catch (SQLException e) {
				throw new ImplSQLException(e);
			}
		}
		Debug.out(this, "getIndexTable");
		return tn;
	}
	
	final private int getUrlId(String url) {
		Debug.into(this, "getUrlId");
		int ret;
		ResultSet rs = null;
		PreparedStatement ps = null;
		
		try {
			ps = conn.prepareStatement("SELECT id FROM gate_url_list WHERE url = ? ;");
			ps.setString(1, url);
			Debug.println(3, "Querying url id of '" + url + "'");
			rs = ps.executeQuery();
			ps.close();

			if (rs.next()) {
				ret = rs.getInt("id");
			} else {
				Debug.println(3, "New url, assign a new id.");
				ps = conn.prepareStatement("INSERT INTO gate_url_list ( url ) VALUES ( ? ) ;");
				ps.setString(1, url);
				ps.executeUpdate();
				ps.close();
				ps = conn.prepareStatement("SELECT id FROM gate_url_list WHERE url = ? ;");
				ps.setString(1, url);
				rs = ps.executeQuery();
				rs.next();
				ret = rs.getInt("id");
			}
		} catch (SQLException e) {
			throw new ImplSQLException(e);
		} finally {
			try {
				if (rs != null && !rs.isClosed()) {
					rs.close();
				}
				if (ps != null && !ps.isClosed()) {
					ps.close();
				}
			} catch (SQLException e) {
				throw new ImplSQLException(e);
			}
		}
		
		Debug.out(this, "getUrlId");
		
		return ret;
	}
	
	public void updateIndex(String taskName, String url, Iterable<IndexEntry> ii) {
		Debug.into(this, "updateIndex");
		
		// check taskName
		if (!identifierTest.matcher(taskName).matches()) {
			throw new UnsafeRequestException("unsafe identifier " + taskName);
		}
		PreparedStatement ps = null;
		String table_name;
		int url_id;
		
		table_name = getIndexTable(taskName);
		url_id = getUrlId(url);
		
		Debug.println(2, "Deleting old index entry");
		
		try {
			
			try {
				ps = conn.prepareStatement("DELETE FROM " + table_name + " WHERE url_id = ? ;");
				ps.setInt(1, url_id);
				ps.executeUpdate();
			} finally {
				if (ps != null && !ps.isClosed()) {
					ps.close();
				}
			}
			
			Debug.println(2, "Inserting new index entry");
			
			for (IndexEntry ie : ii) {
				try {
					ps = conn.prepareStatement("INSERT INTO " + table_name + " (url_id, start, end) VALUES (?, ?, ?) ;");
					ps.setInt(1, url_id);
					ps.setInt(2, (int)ie.getStart());
					ps.setInt(3, (int)ie.getEnd());
					ps.executeUpdate();
				} finally {
					if (ps != null && !ps.isClosed()) {
						ps.close();
					}
				}
			}
		} catch (SQLException e) {
			throw new ImplSQLException(e);
		}
		
		Debug.out(this, "updateIndex");
	}
	
	
	//implementing IndexDatastore interface
	
	public void initCenterDatastore() {
		Debug.into(this, "initCenterDatastore");
		
		Debug.println(3, "Trying to drop old tables");
		for (String tableName : listAllTable()) {
			if (tableName.equals("gate_task") || tableName.equals("gate_db") || tableName.equals("gate_cache")) {
				executeUpdate("DROP TABLE " + tableName + " ;");
			}
		}
		
		Debug.println(3, "init new tables of Center Datastore");
		
		executeUpdate("CREATE TABLE gate_db (db_name VARCHAR(127) NOT NULL, db_json TEXT NOT NULL, PRIMARY KEY(db_name));");
		executeUpdate("CREATE TABLE gate_task (task_name VARCHAR(127) NOT NULL, task_json TEXT NOT NULL, db_name VARCHAR(127) NOT NULL, PRIMARY KEY(task_name), FOREIGN KEY (db_name) REFERENCES gate_db(db_name) ;");
		executeUpdate("CREATE TABLE gate_cache (url VARCHAR(2047) NOT NULL, content TEXT NOT NULL, PRIMARY KEY(url));");
		
		Debug.out(this,	"initCenterDatastore");
	}
	
	public boolean centerDatastoreNeedInit() {
		Debug.into(this, "centerDatastoreNeedInit");
		
		Collection<String> lt;
		lt = listAllTable();
		
		Debug.out(this, "centerDatastoreNeedInit");
		return (lt.contains("gate_db") && lt.contains("gate_task") && lt.contains("gate_cache"));
	}
	
	public void setDocumentCache(String url, String content) {
		Debug.into(this, "setDocumentCache");
		
		ResultSet rs = null;
		PreparedStatement ps = null;
		
		try {
			ps = conn.prepareStatement("SELECT COUNT(*) FROM gate_cache WHERE url = ? ;");
			ps.setString(1, url);
			rs = ps.executeQuery();
			boolean update;
			update = rs.next() && rs.getInt(1) > 0;
			rs.close();
			ps.close();
			if (update) {
				Debug.println(3, "Replaceing old cache");
				ps = conn.prepareStatement("UPDATE gate_cache SET content = ? WHERE url = ? ;");
				ps.setString(1, content);
				ps.setString(1, url);
				ps.executeUpdate();
				ps.close();
			} else {
				Debug.println(3, "Creating new cache");
				ps = conn.prepareStatement("INSERT INTO gate_cache (url, content) VALUES ( ? , ? ) ;");
				ps.setString(1, url);
				ps.setString(1, content);
				ps.executeUpdate();
				ps.close();
			}
		
		} catch (SQLException e) {
			throw new ImplSQLException(e);
		} finally {
			try {
				if (rs != null && !rs.isClosed()) {
					rs.close();
				}
				if (ps != null && !ps.isClosed()) {
					ps.close();
				}
			} catch (SQLException e) {
				throw new ImplSQLException(e);
			}
		}
		
		Debug.out(this, "setDocumentCache");
	}

	public void setTask(String name, String taskJson, String datastoreName) {
		Debug.into(this, "setTask");
		
		if (!identifierTest.matcher(name).matches()) {
			throw new UnsafeRequestException();
		}
		
		ResultSet rs = null;
		PreparedStatement ps = null;
		
		try {
			ps = conn.prepareStatement("SELECT COUNT(*) FROM gate_task WHERE task_name = ? ;");
			ps.setString(1, name);
			rs = ps.executeQuery();
			boolean update;
			update = rs.next() && rs.getInt(1) > 0;	
			rs.close();
			ps.close();
			
			if (update) {
				Debug.println(3, "Updating tesk defination to datastore.");
				ps = conn.prepareStatement("UPDATE gate_task SET task_json = ? , db_name = ? WHERE task_name = ? ;");
				ps.setString(1, taskJson);
				ps.setString(2, datastoreName);
				ps.setString(3, name);
				ps.executeUpdate();
				ps.close();
			} else {
				Debug.println(3, "Saving tesk defination to datastore.");
				ps = conn.prepareStatement("INSERT INTO gate_task ( task_json, db_name, task_name ) VALUES ( ? , ? , ?) ;");
				ps.setString(1, taskJson);
				ps.setString(2, datastoreName);
				ps.setString(3, name);
				ps.executeUpdate();
				ps.close();
			}
			
		} catch (SQLException e) {
			throw new ImplSQLException(e);
		} finally {
			try {
				if (rs != null && !rs.isClosed()) {
					rs.close();
				}
				if (ps != null && !ps.isClosed()) {
					ps.close();
				}
			} catch (SQLException e) {
				throw new ImplSQLException(e);
			}
		}
		Debug.out(this, "setTask");
	}

	public String getTask(String name) {
		Debug.into(this, "getTask");

		if (!identifierTest.matcher(name).matches()) {
			throw new UnsafeRequestException();
		}
		
		ResultSet rs = null;
		PreparedStatement ps = null;
		String retVal = null;
		
		try {
			ps = conn.prepareStatement("SELECT task_json FROM gate_task WHERE task_name = ? ;");
			ps.setString(1, name);
			rs = ps.executeQuery();
			if (rs.next()) {
				retVal = rs.getString("task_json");
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
			throw new ImplSQLException(e);
		} finally {
			try {
				if (rs != null && !rs.isClosed()) {
					rs.close();
				}
				if (ps != null && !ps.isClosed()) {
					ps.close();
				}
			} catch (SQLException e) {
				throw new ImplSQLException(e);
			}
		}
		
		Debug.out(this, "getTask");
		return retVal;
	}
	
	public String taskRouteDatastore(String taskName) {
		Debug.into(this, "taskRouteDatastore");

		if (!identifierTest.matcher(taskName).matches()) {
			throw new UnsafeRequestException();
		}
		
		ResultSet rs = null;
		PreparedStatement ps = null;
		String retVal = null;
		
		Debug.println(3, "Querying Database");
		
		try {
			ps = conn.prepareStatement("SELECT db_name FROM gate_task WHERE task_name = ? ;");
			ps.setString(1, taskName);
			rs = ps.executeQuery();
			if (rs.next()) {
				retVal = rs.getString("db_name");
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
			throw new ImplSQLException(e);
		} finally {
			try {
				if (rs != null && !rs.isClosed()) {
					rs.close();
				}
				if (ps != null && !ps.isClosed()) {
					ps.close();
				}
			} catch (SQLException e) {
				throw new ImplSQLException(e);
			}
		}
		
		Debug.out(this, "taskRouteDatastore");
		return retVal;
	}

	public void setDatastore(String name, String datastoreJson) {
		Debug.into(this, "setDatastore");
		
		if (!identifierTest.matcher(name).matches()) {
			throw new UnsafeRequestException();
		}
		
		ResultSet rs = null;
		PreparedStatement ps = null;
		
		try {
			ps = conn.prepareStatement("SELECT COUNT(*) FROM gate_db WHERE db_name = ? ;");
			ps.setString(1, name);
			rs = ps.executeQuery();
			boolean update;
			update = rs.next() && rs.getInt(1) > 0;	
			rs.close();
			ps.close();
			
			if (update) {
				Debug.println(3, "Updating db defination to datastore.");
				ps = conn.prepareStatement("UPDATE gate_db SET datastore_json = ?  WHERE db_name = ? ;");
				ps.setString(1, datastoreJson);
				ps.setString(2, name);
				ps.executeUpdate();
				ps.close();
			} else {
				Debug.println(3, "Saving db defination to datastore.");
				ps = conn.prepareStatement("INSERT INTO gate_db ( db_json, db_name ) VALUES ( ? , ? ) ;");
				ps.setString(1, datastoreJson);
				ps.setString(2, name);
				ps.executeUpdate();
				ps.close();
			}
			
		} catch (SQLException e) {
			throw new ImplSQLException(e);
		} finally {
			try {
				if (rs != null && !rs.isClosed()) {
					rs.close();
				}
				if (ps != null && !ps.isClosed()) {
					ps.close();
				}
			} catch (SQLException e) {
				throw new ImplSQLException(e);
			}
		}
		Debug.out(this, "setDatastore");
	}

	public String getDatastore(String name) {
		Debug.into(this, "getDatastore");

		if (!identifierTest.matcher(name).matches()) {
			throw new UnsafeRequestException();
		}
		
		ResultSet rs = null;
		PreparedStatement ps = null;
		String retVal = null;
		
		Debug.println(3, "Querying Database");
		
		try {
			ps = conn.prepareStatement("SELECT db_json FROM gate_db WHERE db_name = ? ;");
			ps.setString(1, name);
			rs = ps.executeQuery();
			if (rs.next()) {
				retVal = rs.getString("db_json");
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
			throw new ImplSQLException(e);
		} finally {
			try {
				if (rs != null && !rs.isClosed()) {
					rs.close();
				}
				if (ps != null && !ps.isClosed()) {
					ps.close();
				}
			} catch (SQLException e) {
				throw new ImplSQLException(e);
			}
		}
		
		Debug.out(this, "getDatastore");
		return retVal;
	}

	final public void reConnect() {
		Debug.into(this, "reConnect");
		if (isClosed()) {
			Debug.info("Re-connected to Database via JDBC.");
			try {
				conn = DriverManager.getConnection(connStr,u,p);
			} catch (SQLException e) {
				throw new ImplSQLException(e);
			}
			Debug.info("Successfully connected to Database via JDBC.");
		} else {
			Debug.println(3, "Already connected.");
		}
		Debug.out(this, "reConnect");
	}
}