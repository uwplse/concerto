package edu.washington.cse.concerto.instrumentation.forms;

import edu.washington.cse.concerto.interpreter.EmbeddedState;
import edu.washington.cse.concerto.interpreter.ai.ValueMonad;
import edu.washington.cse.concerto.interpreter.heap.Heap;
import edu.washington.cse.concerto.interpreter.value.IValue;

public class HeapAccess {

	private final IValue baseValue;
	private final Heap heap;
	private final EmbeddedState<?> fh;
	private final HeapLocation ref;
	private final ValueMonad<?> monad;

	public HeapAccess(final IValue baseValue, final HeapLocation ref, final Heap heap, final EmbeddedState<?> foreignHeap, final ValueMonad<?> monad) {
		this.baseValue = baseValue;
		this.ref = ref;
		this.heap = heap;
		this.fh = foreignHeap;
		this.monad = monad;
	}

	public RuntimeValue getBasePointerValue() {
		return new RuntimeValue(monad.lift(baseValue), heap, fh, ref.hostType());
	}

	public HeapLocation getField() {
		return this.ref;
	}

	public boolean hasBasePointer() {
		return true;
	}

}
