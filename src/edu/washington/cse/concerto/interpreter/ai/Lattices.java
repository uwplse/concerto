package edu.washington.cse.concerto.interpreter.ai;

import edu.washington.cse.concerto.interpreter.lattice.Lattice;

public interface Lattices<AVal, AHeap, AS> {
	public Lattice<AVal> valueLattice();
	public Lattice<AHeap> heapLattice();
	public Lattice<AS> stateLattice();
}
