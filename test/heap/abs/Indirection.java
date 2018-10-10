package heap.abs;

import heap.concrete.Main.Containee;
import heap.concrete.Main.Container;

public class Indirection {
	protected void doCall(final Container c1, final Containee c2) {
		c1.setContainee(c2);
	}
}
