package com.silverwzw.gate.filter;

import gate.Annotation;

public class FilterFactory {
	@SuppressWarnings("serial")
	public static class FeatureMajorTypeFilter extends AbstractFilter {
		String type;
		FeatureMajorTypeFilter(String type) {
			this.type = new String(type);
		}
		public boolean satisfy(Annotation a) {
			if (a.getFeatures() == null || a.getFeatures().get("majorType") == null) {
				return false;
			}
			return a.getFeatures().get("majorType").equals(type);
		}
		public FeatureMajorTypeFilter clone() {
			return new FeatureMajorTypeFilter(type);
		}
	}
	@SuppressWarnings("serial")
	public static class TypeFilter extends AbstractFilter {
		String type;
		TypeFilter(String type) {
			this.type = new String(type);
		}
		public boolean satisfy(Annotation a) {
			return a.getType().equals(type);
		}
		public TypeFilter clone() {
			return new TypeFilter(type);
		}
	}
	public static ContainsFilter contains(AnnotationFilter parent, AnnotationFilter ... children) {
		return new ContainsFilter(parent,children);
	}
	public static FeatureMajorTypeFilter fMajorType(String m) {
		return new FeatureMajorTypeFilter(m);
	}
	public static TypeFilter type(String m) {
		return new TypeFilter(m);
	}
}
