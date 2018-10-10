package meta.framework.interpreter.ast;

import meta.framework.interpreter.Ast;
import meta.framework.interpreter.LEnvironment;
import meta.framework.interpreter.val.Val;

public class NullNode implements Ast {
	@Override public Val interpret(final LEnvironment env) {
		return null;
	}
}
