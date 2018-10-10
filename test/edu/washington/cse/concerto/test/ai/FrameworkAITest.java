package edu.washington.cse.concerto.test.ai;

import edu.washington.cse.concerto.interpreter.ai.AbstractInterpretation;
import edu.washington.cse.concerto.interpreter.meta.YamlBasedOracle;
import edu.washington.cse.concerto.interpreter.meta.YamlReflectionModel;
import edu.washington.cse.concerto.interpreter.state.CombinedPartialState;
import edu.washington.cse.concerto.interpreter.util.YamlParser;
import meta.framework.FrameworkMain;

import java.util.List;

public class FrameworkAITest extends AbstractCombinedInterpretationTest {
	private static final String[] HANDLER_NAMES = new String[] {
			meta.application.ForwardingAction.class.getName(),
			meta.application.SimpleAction.class.getName(),
			meta.application.UnusedAction.class.getName(),
			meta.application.LoopAction.class.getName(),
			meta.application.ComparisonAction.class.getName(),
			meta.application.FaultAction.class.getName(),
			meta.application.SourceAction.class.getName(),
			meta.application.SinkAction.class.getName(),
			meta.application.MixedAction.class.getName()
	};
	private static final InMemoryReflectionModel REFLECTION_MODEL = new InMemoryReflectionModel(HANDLER_NAMES, new String[] {});
	private final Class<? extends AbstractInterpretation<?, ?, ?, ?>> analysisKlass;

	protected FrameworkAITest(final Class<? extends AbstractInterpretation<?, ?, ?, ?>> analysisKlass) {
		super(FrameworkMain.class.getName(), "testMain", new YamlBasedOracle(System.getProperty("concerto.yawn.test-config")), new YamlReflectionModel(System.getProperty("concerto.yawn.test-config")));
		this.analysisKlass = analysisKlass;
	}
	
	protected void runTestProgram(final int... inputs) {
		final int[] newInput = instrumentFrameworkStartup(inputs);
		super.runTestProgram(analysisKlass, newInput);
	}

	private int[] instrumentFrameworkStartup(final int[] inputs) {
		@SuppressWarnings("unchecked")
		final List<Integer> o = (List<Integer>) YamlParser.parseYamlConf(System.getProperty("concerto.yawn.test-config")).get("det-stream");
		final int[] newInput = new int[o.size() + inputs.length];
		int i = 0;
		for(final Integer elem : o) {
			newInput[i++] = elem;
		}
		System.arraycopy(inputs, 0, newInput, o.size(), inputs.length);
		return newInput;
	}
	
	protected void runTestProgram(final String inputName) {
		super.runTestProgram(analysisKlass, new CombinedPartialState(instrumentFrameworkStartup(new int[0]), System.getProperty("concerto.inputs")  + "/" + inputName));
	}
}
