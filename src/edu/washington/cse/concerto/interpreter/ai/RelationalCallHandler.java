package edu.washington.cse.concerto.interpreter.ai;

import java.util.List;

import edu.washington.cse.concerto.interpreter.meta.InstrumentedState;
import fj.data.Option;
import soot.grimp.internal.GNewInvokeExpr;
import soot.jimple.InstanceInvokeExpr;

public interface RelationalCallHandler<Context, Relation> extends CallHandler<Context> {
	public Option<MethodResult> handleCall(Context callingContext, InstanceInvokeExpr iie, Object receiver, List<Object> arguments, InstrumentedState callState, Relation inputRelation);
	public Option<EvalResult> allocType(GNewInvokeExpr op, InstrumentedState state, List<Object> constrArgs, Context context, Relation inputRelation);
}
