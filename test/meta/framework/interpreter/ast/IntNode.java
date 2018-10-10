package meta.framework.interpreter.ast;

import meta.framework.interpreter.Ast;
import meta.framework.interpreter.LEnvironment;
import meta.framework.interpreter.val.Int;
import meta.framework.interpreter.val.Val;

public class IntNode implements Ast {
	private int read;

	public IntNode(final int read) {
		this.read = read;
	}

	@Override public Val interpret(final LEnvironment env) {
		return new Int(read);
	}
}
