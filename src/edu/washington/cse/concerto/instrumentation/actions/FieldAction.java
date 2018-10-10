package edu.washington.cse.concerto.instrumentation.actions;


public interface FieldAction<AVal, AHeap, AState> {
	public void postBase(ValueReplacement<AVal, AHeap, AState> baseReplacement);
}
