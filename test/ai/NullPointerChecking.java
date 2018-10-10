package ai;

import static intr.Intrinsics.*;

public class NullPointerChecking {
	public static class ApplicationClass {
		public int[] getArray(final int a) {
			return new int[a];
		}
		
		public int[] getArray() {
			return this.getArray(4);
		}
		
		public ApplicationClass getF() {
			return f;
		}

		public void setF(final ApplicationClass f) {
			this.f = f;
		}

		public ApplicationClass getG() {
			return g;
		}

		public void setG(final ApplicationClass g) {
			this.g = g;
		}

		private ApplicationClass f, g;
	}
	
	void main() {
		this.testSimpleNullCheck();
	}
	
	public void testSimpleNullCheck() {
		final ApplicationClass ac = getAC();
		final int[] a = ac.getArray();
		if(a == null) {
			assertFalse("we thought it was null");
		}
		a[0] = 5;
		final int[] concrete;
		if(nondet() == 0) {
			concrete = null;
		} else {
			concrete = new int[3];
		}
		
		final int[] maybeNull;
		if(nondet() == 0) {
			maybeNull = a;
		} else {
			maybeNull = null;
		}
		
		final int[] combined;
		if(nondet() == 3) {
			combined = maybeNull;
		} else {
			combined = concrete;
		}
		if(combined != null) {
			combined[0] = 4;
		}
		if(concrete != null) {
			assertEqual(concrete[0], lift(0, 4)); 
		}
		
		if(maybeNull != null) {
			assertEqual(maybeNull[0], lift(4, 5));
		}
	}

	private ApplicationClass getAC() {
		return allocateType("ai.NullPointerChecking$ApplicationClass");
	}
}
