package edu.washington.cse.concerto.interpreter.exception;

public class FailedExecutionException extends ObjectProgramException {
	public FailedExecutionException(final String message) {
		super("Source program failed: " + message);
	}
}
