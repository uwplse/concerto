package edu.washington.cse.concerto.instrumentation;

import edu.washington.cse.concerto.instrumentation.actions.FieldReadAction;

public interface FieldReadDisjunctionSelector<AVal, AHeap, AState> {
	public FieldReadSelector<FieldReadDisjunctionSelector<AVal, AHeap, AState>> cases();
	public void withAction(FieldReadAction<AVal, AHeap, AState> o);
}
