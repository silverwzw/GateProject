package com.silverwzw.gate.task.filter;

import com.silverwzw.Debug;
import com.silverwzw.JSON.JSON;
import com.silverwzw.gate.task.filter.FilterFactory.FilterBuilder;

import gate.Annotation;

@SuppressWarnings("serial")
public abstract class AttributeFilter extends AnnotationFilter {}

@SuppressWarnings("serial")
class FeatureMajorTypeFilter extends AttributeFilter {
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
	public static FeatureMajorTypeFilter build(JSON json, FilterBuilder fb) {
		
		Debug.into(FeatureMajorTypeFilter.class,"build");
		
		assert "FeatureMajorType".equals((String)json.get("type").toObject());
		
		FeatureMajorTypeFilter f;
		f =  new FeatureMajorTypeFilter((String)json.get("parameter").toObject());

		Debug.out(FeatureMajorTypeFilter.class,"build");
		
		return f;
	}
	public String toString() {
		return "{<FeatureMajorType>: arg=" + type +"}";
	}
}

@SuppressWarnings("serial")
class TypeFilter extends AttributeFilter {
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
	public static TypeFilter build(JSON json, FilterBuilder fb) {
		Debug.into(TypeFilter.class,"build");
		
		assert "Type".equals((String)json.get("type").toObject());
		
		JSON j = json.get("parameter");
		String t = (String) j.toObject();
		TypeFilter f = new TypeFilter(t);
		
		Debug.out(TypeFilter.class,"build");
		
		return f;
	}
	public String toString() {
		return "{<Type>: arg=" + type +"}";
	}
}

@SuppressWarnings("serial")
class ParagraphFilter extends AttributeFilter {
	private static ParagraphFilter instance;
	static {
		instance = new ParagraphFilter();
	}
	public boolean satisfy(Annotation a) {
		return a.getType().equals("p") || a.getType().equals("paragraph");
	}
	public ParagraphFilter clone() {
		return instance;
	}
	public static ParagraphFilter build(JSON json, FilterBuilder fb) {
		Debug.into(ParagraphFilter.class,"build");
		
		assert "Paragraph".equals((String)json.get("type").toObject());
		
		Debug.out(ParagraphFilter.class,"build");
		
		return instance;
	}
	public String toString() {
		return "{<Paragraph>}";
	}
}