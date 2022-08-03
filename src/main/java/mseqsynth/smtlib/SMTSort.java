package mseqsynth.smtlib;

/**
 * SMT-LIB built-in sorts
 */

public enum SMTSort {
	INT("Int"),
	BOOL("Bool"),
	// null for unknown or unconcerned
	;
	
	private String repr;
	
	private SMTSort(String repr) {
		this.repr = repr;
	}
	
	public String toSMTString() {
		return this.repr;
	}
}
