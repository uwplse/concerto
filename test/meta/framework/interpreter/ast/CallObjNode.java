package meta.framework.interpreter.ast;

import intr.Intrinsics;
import meta.framework.interpreter.Ast;
import meta.framework.interpreter.LEnvironment;
import meta.framework.interpreter.val.Int;
import meta.framework.interpreter.val.MethodKey;
import meta.framework.interpreter.val.Val;

import static intr.Intrinsics.fail;

public class CallObjNode implements Ast {
	private final Ast recv;
	private final Ast method;
	private final Ast arg;

	public CallObjNode(final Ast recv, final Ast method, final Ast arg) {
		this.recv = recv;
		this.method = method;
		this.arg = arg;
	}

	@Override public Val interpret(final LEnvironment env) {
		final Val receiver = recv.interpret(env);
		final Val methodKey = method.interpret(env);
		final Val argVal = arg.interpret(env);
		if(receiver.getType() == 4 && methodKey.getType() == 3) {
			final Object o = ((Embedded)receiver).getWrapped();
			final int rtt = Intrinsics.getClass(o);
			final int key = ((MethodKey) methodKey).toKey();
			final Object returned;
			if(argVal.getType() == 2) {
				returned = Intrinsics.invokeObj(o, rtt, key, ((Int) argVal).toInt());
			} else if(argVal.getType() == 4) {
				returned = Intrinsics.invokeObj(o, rtt, key, ((Embedded)argVal).getWrapped());
			} else {
				fail("Bad runtime type");
				returned = null;
			}
			if(returned == null) {
				return null;
			}
			return new Embedded(returned);
		}
		fail("Bad runtime type");
		return null;
	}

}
