import static intr.Intrinsics.*;

public class ReifiedState {
	public void testBasicMerging() {
		final int b;
		if(nondet() == 0) {
			b = read() + read();
		} else {
			b = read();
		}
		assertEqual(b, lift(3, 7));
		assertEqual(read(), nondet());
	}

	public void testHarmlessLoop() {
		final int b = read();
		int c = 0;
		while(nondet() == 0) {
			c += nondet();
		}
		assertEqual(b, 3);
		assertEqual(read(), 4);
	}

	public void testWidening() {
		final int b = read();
		int c = 0;
		while(nondet() == 0) {
			c += read();
		}
		assertEqual(b, 3);
		assertEqual(c, nondet());
		assertEqual(read(), nondet());
	}

	public void testBoundedLoops() {
		final int bound = read();
		int accum = 0;
		for(int i = 0; i < bound; i++) {
			accum += read();
		}
		assertEqual(accum, 15);
		assertEqual(read(), 6);
	}

	public void testMergingAtCall() {
		final Reader r;
		if(nondet() == 0) {
			r = new R1();
		} else {
			r = new R2();
		}
		assertEqual(r.readVals(), lift(3, 7));
		assertEqual(read(), nondet());
		assertEqual(r.readVals(), nondet());
	}

	public void testWideningInRecursion() {
		final int x = this.recurse(0);
		assertEqual(x, nondet());
		assertEqual(read(), nondet());
	}

	private int recurse(final int i) {
		if(nondet() == 0) {
			return i;
		} else {
			return recurse(read()) + i;
		}
	}

	private interface Reader {
		int readVals();
	}

	public static class R1 implements Reader {
		@Override public int readVals() {
			return read();
		}
	}

	private static class R2 implements Reader {
		@Override public int readVals() {
			return read() + read();
		}
	}
}

