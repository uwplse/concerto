package edu.washington.cse.concerto.interpreter.ai.binop;

import edu.washington.cse.concerto.interpreter.ai.CompareResult;

public class NoPrimitiveOps<AVal> extends DefaultPrimitiveOps<AVal> {
	@Override
	public CompareResult cmp(final AVal a, final AVal b) {
		return CompareResult.nondet();
	}

	@Override
	protected AVal binop(final AVal a, final AVal b) {
		throw new UnsupportedOperationException();
	}
}
