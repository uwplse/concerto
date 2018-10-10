package edu.washington.cse.concerto.test.ai;

import edu.washington.cse.concerto.interpreter.ai.instantiation.array.ArrayBoundsChecker;
import edu.washington.cse.concerto.interpreter.ai.instantiation.pta.BasicInterpreter;
import edu.washington.cse.concerto.interpreter.meta.ReflectionModel;
import edu.washington.cse.concerto.interpreter.meta.TypeOracle;
import heap.concrete.Reflection;
import org.testng.annotations.Test;

public class TestReflectionAPIs extends AbstractCombinedInterpretationTest {

	public static final ReflectionModel REFLECTION_TEST_MODEL = new InMemoryReflectionModel(new String[] {
		Reflection.ConcreteSideEffect.class.getName(),
		Reflection.AbstractSideEffect.class.getName(),
		Reflection.ApplicationHandler.class.getName(),
		Reflection.HandlerWrapper.class.getName(),
	}, new String[] {
			Reflection.Intf.class.getName() + " doCommand(int)",
			"int getF()"
	});

	protected TestReflectionAPIs() {
		super(Reflection.class.getName(), null, new TypeOracle() {
			@Override
			public TypeOwner classifyType(final String className) {
				if(className.equals(Reflection.AbstractSideEffect.class.getName()) 
						|| Reflection.ApplicationHandler.class.getName().equals(className) 
						|| Reflection.Application.class.getName().equals(className)) {
					return TypeOwner.APPLICATION;
				} else {
					return TypeOwner.FRAMEWORK;
				}
			}
		}, REFLECTION_TEST_MODEL);
	}

	@Test
	public void testSideEffectConstructors() {
		this.runTestProgram("testSideEffectAllocation", getAbstractInterpreters());
	}
	
	@Test
	public void testReflectionIO() {
		 this.runTestProgram("testReflectionFromIO", getAbstractInterpreters(), 1, 0);
	}
	
	@Test
	public void testNondetReflectiveAllocation() {
		Reflection.NondetAllocChecker.targetDomain = "arr";
		this.runTestProgram("testNondetAllocation", ArrayBoundsChecker.class);
		Reflection.NondetAllocChecker.targetDomain = "jv";
		this.runTestProgram("testNondetAllocation", BasicInterpreter.class);
	}
	
	@Test
	public void testNondetAllocationInBranch() {
		this.runTestProgram("testAllocationInBranch", getAbstractInterpreters());
	}
	
	@Test
	public void testAIReflectiveAllocations() {
		this.runTestProgram("testAIReflectiveAllocations", ArrayBoundsChecker.class);
	}
	
	@Test
	public void testEndToEnd() {
		this.runTestProgram("testEndToEnd", getAbstractInterpreters(), 3, 2, 0);
	}
	
	@Test
	public void testAIReflectiveInvocation() {
		this.runTestProgram("testInvokeInAI", getAbstractInterpreters());
		this.runTestProgram("testInvokeBounds", getAbstractInterpreters());
	}

	@Test
	public void testNullaryInvoke() {
		this.runTestProgram("testInvokeNullConcrete", getAbstractInterpreters());
		this.runTestProgram("testInvokeNullAbstractCallee", getAbstractInterpreters());
		this.runTestProgram("testInvokeNullInAI", getAbstractInterpreters());
	}

	@Test
	public void testTypeOwnerFiltering() {
		this.runTestProgram("testFullResolution", getAbstractInterpreters());
	}

	@Test
	public void testArrayArguments() {
		this.runTestProgram("testInvokeIntArrayArg", getAbstractInterpreters());
		this.runTestProgram("testInvokeObjectArrayArg", getAbstractInterpreters());
	}
}
