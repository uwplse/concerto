package edu.washington.cse.concerto.interpreter.ai;

import soot.toolkits.scalar.Pair;
import edu.washington.cse.concerto.Function;

public abstract class PureFunction<AState> implements Function<AState, Pair<AState, Object>> {
	@Override
	public final Pair<AState, Object> apply(final AState in) {
		return new Pair<>(in, this.innerApply(in));
	}

	protected abstract Object innerApply(final AState in);
}