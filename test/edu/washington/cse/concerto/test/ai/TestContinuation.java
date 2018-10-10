package edu.washington.cse.concerto.test.ai;

import ai.Continuation;
import edu.washington.cse.concerto.interpreter.ai.instantiation.array.ArrayBoundsChecker;
import edu.washington.cse.concerto.interpreter.ai.instantiation.optiflow.OptimisticInformationFlow;
import edu.washington.cse.concerto.interpreter.ai.instantiation.pta.BasicInterpreter;
import edu.washington.cse.concerto.interpreter.meta.ApplicationTokenBasedOracle;
import edu.washington.cse.concerto.interpreter.meta.NullReflectionModel;
import org.testng.annotations.Test;

public class TestContinuation extends AbstractCombinedInterpretationTest {
	public TestContinuation() {
		super(Continuation.class.getName(), "main", new ApplicationTokenBasedOracle("Application"), new NullReflectionModel());
	}

	@Test
	public void testContination() {
		this.runTestProgram(ArrayBoundsChecker.class);
		this.runTestProgram(OptimisticInformationFlow.class);
		this.runTestProgram(BasicInterpreter.class);
	}
}
