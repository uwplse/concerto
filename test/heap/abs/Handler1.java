package heap.abs;

import heap.concrete.Handler;
import heap.concrete.Main.Containee;
import heap.concrete.Main.Container;

public class Handler1 implements Handler {
	@Override
	public void handle(final Container c) {
		final Containee c2 = new Containee();
		(new Indirection()).doCall(c, c2);
		c.getContainee().setField(1);
	}
}
