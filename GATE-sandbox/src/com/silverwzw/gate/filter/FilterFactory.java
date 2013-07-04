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
		subClassRegistor.put("Contains", ContainsFilter.class);
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
		FilterTree getTree(String name) {
			if (name.equals("FALSE")) {
				return FilterTree.FALSE;
			}
			if (name.equals("TRUE")) {
				return FilterTree.TRUE;
			}
			
			JSON j = treeList.get(name);
			
			return new FilterTree(getTree((String) j.get("true").toObject()),getTree((String) j.get("false").toObject()));
		}
		AnnotationFilter getFilter(String name) {
			JSON f = filterList.get(name);
			String filterType;
			AnnotationFilter ret;
			
			filterType = (String) f.get("type").toObject();
			Class<?> filterClass = null;

			Debug.println(3, "Building filter '" + name + "' :" + f.format());
			
			filterClass = subClassRegistor.get(filterType.trim());
			
			assert filterClass != null : "Filter Type Not Registered With FilterFactory";
			
			try {
				ret = (AnnotationFilter) filterClass.getMethod("build", JSON.class, FilterBuilder.class).invoke(null, f, this);
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
			
			ret.setName(name);
			return ret;
		}
	}
	final public static AnnotationFilter build(JSON json) {
		String tname;
		tname = (String) json.get("name").toObject();
		Debug.println(3, "building root filter of task '" + tname + '\'');
		AnnotationFilter f;
		f = (new FilterBuilder(json)).getFilter("ROOT");
		Debug.println(3, "root filter of task '" + tname + "' is:\n" + f.toString());
		return f;
	}
	final public static void register(String name, Class<?> c) {
		subClassRegistor.put(name, c);
	}
}
