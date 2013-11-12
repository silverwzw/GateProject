package com.silverwzw.gate.task.filter;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import com.silverwzw.Debug;
import com.silverwzw.JSON.JSON;
import com.silverwzw.gate.task.filter.FilterFactory.FilterBuilder;

import gate.Annotation;

@SuppressWarnings("serial")
public class WeightedFilter extends AnnotationFilter {
	
	protected float threshold = 0;
	protected Evaluator evaluator = null;
	static public interface Evaluator {
		public void init(Set<Annotation> scenario);
		public void reset();
		public float eval(Annotation annot);
	}
	
	WeightedFilter(float threshold, Evaluator evaluator) {
		this.threshold = threshold;
		this.evaluator = evaluator;
	}
	
	public void resetScenario() {
		super.resetScenario();
		evaluator.reset();
	}

	public void setScenario(Set<Annotation> s) {
		super.setScenario(s);
		evaluator.init(s);
	}
	
	public float getThreshold() {
		return threshold;
	}
	
	public float weight(Annotation a) {
		return evaluator.eval(a);
	}
	
	public boolean satisfy(Annotation a) {
		return evaluator.eval(a) >= threshold;
	}

	public WeightedFilter clone() {
		return new WeightedFilter(threshold, evaluator);
	}
	
	public String toString() {
		return "{<Weighted>: threshold=" + threshold + ", evaluator=" + evaluator.getClass().getName() + "}";
	}

	public static WeightedFilter build(JSON json, FilterBuilder fb) {
		Debug.into(WeightedFilter.class,"build");
		assert "Weighted".equals((String)json.get("type").toObject());
		float t;
		Evaluator e = null;
		t = (Float) json.get("threshold").toObject();
		try {
			e = (Evaluator) Class.forName((String) json.get("evaluator").toObject()).getMethod("build", String.class).invoke(null, json.get("parameters"));
		} catch (IllegalArgumentException e1) {
			throw new RuntimeException(e1);
		} catch (SecurityException e1) {
			throw new RuntimeException(e1);
		} catch (IllegalAccessException e1) {
			throw new RuntimeException(e1);
		} catch (InvocationTargetException e1) {
			throw new RuntimeException(e1);
		} catch (NoSuchMethodException e1) {
			throw new RuntimeException(e1);
		} catch (ClassNotFoundException e1) {
			throw new RuntimeException(e1);
		}
		Debug.out(WeightedFilter.class,"build");
		return new WeightedFilter(t, e);
	}
}
