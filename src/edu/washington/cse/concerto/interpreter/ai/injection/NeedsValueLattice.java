package edu.washington.cse.concerto.interpreter.ai.injection;

import edu.washington.cse.concerto.interpreter.lattice.Lattice;

public interface NeedsValueLattice extends Injectable {
	public void injectValueLattice(Lattice<Object> valueLattice);
}
