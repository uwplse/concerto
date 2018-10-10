package edu.washington.cse.concerto.test.ai;

import edu.washington.cse.concerto.interpreter.Interpreter;
import edu.washington.cse.concerto.interpreter.ai.AbstractInterpretation;
import edu.washington.cse.concerto.interpreter.ai.instantiation.array.ArrayBoundsChecker;
import edu.washington.cse.concerto.interpreter.ai.instantiation.optiflow.OptimisticInformationFlow;
import edu.washington.cse.concerto.interpreter.ai.instantiation.pta.BasicInterpreter;
import edu.washington.cse.concerto.interpreter.meta.MetaInterpreter;
import edu.washington.cse.concerto.interpreter.meta.NullReflectionModel;
import edu.washington.cse.concerto.interpreter.meta.ReflectionModel;
import edu.washington.cse.concerto.interpreter.meta.TypeOracle;
import edu.washington.cse.concerto.interpreter.state.GlobalState;

public abstract class AbstractCombinedInterpretationTest extends CombinedInterpretationTest {
	private final String clsName;
	private final String mainMethod;
	private final TypeOracle oracle;
	private final ReflectionModel rm;
	
	protected AbstractCombinedInterpretationTest(final String className, final String mainMethod, final TypeOracle oracle) {
		this(className, mainMethod, oracle, new NullReflectionModel());
	}

	protected AbstractCombinedInterpretationTest(final String className, final String mainMethod, final TypeOracle oracle, final ReflectionModel rm) {
		this.clsName = className;
		this.mainMethod = mainMethod;
		this.oracle = oracle;
		this.rm = rm;
	}
	
	protected AbstractCombinedInterpretationTest(final String className, final TypeOracle oracle) {
		this(className, null, oracle);
	}

	@Override
	protected void runTestProgram(final Class<? extends AbstractInterpretation<?, ?, ?, ?>> klass, final GlobalState gs) {
		runTestProgram(klass, this.mainMethod, gs);
	}

	@Override
	protected void runTestProgram(final Class<? extends AbstractInterpretation<?, ?, ?, ?>> aiKlass, final String mainMethod, final GlobalState gs) {
		Interpreter.resetGlobalState();
		final AbstractInterpretation<?, ?, ?, ?> ai = getAbstractInterpretation(aiKlass);
		final MetaInterpreter<?, ?, ?, ?> mInterpreter = new MetaInterpreter<>(ai, this.oracle,
				System.getProperty("concerto.classpath"), this.clsName, mainMethod, gs, rm);
		runInterpreter(this.clsName, mainMethod, mInterpreter);
	}

	@SuppressWarnings("unchecked") protected Class<? extends AbstractInterpretation<?, ?, ?, ?>>[] getAbstractInterpreters() {
		return new Class[] {
				ArrayBoundsChecker.class,
				OptimisticInformationFlow.class,
				BasicInterpreter.class
		};
	}

	@SafeVarargs protected final Class<? extends AbstractInterpretation<?, ?, ?, ?>>[] getAbstractInterpreters(final Class<? extends AbstractInterpretation<?, ?, ?, ?>>... ai) {
		return ai;
	}
}
