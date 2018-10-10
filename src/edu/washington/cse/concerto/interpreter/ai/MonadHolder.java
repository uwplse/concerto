package edu.washington.cse.concerto.interpreter.ai;

import edu.washington.cse.concerto.interpreter.ai.injection.NeedsMonads;
import edu.washington.cse.concerto.interpreter.meta.Monads;

public class MonadHolder<AVal, AS> implements NeedsMonads<AVal, AS> {
	public Monads<AVal, AS> monads;
	@Override
	public void inject(final Monads<AVal, AS> monads) {
		this.monads = monads;
	}
	
}
