import static intr.Intrinsics.*;

public class Return {
	private static class A {
		int f = 0;
		int g = 0;
		A(final int f) {
			this.f = f;
		}
	}
	
	public static class B {
		public A next;
		public B() {
			this.next = null;
		}
	}
	
	void backup() {
		final A a = new A(3);
		final A a2 = new A(6);
		final int b = sideEffectReturn(a, a2);
		assertEqual(a.f, lift(3, 4, 10));
		assertEqual(a.g, lift(0, 11));
		assertEqual(a2.f, lift(6, 55));
		assertEqual(b < 10, true);
		assertEqual(b, lift(5, 6));
	}
	
	void nestedReturn() {
		@SuppressWarnings("unused")
		final A a = doReturn();
	}
	
	void transitiveSideEffect() {
		final A w = new A(3);
		doSideEffect(w);
		assertEqual(w.f, lift(3,4,5));
	}
	
	private void doSideEffect(final A a) {
		if(nondet() == 0) {
			return;
		}
		doWrite(a);
		if(nondet() == 0) {
			return;
		}
		a.f = 5;
	}

	private void doWrite(final A a) {
		a.f = 4;
	}

	private A doReturn() {
		final B toSet = new B();
		A toReturn;
		if(nondet() == 4) {
			if(nondet() == 5) {
				return (toSet.next = new A(5));
			}
			toReturn = new A(4);
		} else {
			if(nondet() == 7) {
				return (toSet.next = new A(7));
			}
			toReturn = new A(6);
		}
		return toReturn;
	}

	int sideEffectReturn(final A in, final A in2) {
		if(nondet() == 4) {
			if(nondet() == 5) {
				in.f = 10;
				return 6;
			}
			in.f = 4;
		}
		in2.f = 55;
		in.g = 11;
		return 5;
	}
}
