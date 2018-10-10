package meta.framework.interpreter.ast;

import meta.framework.interpreter.Ast;
import meta.framework.interpreter.LEnvironment;
import meta.framework.interpreter.val.Closure;
import meta.framework.interpreter.val.Val;

public class LambdaNode implements Ast {
	private final int pSlot;
	private final Ast body;

	public LambdaNode(final int read, final Ast ast) {
		this.pSlot = read;
		this.body = ast;
	}

	@Override public Val interpret(final LEnvironment env) {
		return new Closure(env.copy(), pSlot, body);
	}
}
