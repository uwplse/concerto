package edu.washington.cse.concerto.instrumentation.forms;

import soot.Type;
import edu.washington.cse.concerto.interpreter.EmbeddedState;
import edu.washington.cse.concerto.interpreter.heap.Heap;

public class RuntimeValue {

	private final Object value;
	private final Heap heap;
	private final EmbeddedState<?> foreignHeap;
	private final Type typeInfo;

	public RuntimeValue(final Object basePointer, final Heap heap, final EmbeddedState<?> foreignHeap, final Type typeInfo) {
		this.value = basePointer;
		this.heap = heap;
		this.foreignHeap = foreignHeap;
		this.typeInfo = typeInfo;
	}

	public Type getType() {
		return typeInfo;
	}

	public boolean hasValue() {
		return value != null;
	}

	public EmbeddedState<?> getForeignHeap() {
		return foreignHeap;
	}

	public Object getValue() {
		return value;
	}

	public Heap getHeap() {
		return heap;
	}

}
