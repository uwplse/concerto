package edu.washington.cse.concerto.test.ai;

import ai.CombinedInformationFlow;
import edu.washington.cse.concerto.interpreter.ai.instantiation.optiflow.OptimisticInformationFlow;
import edu.washington.cse.concerto.interpreter.ai.instantiation.optiflow.TaintFlow;
import edu.washington.cse.concerto.interpreter.meta.TypeOracle;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CombinedInformationFlowTest extends AbstractCombinedInterpretationTest implements InformationFlowMixin {
	public CombinedInformationFlowTest() {
		super(CombinedInformationFlow.class.getName(), new TypeOracle() {
			@Override public TypeOwner classifyType(final String className) {
				if(className.toLowerCase().contains("application")) {
					return TypeOwner.APPLICATION;
				} else {
					return TypeOwner.FRAMEWORK;
				}
			}
		});
	}

	private void runInformationFlowTest(final String mainMethod, final String sinkingMethod, final String sinkArg) {
		this.runTestProgram(mainMethod, OptimisticInformationFlow.class);
		Assert.assertEquals(this.stream.results.size(), 1);
		Assert.assertTrue(this.stream.results.get(0) instanceof TaintFlow);
		final TaintFlow tf = (TaintFlow) this.stream.results.get(0);
		this.assertSinkInformation(this.stream.results.get(0), sinkingMethod, "void sink(" + sinkArg + ")");
	}

	@Test
	public void testCombinedInformationFlow() {
		this.runInformationFlowTest("basicTest", "basicTest", "int");
		this.runInformationFlowTest("heapTest", "heapTest", "java.lang.Object");
		this.runInformationFlowTest("instrumentInvokeTest", "instrumentInvokeTest", "int");
		this.runInformationFlowTest("instrumentInvokeInAITest", "callWithInvoke", "int");
		this.runInformationFlowTest("instrumentDoubleInvokeInAITest", "invokeSinkWithInvokeSource", "int");
		this.runInformationFlowTest("testTreeWideningInConcrete", "sinkSelf", "int");
		this.runInformationFlowTest("testConcreteHeapTainting", "testConcreteHeapTainting", "java.lang.Object");
	}
}
