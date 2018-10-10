package edu.washington.cse.concerto.interpreter.ai;

import edu.washington.cse.concerto.interpreter.heap.Heap;

public interface RecursiveTransformer<AState, R> {
	public R mapValue(AState state, Heap h, Object o);
}
