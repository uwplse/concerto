package edu.washington.cse.concerto.interpreter.ai.test;

import edu.washington.cse.concerto.interpreter.state.ExecutionState;
import edu.washington.cse.concerto.interpreter.value.IValue;
import fj.function.Effect2;

public interface AssertionChecker {
	interface AssertF extends Effect2<Boolean, String> { }
	
	default void assertValue(final String k, final IValue v, final AssertF assertF) {

	}
	default void assertValue(final String k, final IValue v, final ExecutionState<?, ?> state, final AssertF assertF) {
		assertValue(k, v, assertF);
	}
}
