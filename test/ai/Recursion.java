package ai;

import static intr.Intrinsics.*;

public class Recursion {
	public static class Application1 {
		private final Application2 a2;

		public Application1(final Application2 a2) {
			this.a2 = a2;
		}
		public int a(final int x) {
			if(x == nondet()) {
				return x + 1;
			} else {
				return a2.b(x + 1);
			}
		}

		public int c(final int x) {
			return this.a(x - 1);
		}
	}

	public static class Application2 {
		private Application1 a1;

		public void setA1(final Application1 a1) {
			this.a1 = a1;
		}

		public int b(final int c) {
			if(c < 100) {
				return a1.a(c);
			} else {
				return a1.c(c  - 1);
			}
		}
	}

	public static class IncrementalCycles {
		public int indirection(final int x, final int a, final int b, final int c) {
			if(x == 0) {
				return this.a(a, b, c);
			} else {
				return this.c(a, b, c);
			}
		}

		private int c(final int a, final int b, final int c) {
			return this.d(a, b, c);
		}

		private int a(final int a, final int b, final int c) {
			return this.b(a, b, c);
		}

		private int b(final int a, final int b, final int c) {
			if(a == 0) {
				return b;
			} else {
				return this.c(a + 1, b + 1, c);
			}
		}

		public int d(final int a, final int b, final int c) {
			return this.a(a, b, c);
		}
	}

	public void basicCycle() {
		checkAsserts();
		final Application2 a2 = new Application2();
		final Application1 a1 = new Application1(a2);
		a2.setA1(a1);
		assertEqual(a1.a(0), getPositiveBound(1));
	}

	public void incrementalCycle() {
		checkAsserts();
		final IncrementalCycles ic = new IncrementalCycles();
		assertEqual(ic.indirection(0, 0, 0, 0), getPositiveBound(0));
		assertEqual(ic.indirection(1, 0, 0, 0), getPositiveBound(0));
		// now create a cycle, make b call c by passing 1 for a
		assertEqual(ic.indirection(0, 1, 0, 0), getPositiveBound(0));
	}

	private int getPositiveBound(final int start) {
		int i = start;
		while(nondet() == 0) {
			i++;
		}
		return i;
	}
}
