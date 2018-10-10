package edu.washington.cse.concerto.test.ai;

import ai.InformationFlow;
import edu.washington.cse.concerto.interpreter.ai.instantiation.optiflow.OptimisticInformationFlow;
import edu.washington.cse.concerto.interpreter.meta.NullReflectionModel;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestInformationFlowAnalysis extends AbstractInterpretationTest implements InformationFlowMixin {
	public TestInformationFlowAnalysis() {
		super(InformationFlow.class.getName(), new NullReflectionModel());
	}

	@Test
	public void testOptimisticInformationFlow() {
		this.runTestProgram("ignoreHeapSideEffects", OptimisticInformationFlow.class);
		assertResultSize(0);

		runTestWithSingleIntSink("directFlow");
		runTestWithSingleIntSink("testDirectFlowThroughHeap");
		runTestWithSingleIntSink("testLoopWidening");
		runTestWithSingleIntSink("testInterproceduralFlow");
		runTestWithSingleObjSink("testSubfieldChecking");
		runTestWithSingleObjSink("testConstructorResults");
	}

	private void runTestWithSingleObjSink(final String mainMethod) {
		this.runTestProgram(mainMethod, OptimisticInformationFlow.class);
		assertResultSize(1);
		this.assertResultEquals(0, mainMethod, "void sink(java.lang.Object)");
	}

	protected void runTestWithSingleIntSink(final String mainMethod) {
		this.runTestProgram(mainMethod, OptimisticInformationFlow.class);
		assertResultSize(1);
		this.assertResultEquals(0, mainMethod, "void sink(int)");
	}

	protected void assertResultSize(final int i) {
		Assert.assertEquals(this.stream.results.size(), i);
	}

	private void assertResultEquals(final int i, final String containingMethodName, final String sinkingSubSig) {
		Assert.assertTrue(i < this.stream.results.size());
		final Object result = this.stream.results.get(i);
		this.assertSinkInformation(result, containingMethodName, sinkingSubSig);
	}
}
