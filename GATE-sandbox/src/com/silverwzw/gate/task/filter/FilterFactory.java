package com.silverwzw.gate.task.filter;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import com.silverwzw.Debug;
import com.silverwzw.JSON.JSON;
import com.silverwzw.JSON.JSON.JsonStringFormatException;

public class FilterFactory {
	
	static Map<String, Class<?>> subClassRegistor = new HashMap<String, Class<?>>();
	
	static {
		register("FeatureMajorType", FeatureMajorTypeFilter.class);
		register("Type", TypeFilter.class);
		register("Contains", ContainsFilter.class);
		register("Weighted", WeightedFilter.class);
	}
	static class FilterBuilder {
		JSON treeList, filterList;
		FilterBuilder(JSON json) {
			treeList = json.get("TREE");
			filterList = json;
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
			
			return ret;
		}
	}
	final public static AnnotationFilter build(String filterJson) {
		try {
			return (new FilterBuilder(JSON.parse(filterJson))).getFilter("ROOT");
		} catch (JsonStringFormatException e) {
			throw new RuntimeException(e);
		}
	}
	final public static void register(String name, Class<?> c) {
		subClassRegistor.put(name, c);
	}
}
