package edu.washington.cse.concerto.interpreter.ai.binop;

import edu.washington.cse.concerto.interpreter.ai.CompareResult;
import edu.washington.cse.concerto.interpreter.lattice.Lattice;

public class InfoflowPrimitiveOps<AVal> extends DefaultPrimitiveOps<AVal> {
	private final Lattice<AVal> valueLattice;

	public InfoflowPrimitiveOps(final Lattice<AVal> valLattice) {
		this.valueLattice = valLattice;
	}

	@Override
	public CompareResult cmp(final AVal a, final AVal b) {
		return CompareResult.nondet();
	}

	@Override
	protected AVal binop(final AVal a, final AVal b) {
		return valueLattice.join(a, b);
	}

}
