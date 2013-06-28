package com.silverwzw.gate.filter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import gate.Annotation;

@SuppressWarnings("serial")
final public class ContainsFilter extends CachedFilter {
	AnnotationFilter parent;
	List<AnnotationFilter> children;
	TruthTableTree ttt;
	public ContainsFilter(AnnotationFilter parent, TruthTableTree ttt, List<AnnotationFilter> children) {
		Constructor(parent, ttt, children);
	}
	public ContainsFilter(AnnotationFilter parent, AnnotationFilter ... children) {
		TruthTableTree ttt;
		List<AnnotationFilter> laf;
		
		ttt = TruthTableTree.TRUE;
		laf = new LinkedList<AnnotationFilter>();
		
		for (AnnotationFilter child : children) {
			ttt = new TruthTableTree(ttt, TruthTableTree.FALSE);
			laf.add(child);
		}
		Constructor(parent, ttt, laf);
	}
	private void Constructor(AnnotationFilter p, TruthTableTree t, List<AnnotationFilter> l) {
		parent = p;
		children =  new LinkedList<AnnotationFilter>();
		ttt = t.clone();  
		for (AnnotationFilter c : l) {
			c.resetScenario();
			children.add(c);
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
		cache = new TreeSet<Annotation>(new AbstractFilter.AnnotationComparatorByStartNode());
		
		List<List<Annotation>> childFilterAnnotListList = new ArrayList<List<Annotation>>(children.size());
		List<Integer> currentCur = new ArrayList<Integer>(children.size());
		
		parent.setScenario(scenario);
		
		for (AnnotationFilter c : children) {
			c.setScenario(scenario);
			Set<Annotation> as;
			as = c.findAll();
			List<Annotation> annots = new ArrayList<Annotation>(as.size());
			for (Annotation a : as) {
				annots.add(a);
			}
			childFilterAnnotListList.add(annots);
			currentCur.add((Integer) 0);
		}
		
		for (Annotation p : parent.findAll()) {
			Long start, end;
			TruthTableTree tree = ttt;
			
			start = p.getStartNode().getOffset();
			end = p.getEndNode().getOffset();
			
			for (int i = 0; i < childFilterAnnotListList.size(); i++) {
				boolean has = false;
				int cCurCopy;
				List<Annotation> annotListOfChildFilter = childFilterAnnotListList.get(i);
				cCurCopy = currentCur.get(i);
				while (cCurCopy < annotListOfChildFilter.size() && annotListOfChildFilter.get(cCurCopy).getEndNode().getOffset() <= end) {
					if (annotListOfChildFilter.get(cCurCopy).getStartNode().getOffset() >= start) {
						has = true;
						cCurCopy ++;
						break;
					}
					cCurCopy ++;
				}
				currentCur.set(i, cCurCopy);
				tree = tree.subtree(has);
				if (tree.getResult() != null) {
					break;
				}
			}
			
			assert tree.getResult() != null : "assertion failed: Error found in Structure of Truth Table Tree";
			
			if (tree.getResult()) {
				cache.add(p);
			}
		}
	}
	
	public boolean equals(Object o) {
		return (super.equals(o)) && ((ContainsFilter) o).parent.equals(parent) && ((ContainsFilter) o).children.equals(children) && ttt.equals(((ContainsFilter) o).ttt);
	}
	public void resetScenario() {
		scenario = null;
		for (AnnotationFilter c : children) {
			c.resetScenario();
		}
		parent.resetScenario();
	}
	public ContainsFilter clone() {
		List<AnnotationFilter> childrenClone;
		childrenClone = new LinkedList<AnnotationFilter>();
		for (AnnotationFilter af : children) {
			childrenClone.add(af.clone());
		}
		return new ContainsFilter(parent.clone(), ttt.clone(), childrenClone);
	}
}
