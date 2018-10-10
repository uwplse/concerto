package edu.washington.cse.concerto.interpreter.ai.binop;

public enum ObjectIdentityResult {
	MAY_BE,
	MUST_BE,
	MUST_NOT_BE;

	public ObjectIdentityResult join(final ObjectIdentityResult other) {
		if(other == this) {
			return this;
		} else {
			return MAY_BE;
		}
	}
}