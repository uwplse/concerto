package ai;

import static intr.Intrinsics.*;

public class RelationalTest {
	public void testSimpleUB() {
		final int[] base;
		if(nondet() == 1) {
			base = new int[10];
		} else {
			base = new int[5];
		}
		final int newLen = 2 * base.length + 1;
		final int[] x = new int[newLen];
		for(int i = 0; i < base.length; i++) {
			x[i] = base[i];
		}
		x[base.length] = 4;
	}
	
	public void testEqualityBranches() {
		final int x = nondet();
		final int y = x;
		if(x != y) {
			assertFalse("should be unreachable");
		}
		if(x == y) {
			// fine
		} else {
			assertFalse("oh god, oh man");
		}
	}
	
	public void testLtChecking() {
		final int x = nondet();
		final int y = x - 1;
		if(y >= x) {
			assertFalse("should be unreachable");
		}
		if(y > x) {
			assertFalse("should also be unreachable");
		}
	}
	
	public void testEqualityClosure() {
		final int x = nondet();
		final int y = x - 1;
		
		final int z = nondet();
		final int w = z + 1;
		if(x == z) {
			// we should have that x < w, y < w, y < z
			if(y >= w) {
				assertFalse("bad lb for y");
			}
			if(y >= z) {
				assertFalse("bad lb for y");
			}
			if(x >= w) {
				assertFalse("bad lb for x");
			}
		}
	}

	public void testLtClosure() {
		final int x = nondet();
		final int y = x - 1;
		
		final int z = nondet();
		final int w = z + 1;
		if(x < z) {
			// we should have that x < w, y < w, y < z, x < z
			if(y >= w) {
				assertFalse("bad lb for y");
			}
			if(y >= z) {
				assertFalse("bad lb for y");
			}
			if(x >= w) {
				assertFalse("bad lb for x");
			}
			if(x >= z) {
				assertFalse("bad lb for x");
			}
		}
	}
	
	public void testJoinOperation() {
		checkAsserts();
		int x;
		int y;
		if(nondet() == 1) {
			x = nondet();
			y = x - 1;
		} else {
			x = nondet();
			y = nondet();
		}
		int f;
		if(y < x) {
			f = 0;
		} else {
			f = 1;
		}
		final int bothBranches = lift(0, 1);
		assertEqual(f, bothBranches);
		if(nondet() == 1) {
			x = nondet();
			y = x;
		} else {
			x = nondet();
			y = x -1;
		}
		if(x == y) {
			f = 0;
		} else {
			f = 1;
		}
		assertEqual(f, bothBranches);
		if(y < x) {
			f = 0;
		} else {
			f = 1;
		}
		assertEqual(f, bothBranches);
	}

	public void testParamRelations() {
		final int x = nondet();
		final int[] a = new int[nondet()];
		if(x >= 0 && x < a.length) {
			doWrite(x, a, 4);
		}
	}

	private void doWrite(final int x, final int[] a, final int i) {
		a[x] = i;
	}

	public static class Container {
		int i;
		public Container() {
			this.i = 0;
		}
	}

	public static class ArrContainer {
		int[] arr;
		public ArrContainer() {
			this.arr =  null;
		}
	}

	public void testParamHeapRelations() {
		final int x = nondet();
		final int[] a = new int[nondet()];
		if(x >= 0 && x < a.length) {
			final Container c = new Container();
			final ArrContainer c2 = new ArrContainer();
			c.i = x;
			c2.arr = a;
			doWriteThroughHeap(c, c2, 6);
		}
	}

	private void doWriteThroughHeap(final Container c, final ArrContainer c2, final int i) {
		c2.arr[c.i] = i;
	}
}
