package edu.washington.cse.concerto.test;

import org.testng.annotations.Test;

public class TestHeap extends AbstractInterpreterTest {
	@Test
	public void testCombineReturnHeaps() {
		this.runTestProgram("Interface");
	}
	
	@Test
	public void testBackupHeap() {
		this.runTestProgram("Return", "backup");
		this.runTestProgram("Return", "nestedReturn");
		this.runTestProgram("Return", "transitiveSideEffect");
	}
	
	@Test
	public void testWeakUpdate() {
		this.runTestProgram("Objects", "simpleAliasing");
	}
}
