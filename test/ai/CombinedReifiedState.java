package ai;

import static intr.Intrinsics.*;

public class CombinedReifiedState {
	public void testSimpleReification() {
		final int r = read();
		final int b = allocateType(Application1.class).doRead(this);
		assertEqual(r, 3);
		assertEqual(b, 4);
		assertEqual(read(), 6);
	}

	public void testMergeDueToInsensitivity() {
		final int r = read();
		final Application1 app = allocateType(Application1.class);
		final int b = app.doRead(this);
		assertEqual(b, 4);
		assertEqual(read(), 6);
		final int c = app.doRead(this);
		assertEqual(c, nondet());
	}

	public void testMergeDueToInsensitivityPTA() {
		final int r = read();
		final Application1 app = allocateType(Application1.class);
		final int b = app.doRead(this);
		assertEqual(b, 4);
		assertEqual(read(), 6);
		final int c = app.doReadHop(this);
		assertEqual(c, nondet());
	}

	public void testNoMergeForContext() {
		final int r = read();
		final Application1 app = allocateType(Application1.class);
		final int b = app.doRead(this);
		assertEqual(b, 4);
		assertEqual(read(), 6);
		final int c = app.doReadHop(this);
		assertEqual(c, 7);
	}

	public void testMergeForAILoop() {
		final int r = read();
		final Application1 app = allocateType(Application1.class);
		final int b = app.doRead(this);
		assertEqual(read(), 6);
		assertEqual(nondet(), app.doReadInLoop(this));
		assertEqual(nondet(), read());
	}

	public void testMergeForAIRecursion() {
		final int r = read();
		final Application1 app = allocateType(Application1.class);
		final int b = app.doRead(this);
		assertEqual(read(), 6);
		assertEqual(nondet(), app.doRecursiveRead(this, 0));
		assertEqual(nondet(), read());
	}

	public static class Application1 {
		public int doRead(final CombinedReifiedState r) {
			if(nondet() == 0) {
				return this.doReadIndirection(r);
			} else {
				return this.doReadIndirection2(r);
			}
		}

		private int doReadIndirection2(final CombinedReifiedState r) {
			return this.doReadActual(r);
		}

		private int doReadIndirection(final CombinedReifiedState r) {
			return this.doReadActual(r);
		}

		public int doReadHop(final CombinedReifiedState r) {
			return this.doReadActual(r);
		}

		private int doReadActual(final CombinedReifiedState r) {
			return r.readValue();
		}

		public int doReadInLoop(final CombinedReifiedState r) {
			int toReturn = 0;
			for(int i = 0; i < nondet(); i++) {
				toReturn += r.readValue();
			}
			return toReturn;
		}

		public int doRecursiveRead(final CombinedReifiedState r, final int curr) {
			if(curr == nondet()) {
				return curr;
			} else {
				return doRecursiveRead(r, curr + r.readValue());
			}
		}
	}

	public int readValue() {
		return read();
	}
}
