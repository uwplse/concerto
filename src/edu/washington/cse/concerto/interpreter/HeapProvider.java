package edu.washington.cse.concerto.interpreter;

import edu.washington.cse.concerto.interpreter.heap.Heap;

public interface HeapProvider {
	public Heap getHeap();
	public Object getState();
}
