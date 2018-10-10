package edu.washington.cse.concerto.test.ai;

import ai.CombinedRecursion;
import edu.washington.cse.concerto.interpreter.ai.instantiation.array.ArrayBoundsChecker;
import edu.washington.cse.concerto.interpreter.ai.instantiation.optiflow.OptimisticInformationFlow;
import edu.washington.cse.concerto.interpreter.ai.instantiation.pta.BasicInterpreter;
import edu.washington.cse.concerto.interpreter.meta.ApplicationTokenBasedOracle;
import edu.washington.cse.concerto.interpreter.meta.NullReflectionModel;
import org.testng.annotations.Test;

public class TestCombinedInterpretationWidening extends AbstractCombinedInterpretationTest {
	public TestCombinedInterpretationWidening() {
		super(CombinedRecursion.class.getName(), null, new ApplicationTokenBasedOracle("Application"), new NullReflectionModel());
	}

	@Test
	public void testCombinedWidening() {
		this.runTestProgram("main", OptimisticInformationFlow.class);
		this.runTestProgram("main", ArrayBoundsChecker.class);
		this.runTestProgram("main", BasicInterpreter.class);
	}
}
