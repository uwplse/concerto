package edu.washington.cse.concerto.interpreter.ai;

public interface ContinuationBasedState<C, S> {
	S withContinuation(Continuation<C> cont);
	Continuation<C> getContinuation();
}
