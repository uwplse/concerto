package edu.washington.cse.concerto.interpreter.ai.instantiation.array;

import edu.washington.cse.concerto.interpreter.heap.HeapAccessResult;
import edu.washington.cse.concerto.interpreter.heap.HeapFaultStatus;

public class AbstractHeapAccessResult extends HeapAccessResult {
	public final boolean infeasible;

	public AbstractHeapAccessResult(final HeapFaultStatus npe, final HeapFaultStatus oob, final boolean infeasible) {
		super(npe, oob);
		this.infeasible = infeasible;
	}

	public static final AbstractHeapAccessResult NPE = new AbstractHeapAccessResult(HeapFaultStatus.MUST, HeapFaultStatus.MUST_NOT, false);
	public static final AbstractHeapAccessResult OOB = new AbstractHeapAccessResult(HeapFaultStatus.MUST_NOT, HeapFaultStatus.MUST, false);
	public static final AbstractHeapAccessResult SAFE = new AbstractHeapAccessResult(HeapFaultStatus.MUST_NOT, HeapFaultStatus.MUST_NOT, false);
	public static final AbstractHeapAccessResult INFEASIBLE = new AbstractHeapAccessResult(HeapFaultStatus.BOTTOM, HeapFaultStatus.BOTTOM, true);

	public static AbstractHeapAccessResult lift(final HeapAccessResult res) {
		return new AbstractHeapAccessResult(res.npe, res.oob, false);
	}

	public static AbstractHeapAccessResult join(final AbstractHeapAccessResult a1, final AbstractHeapAccessResult a2) {
		return new AbstractHeapAccessResult(a1.npe.joinWith(a2.npe), a2.oob.joinWith(a2.oob), a2.infeasible && a1.infeasible);
	}

	public boolean shouldPrune() {
		return this.npe == HeapFaultStatus.MUST || this.oob == HeapFaultStatus.MUST || infeasible;
	}

	public AbstractHeapAccessResult npe() {
		if(this.npe == HeapFaultStatus.MAY) {
			return this;
		}
		return new AbstractHeapAccessResult(this.npe.mark(), oob, infeasible);
	}
}
