package heap.concrete;

import edu.washington.cse.concerto.interpreter.ai.instantiation.array.AbstractLocation;
import edu.washington.cse.concerto.interpreter.ai.instantiation.array.PValue;
import edu.washington.cse.concerto.interpreter.ai.instantiation.pta.AbstractAddress;
import edu.washington.cse.concerto.interpreter.ai.instantiation.pta.JValue;
import edu.washington.cse.concerto.interpreter.ai.test.AssertionChecker;
import edu.washington.cse.concerto.interpreter.meta.CombinedValue;
import edu.washington.cse.concerto.interpreter.value.IValue;
import edu.washington.cse.concerto.interpreter.value.IValue.RuntimeTag;
import soot.AnySubType;
import soot.RefType;

import static intr.Intrinsics.*;

public class Reflection {
	public interface Intf {
		int getF();
		int getG();
		void run(Marker m);
	}
	
	public static class ConcreteSideEffect implements Intf {
		public int f;
		public final int g;

		public ConcreteSideEffect() {
			this.f = 10;
			this.g = 12;
		}

		@Override
		public int getF() {
			return this.f;
		}

		@Override
		public int getG() {
			return this.g;
		}

		@Override
		public void run(final Marker m) {
			m.mark1();
		}
	}
	
	public static class ConcreteSideEffect2 implements Intf {
		public Intf delegate;

		public ConcreteSideEffect2() {
			delegate = allocateType(1);
		}

		@Override
		public int getF() {
			return this.delegate.getF();
		}

		@Override
		public int getG() {
			return this.delegate.getG();
		}

		@Override
		public void run(final Marker m) {
			m.mark2();
		}
	}
	
	public static class AbstractSideEffect implements Intf {
		public final int f;
		public final int g;

		public AbstractSideEffect() {
			this.f = 6;
			if(nondet() == 0) {
				this.g = 5;
			} else {
				this.g = 1;
			}
		}
		
		@Override
		public int getF() {
			return this.f;
		}
		
		@Override
		public int getG() {
			return this.g;
		}

		@Override
		public void run(final Marker m) {
			m.mark3();
		}
	}
	
	public void testSideEffectAllocation() {
		final ConcreteSideEffect cse = allocateType("heap.concrete.Reflection$ConcreteSideEffect");
		assertEqual(cse.f, 10);
		assertEqual(cse.g, 12);
		final AbstractSideEffect ase = allocateType("heap.concrete.Reflection$AbstractSideEffect");
		assertEqual(ase.getG(), lift(1, 5));
		assertEqual(ase.getF(), 6);
	}
	
	public void testReflectionFromIO() {
		final int k1 = read();
		final Intf i1 = allocateType(k1);
		final int k2 = read();
		final Intf i2 = allocateType(k2);
		assertEqual(i2.getF(), 10);
		assertEqual(i2.getG(), 12);
		
		assertEqual(i1.getF(), 6);
		assertEqual(i1.getG(), lift(1, 5));
	}
	
	public static class NondetAllocChecker implements AssertionChecker {
		public static String targetDomain;

		@Override
		public void assertValue(final String k, final IValue v, final AssertF assertf) {
			assertf.f(v.getTag() == RuntimeTag.EMBEDDED, v + " is not embedded");
			final Object value = v.aVal.value;
			assertf.f(value instanceof CombinedValue, value + " ss not a combined value");
			final CombinedValue combinedValue = (CombinedValue) value;
			final IValue concr = combinedValue.concreteComponent;
			assertf.f(concr.getTag() == RuntimeTag.BOUNDED_OBJECT, "Concrete component " + concr + " is not bounded");
			final AnySubType expectedBound = AnySubType.v(RefType.v(Intf.class.getName()));
			final AnySubType bType = concr.boundedType;
			assertf.f(bType == expectedBound, "Incorrect bound");
			if(targetDomain.equals("jv")) {
				final JValue jv = (JValue) combinedValue.abstractComponent;
				assertf.f(jv.addressSet.size() == 1, "");
				final AbstractAddress addr = jv.addressSet.iterator().next();
				assertf.f(addr.t == expectedBound, "Incorrect abstract bound");
			} else if(targetDomain.equals("arr")) {
				final PValue pv = (PValue) combinedValue.abstractComponent;
				assertf.f(!pv.isInterval(), "");
				assertf.f(pv.address.size() == 1, "");
				final AbstractLocation absLoc = pv.address.iterator().next();
				assertf.f(absLoc.type == expectedBound, "Incorrect type bound");
			}
		}
	}
	
	private static class Marker {
		public int f1 = 0;
		public int f2 = 0;
		public int f3 = 0;

		public void mark1() {
			this.f1 = 1;
		}
		
		public void mark2() {
			this.f2 = 1;
		}
		
		public void mark3() {
			this.f3 = 1;
		}
	}
	
	public void testNondetAllocation() {
		final int k = nondet();
		final Intf i1 = allocateType(k);
		customAssert(i1, "", NondetAllocChecker.class);
		final Marker m = new Marker();
		i1.run(m);
		assertEqual(m.f1, lift(0, 1));
		assertEqual(m.f2, lift(0, 1));
		assertEqual(m.f3, lift(0, 1));
	}
	
	public interface Handler {
		Intf doCommand(int req);
	}
	
	public interface AppHandler extends Handler {
		
	}
	
	public static class ApplicationHandler implements AppHandler {

		@Override
		public Intf doCommand(final int req) {
			final Intf a = allocateType(req);
			if(req < 4) {
				if(req < a.getF()) {
					return new ConcreteSideEffect();
				} else {
					return new ConcreteSideEffect2();
				}
			} else {
				int k = 0;
				if(nondet() == 0) {
					k++;
				}
				return allocateType(k);
			}
		}
	}
	
	public static class AllocChecker implements AssertionChecker {
		@Override
		public void assertValue(final String k, final IValue v, final AssertF assertF) {
			if(k.equals("basecheck")) {
				assertF.f(v.getTag() == RuntimeTag.EMBEDDED, "abstract object");
				assertF.f(v.aVal.value instanceof PValue, "not pvalue");
				final PValue pv = (PValue) v.aVal.value;
				assertF.f(pv.address.size() == 1, "wrong size");
				assertF.f(pv.addresses().iterator().next().type.toString().equals("Any_subtype_of_" + AppHandler.class.getName()), "wrong bound");
			} else if(k.equals("retcheck")) {
				assertF.f(v.getTag() == RuntimeTag.EMBEDDED, "abstract object");
				assertF.f(v.aVal.value instanceof CombinedValue, "not combined val");
				final CombinedValue cv = (CombinedValue) v.aVal.value;
				assertF.f(cv.concreteComponent.getTag() == RuntimeTag.MULTI_VALUE, "concrete component not multi-val");
				cv.concreteComponent.forEach((iv, mult) -> {
					assertF.f(iv.getTag() == RuntimeTag.OBJECT, "Wrong variant type");
					assertF.f(iv.getSootClass().getName().equals(ConcreteSideEffect.class.getName()), "wrong runtime type");
				});
				final PValue pv = (PValue) ((CombinedValue) v.aVal.value).abstractComponent;
				assertF.f(pv.address.size() == 1, "wrong size");
				pv.address.forEach(aLoc -> {
					assertF.f(aLoc.type instanceof RefType, "bad runtime type repr");
					assertF.f(((RefType)aLoc.type).getSootClass().getName().equals(AbstractSideEffect.class.getName()), "wrong type");
				});
			}
		}
	}
	
	public void testAIReflectiveAllocations() {
		final AppHandler h = allocateType(nondet());
		customAssert(h, "basecheck", AllocChecker.class);
		final Intf ret = h.doCommand(nondet());
		customAssert(ret, "retcheck", AllocChecker.class);
	}
	
	public void testAllocationInBranch() {
		ConcreteSideEffect c = new ConcreteSideEffect();
		Intf i;
		if(nondet() == 1) {
			final Intf n = allocateType(nondet());
			i = n;
		} else {
			c.f = 100;
			i = c;
		}
		i.getF();
		
		c = new ConcreteSideEffect();
		if(nondet() == 1) { 
			final Intf n = allocateType(lift(0, 1));
			i = n;
		} else {
			i = c;
		}
	}
	
	public static class HandlerWrapper implements Handler {
		private Handler wrapped;
		public HandlerWrapper() { }
		public void setWrapped(final Handler h) {
			this.wrapped = h;
		}
		
		@Override
		public Intf doCommand(final int req) {
			if(req < 0) {
				return null;
			}
			return this.wrapped.doCommand(req);
		}
	}
	
	public void testEndToEnd() {
		final int handler = read();
		final Handler h = allocateType(handler);
		if(h instanceof HandlerWrapper) {
			((HandlerWrapper) h).setWrapped(allocateType(read()));
		}
		final int method = read();
		while(nondet() == 0) {
			final int req = nondet();
			final Intf resp = (Intf) invokeObj(h, handler, method, req);
			write(resp.getF());
		}
	}
	
	public static class Application {
		public Intf doInvokeCall(final InvokeTarget callee) {
			return (Intf) invokeObj(callee, nondet(), nondet(), this);
		}

		public Intf r2() {
			return new ConcreteSideEffect2();
		}

		public Intf r1() {
			return new ConcreteSideEffect();
		}

		public int[] doBoundedInvokeCall(final InvokeTarget callee) {
			return (int[]) invokeObj(callee, nondet(), nondet(), 4);
		}

		public int doNullInvokeCall(final Intf f) {
			return invokeInt(f, nondet(), nondet());
		}

		public Object doNullInvokeCallObj(final Object f) {
			return invokeObj(f, nondet(), nondet());
		}

		public Object doInvokeCall(final InvokeTargetIntArray iArgs) {
			return invokeObj(iArgs, nondet(), nondet(), new int[3]);
		}

		public Object doInvokeCall(final InvokeTargetObjectArray it) {
			return invokeObj(it, nondet(), nondet(), new Application[3]);
		}
	}
	
	public static class InvokeTarget {
		private Intf do_not_call(final Application a) {
			assertFalse("incorrect resolution 1");
			return null;
		}
		
		public int do_not_call_2() {
			assertFalse("incorrect resolution 2");
			return 0;
		}
		
		public Intf do_not_call_3() {
			assertFalse("incorrect resolution 3");
			return null;
		}
		
		public Intf callee_1(final Application a) {
			return a.r2();
		}
		
		public Intf callee_2(final Application a) {
			return a.r1();
		}

		public Intf do_not_call_4(final InvokeTarget t) {
			assertFalse("incorrect resolution 4");
			return null;
		}
		
		public Intf do_not_call_4(final int k) {
			assertFalse("incorrect resolution :(");
			return null;
		}
		
		public int[] int_callee_1(final int j) {
			return new int[j];
		}
	}

	public static class InvokeTargetIntArray {
		public Intf call_me_maybe(final int[] a) {
			return new ConcreteSideEffect2();
		}

		public Intf call_me_maybe(final Object a) {
			return new ConcreteSideEffect();
		}

		public Application do_not_call_0(final Object o) {
			return null;
		}

		public void call_me_maybe_2(final int[] a) {
		}

		public void do_not_call_1(final Application a) {
			assertFalse("1");
		}

		public void do_not_call_2() {
			assertFalse("2");
		}

		public Intf do_not_call_3(final Application[] c) {
			assertFalse("3");
			return null;
		}
	}

	public static class InvokeTargetObjectArray {
		public Intf do_not_call_4(final int[] a) {
			assertFalse("4");
			return null;
		}

		public Intf call_me_maybe(final Object a) {
			return new ConcreteSideEffect();
		}

		public Intf call_me_maybe_1(final Application[] c) {
			return new ConcreteSideEffect2();
		}

		public void call_me_please(final Application[] c) {

		}

		public void do_not_call_3(final int[] a) {
		}

		public void do_not_call_1(final Application a) {
			assertFalse("1");
		}

		public void do_not_call_2() {
			assertFalse("2");
		}

		public int do_not_call_0(final Object o) {
			assertFalse("0");
			return 1;
		}
	}
	
	public static class CheckInvokeAsserts implements AssertionChecker {
		@Override
		public void assertValue(final String k, final IValue v, final AssertF assertF) {
			if(k.equals("simple-invoke")) {
				assertF.f(v.isMultiHeap(), "wrong type");
				assertF.f(v.valueStream().length() == 2, "bad variant count");
				assertF.f(v.valueStream().exists(iv -> iv.getTag() == RuntimeTag.OBJECT && iv.getSootClass().getName().equals(ConcreteSideEffect.class.getName())), "missing concrete 1");
				assertF.f(v.valueStream().exists(iv -> iv.getTag() == RuntimeTag.OBJECT && iv.getSootClass().getName().equals(ConcreteSideEffect2.class.getName())), "missing concrete 2");
			} else if(k.equals("array-arg")) {
				assertF.f(v.isMultiHeap(), "wrong type");
				assertF.f(v.valueStream().length() == 3, "bad variant count");
				assertF.f(v.valueStream().exists(iv -> iv.getTag() == RuntimeTag.OBJECT && iv.getSootClass().getName().equals(ConcreteSideEffect.class.getName())), "missing concrete 1");
				assertF.f(v.valueStream().exists(iv -> iv.getTag() == RuntimeTag.OBJECT && iv.getSootClass().getName().equals(ConcreteSideEffect2.class.getName())), "missing concrete 2");
				assertF.f(v.valueStream().exists(iv -> iv.getTag() == RuntimeTag.NULL), "null");
			}
		}
		
	}
	
	public void testInvokeInAI() {
		final Application allocateType = allocateType("heap.concrete.Reflection$Application");
		final Intf ret = allocateType.doInvokeCall(new InvokeTarget());
		customAssert(ret, "simple-invoke", CheckInvokeAsserts.class);
	}
	
	public void testInvokeBounds() {
		final Application allocateType = allocateType("heap.concrete.Reflection$Application");
		final int[] ret = allocateType.doBoundedInvokeCall(new InvokeTarget());
		assertToStringEquals(ret, "[ARRAY@l2:0]");
		assertEqual(ret.length, 4);
	}

	public void testInvokeNullConcrete() {
		final ConcreteSideEffect cse = new ConcreteSideEffect();
		final int a = invokeInt(cse, nondet(), nondet());
		assertEqual(a, lift(10, 12));

		final Marker m = new Marker();
		final Object o = invokeObj(m, nondet(), nondet());
		assertEqual(o, null);
		assertEqual(m.f1, lift(0,1));
		assertEqual(m.f2, lift(0,1));
		assertEqual(m.f3, lift(0,1));
	}

	public void testInvokeNullAbstractCallee() {
		final Application a = allocateType(Application.class);
		final Object out = invokeObj(a, nondet(), nondet());
		customAssert(out, "simple-invoke", CheckInvokeAsserts.class);

		final Intf bounded = allocateType(nondet());
		final int i = invokeInt(bounded, nondet(), nondet());
		assertEqual(i, lift(1, 5, 6, 10, 12));
		assertEqual(i, lift(1, 12));
		final int j = invokeInt(bounded, nondet(), 1);
		assertEqual(j, lift(6, 10));
	}

	public void testInvokeNullInAI() {
		final Application a = allocateType(Application.class);
		final Intf bounded = allocateType(nondet());
		final int i = a.doNullInvokeCall(bounded);
		assertEqual(i, lift(1, 5, 6, 10, 12));
		assertEqual(i, lift(1, 12));

		final Marker m = new Marker();
		final Object o = a.doNullInvokeCallObj(m);
		assertEqual(o, null);
		assertEqual(m.f1, lift(0,1));
		assertEqual(m.f2, lift(0,1));
		assertEqual(m.f3, lift(0,1));
	}

	public void testFullResolution() {
		final Intf bounded = allocateType(nondet());
		final int f = invokeInt(bounded, 0, 1);
		assertEqual(f, 10);

		final int g = invokeInt(bounded, 1, 1);
		assertEqual(g, 6);
	}

	public void testInvokeIntArrayArg() {
		final InvokeTargetIntArray it = new InvokeTargetIntArray();
		final int[] a = new int[3];
		final Object ret = invokeObj(it, nondet(), nondet(), a);
		customAssert(ret, "int-array", CheckInvokeAsserts.class);
		final Application app = allocateType(Application.class);
		final Object ret2 = app.doInvokeCall(it);
		customAssert(ret2, "array-arg", CheckInvokeAsserts.class);
	}

	public void testInvokeObjectArrayArg() {
		final InvokeTargetObjectArray it = new InvokeTargetObjectArray();
		final Application[] a = new Application[3];
		final Object ret = invokeObj(it, nondet(), nondet(), a);
		customAssert(ret, "array-arg", CheckInvokeAsserts.class);
		final Application app = allocateType(Application.class);
		final Object ret2 = app.doInvokeCall(it);
		customAssert(ret2, "array-arg", CheckInvokeAsserts.class);
	}
}
