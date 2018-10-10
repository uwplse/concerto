package edu.washington.cse.concerto.test.ai;

import ai.RelationalTest;
import edu.washington.cse.concerto.interpreter.ai.instantiation.array.ArrayBoundsChecker;
import edu.washington.cse.concerto.interpreter.meta.NullReflectionModel;
import org.testng.annotations.Test;

public class TestRelationalAnalysis extends AbstractInterpretationTest {
	protected TestRelationalAnalysis() {
		super(RelationalTest.class.getName(), new NullReflectionModel());
	}
	
	@Test
	public void testRelationalReasoning() {
		this.runTestProgram("testSimpleUB", ArrayBoundsChecker.class);
		this.runTestProgram("testEqualityBranches", ArrayBoundsChecker.class);
		this.runTestProgram("testLtChecking", ArrayBoundsChecker.class);
		this.runTestProgram("testEqualityClosure", ArrayBoundsChecker.class);
		this.runTestProgram("testLtClosure", ArrayBoundsChecker.class);
		this.runTestProgram("testJoinOperation", ArrayBoundsChecker.class);
		this.runTestProgram("testParamRelations", ArrayBoundsChecker.class);
		this.runTestProgram("testParamHeapRelations", ArrayBoundsChecker.class);
	}
}
