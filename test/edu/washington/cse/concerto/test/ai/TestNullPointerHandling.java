package edu.washington.cse.concerto.test.ai;

import org.testng.annotations.Test;

import ai.NullPointerChecking;
import ai.NullPointerChecking.ApplicationClass;
import edu.washington.cse.concerto.interpreter.ai.instantiation.array.ArrayBoundsChecker;
import edu.washington.cse.concerto.interpreter.meta.TypeOracle;

public class TestNullPointerHandling extends AbstractCombinedInterpretationTest {
	public TestNullPointerHandling() {
		super(NullPointerChecking.class.getName(), new TypeOracle() {
			@Override
			public TypeOwner classifyType(final String className) {
				if(className.equals(ApplicationClass.class.getName())) {
					return TypeOwner.APPLICATION;
				} else {
					return TypeOwner.FRAMEWORK;
				}
			}
		});
	} 
	
	@Test
	public void testNullHandling() {
		this.runTestProgram("testSimpleNullCheck", ArrayBoundsChecker.class);
	}
}
