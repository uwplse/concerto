package edu.washington.cse.concerto.interpreter.ai;

import edu.washington.cse.concerto.interpreter.heap.Heap;
import edu.washington.cse.concerto.interpreter.heap.HeapAccessResult;
import edu.washington.cse.concerto.interpreter.heap.HeapFaultStatus;
import edu.washington.cse.concerto.interpreter.value.IValue;

public class HeapUpdateResult<AState> extends UpdateResult<AState, IValue> {
	public final HeapFaultStatus nullBasePointer;
	public final HeapFaultStatus oobAccess;

	public HeapUpdateResult(final AState state, final Heap h, final IValue v, final HeapFaultStatus nullBasePointer, final HeapFaultStatus oobAccess) {
		super(state, h, v);
		this.nullBasePointer = nullBasePointer;
		this.oobAccess = oobAccess;
	}
	
	public HeapUpdateResult(final AState state, final Heap h, final IValue v, final HeapAccessResult stat) {
		super(state, h, v);
		this.nullBasePointer = stat.npe;
		this.oobAccess = stat.oob;
	}
}
