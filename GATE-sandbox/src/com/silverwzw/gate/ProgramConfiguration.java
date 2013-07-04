package com.silverwzw.gate;

import gate.Corpus;
import gate.CorpusController;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.ProcessingResource;
import gate.corpora.DocumentImpl;
import gate.creole.ResourceInstantiationException;
import gate.creole.SerialAnalyserController;
import gate.creole.gazetteer.Lookup;
import gate.util.GateException;
import gate.util.SimpleFeatureMapImpl;
import gate.util.persistence.PersistenceManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import com.ontotext.gate.gazetteer.HashGazetteer;
import com.silverwzw.Debug;
import com.silverwzw.JSON.JSON;
import com.silverwzw.JSON.JSON.JsonStringFormatException;
import com.silverwzw.gate.datastore.Datastore;
import com.silverwzw.gate.datastore.JDBCimpl;
import com.silverwzw.gate.filter.AnnotationFilter;
import com.silverwzw.gate.filter.FilterFactory;

final public class ProgramConfiguration {
	
	public static class Task {
		private String n;
		private CorpusController g;
		private AnnotationFilter f;
		Task(String name, CorpusController gapp, AnnotationFilter filter) {
			n = name;
			g = gapp;
			f = filter;
		}
		public String getName() {
			return n;
		}
		public CorpusController getController() {
			return g;
		}
		public AnnotationFilter getFilter() {
			return f;
		}
	}
	
	protected List<String> conf = new LinkedList<String>();
	protected List<String> docConf = new LinkedList<String>();
	protected String gateHome = "E:/Steven/Life/NCSU/Research/gate";
	protected String gatePluginHome = "E:/Steven/Life/NCSU/Research/gate/plugins";
	protected List<String> gateCreoleDir = new LinkedList<String>();
	protected String jdbcConf = null;
	protected Datastore ds = null;
	protected gate.Corpus corpus = null;
	protected List<Task> tasks = null;

	protected static String helpString =
			"Usage: -t <file ..> -u <url ..> [Other Options]\n\n" + 
			"Arguments:\n" +
			"   -t <file ..>        Required. Task Configuration File(s)\n" +
			"   -u <url ..>         Required. Specify document(s) or document directory(ies) to process\n" +
			"   -g <path>           Optional. Path to GATE home\n" +
			"   -p <path>           Optional. Path to GATE plugin home\n" +
			"   -c <url ..>         Optional. Path(s) of Creole plugin directory\n" +
			"   -d <JDBC conf file> Optional. Set JDBC config file\n" +
			"   -v [level]          Optional. Enable debug mode\n" +
			"   -h or --help        Optional. Show help message\n";

	@SuppressWarnings("serial")
	private class ArgParserException extends Exception {};
	
	public ProgramConfiguration(String ... args) {
		
		Debug.into(this, "<Constructor>");
		
		// always register ANNIE (for convenience)
		gateCreoleDir.add("file:///E:/Steven/Life/NCSU/Research/gate/plugins/ANNIE");
		
		try {
			
			if (args.length == 0) {
				throw new ArgParserException();
			}
		
			boolean t,u;
			int i;
			
			t = false;
			u = false;
			i = 0;
		
			
			while(i < args.length) {
				
				Debug.println(3, "Processing option:" + args[i]);
				
				if (args[i].equals("--help")) {
					System.out.print(helpString);
					System.exit(0);
				}
				
				if (args[i].charAt(0) != '-' || args[i].length() != 2) {
					throw new ArgParserException();
				}
				
				switch (args[i].charAt(1)) {
					case 't':
						while (++i < args.length && args[i].charAt(0) != '-') {
							t = true;	// ensure at least 1 configuration is added
							conf.add(args[i]);
							Debug.println(3, "Adding '" + args[i] + "' to task configuaration");
						}
						break;
						
					case 'u':
						while (++i < args.length && args[i].charAt(0) != '-') {
							u = true;	// ensure at least 1 doc is added
							docConf.add(args[i]);
							Debug.println(3, "Adding doc description file '" + args[i] + "'");
						}
						break;
					
					case 'g':
						gateHome = args[++i];
						Debug.println(3, "Reading GATE HOME Parameter '" + args[i] + "'");
						i++;
						break;
						
					case 'p':
						gatePluginHome = args[++i];
						Debug.println(3, "Reading GATE PLUGIN HOME parameter '" + args[i] + "'");
						i++;
						break;
					
					case 'c':
						while (++i < args.length && args[i].charAt(0) != '-') {
							if (args[i].startsWith("file:/")) {
								gateCreoleDir.add(args[i]);
							} else {
								Debug.info("CreoleDir does not have a proper URL prefix, auto fix by adding prefix 'file:///'");
								gateCreoleDir.add("file:///" + args[i]);
							}
							Debug.println(3, "Reading Creole Dir parameter '" + args[i] + "'");
						}
						break;
					
					case 'd':
						jdbcConf = args[++i];
						Debug.println(3, "Reading JDBC conf parameter '" + args[i] + "'");
						i++;
						break;
						
					case 'v':
						int level = 1;
						if (++i < args.length && args[i].charAt(0) != '-') {
							try {
								level = Integer.parseInt(args[i]);
								if (level < 0) {
									throw new NumberFormatException();
								}
							} catch (NumberFormatException e) {
								Debug.info("cannot parse debug level into non-negative integer, resetting to 1");
								level = 1;
							}
							i++;
						}
						Debug.set(level);
						break;

					case 'h':
						System.out.print(helpString);
						break;
						
					default:
						System.out.print("Unknown argument: " + args[i] + "\n");
						throw new ArgParserException();
				}
				
			}
			
			if (!(u && t)) {
				throw new ArgParserException();
			}
			
			Debug.out(this, "<Constructor>");
			
		} catch (Exception e) {
			if (e.getClass().equals(ArgParserException.class) || e.getClass().equals(ArrayIndexOutOfBoundsException.class)) {
				Debug.exception(e);
				System.out.print(helpString);
				System.exit(0);
			} else {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
	
	public void InitGate() {
		Debug.into(this, "InitGate");
		Gate.setGateHome(new File(gateHome));
		Gate.setPluginsHome(new File(gatePluginHome));
		try {
			Debug.into(Gate.class, "init");
			Gate.init();
			Debug.out(Gate.class, "init");
			for (String s : gateCreoleDir) {
				Debug.println(3, "Registering Creole Directory : " + s);
				Gate.getCreoleRegister().registerDirectories(new URL(s));
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		Debug.out(this, "InitGate");
	}
	
	public Datastore getDatastore() {
		Debug.into(this, "getDatastore");
		if (ds == null) {
			Debug.println(3, "lazy load - build new datastore instance.");
			if (jdbcConf == null) {
				Debug.println(3, "Using default datastore");
				return new JDBCimpl("jdbc:mysql://localhost:3306/gate", "root", com.silverwzw.gate.datastore.GitIgnore.mySQLpasswd());
			}
			
			File f;
			Debug.println(3, "Reading JDBC configure file");
			
			f = new File(jdbcConf);
			
			JSON json = null;
			
			try {
				json = JSON.parse(f);
			} catch (JSON.JsonStringFormatException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			
			Debug.println(3, "The JDBC JSON object read in is:\n " + json.format());
			
			ds = new JDBCimpl((String)json.get("jdbc").toObject(), (String)json.get("user").toObject(), (String)json.get("passwd").toObject());
		} else {
			Debug.println(3, "lazy load - re-use datastore instance.");
		}
		Debug.out(this, "getDatastore");
		return ds;
	}
	
	final public Corpus getCorpus() throws ResourceInstantiationException {
		String name;
		Calendar cal;
		
		cal = Calendar.getInstance();
		name = "Corpus " + cal.get(Calendar.YEAR);
		name += cal.get(Calendar.MONTH) < 9 ? "0" + (1 + cal.get(Calendar.MONTH)) : "" + (1 + cal.get(Calendar.MONTH));
		name += cal.get(Calendar.DAY_OF_MONTH) < 10 ? "0" + cal.get(Calendar.DAY_OF_MONTH) : "" + cal.get(Calendar.DAY_OF_MONTH);
		name += cal.get(Calendar.HOUR_OF_DAY) < 10 ? "0" + cal.get(Calendar.HOUR_OF_DAY) : "" + cal.get(Calendar.HOUR_OF_DAY);
		name += cal.get(Calendar.MINUTE) < 10 ? "0" + cal.get(Calendar.MINUTE) : "" + cal.get(Calendar.MINUTE);
		name += cal.get(Calendar.SECOND) < 10 ? "0" + cal.get(Calendar.SECOND) : "" + cal.get(Calendar.SECOND);
		return getCorpus(name);
	}
	
	public Corpus getCorpus(String corpusName) throws ResourceInstantiationException {
		Debug.into(this, "getCorpus");
		
		if (!Gate.isInitialised()) {
			Debug.info("Gate not initialised, try to initialise it");
			InitGate();
		}
		
		if (corpus == null) {
			Debug.println(3, "lazy load - build new corpus instance.");
			corpus = Factory.newCorpus(corpusName);
			
			List<URL> urlList;
			urlList = new LinkedList<URL>();
			
			for(String docDes : docConf) {
				Debug.println(3, "Reading doc description file: " + docDes);
				
				// parse the des json file
				JSON json;
				try {
					json = JSON.parse(new File(docDes));
				} catch (JSON.JsonStringFormatException e) {
					throw new RuntimeException(e);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}

				Debug.println(3, "The doc description file read in is:\n" + json.format());

				// process local documents
				Debug.println(3, "process local file/directory list to URL list");
				JSON local = json.get("local");
				if (local != null) {
					for (Entry<String, JSON> e : local) {
						addLocal(urlList, new File((String) e.getValue().toObject()));
					}
				}

				// process web documents
				Debug.println(3, "process web URL list");
				JSON web = json.get("web");
				if (web != null) {
					for (Entry<String, JSON> e : web) {
						String urlStr;
						urlStr = (String) e.getValue().toObject();
						Debug.println(3, "found web doc " + urlStr);
						try {
							urlList.add(new URL(urlStr));
						} catch (MalformedURLException ex) {
							throw new RuntimeException(ex);
						}
					}
				}
			}
			
			for (URL u : urlList) {
				Document doc = new DocumentImpl();
				Debug.println(3, "change url to gate.Document : " + u);
	    		doc.setSourceUrl(u);
	    		doc.init();
				Debug.println(3, "add document to Corpus: " + corpus.getName());
	    		corpus.add(doc);
			}
			
		} else {
			Debug.println(3, "lazy load - re-use corpus instance.");
		}

		Debug.out(this, "getCorpus");
		return corpus;
	}
	
	final private void addLocal(List<URL> ul, File f) {
		if (f.isFile()) {
			Debug.println(3, "found file " + f.getAbsolutePath());
			try {
				ul.add(new URL("file:///" + f.getAbsolutePath()));
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}
		
		if (f.isDirectory()) {
			Debug.println(3, "search directory " + f.getAbsolutePath());
			for(File fChild : f.listFiles()) {
				addLocal(ul, fChild);
			}
		}
	}
	
	public List<Task> getTaskList() {
		Debug.into(this, "getTaskList");
		
		if (!Gate.isInitialised()) {
			Debug.info("Gate not initialised, try to initialise it");
			InitGate();
		}
		
		if (tasks == null) {
			Debug.println(3, "lazy load - build new tasks instance.");
			tasks = new LinkedList<Task>();
			for (String confFilePath : conf) {
				JSON json = null;
				Debug.println(3, "Reading task configration file: " + confFilePath);
				try {
					json = JSON.parse(new File(confFilePath));
				} catch (JsonStringFormatException e) {
					throw new RuntimeException(e);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				Debug.println(3, "The task configration file read in is:\n " + json.format());
				tasks.add(buildTask(json));
			}
		} else {
			Debug.println(3, "lazy load - re-use tasks instance.");
		}
		Debug.out(this, "getTaskList");
		return tasks;
	}
	
	private Task buildTask(JSON json) {
		String name;
		
		name = (String) json.get("name").toObject();
		
		Debug.println(2, "Building task " + (String)json.get("name").toObject());
		
		return new Task(name, buildController(json), FilterFactory.build(json));
	}
	
	private CorpusController buildController (JSON json) {
		Debug.println(2, "Building controller of task " + (String)json.get("name").toObject());
		
		SerialAnalyserController cc;
		
		if (json.get("template") != null) {
			String path;
			path = (String) json.get("template").toObject();
			Debug.println(3, "Loading template from file " + path);
			try {
				cc = (SerialAnalyserController) PersistenceManager.loadObjectFromFile(new File(path));
			} catch (GateException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else {
			Debug.println(3, "no template - creating empty controller");
			try {
				cc = (SerialAnalyserController) Factory.createResource("gate.creole.SerialAnalyserController");
			} catch (GateException e) {
				throw new RuntimeException(e);
			}
		}
		
		JSON decorators = json.get("decorator");
		if (decorators != null) {
		
			for (Entry<String, JSON> e : decorators) {
				
				JSON decorator = e.getValue();
				String prPath = (String) decorator.get("path").toObject();
				ProcessingResource pr;
				
				Debug.println(3, "Building PR  " + prPath);
				if (decorator.get("featureMap") != null) {
					FeatureMap fm = new SimpleFeatureMapImpl();
					Debug.println(3, "Building FeatureMap of " + prPath);
					for (Entry<String,JSON> e2: decorator.get("featureMap")) {
						fm.put(e2.getKey(), e2.getValue().toObject());
					}
					try {
						pr = (ProcessingResource) Factory.createResource(prPath, fm);
					} catch (GateException ex) {
						throw new RuntimeException(ex);
					}
				} else {
					try {
						pr = (ProcessingResource) Factory.createResource(prPath);
					} catch (ResourceInstantiationException ex) {
						throw new RuntimeException(ex);
					}
				}
				
				if (pr instanceof com.ontotext.gate.gazetteer.HashGazetteer) {
					Debug.println(3, "Building Lookups.");
					assert decorator.get("list") != null : "HashGazetteer should have at least one word list";
					
					HashGazetteer hg = (HashGazetteer) pr;
					
					for (Entry<String,JSON> en : decorator.get("list")) {
						Debug.println(3, "Building Lookup " + en.getKey());
						
						FileReader fr = null;
						BufferedReader reader = null;
						Lookup lkup = null;
						
						lkup = new Lookup((String) en.getValue().toObject(), en.getKey(), "", "", "Lookup");
						
						try {
							fr = new FileReader(new File((String)en.getValue().toObject()));
							reader = new BufferedReader(fr);
							
							String word;
							while ((word = reader.readLine()) != null) {
								hg.add(word, lkup);
							}
						} catch (FileNotFoundException ex) {
							throw new RuntimeException(ex);
						} catch (IOException ex) {
							throw new RuntimeException(ex);
						} finally {
							try {
								if (reader != null) {
									reader.close();	
								}
								if (fr != null) {
									fr.close();
								}
							} catch (IOException ex) {
								throw new RuntimeException(ex);
							}
						}
					}
					
				}

				Debug.println(3, "Adding PR " + prPath + " to Controller in task " + (String)json.get("name").toObject());
				cc.add(pr);
			}
		}
		
		Debug.println(2, "Finish building Controller in task " + (String)json.get("name").toObject());
		return cc;
	}
}
