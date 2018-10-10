package edu.washington.cse.concerto.interpreter.ai;

import edu.washington.cse.concerto.interpreter.ai.binop.NoPrimitiveOps;
import edu.washington.cse.concerto.interpreter.ai.binop.PrimitiveOperations;

public abstract class DefaultAbstractInterpretation<AVal, AHeap, AS, Context> implements AbstractInterpretation<AVal, AHeap, AS, Context> {
	@Override
	public PrimitiveOperations<AVal> primitiveOperations() {
		return new NoPrimitiveOps<>();
	}
}
