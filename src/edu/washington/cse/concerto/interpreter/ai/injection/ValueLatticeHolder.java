package edu.washington.cse.concerto.interpreter.ai.injection;

import edu.washington.cse.concerto.interpreter.lattice.Lattice;

public class ValueLatticeHolder implements NeedsValueLattice {
	public Lattice<Object> valueLattice;

	@Override
	public void injectValueLattice(final Lattice<Object> valueLattice) {
		this.valueLattice = valueLattice;
	}
}
