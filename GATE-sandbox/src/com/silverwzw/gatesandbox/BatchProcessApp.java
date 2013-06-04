/*
 *  BatchProcessApp.java
 *
 *
 * Copyright (c) 2006, The University of Sheffield.
 *
 * This file is part of GATE (see http://gate.ac.uk/), and is free
 * software, licenced under the GNU Library General Public License,
 * Version 2, June1991.
 *
 * A copy of this licence is included in the distribution in the file
 * licence.html, and is also available at http://gate.ac.uk/gate/licence.html.
 *
 *  Ian Roberts, March 2006
 *
 *  $Id: BatchProcessApp.java,v 1.5 2006/06/11 19:17:57 ian Exp $
 */
package com.silverwzw.gatesandbox;

import gate.Annotation;
import gate.Document;
import gate.Corpus;
import gate.CorpusController;
import gate.AnnotationSet;
import gate.Gate;
import gate.Factory;
import gate.creole.ResourceInstantiationException;
import gate.creole.SerialAnalyserController;
import gate.creole.SerialController;
import gate.creole.gazetteer.Gazetteer;
import gate.util.persistence.PersistenceManager;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import java.io.File;

public class BatchProcessApp {
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
			
    parseCommandLine(args);

    Gate.init();

    CorpusController application =
      (CorpusController) PersistenceManager.loadObjectFromFile(gappFile);

    Corpus corpus = Factory.newCorpus("BatchProcessApp Corpus");
    application.setCorpus(corpus);

    for(int i = firstFile; i < args.length; i++) {
      File docFile = new File(args[i]);
      System.out.print("Processing document " + docFile + "...");
	  Document doc = Factory.newDocument(docFile.toURL(), encoding);
      
      corpus.add(doc);
      application.execute();
      corpus.clear();

      
      AnnotationSet defaultAnnotset = doc.getAnnotations();
      System.out.println(defaultAnnotset.size());
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
      
      Factory.deleteResource(doc);



      System.out.println("done");
    }
    
    System.out.println("All done");
  }


  /**
   * Parse command line options.
   */
  private static void parseCommandLine(String[] args) throws Exception {
    int i;
    // iterate over all options (arguments starting with '-')
    for(i = 0; i < args.length && args[i].charAt(0) == '-'; i++) {
      switch(args[i].charAt(1)) {
        // -a type = write out annotations of type a.
        case 'a':
          if(annotTypesToWrite == null) annotTypesToWrite = new ArrayList();
          annotTypesToWrite.add(args[++i]);
          break;

        // -g gappFile = path to the saved application
        case 'g':
          gappFile = new File(args[++i]);
          break;

        // -e encoding = character encoding for documents
        case 'e':
          encoding = args[++i];
          break;

        default:
          System.err.println("Unrecognised option " + args[i]);
          usage();
      }
    }

    // set index of the first non-option argument, which we take as the first
    // file to process
    firstFile = i;

    // sanity check other arguments
    if(gappFile == null) {
      System.err.println("No .gapp file specified");
      usage();
    }
  }

  /**
   * Print a usage message and exit.
   */
  private static final void usage() {
    System.err.println(
   "Usage:\n" +
   "   java sheffield.examples.BatchProcessApp -g <gappFile> [-e encoding]\n" +
   "            [-a annotType] [-a annotType] file1 file2 ... fileN\n" +
   "\n" +
   "-g gappFile : (required) the path to the saved application state we are\n" +
   "              to run over the given documents.  This application must be\n" +
   "              a \"corpus pipeline\" or a \"conditional corpus pipeline\".\n" +
   "\n" + 
   "-e encoding : (optional) the character encoding of the source documents.\n" +
   "              If not specified, the platform default encoding (currently\n" +
   "              \"" + System.getProperty("file.encoding") + "\") is assumed.\n" +
   "\n" + 
   "-a type     : (optional) write out just the annotations of this type as\n" +
   "              inline XML tags.  Multiple -a options are allowed, and\n" +
   "              annotations of all the specified types will be output.\n" +
   "              This is the equivalent of \"save preserving format\" in the\n" +
   "              GATE GUI.  If no -a option is given the whole of each\n" +
   "              processed document will be output as GateXML (the equivalent\n" +
   "              of \"save as XML\")."
   );

    System.exit(1);
  }

  /** Index of the first non-option argument on the command line. */
  private static int firstFile = 0;

  /** Path to the saved application file. */
  private static File gappFile = null;

  /** 
   * List of annotation types to write out.  If null, write everything as
   * GateXML.
   */
  private static List annotTypesToWrite = null;

  /**
   * The character encoding to use when loading the docments.  If null, the
   * platform default encoding is used.
   */
  private static String encoding = null;
}
/*
class Helper {
	static SerialController getController() throws MalformedURLException, ResourceInstantiationException{
		SerialController sc;
		sc = new SerialAnalyserController();
		sc.add(getGazetteer());
		return sc;
	}
	static Gazetteer getGazetteer() throws MalformedURLException, ResourceInstantiationException {
		HashGazetteer hg;
		hg = (HashGazetteer) Factory.createResource("com.ontotext.gate.gazetteer.HashGazetteer");;
		hg.setListsURL(new URL("file:///E:/Steven/Life/NCSU/Research/gate/plugins/ANNIE/resources/gazetteer/lists.def"));
		//hg.setEncoding("UTF-8");
		hg.setCaseSensitive(true);
		hg.init();
		return hg;
	}
	
}*/