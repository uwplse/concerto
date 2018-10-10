package heap.concrete;

import static intr.Intrinsics.allocateType;
import static intr.Intrinsics.debug;

public class AllocatingHandler implements SubHandler {
	private final ResultProvider rp;

	public AllocatingHandler() {
		this.rp = allocateType("heap.abs.AbstractResultProvider");
	}

	@Override public void handle(final Main.Container c) {
		Main.Containee cont = new Main.Containee();
		c.setContainee(cont);
		cont.setField(rp.provideResult());
	}
}
