package edu.washington.cse.concerto.test;

import edu.washington.cse.concerto.interpreter.ai.instantiation.array.ArrayBoundsChecker;
import edu.washington.cse.concerto.interpreter.meta.PackageBasedOracle;
import edu.washington.cse.concerto.interpreter.meta.TypeOracle;
import edu.washington.cse.concerto.test.ai.AbstractCombinedInterpretationTest;
import heap.concrete.SplitTypeMain;
import org.testng.annotations.Test;

import java.lang.reflect.Array;

public class TestPruningConcreteCalls extends AbstractCombinedInterpretationTest {
	public TestPruningConcreteCalls() {
		super(SplitTypeMain.class.getName(), new PackageBasedOracle("heap.concrete", "heap.abs"));
	}

	@Test
	public void testConcretePruning() {
		this.runTestProgram("directCall", ArrayBoundsChecker.class);
		this.runTestProgram("indirectCall", ArrayBoundsChecker.class);

		this.runTestProgram("allPruneDirect", ArrayBoundsChecker.class);
		this.runTestProgram("allPruneInvoke", ArrayBoundsChecker.class);
		this.runTestProgram("allocConstructorPrunes", ArrayBoundsChecker.class);
	}
}
