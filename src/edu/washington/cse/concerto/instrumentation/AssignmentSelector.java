package edu.washington.cse.concerto.instrumentation;

import edu.washington.cse.concerto.instrumentation.filter.ValuePredicate;

public interface AssignmentSelector<Return> {
	ValuePredicate<AssignmentSelector<Return>> leftOp();
	ValuePredicate<AssignmentSelector<Return>> rightOp();
	Return build();
}
