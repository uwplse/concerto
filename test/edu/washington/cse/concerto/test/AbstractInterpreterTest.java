package edu.washington.cse.concerto.test;

import java.util.Arrays;

import org.testng.Assert;

import soot.SootClass;
import edu.washington.cse.concerto.interpreter.Interpreter;
import edu.washington.cse.concerto.interpreter.exception.FailedObjectLanguageAssertionException;
import edu.washington.cse.concerto.interpreter.exception.InterpreterException;
import edu.washington.cse.concerto.interpreter.exception.ObjectProgramException;
import edu.washington.cse.concerto.interpreter.mock.SimpleForeignHeap;
import edu.washington.cse.concerto.interpreter.state.GlobalState;
import edu.washington.cse.concerto.interpreter.state.InMemoryGlobalState;
import edu.washington.cse.concerto.interpreter.state.NondetGlobalState;

@SuppressWarnings("unchecked")
public abstract class AbstractInterpreterTest {
	protected void runTestProgram(final String clsName) {
		this.runTestProgram(clsName, "main");
	}
	
	protected void runTestProgram(final String clsName, final String methodName) {
		this.runTestProgram(clsName, methodName, new NondetGlobalState());
	}
	
	protected void runTestProgram(final String clsName, final int... inputs) {
		this.runTestProgram(clsName, "main", inputs);
	}
	
	protected void runTestProgram(final String clsName, final String mainMethod, final int... inputs) {
		this.runTestProgram(clsName, mainMethod, new InMemoryGlobalState(inputs));
	}
	
	private void runTestProgram(final String clsName, final String mainMethod, final GlobalState gs) {
		final SootClass cls = Interpreter.setupSoot(clsName, System.getProperty("concerto.classpath"));
		final Interpreter<Void, Void> i = new Interpreter<>(gs);
		try {
			i.start(cls, mainMethod);
		} catch(final FailedObjectLanguageAssertionException e) {
			fail("Equality assertion failed", e, clsName, mainMethod);
		} catch(final ObjectProgramException e) {
			fail("Source program fault", e, clsName, mainMethod);
		} catch(final InterpreterException e) {
			fail("Unexpected interpreter exception", e, clsName, mainMethod);
		} catch(final Exception e) {
			fail("Interpreter threw an exception", e, clsName, mainMethod);
		}
	}
	
	@SafeVarargs
	protected final void assertProgramFaults(final String clsName, final String mainMethod, final Class<? extends ObjectProgramException>... exceptions) {
		this.assertProgramFaults(clsName, mainMethod, new NondetGlobalState(), exceptions);
	}
	
	protected final void assertProgramFaults(final String clsName, final String mainMethod, final GlobalState gs, final Class<? extends ObjectProgramException>... exceptions) {
		final SootClass cls = Interpreter.setupSoot(clsName, System.getProperty("concerto.classpath"));
		final Interpreter<Void, SimpleForeignHeap> i = new Interpreter<>(gs);
		try {
			i.start(cls, mainMethod);
			failNoThrow(clsName, mainMethod, exceptions);
		} catch(final ObjectProgramException e) {
			for(final Class<? extends ObjectProgramException> expected : exceptions) {
				if(e.getClass() == expected) {
					return;
				}
			}
			failNoThrow(clsName, mainMethod, exceptions, e);
		} catch(final Exception e) {
			failNoThrow(clsName, mainMethod, exceptions, e);
		}
	}

	private void failNoThrow(final String clsName, final String mainMethod, final Class<? extends ObjectProgramException>[] exceptions, final Exception e) {
		Assert.fail(noThrowMessage(clsName, mainMethod, exceptions) + " (It did throw a " + e.getClass().getName() + " exception.)", e);
	}

	private void failNoThrow(final String clsName, final String mainMethod, final Class<? extends ObjectProgramException>... exceptions) {
		Assert.fail(noThrowMessage(clsName, mainMethod, exceptions));
	}

	private String noThrowMessage(final String clsName, final String mainMethod, final Class<? extends ObjectProgramException>... exceptions) {
		final String[] s = new String[exceptions.length];
		int i = 0;
		for(final Class<? extends ObjectProgramException> cls : exceptions) {
			s[i++] = cls.getName();
		}
		final String exceptionString = Arrays.toString(s);
		return clsName + "." + mainMethod + " did not throw any of: " + exceptionString;
	}

	private void fail(final String message, final Exception e, final String cls, final String mainMethod) {
		Assert.fail("In " + cls + "." + mainMethod + ": " +message, e);
	}
}
