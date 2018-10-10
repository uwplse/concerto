package edu.washington.cse.concerto.interpreter.lattice;

import edu.washington.cse.concerto.interpreter.MethodState;

public class StateLattice implements Lattice<MethodState> {

	@Override
	public MethodState widen(final MethodState prev, final MethodState next) {
		return MethodState.widen(prev, next);
	}

	@Override
	public MethodState join(final MethodState first, final MethodState second) {
		return MethodState.join(first, second);
	}

	@Override
	public boolean lessEqual(final MethodState first, final MethodState second) {
		return first.lessEqual(second);
	}

}
