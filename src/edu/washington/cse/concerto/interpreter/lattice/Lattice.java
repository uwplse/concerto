package edu.washington.cse.concerto.interpreter.lattice;

public interface Lattice<T> {
	public T widen(T prev, T next);
	public T join(T first, T second);
	public boolean lessEqual(T first, T second);
}
