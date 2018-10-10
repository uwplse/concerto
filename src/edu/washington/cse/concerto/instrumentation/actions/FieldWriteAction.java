package edu.washington.cse.concerto.instrumentation.actions;


public interface FieldWriteAction<AVal, AHeap, AState> extends FieldAction<AVal, AHeap, AState> {
	public void preWrite(ValueReplacement<AVal, AHeap, AState> valueReplacement);
}
