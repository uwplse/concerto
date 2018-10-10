package edu.washington.cse.concerto.test;

import org.testng.annotations.Test;

import edu.washington.cse.concerto.interpreter.exception.NullPointerException;
import edu.washington.cse.concerto.interpreter.exception.OutOfBoundsArrayAccessException;

public class TestNullChecking extends AbstractInterpreterTest {
	
	private void assertThrows(final String methodName) {
		this.assertProgramFaults("NullChecks", methodName, NullPointerException.class);
	}
	
	private void assertOkay(final String methodName) {
		this.runTestProgram("NullChecks", methodName);
	}
	
	public void assertThrows(final String... methods) {
		for(final String m : methods) {
			assertThrows(m);
		}
	}
	
	@Test
	public void testBasicChecks() {
		assertThrows(
			"npeRead",
			"npeWrite",
			"npeArrayRead",
			"npeArrayWrite"
		);
	}
	
	@Test
	public void testMultiAccess() {
		assertOkay("oneNonNullObject");
		assertOkay("oneNonNullArray");
	}
	
	@Test
	public void testOOBMultiArray() {
		this.assertProgramFaults("NullChecks", "multiArrayOOBRead", OutOfBoundsArrayAccessException.class);
		this.assertProgramFaults("NullChecks", "multiArrayOOBWrite", OutOfBoundsArrayAccessException.class);
	}
}
