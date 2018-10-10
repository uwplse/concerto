package edu.washington.cse.concerto.interpreter;

import edu.washington.cse.concerto.interpreter.state.ExecutionState;
import edu.washington.cse.concerto.interpreter.value.IValue;
import fj.data.Option;
import soot.SootMethod;
import soot.jimple.InvokeExpr;

public interface InvokeInterpreterExtension<FH, Context> {
	public Option<IValue> interpretCall(final ExecutionState<FH, Context> es, final IValue base, final InvokeExpr op, SootMethod method);
	public Object createAllocationContext(final ExecutionState<FH, Context> es);
}
