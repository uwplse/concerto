package edu.washington.cse.concerto.test;

import org.testng.annotations.Test;

public class TestLoops extends AbstractInterpreterTest {
	@Test
	public void testNestedLoops() {
		this.runTestProgram("Loop", "nestedLoop");
	}

	@Test
	public void testLoopReturnBackup() {
		this.runTestProgram("Loop", "returnBackup");
	}
	
	@Test
	public void testLoopConditionMutation() {
		this.runTestProgram("Loop", "loopConditionMutation");
	}

	@Test
	public void testFiniteLoopWithJoin() {
		this.runTestProgram("SimpleLoop", "main");
	}
}
