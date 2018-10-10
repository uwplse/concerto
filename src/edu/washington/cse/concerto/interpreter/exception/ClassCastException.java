package edu.washington.cse.concerto.interpreter.exception;

public class ClassCastException extends ObjectProgramException {
	public ClassCastException() {
		super();
	}
	
	public ClassCastException(final String msg) {
		super(msg);
	}
}
