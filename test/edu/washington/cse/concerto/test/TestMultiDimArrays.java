package edu.washington.cse.concerto.test;

import org.testng.annotations.Test;

public class TestMultiDimArrays extends AbstractInterpreterTest {
	@Test
	public void deterministicMultiArrays() {
		this.runTestProgram("MultiDimensionalArrays", "completeAllocation");
		this.runTestProgram("MultiDimensionalArrays", "partialSingleDimAllocation");
		this.runTestProgram("MultiDimensionalArrays", "partialMultiDimAllocation");
	}
	
	@Test
	public void nonDeterministicMultiArrays() {
		this.runTestProgram("MultiDimensionalArrays", "completeNondetAllocation");
		this.runTestProgram("MultiDimensionalArrays", "mixedAllocation");
		this.runTestProgram("MultiDimensionalArrays", "partialNondetAllocation");
	}
}
