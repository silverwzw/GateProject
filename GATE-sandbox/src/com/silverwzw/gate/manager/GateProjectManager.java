package com.silverwzw.gate.manager;

import gate.Gate;
import gate.util.GateException;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import com.silverwzw.Debug;
import com.silverwzw.gate.datastore.DatastoreRouter;

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
		Debug.info("new Thread '" + this.hashCode() + "' is running.");
		InitGate();
		dr.reConnectCenter();
		Debug.info("Thread '" + this.hashCode() + "' stopped.");
	};
}
