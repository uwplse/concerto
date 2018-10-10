import static intr.Intrinsics.*;

public class NullChecks {
	public static class A {
		int f = 0;
		public A(final int f) {
			this.f = f;
		}
	}
	
	public void npeWrite() {
		final A a = allocate();
		a.f = 2;
	}
	
	public void npeRead() {
		final A a = allocate();
		println(a.f);
	}
	
	public void npeArrayRead() {
		println(allocateArray()[0]);
	}
	
	public void npeArrayWrite() {
		allocateArray()[0] = 1;
	}
	
	public A allocate() {
		return null;
	}
	
	public int[] allocateArray() {
		return null;
	}
	
	public A maybeAllocate() {
		if(nondet() == 1) {
			return new A(0);
		} else {
			return allocate();
		}
	}

	public int[] maybeAllocateArray() { 
		if(nondet() == 1) {
			return new int[3];
		} else {
			return allocateArray();
		}
	}
	
	public void oneNonNullObject() {
		final A a = maybeAllocate();
		a.f = 3;
		assertEqual(a.f, lift(3, 0));
	}
	
	public void oneNonNullArray() {
		final int[] a = maybeAllocateArray();
		a[0] = 3;
		assertEqual(a[0], lift(3, 0));
	}
	
	public void multiArrayOOBWrite() {
		final int[] a = maybeAllocateArray();
		a[3] = 3;
	}
	
	public void multiArrayOOBRead() {
		final int[] a = maybeAllocateArray();
		println(a[3]);
	}
	
}
