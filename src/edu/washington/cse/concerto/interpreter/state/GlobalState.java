package edu.washington.cse.concerto.interpreter.state;

import edu.washington.cse.concerto.interpreter.value.IValue;

public interface GlobalState {
	IValue readDeterministic(int position);
	IValue readNonDeterministic(int position);

}