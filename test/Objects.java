import static intr.Intrinsics.*;

public class Objects {
	public static class Container {
		int f = 0;
	}
	public void simpleAliasing() {
		final Container o1 = new Container();
		final Container o2 = new Container();
		Container oAlias;
		if(nondet() == 1) {
			oAlias = o1;
		} else {
			oAlias = o2;
		}
		oAlias.f = 3;
		assertEqual(o1.f, lift(0, 3));
		assertEqual(oAlias.f, lift(0, 3));
		o1.f = 4;
		assertEqual(o1.f, 4);
		assertEqual(oAlias.f, lift(0, 3, 4));
		o2.f = 6;
		assertEqual(o2.f, 6);
		assertEqual(oAlias.f, lift(6, 4));
	}
}
