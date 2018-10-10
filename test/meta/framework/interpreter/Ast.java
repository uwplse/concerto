package meta.framework.interpreter;

import meta.framework.interpreter.val.Val;

public interface Ast {
	Val interpret(LEnvironment env);
}
