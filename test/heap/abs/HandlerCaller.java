package heap.abs;

import heap.concrete.Handler;
import heap.concrete.Main;

import static intr.Intrinsics.*;

public class HandlerCaller {
	public Main.Containee callHandlerDirect(Handler h) {
		Main.Container container = getContainer();
		h.handle(container);
		return container.getContainee();
	}

	protected Main.Container getContainer() {
		Main.Container container = new Main.Container();
		container.setResultProvider(new AbstractResultProvider());
		return container;
	}

	public Main.Containee callHandlerInvoke(final Handler h) {
		Main.Container c = getContainer();
		invokeObj(h, nondet(), nondet(), c);
		return c.getContainee();
	}

}
