package com.silverwzw.gatesandbox;

import gate.Annotation;
import gate.Document;
import gate.Corpus;
import gate.AnnotationSet;
import gate.FeatureMap;
import gate.Gate;
import gate.Factory;
import gate.ProcessingResource;
import gate.corpora.DocumentContentImpl;
import gate.corpora.DocumentImpl;
import gate.creole.SerialAnalyserController;

import java.util.Comparator;
import java.util.List;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import java.io.File;
import java.net.URL;

public class GateSandBox {
  final static class AnnotationComparator implements Comparator<Annotation>{
	public int compare(Annotation annot, Annotation o) {
		long r;
		r = annot.getStartNode().getOffset() - o.getStartNode().getOffset();
		if (r == 0) {
			return 0;
		}
		return r>0?1:-1;
	}
  }

  public static void main(String[] args) throws Exception {
	  Gate.setGateHome(new File("E:/Steven/Life/NCSU/Research/gate"));
	  Gate.setPluginsHome(new File("E:/Steven/Life/NCSU/Research/gate/plugins"));
	  Gate.init();
	  
	  Gate.getCreoleRegister().registerDirectories(new URL("file:///E:/Steven/Life/NCSU/Research/gate/plugins/ANNIE"));
	  
	  Corpus corpus = Factory.newCorpus("GateSandbox Corpus");
	  
	  Document doc = new DocumentImpl();
	  doc.setContent(new DocumentContentImpl("Jessica is a good girl. Orange is of color orange and it's round. Ballons are of various color and most of them are round. There's a red square on the paper."));
      
	  FeatureMap fm = Factory.newFeatureMap();
	  fm.put("listsURL", "file:/E:/Steven/Life/NCSU/Research/gate/plugins/ANNIE/resources/gazetteer/colorandshape.def");
	  fm.put("encoding", "UTF-8");
	  fm.put("caseSensitive", (Boolean) true);
      ProcessingResource token = (ProcessingResource) Factory.createResource("gate.creole.tokeniser.DefaultTokeniser");
      ProcessingResource sspliter = (ProcessingResource) Factory.createResource("gate.creole.splitter.SentenceSplitter");
      ProcessingResource colorandshape = (ProcessingResource) Factory.createResource("com.ontotext.gate.gazetteer.HashGazetteer", fm);
      
      SerialAnalyserController app = (SerialAnalyserController) Factory.createResource("gate.creole.SerialAnalyserController");

      app.add(token);
      app.add(sspliter);
      app.add(colorandshape);

      app.setCorpus(corpus);
      corpus.add(doc);
      app.execute();
      corpus.clear();

      
      AnnotationSet defaultAnnotset = doc.getAnnotations();
      Iterator<Annotation> annotItr = defaultAnnotset.iterator();
	  
	  SortedSet<Annotation> annotSorted;
	  annotSorted = new TreeSet<Annotation>(new AnnotationComparator());
	  
	  while (annotItr.hasNext()) {
	  	Annotation annot;
	  	String mt;
	   	annot = annotItr.next();
	   	if (annot.getType().equals("Sentence")) {
	   		annotSorted.add(annot);
	   	}
	   	
	   	if (annot.getFeatures() == null || annot.getFeatures().get("majorType") == null) {
	   		continue;
	   	}
	   	mt = annot.getFeatures().get("majorType").toString();
	   	if (mt.equals("color")) {
	   		annotSorted.add(annot);
	   	} else if (mt.equals("shape")) {
	   		annotSorted.add(annot);
	   	}
	  }
	  
	  System.out.println(annotSorted.size());
	  Annotation sentence = null;
	  boolean color=false, shape=false;
	  for (Annotation a : annotSorted) {
		  if (a.getType().equals("Sentence")) {
			  if (sentence != null) {
				  System.out.println("Sentence {" + sentence.getStartNode().getOffset() + "," + sentence.getEndNode().getOffset() + "} => {color:" + color + ", shape:" + shape + "}");
			  }
			  sentence = a;
			  color = false;
			  shape = false;
			  continue;
		  }
		  if (sentence == null) {
			  continue;
		  }
		  if (a.getEndNode().getOffset() > sentence.getEndNode().getOffset()) {
			  System.out.println("Sentence {" + sentence.getStartNode().getOffset() + "," + sentence.getEndNode().getOffset() + "} => {color:" + color + ", shape:" + shape + "}");
			  sentence = null;
			  continue;
		  }
		  String type;
		  type = a.getFeatures().get("majorType").toString();
		  if (type.equals("color")) {
			  color = true;
		  }
		  if (type.equals("shape")) {
			  shape = true;
		  }
	  }
	  if (sentence != null) {
		  System.out.println("Sentence {" + sentence.getStartNode().getOffset() + "," + sentence.getEndNode().getOffset() + "} => {color:" + color + ", shape:" + shape + "}");
	  }
      Factory.deleteResource(doc);



      System.out.println("done");
    
  }

  private static List annotTypesToWrite = null;
  private static String encoding = null;
}