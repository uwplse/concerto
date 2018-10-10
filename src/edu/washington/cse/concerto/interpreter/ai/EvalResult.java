package edu.washington.cse.concerto.interpreter.ai;

import edu.washington.cse.concerto.interpreter.meta.InstrumentedState;
import fj.F2;

public class EvalResult {
	public final Object value;
	public final InstrumentedState state;

	public EvalResult(final InstrumentedState state, final Object value) {
		this.state = state;
		this.value = value;
	}

	public EvalResult(final MethodResult mr) {
		this(mr.getState(), mr.getReturnValue());
	}
	
	public EvalResult map(final F2<InstrumentedState, Object, EvalResult> mapper) {
		return mapper.f(state, value);
	}
	
	public <R> R mapTo(final F2<InstrumentedState, Object, R> m) {
		return m.f(state, value);
	}

	@Override public String toString() {
		return "[ST: " + state + " X " + value + "]";
	}
}
