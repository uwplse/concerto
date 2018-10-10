package edu.washington.cse.concerto.interpreter.state;

import edu.washington.cse.concerto.interpreter.value.IValue;

public class CombinedPartialState implements GlobalState {
	
	private final PartialConcreteState wrapped;
	private final int[] inMemory;

	public CombinedPartialState(final int[] inMemory, final String valueFile) {
		this.wrapped = new PartialConcreteState(valueFile);
		this.inMemory = inMemory;
	}

	@Override
	public IValue readDeterministic(final int inMemoryPtr) {
		if(inMemoryPtr < this.inMemory.length) {
			return IValue.lift(this.inMemory[inMemoryPtr]);
		} else {
			return this.wrapped.readDeterministic(inMemoryPtr - this.inMemory.length);
		}
	}

	@Override public IValue readNonDeterministic(final int ptr) {
		return IValue.nondet();
	}

}
