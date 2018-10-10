package edu.washington.cse.concerto.instrumentation;

import edu.washington.cse.concerto.instrumentation.filter.FieldFilter;
import edu.washington.cse.concerto.instrumentation.filter.ValuePredicate;

public interface HeapAccessSelector<Return, Self extends HeapAccessSelector<Return, Self>> {
	public FieldFilter<Self> fieldFilter();
	public ValuePredicate<Self> basePointerFilter();
	public Return build();
}
