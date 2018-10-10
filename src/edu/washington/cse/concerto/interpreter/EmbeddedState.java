package edu.washington.cse.concerto.interpreter;

import edu.washington.cse.concerto.interpreter.lattice.Lattice;

public class EmbeddedState<T> {
	public final T state;
	public final Lattice<T> stateLattice;
	
	public EmbeddedState(final T state, final Lattice<T> stateLattice) {
		this.state = state;
		this.stateLattice = stateLattice;
	}
	
	public boolean lessThan(final EmbeddedState<T> other) {
		assert this.stateLattice == other.stateLattice;
		return this.stateLattice.lessEqual(this.state, other.state);
	}
	
	public EmbeddedState<T> join(final EmbeddedState<T> other) {
		assert this.stateLattice == other.stateLattice;
		return new EmbeddedState<>(this.stateLattice.join(this.state, other.state), this.stateLattice);
	}
	
	public EmbeddedState<T> widen(final EmbeddedState<T> next) {
		assert this.stateLattice == next.stateLattice;
		return new EmbeddedState<>(this.stateLattice.widen(this.state, next.state), this.stateLattice);
	}
}
