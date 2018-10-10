package edu.washington.cse.concerto.interpreter.ai;

import soot.Value;

public interface StateValueUpdater<AS> {
	public AS updateForValue(Value v, AS state, Object value);
}
