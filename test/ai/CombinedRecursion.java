package ai;

import edu.washington.cse.concerto.interpreter.ai.test.AssertionChecker;
import edu.washington.cse.concerto.interpreter.value.IValue;
import fj.Ord;
import fj.data.Set;

import static intr.Intrinsics.*;

public class CombinedRecursion {
	public static class Container {
		private final Container next;
		public Container(final Container next) {
			this.next = next;
		}

		public Container() {
			this.next = null;
		}

	}
	public static class ApplicationType {

		public Container frameworkCall(final CombinedRecursion combinedRecursion) {
			if(nondet() == 0) {
				return this.doRecursiveCall(combinedRecursion);
			} else if(nondet() == 1) {
				return new Container(this.doRecursiveCall(combinedRecursion));
			} else {
				return new Container();
			}
		}

		public Container doRecursiveCall(final CombinedRecursion combinedRecursion) {
			return combinedRecursion.doCallback(this);
		}
	}
	public Container doCallback(final ApplicationType at) {
		return at.frameworkCall(this);
	}

	public static class ContainerChecker implements AssertionChecker {
		@Override public void assertValue(final String k, final IValue v, final AssertF assertF) {
			assert k.equals("");
			assertF.f(v.isMultiHeap(), "not multi-heap");
			assertF.f(v.valueStream().length() == 3, "wrong length");
			assertF.f(v.valueStream().forall(iv -> iv.getTag() == IValue.RuntimeTag.OBJECT), "wrong variant types");
			assertF.f(v.valueStream().filter(iv -> !iv.getLocation().isSummary).length() == 2, "wrong number of summary objects");
			final Set<Integer> nonSummarized = Set.iterableSet(Ord.intOrd, v.valueStream().filter(iv -> !iv.getLocation().isSummary).map(iv -> iv.getLocation().contextNumber));
			assertF.f(v.valueStream().exists(iv -> iv.getLocation().isSummary && nonSummarized.member(iv.getLocation().contextNumber)), "no matching summary object");
		}
	}

	public void main() {
		final ApplicationType at = allocateType(ApplicationType.class);
		final Container container = at.doRecursiveCall(this);
		customAssert(container, "", ContainerChecker.class);
	}
}
