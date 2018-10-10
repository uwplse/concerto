package edu.washington.cse.concerto.test.ai;

import edu.washington.cse.concerto.interpreter.ai.AbstractInterpretation;
import edu.washington.cse.concerto.interpreter.ai.instantiation.array.ArrayBoundsChecker;
import edu.washington.cse.concerto.interpreter.ai.instantiation.optiflow.OptimisticInformationFlow;
import edu.washington.cse.concerto.interpreter.ai.instantiation.pta.BasicInterpreter;
import heap.concrete.Reflection;
import org.testng.annotations.Test;

public class TestAbstractInterpretationWithReflection extends AbstractInterpretationTest {

	protected TestAbstractInterpretationWithReflection() {
		super(Reflection.class.getName(), TestReflectionAPIs.REFLECTION_TEST_MODEL);
	}
	
	@Test
	public void testArrayAnalysis() {
		runReflectionTests(ArrayBoundsChecker.class);
	}

	@Test
	public void testPtsAnalysis() {
		runReflectionTests(BasicInterpreter.class);
	}

	@Test
	public void testIflowAnalysis() {
		runReflectionTests(OptimisticInformationFlow.class);
	}

	protected void runReflectionTests(final Class<? extends AbstractInterpretation<?, ?, ?, ?>> analysisKlass) {
		this.runTestProgram("testSideEffectAllocation", analysisKlass);
		this.runTestProgram("testReflectionFromIO", analysisKlass, 1, 0);
		this.runTestProgram("testNondetAllocation", analysisKlass);
		this.runTestProgram("testAllocationInBranch", analysisKlass);
		this.runTestProgram("testAIReflectiveAllocations", analysisKlass);
		this.runTestProgram("testEndToEnd", analysisKlass, 3, 2, 0);
		this.runTestProgram("testInvokeInAI", analysisKlass);
		this.runTestProgram("testInvokeBounds", analysisKlass);
		this.runTestProgram("testInvokeNullConcrete", analysisKlass);
		this.runTestProgram("testInvokeNullAbstractCallee", analysisKlass);
		this.runTestProgram("testInvokeNullInAI", analysisKlass);
		this.runTestProgram("testFullResolution", analysisKlass);
		this.runTestProgram("testInvokeObjectArrayArg", analysisKlass);
		this.runTestProgram("testInvokeIntArrayArg", analysisKlass);
	}
}
