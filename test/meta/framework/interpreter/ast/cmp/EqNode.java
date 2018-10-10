package meta.framework.interpreter.ast.cmp;

import meta.framework.interpreter.CmpNode;
import meta.framework.interpreter.val.Int;
import meta.framework.interpreter.val.Val;

public class EqNode implements CmpNode {
	@Override public boolean compare(final Val a, final Val b) {
		return ((Int)a).toInt() == ((Int)b).toInt();
	}
}
