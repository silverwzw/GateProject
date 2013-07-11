package com.silverwzw.gate.manager;

import gate.Annotation;
import gate.Corpus;
import gate.Document;
import gate.Factory;
import gate.Gate;
import gate.corpora.DocumentImpl;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.util.GateException;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.silverwzw.Debug;
import com.silverwzw.JSON.JSON;
import com.silverwzw.gate.datastore.DatastoreRouter;
import com.silverwzw.gate.task.Task;
import com.silverwzw.gate.task.filter.AnnotationFilter;

final public class GateProjectManager implements Runnable {
	
	private String gateHome = null;
	private String gatePluginHome = null;
	private Collection<URL> gateCreoleDir = null;
	private int debugLevel = 0;
	private DatastoreRouter dr;
	private List<String> taskName;
	private String doclistJsonStr;
	
	
	public GateProjectManager(DatastoreRouter dr, List<String> taskName, String doclistJsonStr){
		this.dr = dr;
		this.taskName = taskName;
		this.doclistJsonStr = doclistJsonStr;
	}
	public void setGate(String home, String plugin, Collection<URL> gateCreoleDir){
		gateHome = home;
		gatePluginHome = plugin;
		this.gateCreoleDir = gateCreoleDir;
	}
	public void setDebug(int i) {
		debugLevel = i;
	}
	public void InitGate() {
		Debug.into(GateProjectManager.class, "InitGate");
		if (gateHome != null) {
			Gate.setGateHome(new File(gateHome));
		}
		if (gatePluginHome != null) {
			Gate.setPluginsHome(new File(gatePluginHome));
		}
		try {
			Debug.into(Gate.class, "init");
			Gate.init();
			Debug.out(Gate.class, "init");
			for (URL url : gateCreoleDir) {
				Debug.println(3, "Registering Creole Directory : " + url.toString());
				Gate.getCreoleRegister().registerDirectories(url);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		try {
			Gate.getCreoleRegister().registerDirectories((new File("E:/Steven/Life/NCSU/Research/gate/plugins/ANNIE")).toURI().toURL());
		} catch (MalformedURLException e) {
			System.err.println("Warning! Error trying to load default ANNIE Plugin!");
		} catch (GateException e) {
			System.err.println("Warning! Error trying to load default ANNIE Plugin!");
		}
		Debug.out(GateProjectManager.class, "InitGate");
	}
	public void run() {
		Debug.set(debugLevel);
		Debug.info("new Thread '" + Thread.currentThread().getName() + "' is running.");
		InitGate();
		dr.reConnectCenter();
		
		Collection<Task> taskCollection;
		taskCollection = new LinkedList<Task>();
		for (String taskN : taskName) {
			Debug.println(3, "Building Task '" + taskN + "'.");
			taskCollection.add(new Task(dr.getTask(taskN)));
		}
		process(taskCollection, createInitCorpus());
		Debug.info("Thread '" + Thread.currentThread().getName() + "' stopped.");
	}
	final private Corpus createInitCorpus() {
		Debug.into(this, "createInitCorpus");
		JSON json;
		Collection<URL> localUrlList,webUrlList;
		Corpus corpus;

		localUrlList = new LinkedList<URL>();
		webUrlList = new LinkedList<URL>();
		
		try {
			json = JSON.parse(doclistJsonStr);
		} catch (JSON.JsonStringFormatException e) {
			throw new RuntimeException(e);
		}
		
		// process local documents
		Debug.println(3, "process local file/directory list to URL list");
		JSON local = json.get("local");
		if (local != null) {
			for (Entry<String, JSON> e : local) {
				addLocal(localUrlList, new File((String) e.getValue().toObject()));
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
					webUrlList.add(new URL(urlStr));
				} catch (MalformedURLException ex) {
					throw new RuntimeException(ex);
				}
			}
		}
		
		try {
			corpus =  Factory.newCorpus("init Corpus");
			for (URL u : localUrlList) {
				Document doc = new DocumentImpl();
				Debug.println(3, "change local url to gate.Document : " + u);
	    		doc.setSourceUrl(u);
	    		doc.init();
				Debug.println(3, "add document to Corpus: " + corpus.getName());
	    		corpus.add(doc);
			}
			for (URL u : webUrlList) {
				Document doc = new DocumentImpl();
				Debug.println(3, "change web url to gate.Document : " + u);
	    		doc.setSourceUrl(u);
	    		doc.setMarkupAware(true);
	    		doc.init();
				Debug.println(3, "add document to Corpus: " + corpus.getName());
	    		corpus.add(doc);
			}
		} catch (ResourceInstantiationException e) {
			throw new RuntimeException(e);
		}
		
		Debug.out(this, "createInitCorpus");
		return corpus;
	}
	
	final private void addLocal(Collection<URL> urlList, File f) {
		if (f.isFile()) {
			Debug.println(3, "found file " + f.getAbsolutePath());
			try {
				urlList.add(new URL("file:///" + f.getAbsolutePath()));
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}
		
		if (f.isDirectory()) {
			Debug.println(3, "search directory " + f.getAbsolutePath());
			for(File fChild : f.listFiles()) {
				addLocal(urlList, fChild);
			}
		}
	}
	
	final private void process(Collection<Task> taskCollection, Corpus corpus) {
		Debug.into(GateProjectManager.class, "process");
		for (Task task : taskCollection) {
			Debug.info("Executing task '" + task.getName() + "'.");
			task.getController().setCorpus(corpus);
			try {
				task.getController().execute();
			} catch (ExecutionException e) {
				throw new RuntimeException(e);
			}
		}
		

		for (Document doc : corpus) {
			String url, cache;
			SortedSet<Annotation> allTaskResult, taskResult;
			
			url = doc.getSourceUrl().toString();
			allTaskResult = new TreeSet<Annotation>(new AnnotationFilter.AnnotationComparatorByStartNode());
			
			for (Task task : taskCollection) {
				Set<Annotation> allAnnotSet;
				allAnnotSet = new HashSet<Annotation>();
				
				for (String setNames : doc.getAnnotationSetNames()) {
					allAnnotSet.addAll(doc.getAnnotations(setNames));
				}
				allAnnotSet.addAll(doc.getAnnotations());
				
				task.getFilter().resetScenario();
				task.getFilter().setScenario(allAnnotSet);
				Debug.info("Saving indexes of task '" + task.getName() + "' on Document '" + url + "'.");
				taskResult = task.getFilter().findAll();
				dr.saveIndex(task, url, taskResult);
				allTaskResult.addAll(taskResult);
			}
			
			cache = cache(doc.getContent().toString(), allTaskResult);
			if (cache != "" && !cache.equals("")) {
				dr.saveCache(doc.getSourceUrl().toString(), cache);
			}
		}
		
		Debug.out(GateProjectManager.class, "process");
	}
	
	private String cache(String doc, SortedSet<Annotation> al) {
		SortedSet<Annotation> ssa;
		String retVal;
		long start,end;
		
		start = 0;
		end = 0;
		retVal = "";
		ssa = new TreeSet<Annotation>(new AnnotationFilter.AnnotationComparatorByStartNode());
		ssa.addAll(al);
		
		for (Annotation a : al) {
			long soa, eoa;
			soa = a.getStartNode().getOffset();
			eoa = a.getEndNode().getOffset();
			if (soa > end) {
				retVal += doc.substring((int)start, (int)end) + "\n\n";
				start = soa;
				end = eoa;
			} else if (end < eoa) {
				end = eoa ;
			}
		}
		return retVal + doc.substring((int)start, (int)end);
	}
	
	private void printAnnotSet(Set<Annotation> as) {
		for (Annotation a : as) {
			if (!a.getType().equals("Lookup") && !a.getType().equals("Token") && !a.getType().equals("SpaceToken") && !a.getType().equals("Split") && !a.getType().equals("Sentence")) {
				Debug.info(a.toString());
			}
		}
	}
}
