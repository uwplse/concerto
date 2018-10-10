package edu.washington.cse.concerto.interpreter.ai;

import edu.washington.cse.concerto.interpreter.heap.Heap;
import edu.washington.cse.concerto.interpreter.heap.HeapReadResult;
import edu.washington.cse.concerto.interpreter.value.IValue;
import soot.SootFieldRef;

public interface HeapReader<AS, AVal> {
	public HeapReadResult<Object> readNondetIndex(Heap h, IValue base);
	public HeapReadResult<Object> readField(final Heap h, final IValue b, SootFieldRef field);
	public HeapReadResult<Object> readIndex(Heap h, IValue base, IValue indexValue);
	public <Ret> Ret forEachField(IValue v, AS state, Heap h, RecursiveTransformer<AS, Ret> recursor, Merger<Ret> accum);
}
