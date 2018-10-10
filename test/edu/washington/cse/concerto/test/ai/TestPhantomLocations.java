package edu.washington.cse.concerto.test.ai;

import heap.concrete.Main;

import org.testng.annotations.Test;

import edu.washington.cse.concerto.interpreter.ai.instantiation.array.ArrayBoundsChecker;
import edu.washington.cse.concerto.interpreter.ai.instantiation.pta.BasicInterpreter;
import edu.washington.cse.concerto.interpreter.meta.PackageBasedOracle;

public class TestPhantomLocations extends AbstractCombinedInterpretationTest {
	protected TestPhantomLocations() {
		super(Main.class.getName(), "main", new PackageBasedOracle("heap.concrete", "heap.abs"));
	}

	@Test
	public void testPointsToAnalysis() {
		this.runTestProgram(BasicInterpreter.class, 1);
	}
	
	@Test
	public void testIntervalAnalysis() {
		this.runTestProgram(ArrayBoundsChecker.class, 2);
	}
}
