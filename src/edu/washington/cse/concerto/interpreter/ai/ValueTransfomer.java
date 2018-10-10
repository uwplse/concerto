package edu.washington.cse.concerto.interpreter.ai;

import edu.washington.cse.concerto.interpreter.heap.Heap;
import edu.washington.cse.concerto.interpreter.value.IValue;

public interface ValueTransfomer<AVal, AState, ARet, CRet, RRet, HAccess> {
	public ARet mapAbstract(AVal val, AState state, Heap h, RecursiveTransformer<AState, RRet> recursor);
	public CRet mapConcrete(IValue v, AState state, Heap h, HAccess heapAccessor, RecursiveTransformer<AState, RRet> recursor);
}
