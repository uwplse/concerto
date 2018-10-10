package edu.washington.cse.concerto.interpreter.ai;

public interface BranchAwareAbstractInterpretation<AVal, AHeap, AS, Context> extends AbstractInterpretation<AVal, AHeap, AS, Context> {
	public void setBranchInterpreter(BranchInterpreter<AVal, AS> bInterp);
}
