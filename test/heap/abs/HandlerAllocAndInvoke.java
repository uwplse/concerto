package heap.abs;

import heap.concrete.ForwardingHandler;
import heap.concrete.Handler;
import heap.concrete.Main;
import heap.concrete.SubHandler;

import static intr.Intrinsics.allocateType;
import static intr.Intrinsics.nondet;

public class HandlerAllocAndInvoke {
	public Main.Container doCall() {
		SubHandler h = allocateType(nondet());
		Main.Container c = new Main.Container();
		h.handle(c);
		return c;
	}

	public Main.Container secondCall() {
		Handler h = new ForwardingHandler();
		Main.Container c = new Main.Container();
		h.handle(c);
		return c;
	}
}
