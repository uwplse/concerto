import static intr.Intrinsics.*;

public class BoundChecks {
	public void outOfBoundsWrite() {
		final int[] a = new int[1];
		a[1] = 3; 
	}
	
	public void outOfBoundsRead() {
		final int[] a = new int[1];
		a[0] = 3;
		println(a[1]);
	}
	
	public void inBoundsAccesses() {
		final int[] a = new int[1];
		a[0] = 3;
		assertEqual(a[0], 3);
	}
	
	public void allOOBIndexReads() {
		final int[] a = new int[1];
		int i;
		if(nondet() == 0) {
			i = 3;
		} else {
			i = 1;
		}
		println(a[i]);
	}
	
	public void allOOBIndexWrites() {
		final int[] a = new int[1];
		int i;
		if(nondet() == 0) {
			i = 3;
		} else {
			i = 1;
		}
		a[i] = 3;
	}
	
	public void oneInBoundIndexRead() {
		final int[] a = new int[1];
		int i;
		if(nondet() == 0) {
			i = 3;
		} else {
			i = 0;
		}
		println(a[i]);
	}
	
	public void oneInBoundIndexWrite() {
		final int[] a = new int[1];
		int i;
		if(nondet() == 0) {
			i = 3;
		} else {
			i = 0;
		}
		a[i] = 3;
		assertEqual(a[0], lift(0, 3));
	}
	
	public void allOOBBaseWrites() {
		int[] a;
		if(nondet() == 3) {
			a = new int[1];
		} else {
			a = new int[2];
		}
		a[2] = 4;
	}
	
	public void allOOBBaseReads() {
		int[] a;
		if(nondet() == 3) {
			a = new int[1];
		} else {
			a = new int[2];
		}
		println(a[2]);
	}
	
	public void oneInBoundsBaseWrite() {
		int[] a;
		if(nondet() == 3) {
			a = new int[1];
		} else {
			a = new int[2];
		}
		a[1] = 4;
		assertEqual(a[1], lift(0, 4));
	}
	
	public void oneInBoundsBaseRead() {
		int[] a;
		if(nondet() == 3) {
			a = new int[1];
		} else {
			a = new int[2];
			a[1] = 4;
		}
		assertEqual(a[1], 4);
	}
	
	public void cartesianAllOOBRead() {
		final int[] a;
		if(nondet() == 0) {
			a = new int[1];
		} else {
			a = new int[2];
		}
		int i;
		if(nondet() == 0) {
			i = 3;
		} else {
			i = 2;
		}
		println(a[i]);
	}
	
	public void cartesianAllOOBWrite() {
		final int[] a;
		if(nondet() == 0) {
			a = new int[1];
		} else {
			a = new int[2];
		}
		int i;
		if(nondet() == 0) {
			i = 3;
		} else {
			i = 2;
		}
		a[i] = 3;
	}
	
	public void cartesianOneInBoundsWrite() {
		final int[] a;
		if(nondet() == 0) {
			a = new int[1];
		} else {
			a = new int[2];
		}
		int i;
		if(nondet() == 0) {
			i = 3;
		} else {
			i = 1;
		}
		a[i] = 3;
		assertEqual(a[1], lift(0, 3));
	}
	
	public void cartesianOneInBoundsRead() {
		final int[] a;
		if(nondet() == 0) {
			a = new int[1];
		} else {
			a = new int[2];
			a[1] = 4;
		}
		int i;
		if(nondet() == 0) {
			i = 3;
		} else {
			i = 1;
		}
		assertEqual(a[i], 4);
	}
}
