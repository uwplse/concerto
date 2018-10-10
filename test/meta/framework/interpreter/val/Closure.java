package meta.framework.interpreter.val;

import meta.framework.interpreter.Ast;
import meta.framework.interpreter.LEnvironment;

public class Closure implements Val {
	private final LEnvironment env;
	private final Ast body;
	private final int param;

	public Closure(LEnvironment env, int param, Ast body) {
		this.env = env;
		this.param = param;
		this.body = body;
	}
	
	@Override public int getType() {
		return 1;
	}

	public Val apply(final Val toPass) {
		LEnvironment toExecute = this.env.copy();
		toExecute.with(param, toPass);
		return body.interpret(toExecute);
	}
}
