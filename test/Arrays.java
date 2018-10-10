import static intr.Intrinsics.*;

public class Arrays {
	public void testNondetLength() {
		final int[] a = new int[nondet()];
		assertEqual(a.length, nondet());
	}
	
	public void testDeterministicLength() {
		final int[] a = new int[4];
		assertEqual(a.length, 4);
	}
	
	public void testDefaultPrimitiveValue() {
		final int[] a = new int[3];
		for(int i = 0; i < a.length; i++) {
			assertEqual(a[i], 0);
		}
	}
	
	public void testDefaultObjectValue() {
		final A[] a = new A[3];
		for(int i = 0; i < a.length; i++) {
			assertEqual(a[i], null);
		}
	}
	
	public static class A {
		
	}
	
	public void testNondetIndexingWrite() {
		final int[] a = new int[]{
				0, 1, 2, 3
		};
		a[nondet()] = 5;
		for(int i = 0; i < a.length; i++) {
			assertEqual(a[i], lift(i, 5));
		}
	}
	
	public void testNondetIndexingRead() {
		final int[] a = new int[]{
				0, 1, 2, 3
		};
		assertEqual(a[nondet()], lift(0, 1, 2, 3));
	}
	
	public void testNondetIndexingReadAndWrite() {
		final int[] a = new int[]{
				0, 1, 2, 3
		};
		a[nondet()] = 5;
		assertEqual(a[nondet()], lift(0, 1, 2, 3, 5));
	}
	
	public void testNondetSizeDefaultValue() {
		final int[] a = new int[nondet()];
		assertEqual(a[1], 0);
	}
	
	public void testNondetSizeReadAndWrite() {
		final int[] a = new int[nondet()];
		a[2] = 3;
		a[4] = 6;
		assertEqual(a[1], lift(0,3,6));
		assertEqual(a[nondet()], lift(0,3,6));
		a[nondet()] = 5;
		assertEqual(a[1], lift(0,3,5,6));
		assertEqual(a[nondet()], lift(0,3,5,6));
	}

	public void testArrayDowncast() {
		final Object o;
		int[] arrAlias = null;
		if(nondet() == 0) {
			o = this;
		} else {
			o = arrAlias = new int[4];
		}
		if(o instanceof int[]) {
			((int[])o)[0] = 3;
		}
		assertEqual(arrAlias[0], lift(0, 3));
	}

	public void testArrayInstances() {
		final int[] a = new int[4];
		final int f = a instanceof Object ? 1 : 0;
		assertEqual(f, 1);
	}
}
