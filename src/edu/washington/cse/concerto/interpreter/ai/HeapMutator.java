package edu.washington.cse.concerto.interpreter.ai;

import soot.SootFieldRef;
import edu.washington.cse.concerto.interpreter.heap.Heap;
import edu.washington.cse.concerto.interpreter.value.IValue;

public interface HeapMutator {
	public <AState> HeapUpdateResult<AState> updateAtIndex(AState s, Heap h, IValue base, IValue index, Object toSet);
	public <AState> HeapUpdateResult<AState> updateNondetIndex(AState s, Heap h, IValue base, Object toSet);
	public <AState> HeapUpdateResult<AState> updateField(final AState s, final Heap h, final IValue b, SootFieldRef field, Object toSet);
}
