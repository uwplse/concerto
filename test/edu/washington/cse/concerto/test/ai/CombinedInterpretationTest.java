package edu.washington.cse.concerto.test.ai;

import edu.washington.cse.concerto.interpreter.ai.AbstractInterpretation;
import edu.washington.cse.concerto.interpreter.ai.ResultCollectingAbstractInterpretation;
import edu.washington.cse.concerto.interpreter.ai.ResultStream;
import edu.washington.cse.concerto.interpreter.exception.FailedObjectLanguageAssertionException;
import edu.washington.cse.concerto.interpreter.exception.InterpreterException;
import edu.washington.cse.concerto.interpreter.exception.ObjectProgramException;
import edu.washington.cse.concerto.interpreter.meta.CombinedInterpretation;
import edu.washington.cse.concerto.interpreter.state.GlobalState;
import edu.washington.cse.concerto.interpreter.state.InMemoryGlobalState;
import edu.washington.cse.concerto.interpreter.state.PartialConcreteState;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.List;

public abstract class CombinedInterpretationTest {
	protected final InMemoryResultStream stream = new InMemoryResultStream();

	protected void runTestProgram(final String mainMethod, final Class<? extends AbstractInterpretation<?, ?, ?, ?>> klass, final int... inputs) {
		this.runTestProgram(klass, mainMethod, new InMemoryGlobalState(inputs));
	}
	
	protected void runTestProgram(final String mainMethod, final Class<? extends AbstractInterpretation<?, ?, ?, ?>> klass, final String detFile) {
		this.runTestProgram(klass, mainMethod, new PartialConcreteState(detFile));
	}
	
	protected void runTestProgram(final Class<? extends AbstractInterpretation<?, ?, ?, ?>> klass, final int... inputs) {
		this.runTestProgram(klass, new InMemoryGlobalState(inputs));
	}
	
	protected void runTestProgram(final Class<? extends AbstractInterpretation<?, ?, ?, ?>> klass, final String detFile) {
		this.runTestProgram(klass, new PartialConcreteState(detFile));
	}

	protected void runTestProgram(final String mainMethod, final Class<? extends AbstractInterpretation<?, ?, ?, ?>>[] klass, final int... inputs) {
		for(final Class<? extends AbstractInterpretation<?, ?, ?, ?>> klass1 : klass) {
			System.out.println("Testing: " + mainMethod + " X " + klass1.getName());
			this.runTestProgram(klass1, mainMethod, new InMemoryGlobalState(inputs));
		}
	}

	protected void runTestProgram(final String mainMethod, final Class<? extends AbstractInterpretation<?, ?, ?, ?>>[] klass, final String detFile) {
		// TODO: refactor this once we have a way to rewind global state
		for(final Class<? extends AbstractInterpretation<?, ?, ?, ?>> klass1 : klass) {
			System.out.println("Testing: " + mainMethod + " X " + klass1.getName());
			this.runTestProgram(klass1, mainMethod, new PartialConcreteState(detFile));
		}
	}

	protected void runTestProgram(final Class<? extends AbstractInterpretation<?, ?, ?, ?>> klass[], final int... inputs) {
		this.runTestProgram(klass, new InMemoryGlobalState(inputs));
	}

	private void runTestProgram(final Class<? extends AbstractInterpretation<?, ?, ?, ?>>[] klasses, final GlobalState globalState) {
		for(final Class<? extends AbstractInterpretation<?, ?, ?, ?>> klass : klasses) {
			this.runTestProgram(klass, globalState);
		}
	}

	protected void runTestProgram(final Class<? extends AbstractInterpretation<?, ?, ?, ?>> klass[], final String detFile) {
		this.runTestProgram(klass, new PartialConcreteState(detFile));
	}

	protected abstract void runTestProgram(Class<? extends AbstractInterpretation<?, ?, ?, ?>> klass, GlobalState partialConcreteState);
	protected abstract void runTestProgram(Class<? extends AbstractInterpretation<?, ?, ?, ?>> klass, String mainMethod, GlobalState partialConcreteState);

	protected AbstractInterpretation<?, ?, ?, ?> getAbstractInterpretation(final Class<? extends AbstractInterpretation<?, ?, ?, ?>> aiKlass) {
		final AbstractInterpretation<?, ?, ?, ?> ai;
		try {
			ai = aiKlass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			Assert.fail("Failed to instantiate the analysis", e);
			return null;
		}
		if(ai instanceof ResultCollectingAbstractInterpretation) {
			stream.results.clear();
			((ResultCollectingAbstractInterpretation) ai).setResultStream(stream);
		}
		return ai;
	}


	protected void runInterpreter(final String clsName, final String mainMethod, final CombinedInterpretation mInterpreter) {
		try {
			mInterpreter.run();
		} catch(final FailedObjectLanguageAssertionException e) {
			fail("Source program assertion failed", e, clsName, mainMethod);
		} catch(final ObjectProgramException e) {
			fail("Source program fault", e, clsName, mainMethod);
		} catch(final InterpreterException e) {
			fail("Unexpected interpreter exception", e, clsName, mainMethod);
		} catch(final Exception e) {
			fail("Interpreter threw an exception", e, clsName, mainMethod);
		}
	}

	protected void fail(final String message, final Exception e, final String cls, final String mainMethod) {
		Assert.fail("In " + cls + "." + mainMethod + ": " +message, e);
	}

	protected static class InMemoryResultStream implements ResultStream {
		public List<Object> results = new ArrayList<>();
		@Override public void outputAnalysisResult(final Object result) {
			this.results.add(result);
		}
	}
}
