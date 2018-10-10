package edu.washington.cse.concerto.interpreter.ai;

import edu.washington.cse.concerto.interpreter.heap.Heap;

public abstract class ConcreteValueMapper<AVal, AHeap, R> implements ValueMapper<AVal, AHeap, R> {
	@Override
	public R mapAbstract(final AVal val, final AHeap state, final Heap h, final RecursiveTransformer<AHeap, R> recursor) {
		throw new RuntimeException(val.toString());
	}
	
	@Override
	public R merge(final R v1, final R v2) {
		if(v1 == null) {
			return v2;
		} else if(v1 == null) {
			return v2;
		} else {
			throw new RuntimeException();
		}
	}
}
