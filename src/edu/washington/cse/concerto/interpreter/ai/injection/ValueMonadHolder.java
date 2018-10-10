package edu.washington.cse.concerto.interpreter.ai.injection;

import edu.washington.cse.concerto.interpreter.ai.ValueMonad;
import edu.washington.cse.concerto.interpreter.meta.Monads;

public class ValueMonadHolder<AVal, AS> implements NeedsMonads<AVal, AS> {
	public ValueMonad<AVal> vMonad;
	
	@Override
	public void inject(final Monads<AVal, AS> monads) {
		this.vMonad = monads.valueMonad;
	}
	
}
