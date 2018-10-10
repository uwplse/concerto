package ai;

import static intr.Intrinsics.nondet;

public class InformationFlow {
	public static int source() {
		return 0;
	}

	public static void sink(final Object o) { }
	public static void sink(final int o) { }

	public void directFlow() {
		sink(source());
	}

	public static class Container {
		int f = 1;
		Container nextPointer;

		public int getF() {
			return f;
		}
	}

	public void ignoreHeapSideEffects() {
		final Container c = new Container();
		final int tainted = source();
		setF(c, tainted);
		sink(c.f);
	}

	public void testDirectFlowThroughHeap() {
		final Container c = new Container();
		final int tainted = source();
		c.f = tainted;
		sink(c.f);
	}

	public void testLoopWidening() {
		final int tainted = source();
		final Container c = new Container();
		c.f = tainted;
		Container curr = c;
		while(nondet() == 0) {
			final Container next = new Container();
			next.nextPointer = curr;
			curr = next;
		}
		sink(curr.nextPointer.nextPointer.nextPointer.f);

		final Container tmp = curr.nextPointer.nextPointer.nextPointer.nextPointer.nextPointer.nextPointer;
		final Container next = new Container();
		next.nextPointer = tmp;
	}

	public void main() {
		this.testLoopWidening();
	}

	public void testInterproceduralFlow() {
		final int tainted = source();
		final Container c = makeContainer(tainted);
		final int f = c.getF();
		sink(f);
	}

	public void testSubfieldChecking() {
		final int tainted = source();
		final Container c = makeContainer(tainted);
		sink(c);
	}

	private Container makeContainer(final int tainted) {
		final Container c = new Container();
		c.f = tainted;
		return c;
	}

	private void setF(final Container c, final int tainted) {
		c.f = tainted;
	}

	public void testConstructorResults() {
		final ConstructorWithSideEffect se = new ConstructorWithSideEffect(source());
		sink(se);
	}

	private static class ConstructorWithSideEffect {
		private final int g;

		public ConstructorWithSideEffect(final int g) {
			this.g = g;
		}
	}

}
