package edu.washington.cse.concerto.interpreter.meta;

import java.util.List;

import edu.washington.cse.concerto.interpreter.value.IValue;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Value;
import soot.jimple.InvokeExpr;

/*
 * Represents and explicit method call, or an implicit method call via reflection
 */
public interface ConcreteMethodCallMirror {
	public List<SootMethod> resolveMethod(IValue receiver);
	public SootMethodRef getMethodRef();
	public InvokeExpr invokeExpr();
	public Value argExpr(int i);
}
