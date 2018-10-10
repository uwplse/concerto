package edu.washington.cse.concerto.interpreter;

import edu.washington.cse.concerto.interpreter.meta.ReflectionModel;
import edu.washington.cse.concerto.interpreter.meta.ReflectionModel.InvokeMoke;
import fj.data.Option;
import soot.RefType;
import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;

import java.util.HashMap;
import java.util.Map;

public class ReflectionEnvironment {
	private static ReflectionEnvironment instance;
	private final ReflectionModel model;

	private ReflectionEnvironment(final ReflectionModel m) {
		this.model = m;
	}

	public static ReflectionEnvironment v() {
		if(instance == null) {
			throw new IllegalStateException();
		}
		return instance;
	}
	
	public static void init(final ReflectionModel m) {
		instance = new ReflectionEnvironment(m);
	}
	
	public Option<SootMethod> resolve(final SootClass klass, final int key) {
		final String signature = this.model.resolveSignature(key);
		if(klass.declaresMethod(signature)) {
			return Option.some(klass.getMethod(signature));
		} else {
			return Option.none();
		}
	}
	
	public Option<SootMethodRef> resolve(final int classKey, final int methodKey) {
		final Option<SootClass> resolved = this.resolve(classKey);
		return resolved.bind(klass -> this.resolve(klass, methodKey)).map(SootMethod::makeRef);
	}
	
	public Option<SootClass> resolve(final int key) {
		if(reverseCanon.containsKey(key)) {
			return Option.some(reverseCanon.get(key));
		}
		try {
			final SootClass toRet = BodyManager.loadClass(model.resolveClassName(key));
			return Option.some(toRet);
		} catch(final RuntimeException e) {
			return Option.none();
		}
	}
	
	public InvokeMoke invokeResolutionMode() {
		return this.model.resolutionMode();
	}

	private final Map<SootClass, Integer> canonization = new HashMap<>();
	private final Map<Integer, SootClass> reverseCanon = new HashMap<>();
	private int nextCanonKey = -1;

	public int getKeyForClass(final RefType t) {
		if(canonization.containsKey(t.getSootClass())) {
			return canonization.get(t.getSootClass());
		} else {
			final int rev = model.reverseResolution(t.getSootClass());
			if(rev >= 0) {
				return rev;
			}
			if(canonization.isEmpty()) {
				assert nextCanonKey == -1;
				nextCanonKey = (rev * -1) - 1;
			}
			final int newMapping = nextCanonKey++;
			canonization.put(t.getSootClass(), newMapping);
			reverseCanon.put(newMapping, t.getSootClass());
			return newMapping;
		}
	}
}
