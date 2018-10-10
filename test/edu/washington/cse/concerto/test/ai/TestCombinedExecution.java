package edu.washington.cse.concerto.test.ai;

import ai.BasicTest;
import edu.washington.cse.concerto.interpreter.ai.instantiation.array.ArrayBoundsChecker;
import edu.washington.cse.concerto.interpreter.meta.TypeOracle;
import org.testng.annotations.Test;

public class TestCombinedExecution extends AbstractCombinedInterpretationTest {

	protected TestCombinedExecution() {
		super(BasicTest.class.getName(), new TypeOracle() {
			@Override
			public TypeOwner classifyType(final String className) {
				if(className.equals(BasicTest.ApplicationType.class.getName())) {
					return TypeOwner.APPLICATION;
				} else {
					return TypeOwner.FRAMEWORK;
				}
			}
		});
	}
	
	@Test
	public void testBasicIndexing() {
		this.runTestProgram("testIndexing", ArrayBoundsChecker.class);
	}
	
	@Test
	public void testBranchPropagation() {
		this.runTestProgram("testCombinedPropagation", ArrayBoundsChecker.class);
		this.runTestProgram("testPointerComparison", ArrayBoundsChecker.class);
		this.runTestProgram("testEqualsPropagation", ArrayBoundsChecker.class);
	}
	
	@Test
	public void testWidening() {
		this.runTestProgram("testWidening", ArrayBoundsChecker.class);
		this.runTestProgram("testWidening2", ArrayBoundsChecker.class);
	}
	
	@Test
	public void testObjectOperations() {
		this.runTestProgram("testDirectReadWrite", ArrayBoundsChecker.class);
		this.runTestProgram("testCombinedReadWrite", ArrayBoundsChecker.class);
		
		this.runTestProgram("testArrayLength", ArrayBoundsChecker.class);
	}
	
	@Test
	public void testEqualityTests() {
		this.runTestProgram("testNullPointerEquality", ArrayBoundsChecker.class);
	}
	
	@Test
	public void testRecursionFixpoint() {
		this.runTestProgram("testRecursion", ArrayBoundsChecker.class);
	}
	
	@Test
	public void testQuadNesting() {
		this.runTestProgram("testQuadNesting", ArrayBoundsChecker.class);
	}

	@Test
	public void testHeapSummaries() {
		this.runTestProgram("testAIHeapSummaries", ArrayBoundsChecker.class);
		this.runTestProgram("testHeapSummaries", ArrayBoundsChecker.class);
	}
}
