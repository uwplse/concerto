package heap.concrete;

import static intr.Intrinsics.*;

public class Main {
	public static class Container {
		public Containee c;
		private ResultProvider rp;

		public void setContainee(final Containee c) {
			this.c = c;
		}

		public Containee getContainee() {
			return this.c;
		}

		public void setResultProvider(ResultProvider rp) {
			this.rp = rp;
		}

		public ResultProvider getResultProvider() {
			return this.rp;
		}
	}
	
	public static class Containee { 
		public int f;
		public void setField(final int i) {
			this.f = i;
		}
	}
	
	public void main() {
		Handler h;
		if(nondet() == 1) {
			h = allocateType("heap.abs.Handler1");
		} else {
			h = allocateType("heap.abs.Handler2");
		}
		Container c = new Container();
		h.handle(c);
		assertEqual(c.c.f, lift(1,2));
		c = new Container();
		final int analysisPrecision = read();
		if(nondet() == 1) {
			h = allocateType("heap.abs.Handler1");
			h.handle(c);
			if(analysisPrecision == 1) {
				assertEqual(c.c.f, lift(1,2));
			} else if(analysisPrecision == 2) {
				assertEqual(c.c.f, 1);
			} else {
				fail("Unrecognized precision");
			}
		} else {
			h = allocateType("heap.abs.Handler2");
			h.handle(c);
			if(analysisPrecision == 1) {
				assertEqual(c.c.f, lift(1,2));
			} else if(analysisPrecision == 2) {
				assertEqual(c.c.f, 2);
			} else {
				fail("Unrecognized precision");
			}
		}
	}
}
