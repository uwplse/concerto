package ai;

import static intr.Intrinsics.allocateType;

public class Continuation {
	private int f;

	public void setF(final int f) {
		this.f = f;
	}

	public static class Application {
		public void mutate(final Continuation c) {
			c.setF(5);
		}

		public void doCalls(final Continuation c) {
			c.setF(4);
			c.callMeMaybe(this);
			c.setF(6);
			c.callMeMaybe(this);
		}
	}

	public void callMeMaybe(final Application application) {
		application.mutate(this);
	}

	public void main() {
		this.f = 0;
		final Application c = allocateType(Application.class);
		c.doCalls(this);
	}
}
