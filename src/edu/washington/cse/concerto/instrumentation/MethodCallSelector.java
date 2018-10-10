package edu.washington.cse.concerto.instrumentation;

import edu.washington.cse.concerto.instrumentation.filter.MethodFilter;
import edu.washington.cse.concerto.instrumentation.filter.ValuePredicate;


public interface MethodCallSelector<Return> {
	public MethodFilter<MethodCallSelector<Return>> methodFilter();
	public ValuePredicate<MethodCallSelector<Return>> basePointerFilter();
	public ValuePredicate<MethodCallSelector<Return>> argumentIs(int num);
	public Return build();
}
