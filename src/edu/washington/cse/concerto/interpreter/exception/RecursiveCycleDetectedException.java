package edu.washington.cse.concerto.interpreter.exception;

import soot.SootMethod;

public class RecursiveCycleDetectedException extends RuntimeException {
	public final SootMethod cycleMethod;

	public RecursiveCycleDetectedException(final SootMethod m) {
		this.cycleMethod = m;
	}
}
