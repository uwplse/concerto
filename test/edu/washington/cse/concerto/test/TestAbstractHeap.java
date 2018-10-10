package edu.washington.cse.concerto.test;

import org.testng.annotations.Test;

public class TestAbstractHeap extends AbstractInterpreterTest {
	@Test
	public void testObjectWidening() {
		this.runTestProgram("Loop");
	}
	
	@Test
	public void testAbstractBaseObject() {
		this.runTestProgram("AbstractReceiver");
	}
}
