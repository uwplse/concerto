package edu.washington.cse.concerto.test.ai;

import ai.Recursion;
import edu.washington.cse.concerto.interpreter.ai.instantiation.array.ArrayBoundsChecker;
import edu.washington.cse.concerto.interpreter.meta.NullReflectionModel;
import org.testng.annotations.Test;

public class TestRecursionWidening extends AbstractInterpretationTest {
	protected TestRecursionWidening() {
		super(Recursion.class.getName(), new NullReflectionModel());
	}

	@Test
	public void testWideningForRecusion() {
		this.runTestProgram("basicCycle", ArrayBoundsChecker.class);
		this.runTestProgram("incrementalCycle", ArrayBoundsChecker.class);
	}
}
