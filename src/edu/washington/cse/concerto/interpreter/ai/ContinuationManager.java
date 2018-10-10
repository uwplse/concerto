package edu.washington.cse.concerto.interpreter.ai;

import edu.washington.cse.concerto.interpreter.meta.InstrumentedState;
import fj.Ord;
import fj.P;
import fj.P2;
import fj.data.Option;
import fj.data.Set;

import java.util.HashMap;

public class ContinuationManager<Cont, AnalysisContext, State extends ContinuationBasedState<Cont, State>> {
	private final Ord<Option<Cont>> continuationOrd;
	private final StateMonad<State, ?> stateMonad;
	public final HashMap<AnalysisContext, InstrumentedState> initialStates;

	public ContinuationManager(final Ord<Cont> continuationOrd, final StateMonad<State, ?> stateMonad) {
		this.continuationOrd = Ord.optionOrd(continuationOrd);
		this.stateMonad = stateMonad;
		this.initialStates = new HashMap<>();
	}

	/*
	 * Returns (st, true) if the incoming state is NOT less or equal to our current initial state. If this is the case, then the
	 * returned state will be instrumented with a continuation with a version number which can help ensure termination.
	 *
	 * Returns (st, false) if the incoming state IS less or equal to the current initial state. If so, the returned state
	 * is the currently registered initial state.
	 */
	public P2<InstrumentedState, Boolean> getInitialState(final InstrumentedState withContinuation, final AnalysisContext contextKey) {
		if(!this.initialStates.containsKey(contextKey)) {
			this.initialStates.put(contextKey, withContinuation);
			return P.p(withContinuation, true);
		} else {
			final InstrumentedState initialState = this.initialStates.get(contextKey);
			if(!this.stateMonad.lessEqual(withContinuation, initialState)) {
				/* ignoring continuation components, the new input state MUST be >= than the last initial state we saw. If so, bump the
				   version number up
				 */
				final Continuation<Cont> cont = stateMonad.map(initialState, ContinuationBasedState::getContinuation);
				assert this.stateMonad.lessEqual(initialState, stateMonad.mapState(withContinuation, ar -> ar.withContinuation(cont))) : "\n" + initialState  + "\n\n" + withContinuation;
				final InstrumentedState newIteration = stateMonad.mapState(withContinuation, ar -> ar.withContinuation(ar.getContinuation().gtThan(cont)));
				this.initialStates.put(contextKey, newIteration);
				return P.p(newIteration, true);
			} else {
				return P.p(initialState, false);
			}
		}
	}

	public boolean isCompatibleReturn(final AnalysisContext cont, final InstrumentedState state) {
		return this.stateMonad.map(this.initialStates.get(cont), ar ->
				stateMonad.map(state, ContinuationBasedState::getContinuation).version == ar.getContinuation().version
		);
	}

	public Continuation<Cont> initContinuation(final Option<Cont> cont) {
		return new Continuation<>(Set.single(continuationOrd, cont), 0);
	}

	public boolean hasInitialState(final AnalysisContext key) {
		return this.initialStates.containsKey(key);
	}

	public boolean hasEqualState(final InstrumentedState state, final AnalysisContext key) {
		return this.hasInitialState(key) && stateMonad.lessEqual(state, this.initialStates.get(key)) && stateMonad.lessEqual(this.initialStates.get(key), state);
	}
}
