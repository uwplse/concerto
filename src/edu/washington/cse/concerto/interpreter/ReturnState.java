package edu.washington.cse.concerto.interpreter;

import edu.washington.cse.concerto.interpreter.heap.Heap;
import edu.washington.cse.concerto.interpreter.value.IValue;
import edu.washington.cse.concerto.interpreter.value.ValueMerger;

public class ReturnState<FH> {
	public final IValue returnValue;
	public final Heap h;
	public final EmbeddedState<FH> foreignHeap;
	
	ReturnState(final IValue returnValue, final Heap h, final EmbeddedState<FH> foreignHeap) {
		this.returnValue = returnValue;
		this.h = h;
		this.foreignHeap = foreignHeap;
	}

	public static <FH> ReturnState<FH> widen(final ReturnState<FH> r1, final ReturnState<FH> r2) {
		IValue returnValue;
		if(r1.returnValue != null) {
			returnValue = ValueMerger.WIDENING_MERGE.merge(r1.returnValue, r2.returnValue);
		} else {
			returnValue = null;
		}
		EmbeddedState<FH> toWiden;
		if(r1.foreignHeap != null) {
			assert r2.foreignHeap != null;
			toWiden = r1.foreignHeap.widen(r2.foreignHeap);
		} else {
			toWiden = null;
		}
		return new ReturnState<>(returnValue, Heap.widen(r1.h, r2.h), toWiden);
	}

	public void dump() {
		System.out.println(toString());
	}
	
	@Override
	public String toString() {
		return "Return value: " + returnValue + "\nHeap: " + h + "\nForeign heap: " + foreignHeap;
	}

	public boolean lessEqual(final ReturnState<FH> curr) {
		if((this.returnValue == null) != (curr.returnValue == null)) {
			return false;
		}
		if(this.returnValue != null && !this.returnValue.lessEqual(curr.returnValue)) {
			return false;
		}
		if(this.foreignHeap != null) {
			if(curr.foreignHeap == null) {
				return false;
			} else if(!this.foreignHeap.lessThan(curr.foreignHeap)) {
				return false;
			}
		}
		return h.lessEqual(curr.h);
	}
}
