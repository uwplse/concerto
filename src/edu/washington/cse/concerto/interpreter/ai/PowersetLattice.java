package edu.washington.cse.concerto.interpreter.ai;

import edu.washington.cse.concerto.interpreter.lattice.Lattice;
import fj.Ord;
import fj.data.Set;

public class PowersetLattice<T> implements Lattice<Set<T>> {
	private final Ord<T> elemOrd;

	public PowersetLattice(final Ord<T> elemOrd) {
		this.elemOrd = elemOrd;
	}

	@Override public Set<T> widen(final Set<T> prev, final Set<T> next) {
		return this.join(prev, next);
	}

	@Override public Set<T> join(final Set<T> first, final Set<T> second) {
		return first.union(second);
	}

	@Override public boolean lessEqual(final Set<T> first, final Set<T> second) {
		return first.subsetOf(second);
	}
}
