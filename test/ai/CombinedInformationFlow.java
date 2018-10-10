package ai;

import static intr.Intrinsics.*;

public class CombinedInformationFlow {
	private Object o;

	private CombinedInformationFlow selfRef;

	public static int source() {
		return 0;
	}

	public void sink(final Object o) { }
	public void sink(final int o) { }

	public int _test_sensitiveData() {
		return 0;
	}

	public static class ApplicationType {
		public Object getTaintedObject() {
			final int[] a = new int[]{source()};
			return a;
		}

		public int getTaintedInt() {
			return source();
		}

		public int getTaintedInt(final int k) {
			return source();
		}

		public void callWithInvoke(final CombinedInformationFlow c) {
			invokeObj(c, nondet(), nondet(), this.getTaintedInt());
		}

		public void invokeSinkWithInvokeSource(final CombinedInformationFlow c) {
			invokeObj(c, nondet(), nondet(), invokeInt(this, nondet(), nondet(), 0));
		}

		public ApplicationContainer getWrappedTaint() {
			final ApplicationContainer c = new ApplicationContainer();
			c.f = source();
			return c;
		}
	}

	public static class ApplicationContainer {
		public int f;
		public ApplicationContainer next;
		public ApplicationContainer nestSelf() {
			final ApplicationContainer toReturn = new ApplicationContainer();
			toReturn.next = this;
			return toReturn;
		}

		public void sinkSelf(final CombinedInformationFlow c) {
			c.sink(this.next.next.next.next.f);
		}
	}

	private ApplicationType getAT() {
		return allocateType("ai.CombinedInformationFlow$ApplicationType");
	}

	public void basicTest() {
		sink(getAT().getTaintedInt());
	}

	public void heapTest() {
		sink(getAT().getTaintedObject());
	}

	public void instrumentInvokeTest() {
		invokeObj(this, nondet(), nondet(), getAT().getTaintedInt());
	}

	public void instrumentInvokeInAITest() {
		getAT().callWithInvoke(this);
	}

	public void instrumentDoubleInvokeInAITest() {
		getAT().invokeSinkWithInvokeSource(this);
	}

	public void testTreeWideningInConcrete() {
		ApplicationContainer wrapped = getAT().getWrappedTaint();
		while(nondet() == 0) {
			wrapped = wrapped.nestSelf();
		}
		wrapped.sinkSelf(this);
	}

	public void testConcreteHeapTainting() {
		final ApplicationType at = getAT();
		final ApplicationContainer wrappedTaint = at.getWrappedTaint();
		this.setField(wrappedTaint);
		this.selfRef = this;
		sink(this);
	}

	public void main() {
		this.testConcreteHeapTainting();
	}

	public void setField(final Object o) {
		this.o = o;
	}
}
