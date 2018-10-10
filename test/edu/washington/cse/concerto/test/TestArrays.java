package edu.washington.cse.concerto.test;

import org.testng.annotations.Test;

public class TestArrays extends AbstractInterpreterTest {
	@Test
	public void testLength() {
		this.runTestProgram("Arrays", "testNondetLength");
		this.runTestProgram("Arrays", "testDeterministicLength");
	}
	
	@Test
	public void testNondetIndexing() {
		this.runTestProgram("Arrays", "testNondetIndexingWrite");
		this.runTestProgram("Arrays", "testNondetIndexingRead");
		this.runTestProgram("Arrays", "testNondetIndexingReadAndWrite");
	}
	
	@Test
	public void testNondetSize() {
		this.runTestProgram("Arrays", "testNondetSizeReadAndWrite");
	}
	
	@Test
	public void testDefaultValues() {
		this.runTestProgram("Arrays", "testDefaultPrimitiveValue");
		this.runTestProgram("Arrays", "testDefaultObjectValue");
		this.runTestProgram("Arrays", "testNondetSizeDefaultValue");
	}

	@Test
	public void testRuntimeTypes() {
		this.runTestProgram("Arrays", "testArrayDowncast");
	}
}
