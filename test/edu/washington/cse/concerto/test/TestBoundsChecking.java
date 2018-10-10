package edu.washington.cse.concerto.test;

import org.testng.annotations.Test;

import edu.washington.cse.concerto.interpreter.exception.OutOfBoundsArrayAccessException;

public class TestBoundsChecking extends AbstractInterpreterTest {
	@Test
	public void testBasicBoundsChecks() {
		this.runTestProgram("BoundChecks", "inBoundsAccesses");
		assertThrows("outOfBoundsWrite");
		assertThrows("outOfBoundsRead");
	}

	private void assertThrows(final String methodName) {
		this.assertProgramFaults("BoundChecks", methodName, OutOfBoundsArrayAccessException.class);
	}
	
	private void assertOkay(final String methodName) {
		this.runTestProgram("BoundChecks", methodName);
	}
	
	@Test
	public void testMultiIndexBoundsChecks() {
		assertThrows("allOOBIndexReads");
		assertThrows("allOOBIndexWrites");
		assertOkay("oneInBoundIndexRead");
		assertOkay("oneInBoundIndexWrite");
	}
	
	@Test
	public void testMultiBaseBoundsChecks() {
		assertThrows("allOOBBaseWrites");
		assertThrows("allOOBBaseReads");
		assertOkay("oneInBoundsBaseRead");
		assertOkay("oneInBoundsBaseWrite");
	}
	
	@Test
	public void testCartesianBoundsChecks() {
		assertThrows("cartesianAllOOBRead");
		assertThrows("cartesianAllOOBWrite");
		assertOkay("cartesianOneInBoundsRead");
		assertOkay("cartesianOneInBoundsWrite");
	}
}
