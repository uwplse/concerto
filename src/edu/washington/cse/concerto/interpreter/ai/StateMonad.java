package edu.washington.cse.concerto.interpreter.ai;

import edu.washington.cse.concerto.Function;
import edu.washington.cse.concerto.interpreter.heap.Heap;
import edu.washington.cse.concerto.interpreter.meta.InstrumentedState;
import fj.F2;
import soot.toolkits.scalar.Pair;

public interface StateMonad<AS, AVal> {
	InstrumentedState join(InstrumentedState o1, InstrumentedState o2);
	InstrumentedState widen(InstrumentedState prev, InstrumentedState next);
	boolean lessEqual(InstrumentedState o1, InstrumentedState o2);
	
	InstrumentedState lift(AS state, Heap h);
	
	<R> R map(InstrumentedState state, Function<AS, R> mapper);
	InstrumentedState mapState(InstrumentedState state, Function<AS, AS> mapper);
	<R> R mapValue(final InstrumentedState state, Object value, ValueMapper<AVal, AS, R> mapper);
	InstrumentedState updateValue(InstrumentedState state, Object value, ValueStateTransformer<AVal, AS> mapper, StateUpdater<AS> updater);
	String toString(InstrumentedState newState);

	// convenience methods
	<V> InstrumentedState iterateState(InstrumentedState startState, Iterable<V> iter, F2<InstrumentedState, V, InstrumentedState> mapper);
	EvalResult mapToResult(InstrumentedState state, Function<AS, Pair<AS, Object>> result);
	InstrumentedState mapInHeaps(InstrumentedState srcState, InstrumentedState destState);
}
