package edu.washington.cse.concerto.test.ai;

import edu.washington.cse.concerto.interpreter.ai.instantiation.optiflow.TaintFlow;
import org.testng.Assert;

public interface InformationFlowMixin {
	default void assertSinkInformation(final Object result, final String containingMethodName, final String sinkingSubSig) {
		Assert.assertTrue(result instanceof TaintFlow);
		final TaintFlow tf = (TaintFlow) result;
		Assert.assertEquals(tf.containingMethod.getName(), containingMethodName);
		Assert.assertEquals(tf.sinkingMethod.getSubSignature().toString(), sinkingSubSig);
	}
}
