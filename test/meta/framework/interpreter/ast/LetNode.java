package meta.framework.interpreter.ast;

import meta.framework.interpreter.Ast;
import meta.framework.interpreter.LEnvironment;
import meta.framework.interpreter.val.Val;

public class LetNode implements Ast {
	private final int[] bindTarget;
	private final Ast[] bindArg;
	private final Ast body;

	public LetNode(final int[] bindTarget, final Ast[] bindArg, final Ast body) {
		this.bindTarget = bindTarget;
		this.bindArg = bindArg;
		this.body = body;
	}

	@Override public Val interpret(final LEnvironment env) {
		for(int i = 0; i < bindArg.length; i++) {
			env.with(bindTarget[i], bindArg[i].interpret(env));
		}
		return body.interpret(env);
	}
}
