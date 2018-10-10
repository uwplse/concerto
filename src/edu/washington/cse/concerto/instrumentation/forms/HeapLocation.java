package edu.washington.cse.concerto.instrumentation.forms;

import soot.SootFieldRef;
import soot.Type;

public class HeapLocation {
	private final Type outputType;
	private final Type hostType;
	private final SootFieldRef ref;

	public HeapLocation(final SootFieldRef r) {
		this.ref = r;
		this.outputType = null;
		this.hostType = null;
	}
	
	public HeapLocation(final Type hostType, final Type outputType) {
		this.ref = null;
		this.hostType = hostType;
		this.outputType = outputType;
	}
	
	public boolean isArrayAccess() {
		return ref == null;
	}
	
	public String getName() {
		if(ref == null) {
			return "*";
		} else {
			return ref.name();
		}
	}
	
	public Type hostType() {
		return ref != null ? ref.declaringClass().getType() : hostType;
	}
	
	public Type getResultType() {
		return ref != null ? ref.type() : outputType; 
	}
	
	public String getSignature() {
		return ref != null ? ref.getSignature() : null;
	}
}
