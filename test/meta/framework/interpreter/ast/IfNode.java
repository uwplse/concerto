package meta.framework.interpreter.ast;

import meta.framework.interpreter.Ast;
import meta.framework.interpreter.CmpNode;
import meta.framework.interpreter.LEnvironment;
import meta.framework.interpreter.val.Val;

public class IfNode implements Ast {
	private final Ast lop;
	private final CmpNode cmp;
	private final Ast rop;
	private final Ast trueBranch;
	private final Ast falseBranch;

	public IfNode(final Ast lop, final CmpNode cmp, final Ast rop, final Ast trueBranch, final Ast falseBranch) {
		this.lop = lop;
		this.cmp = cmp;
		this.rop = rop;
		this.trueBranch = trueBranch;
		this.falseBranch = falseBranch;
	}

	@Override public Val interpret(final LEnvironment env) {
		Val lhs = this.lop.interpret(env);
		Val rhs = this.rop.interpret(env);
		if(this.cmp.compare(lhs, rhs)) {
			return this.trueBranch.interpret(env);
		} else {
			return this.falseBranch.interpret(env);
		}
	}
}
