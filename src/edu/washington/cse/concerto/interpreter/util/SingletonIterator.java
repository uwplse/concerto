package edu.washington.cse.concerto.interpreter.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class SingletonIterator<E> implements Iterator<E> {
	
	boolean read = false;
	private final E item;

	public SingletonIterator(final E item) {
		this.item = item;
	}

	@Override
	public boolean hasNext() {
		return !read;
	}

	@Override
	public E next() {
		if(read) {
			throw new NoSuchElementException();
		}
		read = true;
		return item;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

}
