package com.silvrewzw.gate.task;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import gate.CorpusController;
import gate.Factory;
import gate.FeatureMap;
import gate.ProcessingResource;
import gate.creole.ResourceInstantiationException;
import gate.creole.SerialAnalyserController;
import gate.creole.gazetteer.Lookup;
import gate.util.GateException;
import gate.util.SimpleFeatureMapImpl;
import gate.util.persistence.PersistenceManager;

import com.ontotext.gate.gazetteer.HashGazetteer;
import com.silverwzw.Debug;
import com.silverwzw.JSON.JSON;
import com.silverwzw.JSON.JSON.JsonStringFormatException;
import com.silverwzw.gate.task.filter.AnnotationFilter;
import com.silverwzw.gate.task.filter.FilterFactory;

public class Task {
	private String n;
	private CorpusController g;
	private AnnotationFilter f;
	public Task(String taskJson) {
		Debug.into(this, "<Contructor>");
		
		String name;
		JSON json;
		List<String> decList;
		
		try {
			json = JSON.parse(taskJson);
		} catch (JsonStringFormatException e) {
			Debug.info("Exception parsing JSON : " + taskJson);
			throw new RuntimeException(e);
		}
		
		decList = new LinkedList<String>();
		name = (String) json.get("name").toObject();
		
		Debug.println(2, "Building task " + name);
		
		n = name;
		
		if (json.get("decorators") != null) {
			for (Entry<String, JSON> dec : json.get("decorators")) {
				decList.add(dec.getValue().toString());
			}
		}
		
		
		JSON template;
		String templateString;
		template = json.get("template");
		
		if (template == null) {
			templateString = "";
		} else {
			templateString = (String) template.toObject();
		}

		Debug.println(2, "Building controller of task " + name);
		g = buildController(templateString, decList);
		Debug.println(2, "Finish building Controller in task " + name);
		
		Debug.println(3, "building ROOT filter of task '" + name + '\'');
		f = FilterFactory.build(json.get("filter").toString());
		Debug.println(3, "ROOT filter of task '" + name + "' is:\n" + f.toString());
		
		Debug.out(this, "<Contructor>");
	}
	
	private CorpusController buildController (String templateString, List<String> decoratorList) {
		Debug.into(this, "buildController");
		
		SerialAnalyserController cc;
		
		if (templateString != null && !"".equals(templateString)) {
			
			Debug.println(3, "Loading template from file " + templateString);
			
			try {
				cc = (SerialAnalyserController) PersistenceManager.loadObjectFromFile(new File(templateString));
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
		
		if (decoratorList.size() > 0) {
			for (String decJsonString : decoratorList) {
				JSON decorator;
				
				try {
					decorator= JSON.parse(decJsonString);
				} catch (JsonStringFormatException e) {
					throw new RuntimeException(e);
				}

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

				Debug.println(3, "Adding PR " + prPath + " to Controller in task '" + n + '\'');
				cc.add(pr);
			}
			
		}

		Debug.out(this, "buildController");
		return cc;
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