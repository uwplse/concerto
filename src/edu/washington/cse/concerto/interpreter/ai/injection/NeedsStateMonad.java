package edu.washington.cse.concerto.interpreter.ai.injection;

import edu.washington.cse.concerto.interpreter.ai.StateMonad;

public interface NeedsStateMonad<AState, AVal> {
	default public void injectStateMonad(final StateMonad<AState, AVal> vm) { }
}