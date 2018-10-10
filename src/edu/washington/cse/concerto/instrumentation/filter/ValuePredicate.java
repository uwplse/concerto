package edu.washington.cse.concerto.instrumentation.filter;


public interface ValuePredicate<Return> {
	public Return build();
	public TypeFilterBuilder<ValuePredicate<Return>> typeFilter();
	public ValuePredicate<Return> filter(WrappedPredicate handler);
}
