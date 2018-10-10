package edu.washington.cse.concerto.interpreter.ai;

import java.util.List;

import edu.washington.cse.concerto.interpreter.ai.injection.NeedsMonads;
import edu.washington.cse.concerto.interpreter.meta.InstrumentedState;
import edu.washington.cse.concerto.interpreter.state.ExecutionState;
import fj.data.Either;
import soot.SootMethod;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.toolkits.scalar.Pair;


public interface ContextManager<Context, AVal, AS, AHeap> extends NeedsMonads<AVal, AS> {
	/*
	 * Compute a context for an invocation from AI -> AI, or FR -> AI.
	 * + in the case of AI -> FR -> AI, callingContext.right.rootContext will be non-null
	 * + In the case of (initial calls) FR -> AI, callingContext.right.rootContext will be null  
	 */
	public Context contextForCall(Either<Pair<Context, InstrumentedState>, ExecutionState<AHeap, Context>> callingContext, SootMethod targetMethod, Object base, List<Object> values, InvokeExpr callExpr);
	/*
	 * Compute a context for an allocation.
	 */
	public Context contextForAllocation(Either<Pair<Context, InstrumentedState>, ExecutionState<AHeap, Context>> allocContext, Value allocExpr);
}
