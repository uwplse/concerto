package edu.washington.cse.concerto.test.ai;

import edu.washington.cse.concerto.interpreter.ai.AbstractInterpretation;
import edu.washington.cse.concerto.interpreter.ai.instantiation.array.ArrayBoundsChecker;
import edu.washington.cse.concerto.interpreter.ai.instantiation.optiflow.OptimisticInformationFlow;
import edu.washington.cse.concerto.interpreter.ai.instantiation.pta.BasicInterpreter;
import edu.washington.cse.concerto.interpreter.meta.YamlBasedOracle;
import edu.washington.cse.concerto.interpreter.meta.YamlGlobalState;
import edu.washington.cse.concerto.interpreter.meta.YamlReflectionModel;
import meta.framework.FrameworkMain;
import org.testng.annotations.Test;

public class IntegrationTest extends AbstractCombinedInterpretationTest {
	protected IntegrationTest() {
		super(FrameworkMain.class.getName(), "main",
				new YamlBasedOracle(System.getProperty("concerto.yawn.config")),
				new YamlReflectionModel(System.getProperty("concerto.yawn.config")));
	}

	@Test
	public void runIntegrationTest() {
		this.runTest(ArrayBoundsChecker.class);
		this.runTest(OptimisticInformationFlow.class);
		this.runTest(BasicInterpreter.class);
	}

	private void runTest(final Class<? extends AbstractInterpretation<?, ?, ?, ?>> ai) {
		this.runTestProgram(ai, new YamlGlobalState(System.getProperty("concerto.yawn.config")));
	}
}
