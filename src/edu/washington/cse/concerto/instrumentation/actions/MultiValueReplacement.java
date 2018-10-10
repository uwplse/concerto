package edu.washington.cse.concerto.instrumentation.actions;

import edu.washington.cse.concerto.interpreter.ai.ValueStateTransformer;

public interface MultiValueReplacement<AVal, AHeap, AState> {
	public static final int RETURN_SLOT = Integer.MIN_VALUE;
	public void update(int i, ValueStateTransformer<AVal, AState> tr);
	public MultiValueReader<AVal, AHeap, AState> getReader();
}
