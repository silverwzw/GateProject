package com.silverwzw.gate.filter;

import gate.Annotation;

import java.util.Set;

public interface AnnotationFilter {
	public Set<Annotation> findAll();
	public boolean satisfy(Annotation a);
	public void setScenario(Set<Annotation> as);
	public void resetScenario();
}
