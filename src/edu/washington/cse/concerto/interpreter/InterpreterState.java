package edu.washington.cse.concerto.interpreter;

import soot.Unit;
import edu.washington.cse.concerto.interpreter.heap.Heap;
import edu.washington.cse.concerto.interpreter.state.ExecutionState;

public class InterpreterState<FH> {
	public final MethodState ms;
	public final Heap h;
	public final ReturnState<FH> rs;
	public final Unit stopUnit;
	public final EmbeddedState<FH> foreignHeap;
	
	InterpreterState(final MethodState ms, final Heap h, final ReturnState<FH> rs, final Unit stopUnit, final EmbeddedState<FH> foreignHeap) {
		this.ms = ms;
		this.h = h;
		this.rs = rs;
		this.stopUnit = stopUnit;
		this.foreignHeap = foreignHeap;
	}
	
	InterpreterState(final ReturnState<FH> rs) {
		this(null, null, rs, null, null);
	}

	public static <FH> InterpreterState<FH> lift(final ExecutionState<FH, ?> fork) {
		return new InterpreterState<>(fork.ms, fork.heap, null, null, fork.foreignHeap);
	}
}