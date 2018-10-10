package meta.framework.interpreter.ast;

import meta.framework.interpreter.Ast;
import meta.framework.interpreter.LEnvironment;
import meta.framework.interpreter.val.Int;
import meta.framework.interpreter.val.Val;

import static intr.Intrinsics.fail;

public class IntArrayNode implements Ast {
	private Ast[] items;

	public IntArrayNode(final Ast[] items) {
		this.items = items;
	}

	@Override public Val interpret(final LEnvironment env) {
		int[] wrapped = new int[items.length];
		for(int i = 0; i < wrapped.length; i++) {
			Val itemResult = items[i].interpret(env);
			if(itemResult.getType() != 2) {
				fail("wrong runtime type");
			}
			wrapped[i] = ((Int)itemResult).toInt();
		}
		return new Embedded(wrapped);
	}
}
