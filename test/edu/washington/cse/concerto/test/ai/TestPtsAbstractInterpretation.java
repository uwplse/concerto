package edu.washington.cse.concerto.test.ai;

import edu.washington.cse.concerto.interpreter.BodyManager;
import edu.washington.cse.concerto.interpreter.ai.instantiation.pta.BasicInterpreter;
import edu.washington.cse.concerto.interpreter.meta.NullReflectionModel;
import heap.concrete.Main;
import org.testng.annotations.Test;

import java.util.Arrays;

public class TestPtsAbstractInterpretation {
	private static class AITestWrapper extends AbstractInterpretationTest {
		protected AITestWrapper(final Class<?> klass) {
			super(klass.getName(), new NullReflectionModel());
		}

		public void runTest(final String methodName) {
			this.runTestProgram(methodName, BasicInterpreter.class);
		}

		@Override protected void preRunHook() {
			BodyManager.setInclude(Arrays.asList("meta.framework", "meta.application"));
		}
	}


	@Test
	public void testBasicPointsToAnalysis() {
		new AITestWrapper(Main.class).runTest("main");
	}
}
