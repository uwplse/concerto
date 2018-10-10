package edu.washington.cse.concerto.instrumentation;

import edu.washington.cse.concerto.instrumentation.actions.AssignmentAction;

public interface AssignmentDisjunctionSelector<AVal, AHeap, AState> {
	public AssignmentSelector<AssignmentDisjunctionSelector<AVal, AHeap, AState>> cases();
	public void withAction(AssignmentAction<AVal, AHeap, AState> o);
}
