package edu.washington.cse.concerto.test;

import org.testng.annotations.Test;

public class TestRecursionFixpoint extends AbstractInterpreterTest {
	@Test
	public void testSimpleRecursion() {
		this.runTestProgram("Recursion", "basicRecursion");
	}
	
	@Test
	public void testHeapRecursion() {
		this.runTestProgram("Recursion", "heapRecursion");
	}
	
	@Test
	public void testExpandedCycle() {
		this.runTestProgram("Recursion", "expandedCycle");
	}
	
	@Test
	public void testCycleWithLoop() {
		this.runTestProgram("Recursion", "withLoop");
	}
}
