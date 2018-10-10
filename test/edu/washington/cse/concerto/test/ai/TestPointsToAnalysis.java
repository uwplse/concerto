package edu.washington.cse.concerto.test.ai;

import org.testng.annotations.Test;

import edu.washington.cse.concerto.interpreter.ai.instantiation.pta.BasicInterpreter;

public class TestPointsToAnalysis extends FrameworkAITest {
	protected TestPointsToAnalysis() {
		super(BasicInterpreter.class);
	}
	
	@Test
	public void testPTA() {
		this.runTestProgram("pta.input");
	}
	
	@Test
	public void testPTAIndirection() {
		this.runTestProgram("pta_ind.input");
	}
}
