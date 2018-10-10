package edu.washington.cse.concerto.interpreter.ai.instantiation.optiflow;

import java.util.IdentityHashMap;

import soot.Local;
import soot.SootFieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.JimpleLocal;
import soot.util.MapNumberer;
import soot.util.Numberer;
import fj.data.Seq;

public class AP {
	public final Local base;
	public final Seq<String> fields;
	
	private static final Numberer<InvokeExpr> invokeNumberer = new MapNumberer<>();
	private static final IdentityHashMap<InvokeExpr, AP> memo = new IdentityHashMap<>();

	public AP(final Local base, final Seq<String> fields) {
		this.base = base;
		this.fields = fields;
	}

	public AP(final Local base) {
		this(base, Seq.<String>empty());
	}

	public AP appendField(final SootFieldRef fieldRef) {
		return new AP(base, fields.snoc(fieldRef.getSignature()));
	}

	public static AP ofLocal(final Local leftOp) {
		return new AP(leftOp, Seq.<String>empty());
	}

	public AP appendArray() {
		return new AP(base, fields.snoc("*"));
	}

	public static AP ofCall(final InvokeExpr leftOp) {
		if(memo.containsKey(leftOp)) {
			return memo.get(leftOp);
		}
		invokeNumberer.add(leftOp);
		final long l = invokeNumberer.get(leftOp);
		final Local tmp = new JimpleLocal("<<invoke:" + l + ">", leftOp.getType());
		final AP toReturn = new AP(tmp);
		memo.put(leftOp, toReturn);
		return toReturn;
	}

	public boolean isPlainLocal() {
		return fields.isEmpty();
	}

	public Local getBase() {
		return base;
	}

	@Override
	public String toString() {
		return base + "." + fields;
	}
}
