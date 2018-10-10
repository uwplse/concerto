package edu.washington.cse.concerto.instrumentation.actions;

import edu.washington.cse.concerto.interpreter.ai.HeapReader;

public interface AssignmentAction<AVal, AHeap, AState> {
	public ValueReplacement<AVal, AHeap, AState> preAssign(HeapReader<AState, AVal> reader, ValueReplacement<AVal, AHeap, AState> argReplacer);
}
