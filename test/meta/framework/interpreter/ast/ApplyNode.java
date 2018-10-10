package meta.framework.interpreter.ast;

import meta.framework.interpreter.Ast;
import meta.framework.interpreter.LEnvironment;
import meta.framework.interpreter.val.Closure;
import meta.framework.interpreter.val.Val;

import static intr.Intrinsics.fail;

public class ApplyNode implements Ast {
	private final Ast lambda;
	private final Ast arg;

	public ApplyNode(final Ast lambda, final Ast arg) {
		this.lambda = lambda;
		this.arg = arg;
	}

	@Override public Val interpret(final LEnvironment env) {
		Val closure = this.lambda.interpret(env);
		Val toPass = this.arg.interpret(env);
		if(closure.getType() == 1) {
			return ((Closure)closure).apply(toPass);
		} else {
			fail("Bad runtime type");
			return null;
		}
	}
}
