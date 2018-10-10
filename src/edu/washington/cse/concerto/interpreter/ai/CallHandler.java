package edu.washington.cse.concerto.interpreter.ai;

import java.util.List;

import edu.washington.cse.concerto.interpreter.meta.InstrumentedState;
import fj.data.Option;
import soot.grimp.internal.GNewInvokeExpr;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.StaticInvokeExpr;

public interface CallHandler<Context> {
	/*
	 * Returns the new (in the caller) state (and return value) after executing the call at iie in state callState
	 */
	public Option<MethodResult> handleCall(Context callingContext, InstanceInvokeExpr iie, Object receiver, List<Object> arguments, InstrumentedState callState);
	public Option<EvalResult> allocType(GNewInvokeExpr op, InstrumentedState state, List<Object> constrArgs, Context context);
	public Option<EvalResult> allocType(String allocedTypeName, InstrumentedState state, InvokeExpr op, Context context);
	public Option<EvalResult> allocUnknownType(InstrumentedState state, StaticInvokeExpr op, Context context);
	public Option<MethodResult> handleInvoke(Context callingContext, StaticInvokeExpr expr, List<Object> arguments, InstrumentedState callState);
	public EvalResult allocArray(NewArrayExpr op, InstrumentedState state, Object sz, Context context);
	public EvalResult allocArray(NewMultiArrayExpr op, InstrumentedState state, List<Object> sizes, Context context);
}
