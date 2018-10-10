package edu.washington.cse.concerto.instrumentation.impl;

import edu.washington.cse.concerto.instrumentation.filter.ValueFilter;

public interface FilterAcceptor<T,U> {
	public T accept(ValueFilter<U> accept);
}
