package edu.washington.cse.concerto.interpreter.ai;

import java.util.List;

import edu.washington.cse.concerto.interpreter.meta.BoundaryInformation;
import edu.washington.cse.concerto.interpreter.meta.InstrumentedState;
import fj.data.Option;
import soot.SootMethod;

public interface RelationalAbstractInterpretation<AVal, AHeap, AS, Context, Relation> extends RelationalPathSensitiveAbstractInterpretation<AVal, AHeap, AS, Context> {
	public Option<MethodResult> handleCall(SootMethod m, AVal receiver, List<Object> arguments, InstrumentedState callState,
			Context calleeContext, BoundaryInformation<Context> callerContext, Relation rel);
	
	public Relation defaultRelation();
	
	@Override
	default Option<MethodResult> handleCall(final SootMethod m, final AVal receiver, final List<Object> arguments, final InstrumentedState callState, final Context calleeContext,
			final BoundaryInformation<Context> callerContext) {
		return this.handleCall(m, receiver, arguments, callState, calleeContext, callerContext, this.defaultRelation());
	}
	
	@Override
	default void setCallHandler(final CallHandler<Context> ch) {
		throw new UnsupportedOperationException();
	}
	
	public void setCallHandler(RelationalCallHandler<Context, Relation> ch);
}
