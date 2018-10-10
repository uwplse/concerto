package edu.washington.cse.concerto.interpreter.ai.instantiation.optiflow;

import edu.washington.cse.concerto.interpreter.ai.ValueMonadLattice;
import edu.washington.cse.concerto.interpreter.ai.instantiation.pta.JValue;
import edu.washington.cse.concerto.interpreter.ai.instantiation.pta.StupidHeap;
import edu.washington.cse.concerto.interpreter.lattice.Lattice;
import soot.ArrayType;
import soot.RefType;
import soot.toolkits.scalar.Pair;

public class StupidHeapWithReturnSlot {
	public static StupidHeapWithReturnSlot empty = new StupidHeapWithReturnSlot(StupidHeap.empty);
	public final StupidHeap wrapped;
	public final PathTree returnSlot;

	public StupidHeapWithReturnSlot(final StupidHeap heap, final PathTree returnSlot) {
		this.wrapped = heap;
		this.returnSlot = returnSlot;
	}

	public StupidHeapWithReturnSlot(final StupidHeap heap) {
		this(heap, PathTree.bottom);
	}

	public static ValueMonadLattice<StupidHeapWithReturnSlot> lattice = new ValueMonadLattice<StupidHeapWithReturnSlot>() {
		@Override public void injectValueLattice(final Lattice<Object> valueLattice) {
			StupidHeap.lattice.injectValueLattice(valueLattice);
		}

		@Override public StupidHeapWithReturnSlot widen(final StupidHeapWithReturnSlot prev, final StupidHeapWithReturnSlot next) {
			return new StupidHeapWithReturnSlot(
					StupidHeap.lattice.widen(prev.wrapped, next.wrapped),
					PathTree.lattice.widen(prev.returnSlot, next.returnSlot)
			);
		}

		@Override public StupidHeapWithReturnSlot join(final StupidHeapWithReturnSlot first, final StupidHeapWithReturnSlot second) {
			return new StupidHeapWithReturnSlot(
					StupidHeap.lattice.join(first.wrapped, second.wrapped),
					PathTree.lattice.join(first.returnSlot, second.returnSlot)
			);
		}

		@Override public boolean lessEqual(final StupidHeapWithReturnSlot first, final StupidHeapWithReturnSlot second) {
			return StupidHeap.lattice.lessEqual(first.wrapped, second.wrapped) && PathTree.lattice.lessEqual(first.returnSlot, second.returnSlot);
		}
	};

	public Pair<StupidHeapWithReturnSlot,JValue> allocUnknownType(final RefType upperBound) {
		return this.rewrap(wrapped.allocUnknownType(upperBound));
	}

	private <V> Pair<StupidHeapWithReturnSlot, V> rewrap(final Pair<StupidHeap, V> res) {
		return new Pair<>(new StupidHeapWithReturnSlot(res.getO1(), returnSlot), res.getO2());
	}

	public Pair<StupidHeapWithReturnSlot,JValue> alloc(final RefType t) {
		return this.rewrap(wrapped.alloc(t));
	}

	public Pair<StupidHeapWithReturnSlot,JValue> alloc(final ArrayType t, final int size) {
		return this.rewrap(wrapped.alloc(t, size));
	}

	public StupidHeapWithReturnSlot set(final JValue toSet, final String s, final Object value) {
		return new StupidHeapWithReturnSlot(this.wrapped.set(toSet, s, value), returnSlot);
	}

	public StupidHeapWithReturnSlot withReturnSlot(final PathTree t) {
		return new StupidHeapWithReturnSlot(wrapped, t);
	}

	public StupidHeapWithReturnSlot clearSlot() {
		return withReturnSlot(PathTree.bottom);
	}

	@Override public String toString() {
		return wrapped.toString();
	}
}
