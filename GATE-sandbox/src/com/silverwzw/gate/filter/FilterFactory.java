package com.silverwzw.gate.filter;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.silverwzw.Debug;
import com.silverwzw.JSON.JSON;

import gate.Annotation;

public class FilterFactory {
	
	static Map<String, Class<?>> subClassRegistor = new HashMap<String, Class<?>>();
	
	static {
		subClassRegistor.put("FeatureMajorType", FeatureMajorTypeFilter.class);
		subClassRegistor.put("Type", TypeFilter.class);
		subClassRegistor.put("Contains", TypeFilter.class);
	}
	
	@SuppressWarnings("serial")
	public static class FeatureMajorTypeFilter extends AnnotationFilter {
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
		static FeatureMajorTypeFilter build(JSON json, FilterBuilder fb) {
			assert "FeatureMajorType".equals((String)json.get("type").toObject());
			return FilterFactory.fMajorType((String)json.get("parameter").toObject());
		}
	}
	@SuppressWarnings("serial")
	public static class TypeFilter extends AnnotationFilter {
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
		static FeatureMajorTypeFilter build(JSON json, FilterBuilder fb) {
			assert "Type".equals((String)json.get("type").toObject());
			return FilterFactory.fMajorType((String)json.get("parameter").toObject());
		}
	}
	final public static ContainsFilter contains(AnnotationFilter parent, AnnotationFilter ... children) {
		return new ContainsFilter(parent,children);
	}
	final public static ContainsFilter contains(AnnotationFilter parent, FilterTree tree, List<AnnotationFilter> children) {
		return new ContainsFilter(parent, tree, children);
	}
	final public static FeatureMajorTypeFilter fMajorType(String m) {
		return new FeatureMajorTypeFilter(m);
	}
	final public static TypeFilter type(String m) {
		return new TypeFilter(m);
	}
	static class FilterBuilder {
		JSON treeList, filterList;
		FilterBuilder(JSON json) {
			treeList = json.get("tree");
			filterList = json.get("filter");
		}
		AnnotationFilter build() {
			return getFilter("root");
		}
		FilterTree getTree(String name) {
			if (name.equals("false")) {
				return FilterTree.FALSE;
			}
			if (name.equals("true")) {
				return FilterTree.TRUE;
			}
			
			JSON j = treeList.get(name);
			
			return new FilterTree(getTree((String) j.get("true").toObject()),getTree((String) j.get("true").toObject()));
		}
		AnnotationFilter getFilter(String name) {
			JSON f = filterList.get(name);
			String filterType;
			
			filterType = (String) f.get("type").toObject();
			Class<?> filterClass = null;
			
			filterClass = subClassRegistor.get(filterType.trim());
			
			assert filterClass != null : "Filter Type Not Registered With FilterFactory";
			
			try {
				return (AnnotationFilter) filterClass.getMethod("build", JSON.class, FilterBuilder.class).invoke(null, f, this);
			} catch (IllegalArgumentException e) {
				Debug.info("IllegalArgumentException, probably: " + filterClass.getName() + ".build(JSON, FilterBuilder) isn't a static method");
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				Debug.info("IllegalAccessException, probably: " + filterClass.getName() + ".build(JSON, FilterBuilder) isn't a public/default method");
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e);
			} catch (SecurityException e) {
				throw new RuntimeException(e);
			} catch (NoSuchMethodException e) {
				Debug.info("NoSuchMethodException, probably: " + filterClass.getName() + ".build(JSON, FilterBuilder) method isn't defined");
				throw new RuntimeException(e);
			} catch (ClassCastException e) {
				Debug.info("ClassCastException, probably: " + filterClass.getName() + ".build(JSON, FilterBuilder) method does not return an AnnotationFilter");
				throw e;
			}
		}
	}
	final public static AnnotationFilter build(JSON json) {
		Debug.println(3, "building filter of task " + (String) json.get("name").toObject());
		return (new FilterBuilder(json)).build();
	}
	final public static void register(String name, Class<?> c) {
		subClassRegistor.put(name, c);
	}
}
