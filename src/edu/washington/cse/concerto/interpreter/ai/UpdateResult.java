package edu.washington.cse.concerto.interpreter.ai;

import edu.washington.cse.concerto.interpreter.heap.Heap;

public class UpdateResult<AState, Val> {
	public final AState state;
	public final Heap heap;
	public final Val value;

	public UpdateResult(final AState state, final Heap h, final Val v) {
		this.state = state;
		this.heap = h;
		this.value = v;
	}
}
