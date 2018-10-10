package edu.washington.cse.concerto.interpreter.state;

import edu.washington.cse.concerto.interpreter.value.IValue;

public class PartialConcreteState extends AbstractGlobalState {
	
	public PartialConcreteState(final String detFile) {
		super(detFile);
	}

	@Override
	public IValue readNonDeterministic(final int ptr) {
		return IValue.nondet();
	}

}
