package edu.washington.cse.concerto.interpreter;

public interface InterpreterExtension<FH> {
	public InterpreterState<FH> processResult(final InterpreterState<FH> inState, final Object breakPoint);
	public void markIncompleteExecution();
}
