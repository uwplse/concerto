package edu.washington.cse.concerto.interpreter.ai;

import edu.washington.cse.concerto.interpreter.ai.binop.PathSensitivePrimitiveOperations;

public interface PathSensitiveAbstractInterpretation<AVal, AHeap, AS, Context> extends AbstractInterpretation<AVal, AHeap, AS, Context> {
	@Override
	public PathSensitivePrimitiveOperations<AVal> primitiveOperations();
	public void setBranchInterpreter(PathSensitiveBranchInterpreter<AVal, AS> bInterp);
}
