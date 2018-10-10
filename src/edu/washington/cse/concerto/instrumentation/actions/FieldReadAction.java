package edu.washington.cse.concerto.instrumentation.actions;


public interface FieldReadAction<AVal, AHeap, AState> extends FieldAction<AVal, AHeap, AState> {
	public void postRead(ValueReplacement<AVal, AHeap, AState> valueReplacement);
}
