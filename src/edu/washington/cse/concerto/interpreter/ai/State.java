package edu.washington.cse.concerto.interpreter.ai;

import edu.washington.cse.concerto.interpreter.meta.MergeContext;


public interface State<AHeap, AS> {
	public AS emptyState();
	public AS inject(AS state, AHeap heap);
	public AHeap project(AS state);
	default AHeap merge(AHeap curr, AHeap updated, MergeContext mergeContext) {
		return updated;
	}
}
