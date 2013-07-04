package com.silverwzw.gate.filter;


@SuppressWarnings("serial")
final public class FilterTree implements Cloneable, java.io.Serializable {
	private Boolean finalResult;
	private FilterTree falseSubtree;
	private FilterTree trueSubtree;
	private transient FilterTree cloneRef;
	final public static FilterTree TRUE = new FilterTree(true);
	final public static FilterTree FALSE = new FilterTree(false);
	
	private FilterTree(boolean b) {
		finalResult = b;
		falseSubtree = trueSubtree = null;
		cloneRef = null;
	}
	public FilterTree(FilterTree tTree, FilterTree fTree) {
		finalResult = null;
		falseSubtree = fTree;
		trueSubtree = tTree;
		cloneRef = null;
	}
	final Boolean getResult() {
		return finalResult;
	}
	final FilterTree subtree(boolean b) {
		if (finalResult == null) {
			return b ? trueSubtree : falseSubtree;
		}
		if (finalResult == (Boolean) true) {
			return TRUE;
		} else {
			return FALSE;
		}
	}
	final public FilterTree clone() {
		FilterTree t;
		clone_clean();
		t = clone_main();
		clone_clean();
		t.clone_clean();
		return t;
	}
	final private void clone_clean() {
		cloneRef = null;
		if (finalResult == null) {
			trueSubtree.clone_clean();
			falseSubtree.clone_clean();
		}
	}
	final private FilterTree clone_main() {
		if (finalResult != null) {
			return this;
		}
		if (cloneRef != null) {
			return cloneRef;
		}
		cloneRef = new FilterTree(trueSubtree.clone(), falseSubtree.clone());
		return cloneRef;
	}
	final public boolean equals(Object o) {
		if (o.getClass() != this.getClass()) {
			return false;
		}
		FilterTree lhs;
		lhs = (FilterTree) o;
		if (finalResult != null && lhs.finalResult != null) {
			return finalResult.equals(lhs);
		} else if (finalResult == null && lhs.finalResult == null) {
			return falseSubtree.equals(lhs.falseSubtree) && trueSubtree.equals(lhs.trueSubtree);
		} else {
			return true;
		}
	}
	final public String toString() {
		String tts,fts;
		FilterTree tt,ft;

		tt = subtree(true);
		ft = subtree(false);
		
		if (((Boolean)true).equals(tt.getResult())) {
			tts = "TRUE";
		} else if (((Boolean)false).equals(tt.getResult())) {
			tts = "FALSE";
		} else {
			tts = tt.toString();
		}
		
		if (((Boolean)true).equals(ft.getResult())) {
			fts = "TRUE";
		} else if (((Boolean)false).equals(ft.getResult())) {
			fts = "FALSE";
		} else {
			fts = ft.toString();
		}
		
		return "{true -> " + tts +", false -> " + fts + "}";
	}
}
