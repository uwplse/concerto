package edu.washington.cse.concerto.interpreter.ai.injection;

import edu.washington.cse.concerto.interpreter.ai.ValueMonad;

public interface NeedsValueMonad<AVal> {
	default public void injectValueMonad(final ValueMonad<AVal> vm) { }
}
