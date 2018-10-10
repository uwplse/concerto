package edu.washington.cse.concerto.interpreter.ai.injection;

import edu.washington.cse.concerto.interpreter.meta.Monads;

public interface NeedsMonads<AVal, AS> extends NeedsValueMonad<AVal>, NeedsStateMonad<AS, AVal>, Injectable {
	default public void inject(final Monads<AVal, AS> monads) {
		this.injectValueMonad(monads.valueMonad);
		this.injectStateMonad(monads.stateMonad);
	}
}
