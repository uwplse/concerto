package edu.washington.cse.concerto.interpreter.meta;

import soot.SootClass;

public interface ReflectionModel {
	default int reverseResolution(SootClass sootClass) {
		return -1;
	}

	public static enum InvokeMoke {
		UseDeclaredType,
		UseTransitiveDeclaredType,
		
		UseRuntimeType,
		UseTransitiveRuntimeType
	}
	
	public String resolveClassName(int key);
	public String resolveSignature(int key);
	default public InvokeMoke resolutionMode() {
		return InvokeMoke.UseTransitiveRuntimeType;
	}
}
