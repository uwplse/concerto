package edu.washington.cse.concerto.instrumentation;

import edu.washington.cse.concerto.interpreter.ai.ValueMapper;

public interface ValueReader<AVal, AHeap, AState> {
	public <R> R read(ValueMapper<AVal, AState, R> mapper);
}
