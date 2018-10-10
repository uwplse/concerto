package edu.washington.cse.concerto.interpreter.meta;

import edu.washington.cse.concerto.interpreter.ai.AbstractInterpretation;
import edu.washington.cse.concerto.interpreter.ai.Interruptible;

public interface CombinedInterpretation extends Interruptible {
	void run();
	@Override default void interrupt() {
		throw new UnsupportedOperationException();
	}
	AbstractInterpretation<?, ?, ?, ?> getAbstractInterpretation();
}
