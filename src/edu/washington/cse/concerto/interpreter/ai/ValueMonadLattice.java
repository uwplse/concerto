package edu.washington.cse.concerto.interpreter.ai;

import edu.washington.cse.concerto.interpreter.ai.injection.NeedsValueLattice;
import edu.washington.cse.concerto.interpreter.lattice.Lattice;

public interface ValueMonadLattice<V> extends Lattice<V>, NeedsValueLattice {

}
