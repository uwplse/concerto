package edu.washington.cse.concerto.interpreter.ai.instantiation;

import java.util.HashMap;
import java.util.Map;

import soot.Unit;
import edu.washington.cse.concerto.interpreter.ai.MethodResult;
import edu.washington.cse.concerto.interpreter.meta.InstrumentedState;

public class InterpResult {
	public final Map<Unit, InstrumentedState> successorStates;
	public final MethodResult methodResult;
	public InterpResult(final Unit succOf, final InstrumentedState currState) {
		this.successorStates = new HashMap<>();
		successorStates.put(succOf, currState);
		this.methodResult = null;
	}

	public InterpResult(final InstrumentedState state, final Object returnValue) {
		this.successorStates = null;
		this.methodResult = new MethodResult(state, returnValue);
	}

	public InterpResult(final InstrumentedState state) {
		this.successorStates = null;
		this.methodResult = new MethodResult(state);
	}
	
	private InterpResult(final Map<Unit, InstrumentedState> states) {
		this.successorStates = new HashMap<>(states);
		this.methodResult = null;
	}

	public void add(final Unit succ, final InstrumentedState newState) {
		assert successorStates != null;
		this.successorStates.put(succ, newState);
	}

	public void merge(final InterpResult f) {
		successorStates.putAll(f.successorStates);
	}
	
	public static InterpResult of(final Map<Unit, InstrumentedState> states) {
		return new InterpResult(states);
	}
}