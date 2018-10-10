package edu.washington.cse.concerto.interpreter.exception;

public abstract class ObjectProgramException extends InterpreterException {
	public ObjectProgramException(final String message) {
		super(message);
	}
	
	public ObjectProgramException() {
		super();
	}

}
