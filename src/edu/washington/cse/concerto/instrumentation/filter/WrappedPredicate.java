package edu.washington.cse.concerto.instrumentation.filter;

import edu.washington.cse.concerto.interpreter.EmbeddedState;
import edu.washington.cse.concerto.interpreter.heap.Heap;

public interface WrappedPredicate {
	public boolean accept(Object object, Heap h, EmbeddedState<?> fh);
}
