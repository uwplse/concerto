package edu.washington.cse.concerto.interpreter.ai;

import edu.washington.cse.concerto.interpreter.value.IValue;

public interface AbstractionFunction<AVal> {
	public AVal lift(IValue v);
}
