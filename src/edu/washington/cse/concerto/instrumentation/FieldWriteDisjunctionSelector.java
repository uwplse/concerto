package edu.washington.cse.concerto.instrumentation;

import edu.washington.cse.concerto.instrumentation.actions.FieldWriteAction;

public interface FieldWriteDisjunctionSelector<AVal, AHeap, AState> {
	public FieldWriteSelector<FieldWriteDisjunctionSelector<AVal, AHeap, AState>> cases();
	public void withAction(FieldWriteAction<AVal, AHeap, AState> o);
}
