package edu.washington.cse.concerto.test;

import org.testng.annotations.Test;

public class TestStateReification extends AbstractInterpreterTest {
	@Test
	public void testConcreteReification() {
		this.runTestProgram("ReifiedState", "testBasicMerging", 3, 4, 6);
		this.runTestProgram("ReifiedState", "testHarmlessLoop", 3, 4);
		this.runTestProgram("ReifiedState", "testWidening", 3, 4);
		this.runTestProgram("ReifiedState", "testBoundedLoops", 5, 1, 2, 3, 4, 5, 6);
		this.runTestProgram("ReifiedState", "testMergingAtCall", 3, 4, 6);
		this.runTestProgram("ReifiedState", "testWideningInRecursion", 1);
	}
}
