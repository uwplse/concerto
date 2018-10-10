import edu.washington.cse.concerto.interpreter.ai.test.AssertionChecker;
import edu.washington.cse.concerto.interpreter.heap.Heap;
import edu.washington.cse.concerto.interpreter.heap.HeapObject;
import edu.washington.cse.concerto.interpreter.state.ExecutionState;
import edu.washington.cse.concerto.interpreter.value.IValue;
import fj.data.Stream;

import static intr.Intrinsics.*;

public class SimpleLoop {
	public interface Intf {
		void setNext(Intf i);
	}
	public static class A implements Intf {
		public A() {
			this.next = null;
		}

		private Intf next;

		@Override public void setNext(final Intf i) {
			this.next = i;
		}
	}
	public static class B implements Intf {
		private Intf next;

		public B() {
			this.next = null;
		}

		@Override public void setNext(final Intf i) {
			this.next = i;
		}
	}

	public static class LoopChecker implements AssertionChecker {
		@Override public void assertValue(final String k, final IValue v, final ExecutionState<?, ?> state, final AssertF assertF) {
			assert k.equals("");
			recursiveCheck(v, 0, state.heap, assertF);
		}

		private void recursiveCheck(final IValue v, final int depth, final Heap heap, final AssertF assertF) {
			if(depth != 3) {
				assertF.f(v.isMultiHeap(), "not multi heap");
			}
			if(depth == 0) {
				final Stream<IValue> vStream = v.valueStream();
				assertAndRecurse(heap, assertF, depth, vStream);
			} else if(depth == 1) {
				assertIntermediateResults(heap, assertF, depth, v);
			} else if(depth == 2) {
				assertIntermediateResults(heap, assertF, depth, v);
			} else if(depth == 3) {
				assertF.f(v.getTag() == IValue.RuntimeTag.NULL, "not a null?");
			}
		}

		private void assertIntermediateResults(final Heap heap, final AssertF assertF, final int depth, final IValue v) {
			final Stream<IValue> vStream = v.valueStream();
			assertF.f(vStream.length() == 3, "wrong length :( " + depth + " " + vStream.toStringEager());
			assertF.f(vStream.exists(iv -> iv.getTag() == IValue.RuntimeTag.NULL), "no null found");
			final Stream<IValue> filtered = vStream.filter(iv -> iv.getTag() != IValue.RuntimeTag.NULL);

			assertAndRecurse(heap, assertF, depth, filtered);
		}

		private void assertAndRecurse(final Heap heap, final AssertF assertF, final int depth, final Stream<IValue> filtered) {
			assertF.f(filtered.length() == 2, "wrong length");
			assertF.f(filtered.forall(iv -> iv.getTag() == IValue.RuntimeTag.OBJECT), "wrong type");
			assertF.f(filtered.forall(iv -> iv.getLocation().id == 2 - depth), "wrong contexts");
			assertF.f(filtered.exists(iv -> iv.getSootClass().getName().equals("SimpleLoop$B")), "no B");
			assertF.f(filtered.exists(iv -> iv.getSootClass().getName().equals("SimpleLoop$A")), "no A");
			filtered.foreachDoEffect(iValue -> {
				final HeapObject obj = heap.findObject(iValue.getLocation());
				recursiveCheck(obj.fieldMap.get("next"), depth + 1, heap, assertF);
			});
		}
	}

	public void main() {
		Intf curr = null;
		for(int i = 0; i < 3; i++) {
			final Intf next;
			if(nondet() == 0) {
				next = new A();
			} else {
				next = new B();
			}
			next.setNext(curr);
			curr = next;
		}
		customAssert(curr, "", LoopChecker.class);
	}
}
