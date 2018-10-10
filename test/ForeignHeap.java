import static intr.Intrinsics.*;
import intr.ForeignLocation;

public class ForeignHeap {
	public void readWrite() {
		put(liftLocation("foo"), 4);
		assertEqual(get(liftLocation("foo")), 4);
	}
	
	public void transitiveWrite() {
		this.doWrite(3);
		assertEqual(get(liftLocation("foo")), 3);
	}
	
	private void doWrite(final int i) {
		put(liftLocation("foo"), i);
	}
	
	public void writeInBranch() {
		final ForeignLocation l = liftLocation("foo");
		if(nondet() == 3) {
			put(l, 4);
		} else {
			put(l, 3);
		}
		assertEqual(get(l), nondet());
	}
	
	public void writeInTrueBranch() {
		final ForeignLocation l = liftLocation("foo");
		if(nondet() == 3) {
			put(l, 3);
		} else {
			debug("nope");
		}
		assertEqual(get(l), 3);
	}
	
	public void writeInFalseBranch() {
		final ForeignLocation l = liftLocation("foo");
		if(nondet() == 3) {
			debug("nope");
		} else {
			put(l, 3);
		}
		assertEqual(get(l), 3);
	}
	
	public void writeInReturn() {
		doReturnWrite();
		assertEqual(get(liftLocation("foo")), nondet());
	}

	private void doReturnWrite() {
		final ForeignLocation fl = liftLocation("foo");
		if(nondet() == 3) {
			put(fl, 4);
			return;
		}
		put(fl, 3);
	}
	
	public void writeInReturnBranches() {
		doReturnInBranchWrite();
		assertEqual(get(liftLocation("foo")), nondet());
	}

	private void doReturnInBranchWrite() {
		final ForeignLocation fl = liftLocation("foo");
		if(nondet() == 3) {
			put(fl, 4);
		}
		int x;
		if(nondet() == 3) {
			if(nondet() == 6) {
				put(fl, 4);
			}
			x = 4;
		} else {
			if(nondet() == 6) {
				put(fl, 3);
			}
			x = 3;
		}
		assertEqual(x, lift(3,4));
	}
}
