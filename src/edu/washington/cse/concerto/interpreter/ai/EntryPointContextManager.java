package edu.washington.cse.concerto.interpreter.ai;

import soot.SootMethod;

public interface EntryPointContextManager<Context, AVal, AS, AHeap> extends ContextManager<Context, AVal, AS, AHeap> {
	public Context initialContext(SootMethod m);
	public Context initialAllocationContext(final SootMethod mainMethod);
}
