package meta.framework.db;

import static intr.Intrinsics.println;

public class DirectProvider implements ORM {
	@Override public int readData(final int k) {
		println(k);
		intr.Intrinsics.write(k);
		return intr.Intrinsics.nondet();
	}

	@Override public void setData(final int k, final int v) {
		intr.Intrinsics.write(k);
		intr.Intrinsics.write(v);
	}
}
