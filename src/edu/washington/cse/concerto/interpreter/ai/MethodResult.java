package edu.washington.cse.concerto.interpreter.ai;

import edu.washington.cse.concerto.interpreter.meta.InstrumentedState;

public class MethodResult {
	private final Object returnValue;
	private final InstrumentedState state;

	public MethodResult(final InstrumentedState state, final Object returnValue) {
		this.state = state;
		this.returnValue = returnValue;
		if(returnValue instanceof soot.grimp.internal.GInvokeStmt) {
			throw new RuntimeException();
		}
	}

	public MethodResult(final InstrumentedState state) {
		this.state = state;
		this.returnValue = null;
	}

	public InstrumentedState getState() {
		return state;
	}

	public Object getReturnValue() {
		return returnValue;
	}
	
	@Override
	public String toString() {
		return "<STATE: " + state + "  || RET: " + returnValue + ">";
	}
}
