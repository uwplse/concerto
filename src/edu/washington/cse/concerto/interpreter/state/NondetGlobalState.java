package edu.washington.cse.concerto.interpreter.state;

import edu.washington.cse.concerto.interpreter.value.IValue;

public class NondetGlobalState implements GlobalState {
	@Override
	public IValue readDeterministic(final int ptr) {
		return IValue.nondet();
	}

	@Override
	public IValue readNonDeterministic(final int ptr) {
		return IValue.nondet();
	}
}
