package edu.washington.cse.concerto.interpreter.ai.instantiation.optiflow;

import edu.washington.cse.concerto.interpreter.ai.Continuation;
import edu.washington.cse.concerto.interpreter.ai.ContinuationBasedState;
import edu.washington.cse.concerto.interpreter.ai.MonadicLattice;
import edu.washington.cse.concerto.interpreter.ai.ValueMonad;
import edu.washington.cse.concerto.interpreter.ai.injection.NeedsMonads;
import edu.washington.cse.concerto.interpreter.ai.instantiation.OrderingUtil;
import edu.washington.cse.concerto.interpreter.meta.Monads;
import fj.F;
import fj.F2;
import fj.Ord;
import fj.P2;
import fj.data.Option;
import fj.data.TreeMap;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.toolkits.scalar.Pair;

import java.util.Map;

public class LocalMap implements ContinuationBasedState<Pair<SootMethod,Unit>,LocalMap> {
	protected static MonadicLattice<LocalMap, PV, LocalMap> lattice = new MonadicLattice<LocalMap, PV, LocalMap>() {
		private ValueMonad<PV> valueMonad;

		@Override
		public LocalMap widen(final LocalMap prev, final LocalMap next) {
			final StupidHeapWithReturnSlot widenedHeap = StupidHeapWithReturnSlot.lattice.widen(prev.heap, next.heap);
			final TreeMap<Local, Object> widenedLocals = next.localTable.toStream().foldLeft(new F2<TreeMap<Local, Object>, P2<Local, Object>, TreeMap<Local, Object>>() {
				@Override
				public TreeMap<Local, Object> f(final TreeMap<Local, Object> prevTree, final P2<Local, Object> nextBinding) {
					return prevTree.update(nextBinding._1(), new F<Object, Object>() {
						
						@Override
						public Object f(final Object a) {
							return valueMonad.widen(a, nextBinding._2());
						}
					}, nextBinding._2());
				}
			}, prev.localTable);
			return new LocalMap(widenedLocals, widenedHeap, Continuation.widen(prev.cont, next.cont));
		}

		@Override
		public LocalMap join(final LocalMap first, final LocalMap second) {
			final StupidHeapWithReturnSlot joinedHeap = StupidHeapWithReturnSlot.lattice.join(first.heap, second.heap);
			final TreeMap<Local, Object> joinedLocals = first.localTable.toStream().foldLeft(new F2<TreeMap<Local, Object>, P2<Local, Object>, TreeMap<Local, Object>>() {
				@Override
				public TreeMap<Local, Object> f(final TreeMap<Local, Object> a, final P2<Local, Object> b) {
					return a.update(b._1(), new F<Object, Object>() {
						
						@Override
						public Object f(final Object a) {
							return valueMonad.join(a, b._2());
						}
					}, b._2());
				}
			}, second.localTable);
			return new LocalMap(joinedLocals, joinedHeap, Continuation.join(first.cont, second.cont));
		}

		@Override
		public boolean lessEqual(final LocalMap first, final LocalMap second) {
			final boolean stateCompare = first.localTable.toStream().forall(new F<P2<Local, Object>, Boolean>() {
				@Override public Boolean f(final P2<Local, Object> a) {
					if(!second.localTable.contains(a._1())) {
						return false;
					}
					return valueMonad.lessEqual(a._2(), second.localTable.get(a._1()).some());
				}
			});
			final boolean heapCompare = StupidHeapWithReturnSlot.lattice.lessEqual(first.heap, second.heap);
			final boolean continuationCompare = Continuation.lessEqual(first.cont, second.cont);
			return stateCompare && heapCompare && continuationCompare;
		}

		@Override
		public void inject(final Monads<PV, LocalMap> monads) {
			this.valueMonad = monads.valueMonad;
		}
	};

	public final TreeMap<Local, Object> localTable;
	public final StupidHeapWithReturnSlot heap;
	private static final Ord<Local> localOrdering = OrderingUtil.equalityOrdering();
	private final Continuation<Pair<SootMethod, Unit>> cont;

	public LocalMap(final TreeMap<Local, Object> localTable, final StupidHeapWithReturnSlot h, final Continuation<Pair<SootMethod, Unit>> cont) {
		this.localTable = localTable;
		this.heap = h;
		this.cont = cont;
	}
	
	public LocalMap() {
		this(TreeMap.empty(localOrdering), StupidHeapWithReturnSlot.empty, Continuation.bottom(OptimisticInformationFlow.continuationOrd));
	}

	public LocalMap withLocals(final Map<Local, Object> args) {
		return new LocalMap(TreeMap.fromMutableMap(localOrdering, args), this.heap, this.cont);
	}

	public Object lookup(final Local op) {
		return localTable.get(op).orSome(valueMonad.lift(PV.bottom));
	}
	
	public static ValueMonad<PV> valueMonad;
	public final static NeedsMonads<PV, LocalMap> injector = new NeedsMonads<PV, LocalMap>() {
		@Override
		public void inject(final Monads<PV, LocalMap> monads) {
			valueMonad = monads.valueMonad;
		}
	};

	public LocalMap set(final Local lhs, final Object value) {
		if(value instanceof Option) { 
			throw new IllegalArgumentException();
		}
		return new LocalMap(localTable.set(lhs, value), heap, this.cont);
	}

	public LocalMap withHeap(final StupidHeapWithReturnSlot newHeap) {
		return new LocalMap(localTable, newHeap, this.cont);
	}

	public Object get(final Local base) {
		return localTable.get(base).orSome(PV.bottom);
	}
	
	@Override
	public String toString() {
		return "[STATE: " + this.localTable.toString() + "|HEAP: " + this.heap.toString() + "|CONT: " + cont + "]";
	}

	@Override public LocalMap withContinuation(final Continuation<Pair<SootMethod, Unit>> cont) {
		return new LocalMap(localTable, heap, cont);
	}

	@Override public Continuation<Pair<SootMethod, Unit>> getContinuation() {
		return this.cont;
	}
}
