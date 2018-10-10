import static intr.Intrinsics.assertEqual;
import static intr.Intrinsics.lift;
import static intr.Intrinsics.nondet;

public class Interface {
	private static class A {
		public int f = 0;
		public int g = 0;
		public A(final int f) {
			this.f = f;
		}
	}
	
	private static interface SimpleOp {
		public int op(A in);
	}

	public void main() {
		SimpleOp f;
		if(nondet() == 0) {
			f = new SimpleOp() {
				@Override
				public int op(final A in) {
					final int toReturn = in.f + in.g;
					in.f = 3;
					return toReturn;
				}
			};
		} else {
			f = new SimpleOp() {
				@Override
				public int op(final A in) {
					final int a = in.f;
					in.f = in.g;
					in.g = a;
					return in.g + in.f;
				}
			};
		}
		final A a = new A(4);
		a.g = 5;
		assertEqual(9, f.op(a));
		assertEqual(a.f, lift(5,3));
		assertEqual(a.g, lift(5, 4));
	}
}
