package edu.washington.cse.concerto.test.ai;

import edu.washington.cse.concerto.interpreter.Interpreter;
import edu.washington.cse.concerto.interpreter.ai.AbstractInterpretation;
import edu.washington.cse.concerto.interpreter.meta.CombinedInterpretation;
import edu.washington.cse.concerto.interpreter.meta.MetaInterpreter;
import edu.washington.cse.concerto.interpreter.meta.ReflectionModel;
import edu.washington.cse.concerto.interpreter.state.GlobalState;

public abstract class AbstractInterpretationTest extends CombinedInterpretationTest {
	private final String className;
	private final ReflectionModel rm;
	private final String mainMethod;

	protected AbstractInterpretationTest(final String className, final ReflectionModel rm) {
		this(className, null, rm);
	}
	
	protected AbstractInterpretationTest(final String className, final String mainMethod, final ReflectionModel rm) {
		this.className = className;
		this.mainMethod = mainMethod;
		this.rm = rm;
	}

	@Override
	protected void runTestProgram(final Class<? extends AbstractInterpretation<?, ?, ?, ?>> klass, final GlobalState gs) {
		this.runTestProgram(klass, this.mainMethod, gs);
	}

	@Override
	protected void runTestProgram(final Class<? extends AbstractInterpretation<?, ?, ?, ?>> aiKlass, final String mainMethod, final GlobalState partialConcreteState) {
		Interpreter.resetGlobalState();
		final AbstractInterpretation<?, ?, ?, ?> ai = getAbstractInterpretation(aiKlass);
		assert ai != null;
		final CombinedInterpretation ci = new MetaInterpreter.NullInterpreter<>(ai, System.getProperty("concerto.classpath"), this.className, mainMethod, rm);
		this.preRunHook();
		runInterpreter(this.className, mainMethod, ci);
	}

	protected void preRunHook() {
	}
}
