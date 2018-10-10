package meta.framework.interpreter.ast;

import intr.Intrinsics;
import meta.framework.interpreter.Ast;
import meta.framework.interpreter.LEnvironment;
import meta.framework.interpreter.val.Int;
import meta.framework.interpreter.val.MethodKey;
import meta.framework.interpreter.val.Val;

import static intr.Intrinsics.fail;

public class CallIntNullNode implements Ast {
	private final Ast recv;
	private final Ast method;

	public CallIntNullNode(final Ast recv, final Ast method) {
		this.recv = recv;
		this.method = method;
	}

	@Override public Val interpret(final LEnvironment env) {
		final Val receiver = recv.interpret(env);
		final Val methodKey = method.interpret(env);
		if(receiver.getType() == 4 && methodKey.getType() == 3) {
			final Object o = ((Embedded)receiver).getWrapped();
			final int rtt = Intrinsics.getClass(o);
			final int key = ((MethodKey) methodKey).toKey();
			return new Int(Intrinsics.invokeInt(o, rtt, key));
		}
		fail("Bad runtime type");
		return null;
	}
}
