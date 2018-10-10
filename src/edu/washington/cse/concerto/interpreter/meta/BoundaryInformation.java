package edu.washington.cse.concerto.interpreter.meta;

import soot.jimple.InvokeExpr;

public class BoundaryInformation<Context> {
	public final InstrumentedState rootState;
	public final InvokeExpr rootInvoke;
	public final Context rootContext;

	public BoundaryInformation(final InstrumentedState rootState, final Context rootContext, final InvokeExpr rootInvoke) {
		this.rootState = rootState;
		this.rootInvoke = rootInvoke;
		this.rootContext = rootContext;
	}

	@Override public String toString() {
		return "BoundaryInformation{" + "rootState=" + rootState + ", rootInvoke=" + rootInvoke + ", rootContext=" + rootContext + '}';
	}
}
