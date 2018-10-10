package ai;

import intr.Intrinsics;

import static intr.Intrinsics.*;

public class BasicTest {
	public static interface CBInt {
		public int getValue();
		public int callbackTo(CBInt c);
	}
	
	public static class FrameworkType implements CBInt {
		@Override
		public int getValue() {
			return -1;
		}
		@Override
		public int callbackTo(final CBInt c) {
			return c.getValue();
		}
		public int bounce(final CBInt root) {
			return root.callbackTo(allocateType("ai.BasicTest$ApplicationType"));
		}
		public Container[] doLoopAllocation(final ApplicationType applicationType) {
			final Container[] toReturn = new Container[3];
			while(nondet() == 0) {
				toReturn[0] = toReturn[1];
				toReturn[1] = applicationType.allocFramework();
				toReturn[2] = new Container();
			}
			return toReturn;
		}
	}
	
	public static class Container {
		
	}
	
	public static class ApplicationType implements CBInt {
		private int f;
		public ApplicationType() {
			this.f = 66;
		}
		
		@Override
		public int getValue() { return 4; }
		public Container allocFramework() {
			return new Container();
		}
		public int add3ToNumber(final int a) { return a + 3; }
		public int subtract3FromNumber(final int a) { return a - 3; }
		public int[] getArray() {
			return new int[4];
		}
		
		public int[] getArray(final int len) {
			return new int[len];
		}
		
		public int deref(final int[] a, final int x) {
			return a[x];
		}
		
		public void set(final int[] a, final int i, final int v) {
			a[i] = v;
		}
		
		public int toInterval(final int x) {
			return x + 0;
		}
		
		public int[] getNull() {
			return null;
		}
		
		@Override
		public int callbackTo(final CBInt c) {
			return 100;
		}
		
		public int bounceThrough(final CBInt root) {
			final FrameworkType ft = new FrameworkType();
			return ft.bounce(root);
		}
		public void doCallback(final FrameworkType ft) {
			ft.doLoopAllocation(this);
		}
		public ApplicationType doAllocationsInLoop() {
			ApplicationType curr = this;
			ApplicationType other = this;
			while(nondet() == 0) {
				final ApplicationType a = curr.allocSelf();
				other.increment();
				final ApplicationType at = new ApplicationType();
				if(nondet() == 1) {
					other = at;
				}
				if(nondet() == 1) {
					curr = a;
				} else {
					curr = at;
				}
			}
			return curr;
		}
		
		private void increment() {
			this.f++;
		}

		public int readField() {
			return this.f;
		}
		private ApplicationType allocSelf() {
			return new ApplicationType();
		}
	}
	
	public void testIndexing() {
		final ApplicationType at = getAT();
		final int a = at.getValue();
		final int b = at.subtract3FromNumber(a);
		final int[] arr = at.getArray();
		at.set(arr, b, 5);
		final int get = at.deref(arr, 1);
		assertEqual(get, 5);
		
		final int c = 5 - at.getValue();
		final int get2 = at.deref(arr, c);
		assertEqual(get2, 5);
	}

	private ApplicationType getAT() {
		return Intrinsics.<ApplicationType>allocateType("ai.BasicTest$ApplicationType");
	}
	
	public void testCombinedPropagation() {
		final ApplicationType at = getAT();
		final int len = at.getValue();
		final int[] arr = at.getArray(len);
		
		final int x = nondet();
		if(x >= 0 && x < len) {
			at.set(arr, x, 3);
		}
		for(int i = 0; i < len; i++) {
			assertEqual(at.deref(arr, i), at.toInterval(lift(0, 3)));
		}
	}
	
	public void testPointerComparison() {
		final ApplicationType at = getAT();
		final int[] arr = at.getArray();
		final int[] arr2 = at.getArray(4);
		if(arr == arr2) {
			assertFalse("pointers concluded equal");
		}
		final int[] either;
		if(nondet() == 4) {
			either = arr;
		} else {
			either = arr2;
		}
		if(either == arr) {
			either[0] = 7;
			assertEqual(arr[0], 7);
			assertEqual(arr2[0], 0);
		}
		assertToStringEquals(either[0], "V(<Set()||[0,7]>)");
		assertToStringEquals(arr[0], "V(<Set()||[0,7]>)");
	}

	public void testArrayLength() {
		final ApplicationType at = getAT();
		final int arr[] = at.getArray(5);
		for(int i = 0; i < 10; i++) {
			if(i < arr.length) {
				arr[i] = i;
			}
		}
		for(int i = 0; i < arr.length; i++) {
			assertEqual(arr[i], i);
		}
	}
	
	public void testEqualsPropagation() {
		final ApplicationType at = getAT();
		final int i = nondet();
		final int arr[] = at.getArray();
		if(i >= 0 && i < arr.length) {
			final int j = nondet();
			if(j == i) {
				arr[j] = 4;
			}
		}
		for(int k = 0; k < arr.length; k++) {
			assertToStringEquals(arr[k], "V(<Set()||[0,4]>)");
		}
	}
	
	public void testWidening() {
		final ApplicationType at = getAT();
		final int[] arr = at.getArray(1);
		for(int i = 0; i < nondet(); i++) {
			at.set(arr, 0, at.deref(arr, 0) + 1);
		}
		assertToStringEquals(at.deref(arr, 0), "V(<Set()||[0,∞]>)");
	}
	
	public void testWidening2() {
		final ApplicationType at = getAT();
		final int[] arr = at.getArray(1);
		for(int i = 0; i < nondet(); i++) {
			at.set(arr, 0, at.deref(arr, 0) + 1);
			at.set(arr, 0, 0);
		}
		assertToStringEquals(at.deref(arr, 0), "V(<Set()||[0,0]>)");
	}
	
	public void testDirectReadWrite() {
		final ApplicationType at = getAT();
		final int[] arr = at.getArray();
		arr[0] = 6;
		assertEqual(arr[0], 6);
	}
	
	public void testCombinedReadWrite() {
		final ApplicationType at = getAT();
		final int[] arr;
		if(nondet() == 0) {
			arr = at.getArray();
		} else {
			arr = new int[4];
			arr[0] = -1;
		}
		arr[0] = 6;
		assertToStringEquals(arr[0], "V(<Set()||[-1,6]>)");
	}
	
	public void testNullPointerEquality() {
		final ApplicationType at = getAT();
		final int[] frArray;
		if(nondet() == 0) {
			frArray = null;
		} else {
			frArray = new int[4];
		}
		final int[] appArray = nondet() == 0 ? at.getArray() : at.getNull();
		boolean taken = false;
		if(appArray == frArray) {
			taken = true;
		}
		assertEqual(taken, lift(false, true));
		
		taken = false;
		if(frArray == appArray) {
			taken = true;
		}
		assertEqual(taken, lift(false, true));
		
		final int[] nonNullArray = new int[4];
		if(nonNullArray == appArray) {
			assertFalse("should not be equal");
		}
		
		if(appArray == nonNullArray) {
			assertFalse("should not be equal");
		}
		
		if(appArray != nonNullArray) {
			
		} else { assertFalse("should not be equal"); }
		
		if(nonNullArray != appArray) {
			
		} else { assertFalse("should not be equal"); }
	}
	
	public void testRecursion() {
		final int x = fact(0, getAT());
		assertToStringEquals(x, "V(<Set()||[4,4]>)");
	}

	private int fact(final int p, final ApplicationType applicationType) {
		if(p == nondet()) {
			return applicationType.getValue();
		} else {
			return fact(p + 1, applicationType);
		}
	}
	
	public void testQuadNesting() {
		final ApplicationType at = getAT();
		final FrameworkType ft = new FrameworkType();
		final CBInt root;
		if(nondet() == 1) {
			root = at;
		} else {
			root = ft;
		}
		final int tmp = at.bounceThrough(root);
		assertEqual(tmp, lift(4, 100));
	}
	
	public void testHeapSummaries() {
		final ApplicationType at = getAT();
		final FrameworkType ft = new FrameworkType();
		at.doCallback(ft);
	}
	
	public void testAIHeapSummaries() {
		final ApplicationType at = getAT();
		final ApplicationType alloc = at.doAllocationsInLoop();
		assertToStringEquals(alloc.readField(), "V(<Set()||[66,∞]>)");
	}
}
