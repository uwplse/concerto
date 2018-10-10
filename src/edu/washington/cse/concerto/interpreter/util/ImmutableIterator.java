package edu.washington.cse.concerto.interpreter.util;

import java.util.Iterator;

public final class ImmutableIterator<T> implements Iterator<T> {
	private final Iterator<T> wrapped;

	public ImmutableIterator(final Iterator<T> wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public T next() {
		return wrapped.next();
	}

	@Override
	public boolean hasNext() {
		return wrapped.hasNext();
	}
}