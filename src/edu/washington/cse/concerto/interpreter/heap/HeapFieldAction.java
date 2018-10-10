package edu.washington.cse.concerto.interpreter.heap;

import edu.washington.cse.concerto.interpreter.value.IValue;

public interface HeapFieldAction {
	void accept(String fieldName, IValue value);
}
