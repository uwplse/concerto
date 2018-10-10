package edu.washington.cse.concerto.interpreter.meta;

public class NullReflectionModel implements ReflectionModel {

	@Override
	public String resolveClassName(final int key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String resolveSignature(final int key) {
		throw new UnsupportedOperationException();
	}
}
