package com.silverwzw.gate.filter;

import gate.Annotation;

import java.io.Serializable;
import java.util.Set;

public interface AnnotationFilter extends Cloneable, Serializable{
	public Set<Annotation> findAll();
	public boolean satisfy(Annotation a);
	public void setScenario(Set<Annotation> as);
	public void resetScenario();
	public AnnotationFilter clone();
}