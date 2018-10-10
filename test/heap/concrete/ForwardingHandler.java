package heap.concrete;

import static intr.Intrinsics.allocateType;
import static intr.Intrinsics.nondet;

public class ForwardingHandler implements Handler {
	private final SubHandler h;

	public ForwardingHandler() {
		this.h = allocateType(nondet());
	}

	@Override public void handle(final Main.Container c) {
		this.h.handle(c);
	}
}
