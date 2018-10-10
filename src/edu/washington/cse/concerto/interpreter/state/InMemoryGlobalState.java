package edu.washington.cse.concerto.interpreter.state;

import edu.washington.cse.concerto.interpreter.value.IValue;

public class InMemoryGlobalState implements GlobalState {

	private final int[] stream;

	public InMemoryGlobalState(final int[] stream) {
		this.stream = stream;
	}

	public InMemoryGlobalState(final Integer[] stream) {
		this.stream = new int[stream.length];
		for(int i = 0; i < stream.length; i++) {
			this.stream[i] = stream[i];
		}
	}

	@Override
	public IValue readDeterministic(final int ptr) {
		if(ptr >= stream.length) {
			return IValue.lift(0);
		} else {
			return IValue.lift(stream[ptr]);
		}
	}

	@Override
	public IValue readNonDeterministic(final int ptr) {
		return IValue.nondet();
	}

}
