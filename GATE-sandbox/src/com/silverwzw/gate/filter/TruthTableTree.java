package com.silverwzw.gate.filter;


@SuppressWarnings("serial")
public class TruthTableTree implements Cloneable, java.io.Serializable {
	private Boolean finalResult;
	private TruthTableTree falseSubtree;
	private TruthTableTree trueSubtree;
	private transient TruthTableTree cloneRef;
	final public static TruthTableTree TRUE = new TruthTableTree(true);
	final public static TruthTableTree FALSE = new TruthTableTree(false);
	
	private TruthTableTree(boolean b) {
		finalResult = b;
		falseSubtree = trueSubtree = null;
		cloneRef = null;
	}
	public TruthTableTree(TruthTableTree tTree, TruthTableTree fTree) {
		finalResult = null;
		falseSubtree = fTree;
		trueSubtree = tTree;
		cloneRef = null;
	}
	Boolean getResult() {
		return finalResult;
	}
	TruthTableTree subtree(boolean b) {
		if (finalResult == null) {
			return b ? trueSubtree : falseSubtree;
		}
		if (finalResult == (Boolean) true) {
			return TRUE;
		} else {
			return FALSE;
		}
	}
	public TruthTableTree clone() {
		TruthTableTree t;
		clone_clean();
		t = clone_main();
		clone_clean();
		t.clone_clean();
		return t;
	}
	private void clone_clean() {
		cloneRef = null;
		if (finalResult == null) {
			trueSubtree.clone_clean();
			falseSubtree.clone_clean();
		}
	}
	private TruthTableTree clone_main() {
		if (finalResult != null) {
			return this;
		}
		if (cloneRef != null) {
			return cloneRef;
		}
		cloneRef = new TruthTableTree(trueSubtree.clone(), falseSubtree.clone());
		return cloneRef;
	}
	public boolean equals(Object o) {
		if (o.getClass() != this.getClass()) {
			return false;
		}
		TruthTableTree lhs;
		lhs = (TruthTableTree) o;
		if (finalResult != null && lhs.finalResult != null) {
			return finalResult.equals(lhs);
		} else if (finalResult == null && lhs.finalResult == null) {
			return falseSubtree.equals(lhs.falseSubtree) && trueSubtree.equals(lhs.trueSubtree);
		} else {
			return true;
		}
	}
}
