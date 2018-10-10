package meta.framework.interpreter;

import meta.framework.Dispatcher;
import meta.framework.Request;
import meta.framework.interpreter.ast.Embedded;
import meta.framework.interpreter.val.Int;
import meta.framework.interpreter.val.Val;
import meta.framework.response.ResponseStream;

import static intr.Intrinsics.fail;

public class LispInterpreter {
	private final int varTableSize;
	private final Ast root;

	public LispInterpreter(final int varTableSize, final Ast root) {
		this.varTableSize = varTableSize;
		this.root = root;
	}

	public int interpret(final Dispatcher d, final Request req, final ResponseStream stream) {
		final LEnvironment env = new LEnvironment(varTableSize);
		env.with(0, new Embedded(req));
		env.with(1, new Embedded(d));
		env.with(2, new Embedded(stream));
		final Val result = this.root.interpret(env);
		if(!(result instanceof Int)) {
			fail("bad result");
		}
		return ((Int)result).toInt();
	}
}
