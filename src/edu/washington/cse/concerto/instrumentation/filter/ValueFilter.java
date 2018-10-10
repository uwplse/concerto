package edu.washington.cse.concerto.instrumentation.filter;


public interface ValueFilter<T> {
	public boolean test(T value);
}
