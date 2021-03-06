package com.silverwzw.gate.task.filter;

import java.util.Set;
import java.util.SortedSet;

import gate.Annotation;

@SuppressWarnings("serial")
public abstract class CachedFilter extends AnnotationFilter {
	protected transient SortedSet<Annotation> cache;
	final public synchronized SortedSet<Annotation> findAll() {
		if(cached()) {
			return cache;
		} else {
			buildCache();
			return cache;
		}
	}
	public synchronized void buildCache() {
		cache = super.findAll();
	}
	final protected synchronized boolean cached() {
		return cache != null;
	}
	final public synchronized void setScenario(Set<Annotation> s) {
		cache = null;
		super.setScenario(s);
	}
	final public synchronized void releaseCache() {
		cache = null;
	}
}
