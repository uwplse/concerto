package edu.washington.cse.concerto.interpreter.ai;

import edu.washington.cse.concerto.interpreter.ai.injection.NeedsMonads;
import edu.washington.cse.concerto.interpreter.lattice.Lattice;

public interface MonadicLattice<A, AVal, AS> extends Lattice<A>, NeedsMonads<AVal, AS> {
}
