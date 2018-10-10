package meta.framework.interpreter.ast;

import meta.framework.interpreter.Ast;
import meta.framework.interpreter.LEnvironment;
import meta.framework.interpreter.val.Val;

public class VarNode implements Ast {
	private final int vNum;

	public VarNode(final int vNum) {
		this.vNum = vNum;
	}

	@Override public Val interpret(final LEnvironment env) {
		return env.get(this.vNum);
	}
}
