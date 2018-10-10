package edu.washington.cse.concerto.interpreter.exception;

import edu.washington.cse.concerto.interpreter.InterpreterState;
import edu.washington.cse.concerto.interpreter.state.ExecutionState;

public class ThrowToState extends RuntimeException {
	public final ExecutionState<?, ?> state;
	public final InterpreterState<?> resultState;
	public ThrowToState(final ExecutionState<?, ?> state, final InterpreterState<?> resultState) {
		this.state = state;
		this.resultState = resultState;
	}
}
