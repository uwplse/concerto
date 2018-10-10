package edu.washington.cse.concerto.test.ai;

import ai.CombinedReifiedState;
import edu.washington.cse.concerto.interpreter.ai.instantiation.array.ArrayBoundsChecker;
import edu.washington.cse.concerto.interpreter.ai.instantiation.optiflow.OptimisticInformationFlow;
import edu.washington.cse.concerto.interpreter.ai.instantiation.pta.BasicInterpreter;
import edu.washington.cse.concerto.interpreter.meta.ApplicationTokenBasedOracle;
import org.testng.annotations.Test;

public class TestCombinedStateReification extends AbstractCombinedInterpretationTest {
	public TestCombinedStateReification() {
		super(CombinedReifiedState.class.getName(), new ApplicationTokenBasedOracle("Application"));
	}

	@Test
	public void testCombinedReads() {
		this.runTestProgram("testSimpleReification", getAbstractInterpreters(), 3, 4, 6);
		this.runTestProgram("testMergeDueToInsensitivity", getAbstractInterpreters(), 3, 4, 6, 7);
		this.runTestProgram("testNoMergeForContext", getAbstractInterpreters(OptimisticInformationFlow.class, ArrayBoundsChecker.class), 3, 4, 6, 7);
		this.runTestProgram("testMergeDueToInsensitivityPTA", BasicInterpreter.class, 3, 4, 6, 7);

		this.runTestProgram("testMergeForAILoop", getAbstractInterpreters(), 3, 4, 6, 7);
		this.runTestProgram("testMergeForAIRecursion", getAbstractInterpreters(), 3, 4, 6, 7);
	}
}
