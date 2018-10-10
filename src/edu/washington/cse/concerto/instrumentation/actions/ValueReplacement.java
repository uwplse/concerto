package edu.washington.cse.concerto.instrumentation.actions;

import edu.washington.cse.concerto.instrumentation.ValueReader;
import edu.washington.cse.concerto.interpreter.ai.ValueStateTransformer;

public interface ValueReplacement<AVal, AHeap, AState> {
	public void update(ValueStateTransformer<AVal, AState> tr);
	public ValueReader<AVal, AHeap, AState> getReader();
}
