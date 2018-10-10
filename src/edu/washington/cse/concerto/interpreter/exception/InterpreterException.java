package edu.washington.cse.concerto.interpreter.exception;

public abstract class InterpreterException extends RuntimeException {

	public InterpreterException(final String message) {
		super(message);
	}
	
	public InterpreterException() {
		super();
	}

}
