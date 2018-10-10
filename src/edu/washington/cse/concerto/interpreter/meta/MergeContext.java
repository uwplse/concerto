package edu.washington.cse.concerto.interpreter.meta;

import java.util.List;

import soot.SootMethod;

public class MergeContext {
	public final Object receiver;
	public final List<Object> arguments;
	public final SootMethod callee;
	public final boolean abstractTarget;

	public MergeContext(final Object receiver, final List<Object> arguments, final SootMethod callee, final boolean abstractTarget) {
		this.receiver = receiver;
		this.arguments = arguments;
		this.callee = callee;
		this.abstractTarget = abstractTarget;
	}
	
	public MergeContext() {
		this.abstractTarget = false;
		callee = null;
		receiver = null;
		arguments = null;
	}
}
