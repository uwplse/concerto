package edu.washington.cse.concerto.interpreter.lattice;

import edu.washington.cse.concerto.interpreter.heap.Heap;

public class HeapLattice implements Lattice<Heap> {

	@Override
	public Heap widen(final Heap prev, final Heap next) {
		return Heap.widen(prev, next);
	}

	@Override
	public Heap join(final Heap first, final Heap second) {
		return Heap.join(first, second);
	}

	@Override
	public boolean lessEqual(final Heap first, final Heap second) {
		return first.lessEqual(second);
	}
}
