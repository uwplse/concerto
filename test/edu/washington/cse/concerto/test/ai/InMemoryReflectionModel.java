package edu.washington.cse.concerto.test.ai;

import edu.washington.cse.concerto.interpreter.meta.ReflectionModel;
import fj.data.Option;
import fj.data.Stream;
import soot.SootClass;

import java.util.Arrays;

public class InMemoryReflectionModel implements ReflectionModel {
	private final String[] klassArray;
	private final String[] methodSigs;

	public InMemoryReflectionModel(final String[] klassNames, final String[] methodSigs) {
		this.klassArray = klassNames;
		this.methodSigs = methodSigs;
	}

	@Override
	public String resolveClassName(final int key) {
		return this.klassArray[key];
	}
	
	@Override
	public String resolveSignature(final int key) {
    return this.methodSigs[key];
	}

	@Override public int reverseResolution(final SootClass sootClass) {
		Option<Integer> found = Stream.arrayStream(klassArray).indexOf(sootClass.getName()::equals);
		return found.orSome((klassArray.length + 1) * -1);
	}
}
