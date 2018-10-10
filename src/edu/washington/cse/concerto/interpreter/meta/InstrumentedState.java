package edu.washington.cse.concerto.interpreter.meta;

public abstract class InstrumentedState {
	public InstrumentedState(final PermissionToken tok) {
		if(tok == null) {
			throw new RuntimeException();
		}
	}
}
