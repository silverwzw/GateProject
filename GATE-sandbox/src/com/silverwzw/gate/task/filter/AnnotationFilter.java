package com.silverwzw.gate.task.filter;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;


import gate.Annotation;

@SuppressWarnings("serial")
public abstract class AnnotationFilter implements Cloneable, Serializable {
	protected transient SortedSet<Annotation> scenario;

	public static class AnnotationComparatorByStartNode implements Comparator<Annotation> {
		public int compare(Annotation annot, Annotation o) {
			long s;
			s = annot.getStartNode().getOffset() - o.getStartNode().getOffset();
			if (s == 0) {
				long e;
				e = annot.getEndNode().getOffset() - o.getEndNode().getOffset();
				if (e == 0) {
					return 0;
				}
				return e>0?1:-1;
			}
			return s>0?1:-1;
		}
	}
	protected static class ScenarioAlreadySetException extends RuntimeException {
		ScenarioAlreadySetException() {super();}
		ScenarioAlreadySetException(Exception e) {super(e);}
		ScenarioAlreadySetException(String s) {super(s);}
		ScenarioAlreadySetException(String s, Exception e) {super(s,e);}
	}
	protected static class ScenarioNotSetException extends RuntimeException {
		ScenarioNotSetException() {super();}
		ScenarioNotSetException(Exception e) {super(e);}
		ScenarioNotSetException(String s) {super(s);}
		ScenarioNotSetException(String s, Exception e) {super(s,e);}
	}
	public abstract boolean satisfy(Annotation a);
	public void setScenario(Set<Annotation> s) {
		if (scenario != null) {
			throw new ScenarioAlreadySetException();
		}
		if (s == null) {
			throw new NullPointerException();
		}
		scenario = new TreeSet<Annotation>(new AnnotationComparatorByStartNode());
		scenario.addAll(s);
	}
	public SortedSet<Annotation> findAll() {
		TreeSet<Annotation> result;
		if (scenario == null) {
			throw new ScenarioNotSetException();
		}
		result = new TreeSet<Annotation>(new AnnotationComparatorByStartNode()); 
		for (Annotation a : scenario) {
			if (satisfy(a)) {
				result.add(a);
			}
		}
		return result;
	}
	public boolean equals(Object o) {
		return o.getClass().equals(this.getClass());
	}
	public void resetScenario() {
		scenario = null;
	}
	public abstract AnnotationFilter clone();
	public abstract String toString();
}
