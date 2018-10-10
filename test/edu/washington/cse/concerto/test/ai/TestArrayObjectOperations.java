package edu.washington.cse.concerto.test.ai;

import ai.ArraysInAI;
import edu.washington.cse.concerto.interpreter.ai.AbstractInterpretation;
import edu.washington.cse.concerto.interpreter.ai.instantiation.array.ArrayBoundsChecker;
import edu.washington.cse.concerto.interpreter.meta.ApplicationTokenBasedOracle;
import edu.washington.cse.concerto.interpreter.meta.NullReflectionModel;
import org.testng.annotations.Test;

public class TestArrayObjectOperations extends AbstractCombinedInterpretationTest {
	protected TestArrayObjectOperations() {
		super(ArraysInAI.class.getName(), null, new ApplicationTokenBasedOracle("Application"), new NullReflectionModel());
	}

	@Test
	public void testTypesWithPrimitives() {
		this.runTestProgram("testNondetIndexingInConcrete", ArrayBoundsChecker.class);
		this.runTestProgram("testOnlyAIIsArray", ArrayBoundsChecker.class);
		this.runTestProgram("testOnlyConcreteIsArray", ArrayBoundsChecker.class);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testObjectArrays() {
		final Class<? extends AbstractInterpretation<?, ?, ?, ?>>[] aiKlasses = getAbstractInterpreters();
		for(final Class<? extends AbstractInterpretation<?, ?, ?, ?>> ai : aiKlasses) {
			ArraysInAI.ApplicationVariantChecker.TARGET_AI = ai;
			this.runTestProgram("testConcreteArrays", ai);
			this.runTestProgram("testAbstractArrays", ai);
			this.runTestProgram("testCombinedArrays", ai);
			this.runTestProgram("testCombinedTypeChecking", ai);
		}
	}
}
