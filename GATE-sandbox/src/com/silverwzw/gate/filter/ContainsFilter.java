package com.silverwzw.gate.filter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import gate.Annotation;

final public class ContainsFilter extends CachedFilter {
	AnnotationFilter parent;
	Set<AnnotationFilter> child;
	public ContainsFilter(AnnotationFilter parent, AnnotationFilter ... child) {
		this.parent = parent;
		this.child = new HashSet<AnnotationFilter>();
		for (AnnotationFilter c : child) {
			c.resetScenario();
			this.child.add(c);
		}
	}
	public synchronized boolean satisfy(Annotation a) {
		if (scenario == null) {
			throw new ScenarioNotSetException();
		}
		for (Annotation a0 : findAll()) {
			if (a0.equals(a)) {
				return true;
			}
		}
		return false;
	}
	public synchronized void buildCache() {
		cache = new TreeSet<Annotation>();
		
		List<Iterator<Annotation>> annotIterList = new ArrayList<Iterator<Annotation>>(child.size());
		List<Annotation> cachedAnnotList = new ArrayList<Annotation>(child.size());
		
		parent.setScenario(scenario);
		
		for (AnnotationFilter c : child) {
			Iterator<Annotation> iter;
			c.setScenario(scenario);
			iter = c.findAll().iterator();
			if (!iter.hasNext()) {
				return;
			}
			cachedAnnotList.add(iter.next());
			annotIterList.add(iter);
		}
		
		for (Annotation p : parent.findAll()) {
			Long start, end;
			boolean pass = true;
			
			start = p.getStartNode().getOffset();
			end = p.getEndNode().getOffset();
			
			for (int i = 0; i < annotIterList.size(); i++) {
				boolean findone;
				Iterator<Annotation> iter = annotIterList.get(i);
				while (true) {
					if (cachedAnnotList.get(i).getEndNode().getOffset() > end){
						findone = false; 
						break;
					}
					if (cachedAnnotList.get(i).getStartNode().getOffset() >= start){ 
						findone = true;
						break;
					}
					
					if (iter.hasNext()) { 
						cachedAnnotList.set(i,iter.next());
					} else {
						findone = false;
						break;
					}
				}
				if (!findone) {
					pass = false;
					break;
				}
			}
			
			if (pass) {
				cache.add(p);
			}
		}
	}
	
	public boolean equals(Object o) {
		if (super.equals(o)) {
			return ((ContainsFilter) o).parent.equals(parent) && ((ContainsFilter) o).child.equals(child) ;
		} else {
			return false;
		}
	}
	public void resetScenario() {
		scenario = null;
		for (AnnotationFilter c : child) {
			c.resetScenario();
		}
		parent.resetScenario();
	}
}
