package edu.washington.cse.concerto.test;

import org.testng.annotations.Test;

public class TestForeignHeap extends AbstractInterpreterTest {
	
	@Test
	public void testBasicFunctionality() {
		this.runTestProgram("ForeignHeap", "readWrite");
		this.runTestProgram("ForeignHeap", "transitiveWrite");
	}
	
	@Test
	public void testWriteInBranch() {
		this.runTestProgram("ForeignHeap", "writeInBranch");
		this.runTestProgram("ForeignHeap", "writeInTrueBranch");
		this.runTestProgram("ForeignHeap", "writeInFalseBranch");
	}
	
	@Test
	public void testReturnMerge() {
		this.runTestProgram("ForeignHeap", "writeInReturn");
		this.runTestProgram("ForeignHeap", "writeInReturnBranches");
	}
}
