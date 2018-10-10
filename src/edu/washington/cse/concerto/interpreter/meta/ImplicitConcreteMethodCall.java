package edu.washington.cse.concerto.interpreter.meta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.washington.cse.concerto.interpreter.BodyManager;
import edu.washington.cse.concerto.interpreter.SootTypeParser;
import edu.washington.cse.concerto.interpreter.value.IValue;
import edu.washington.cse.concerto.interpreter.value.IValue.RuntimeTag;
import fj.P3;
import soot.FastHierarchy;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.SootMethodRefImpl;
import soot.Type;
import soot.Value;
import soot.jimple.InvokeExpr;

public class ImplicitConcreteMethodCall implements ConcreteMethodCallMirror {
	private final String subSig;
	private final SootClass baseType;
	private final InvokeExpr op;
	private final List<Value> args;
	private final SootMethodRef methodRef;

	public ImplicitConcreteMethodCall(final SootClass receiverType, final String subSig, final InvokeExpr op, final List<Value> args) {
		this.baseType = receiverType;
		this.subSig = subSig;
		this.op = op;
		this.args = args;
		final P3<Type, String, List<Type>> res = SootTypeParser.parseSubSig(subSig);
		methodRef = new SootMethodRefImpl(baseType, res._2(), res._3(), res._1(), false);
	}

	@Override
	public List<SootMethod> resolveMethod(final IValue receiver) {
		if(receiver.getTag() == RuntimeTag.BOUNDED_OBJECT) {
			final List<SootMethod> toReturn = new ArrayList<>();
			for(final SootClass cls : BodyManager.enumerateFrameworkClasses(receiver.boundedType.getBase(), baseType.getType())) {
				if(cls.declaresMethod(subSig) && cls.getMethod(subSig).isConcrete()) {
					toReturn.add(cls.getMethod(subSig));
				}
			}
			return toReturn;
		} else {
			final RefType type = receiver.getSootClass().getType();
			final FastHierarchy fh = Scene.v().getOrMakeFastHierarchy();
			assert fh.canStoreType(type, baseType.getType());	
			return Collections.singletonList(receiver.getSootClass().getMethod(subSig));
		}
	}

	@Override
	public SootMethodRef getMethodRef() {
		return methodRef;
	}

	@Override
	public InvokeExpr invokeExpr() {
		return this.op;
	}

	@Override
	public Value argExpr(final int i) {
		return this.args.get(i);
	}

}
