package meta.framework.interpreter.ast;

import meta.framework.interpreter.Ast;
import meta.framework.interpreter.LEnvironment;
import meta.framework.interpreter.val.MethodKey;
import meta.framework.interpreter.val.Val;

public class StringPunNode implements Ast {
	private final MethodKey value;

	public StringPunNode(final int key) {
		this.value = new MethodKey(key);
	}

	@Override public Val interpret(final LEnvironment env) {
		return value;
	}
}
