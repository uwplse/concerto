package edu.washington.cse.concerto.interpreter.ai.instantiation.array;

public class OutOfBoundsArrayAccess extends RuntimeException {

	public OutOfBoundsArrayAccess() { }
	
	public OutOfBoundsArrayAccess(final String string) {
		super(string);
	}

}
