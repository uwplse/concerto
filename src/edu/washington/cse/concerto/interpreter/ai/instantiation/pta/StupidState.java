package edu.washington.cse.concerto.interpreter.ai.instantiation.pta;

import edu.washington.cse.concerto.Function;
import edu.washington.cse.concerto.interpreter.ai.Continuation;
import edu.washington.cse.concerto.interpreter.ai.ContinuationBasedState;
import edu.washington.cse.concerto.interpreter.ai.MappedValueLatticeHelper;
import edu.washington.cse.concerto.interpreter.ai.MonadicLattice;
import edu.washington.cse.concerto.interpreter.ai.ValueMonad;
import edu.washington.cse.concerto.interpreter.ai.injection.ValueMonadHolder;
import edu.washington.cse.concerto.interpreter.meta.Monads;
import fj.Ord;
import fj.data.Option;
import fj.data.TreeMap;

import java.util.Map;

public class StupidState implements ContinuationBasedState<ReturnSite, StupidState> {
	public final static StupidState empty = new StupidState(TreeMap.empty(Ord.stringOrd), StupidHeap.empty, Continuation.bottom(ReturnSite.contiuationOrd));
	public final static ValueMonadHolder<JValue, StupidState> vmHolder = new ValueMonadHolder<>();
	
	public final fj.data.TreeMap<String, Object> state;
	protected final StupidHeap heap;

	public static MonadicLattice<StupidState, JValue, StupidState> lattice = new MonadicLattice<StupidState, JValue, StupidState>() {
		
		private ValueMonad<JValue> vMonad;
		private MappedValueLatticeHelper<String, Object> mapHelper;

		@Override
		public StupidState widen(final StupidState prev, final StupidState next) {
			final StupidHeap widenedH = StupidHeap.lattice.widen(prev.heap, next.heap);
			final TreeMap<String, Object> newState = mapHelper.widen(prev.state, next.state);
			return new StupidState(newState, widenedH, Continuation.widen(prev.cont, next.cont));
		}
		
		@Override
		public boolean lessEqual(final StupidState first, final StupidState second) {
			if(!StupidHeap.lattice.lessEqual(first.heap, second.heap)) {
				return false;
			}
			return mapHelper.lessEqual(first.state, second.state) && Continuation.lessEqual(first.cont, second.cont);
		}
		
		@Override
		public StupidState join(final StupidState first, final StupidState second) {
			final StupidHeap widenedH = StupidHeap.lattice.join(first.heap, second.heap);
			final TreeMap<String, Object> newState = mapHelper.join(first.state, second.state);
			return new StupidState(newState, widenedH, Continuation.join(first.cont, second.cont));
		}

		@Override
		public void inject(final Monads<JValue, StupidState> m) {
			this.vMonad = m.valueMonad;
			this.mapHelper = new MappedValueLatticeHelper<>(vMonad);
		}
	};
	private final Continuation<ReturnSite> cont;

	public StupidState(final TreeMap<String, Object> newState, final StupidHeap heap, final Continuation<ReturnSite> cont) {
		this.state = newState;
		this.heap = heap;
		this.cont = cont;
	}
	
	public StupidState put(final String string, final Object value) {
		return new StupidState(state.set(string, value), heap, cont);
	}
	public StupidState setHeap(final StupidHeap newHeap) {
		return new StupidState(state, newHeap, cont);
	}
	
	public StupidState withHeap(final Function<StupidHeap, StupidHeap> function) {
		return setHeap(function.apply(heap));
	}
	
	public Object get(final String name) {
		final Option<Object> option = state.get(name);
		return option.orSome(vmHolder.vMonad.lift(JValue.bottom));
	}
	
	public static StupidState lift(final Map<String, Object> startBindings, final StupidHeap startHeap, final Continuation<ReturnSite> cont) {
		return new StupidState(TreeMap.fromMutableMap(Ord.stringOrd, startBindings), startHeap, cont);
	}
	
	@Override
	public String toString() {
		return "[STATE: " + this.state + " & HEAP: " +heap+"]";
	}

	@Override public StupidState withContinuation(final Continuation<ReturnSite> cont) {
		return new StupidState(state, heap, cont);
	}

	@Override public Continuation<ReturnSite> getContinuation() {
		return this.cont;
	}
}
