package edu.washington.cse.concerto.instrumentation.actions;

import edu.washington.cse.concerto.interpreter.ai.ValueMapper;

public interface MultiValueReader<AVal, AHeap, AState> {
	public static final int RETURN_SLOT = Integer.MIN_VALUE;
	public <R> R read(int i, ValueMapper<AVal, AState, R> map);
	Object readRaw(int i);
}
