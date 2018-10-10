package edu.washington.cse.concerto.interpreter;

import edu.washington.cse.concerto.interpreter.heap.Heap;
import edu.washington.cse.concerto.interpreter.state.ExecutionState;

public class StateAccumulator<FH> {
	public Heap heap = null;
	public EmbeddedState<FH> foreignHeap = null;
	public StateAccumulator() { }
	public void update(final Heap h, final EmbeddedState<FH> up) {
		if(foreignHeap == null) {
			foreignHeap = up;
		} else {
			foreignHeap = Interpreter.joinForeignHeaps(foreignHeap, up);
		}
		if(this.heap == null) {
			this.heap = h;
		} else {
			this.heap = Heap.fullJoin(this.heap, h);
		}
	}
	public void mergeToState(final ExecutionState<FH, ?> es) {
		es.replaceHeap(foreignHeap);
		es.heap.mergeAndPopHeap(heap);
	}
	
	public void applyToState(final ExecutionState<FH, ?> es) {
		es.replaceHeap(foreignHeap);
		es.heap.applyHeap(heap);
	}
}