package edu.washington.cse.concerto.interpreter.ai;

import edu.washington.cse.concerto.interpreter.ai.binop.RelationalPrimitiveOperations;

public interface RelationalPathSensitiveAbstractInterpretation<AVal, AHeap, AS, Context> extends PathSensitiveAbstractInterpretation<AVal, AHeap, AS, Context> {
	@Override
	public RelationalPrimitiveOperations<AVal, AS> primitiveOperations();
}
