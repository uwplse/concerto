package edu.washington.cse.concerto.instrumentation;

import edu.washington.cse.concerto.instrumentation.actions.MethodCallAction;

public interface MethodDisjunctionSelector<AVal, AHeap, AState> {
	public MethodCallSelector<MethodDisjunctionSelector<AVal, AHeap, AState>> cases();
	public void withAction(MethodCallAction<AVal, AHeap, AState> o);
}
