package com.silverwzw.gate;

import gate.Document;
import gate.Corpus;
import gate.Gate;
import gate.Factory;
import gate.ProcessingResource;
import gate.corpora.DocumentImpl;
import gate.creole.SerialAnalyserController;
import gate.creole.gazetteer.Lookup;

import com.ontotext.gate.gazetteer.HashGazetteer;

import com.silverwzw.gate.datastore.GitIgnore;
import com.silverwzw.gate.datastore.MySQLimpl;
import com.silverwzw.gate.filter.AnnotationFilter;
import com.silverwzw.gate.filter.FilterFactory;
import com.silverwzw.gate.index.AnnotationIndex;

import java.util.LinkedList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;

public class GateProject1 {
	
	boolean debug = false;
	static LinkedList<String> listA;
	static LinkedList<String> listB;
	static File filelist[];
	
	static {
		Gate.setGateHome(new File("E:/Steven/Life/NCSU/Research/gate"));
		Gate.setPluginsHome(new File("E:/Steven/Life/NCSU/Research/gate/plugins"));
		try {
			Gate.init();
			Gate.getCreoleRegister().registerDirectories(new URL("file:///E:/Steven/Life/NCSU/Research/gate/plugins/ANNIE"));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}  
	}

	public static void main(String[] args) throws Exception {
	    
		Corpus corpus;
		ProcessingResource token,sspliter,annieGazetteer;
		HashGazetteer hashGazetteer;
		Lookup listALookup, listBLookup;
		SerialAnalyserController app; 
		
		parse(args);
	    
		corpus = Factory.newCorpus("GateSandbox Corpus");
		
		token = (ProcessingResource) Factory.createResource("gate.creole.tokeniser.DefaultTokeniser");
		sspliter = (ProcessingResource) Factory.createResource("gate.creole.splitter.SentenceSplitter");
		annieGazetteer = (ProcessingResource) Factory.createResource("gate.creole.gazetteer.DefaultGazetteer");
		
		hashGazetteer = (HashGazetteer) Factory.createResource("com.ontotext.gate.gazetteer.HashGazetteer");
		
		app = (SerialAnalyserController) Factory.createResource("gate.creole.SerialAnalyserController");

		listALookup = new Lookup("A.lst","A","","","Lookup");
		listBLookup = new Lookup("B.lst","B","","","Lookup");
      
      
		for (String word : listA) {
    		hashGazetteer.add(word, listALookup);
		}
		for (String word : listB) {
    		hashGazetteer.add(word, listBLookup);
		}
      

    	app.add(token);
    	app.add(sspliter);
    	app.add(hashGazetteer);
    	app.add(annieGazetteer);
    	
    	for (File file : filelist) {
    		Document doc = new DocumentImpl();
    		doc.setSourceUrl(new URL("file:///" + file.getAbsolutePath()));
    		doc.init();
    		corpus.add(doc);
    	}
    	
    	AnnotationIndex annotIndex;
    	AnnotationFilter filter;
    	
    	annotIndex = new AnnotationIndex();
    	filter = FilterFactory.contains(FilterFactory.type("Sentence"), FilterFactory.fMajorType("A"), FilterFactory.fMajorType("B"));
    	
    	annotIndex.buildIndex(app, corpus, filter);
    	annotIndex.saveIndex(new MySQLimpl("jdbc:mysql://localhost:3306/pg_development","root",GitIgnore.mySQLpasswd()), "test");

    	System.out.println(annotIndex);
    
	}
	public static void parse(String ... args) throws IOException {
		BufferedReader reader = null;

		
		assert args.length == 4 && (args[3].equals("paragraph") || args[3].equals("sentance")): "Usage: <list1> <list2> <document directory> [paragraph|sentence]";
		System.out.println("list A				= " + args[0]);
		System.out.println("list B				= " + args[1]);
		System.out.println("document directory	= " + args[2]);
		System.out.println("Mode				= " + args[3]);

		listA = new LinkedList<String>();
		listB = new LinkedList<String>();
	  
		try {
			reader = new BufferedReader(new FileReader(new File(args[0])));
			String word;
			while ((word = reader.readLine()) != null) {
				listA.add(word);
			}
			reader.close();
			reader = new BufferedReader(new FileReader(new File(args[1])));
			while ((word = reader.readLine()) != null) {
				listB.add(word);
			}
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
	  
    	filelist = (new File(args[2])).listFiles();
	}
}
