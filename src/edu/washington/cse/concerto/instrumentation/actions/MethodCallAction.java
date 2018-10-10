package edu.washington.cse.concerto.instrumentation.actions;

import fj.data.Option;
import soot.SootMethodRef;
import soot.jimple.InvokeExpr;

public interface MethodCallAction<AVal, AHeap, AState> {
	default Option<AVal> preCall(final MultiValueReplacement<AVal, AHeap, AState> argReplacer, final InvokeExpr op, final SootMethodRef target) {
		return Option.none();
	}
	default void postCall(final MultiValueReplacement<AVal, AHeap, AState> argReplacer, final InvokeExpr op, final SootMethodRef target) {

	}
}
