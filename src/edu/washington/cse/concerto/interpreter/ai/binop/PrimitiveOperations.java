package edu.washington.cse.concerto.interpreter.ai.binop;

import edu.washington.cse.concerto.interpreter.ai.CompareResult;

public interface PrimitiveOperations<AVal> {
	AVal plus(AVal a, AVal b);
	AVal minus(AVal a, AVal b);
	AVal mult(AVal a, AVal b);
	AVal div(AVal a, AVal b);
	
	default CompareResult cmp(final AVal a, final AVal b) {
		return CompareResult.NONDET;
	}
}
