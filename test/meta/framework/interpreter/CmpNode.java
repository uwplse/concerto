package meta.framework.interpreter;

import meta.framework.interpreter.val.Val;

public interface CmpNode {
	public boolean compare(Val a, Val b);
}
