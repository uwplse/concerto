package edu.washington.cse.concerto.interpreter.meta;

import java.util.List;

import edu.washington.cse.concerto.interpreter.value.IValue;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Value;
import soot.jimple.InvokeExpr;

public class ExplicitConcreteMethodCall implements ConcreteMethodCallMirror {
	private final CooperativeInterpreter<?, ?, ?, ?> parent;
	private final InvokeExpr ie;

	public ExplicitConcreteMethodCall(final CooperativeInterpreter<?, ?, ?, ?> parent, final InvokeExpr ie) {
		this.parent = parent;
		this.ie = ie;
	}
	
	@Override
	public List<SootMethod> resolveMethod(final IValue receiver) {
		return this.parent.resolveMethod(receiver, ie);
	}

	@Override
	public SootMethodRef getMethodRef() {
		return ie.getMethodRef();
	}

	@Override
	public InvokeExpr invokeExpr() {
		return ie;
	}

	@Override
	public Value argExpr(final int i) {
		return ie.getArg(i);
	}
}
