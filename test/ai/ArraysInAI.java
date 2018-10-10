package ai;

import edu.washington.cse.concerto.interpreter.ai.instantiation.array.ArrayBoundsChecker;
import edu.washington.cse.concerto.interpreter.ai.instantiation.array.PValue;
import edu.washington.cse.concerto.interpreter.ai.instantiation.optiflow.OptimisticInformationFlow;
import edu.washington.cse.concerto.interpreter.ai.instantiation.pta.BasicInterpreter;
import edu.washington.cse.concerto.interpreter.ai.instantiation.pta.JValue;
import edu.washington.cse.concerto.interpreter.ai.test.AssertionChecker;
import edu.washington.cse.concerto.interpreter.value.IValue;
import fj.data.Stream;
import soot.NullType;
import soot.RefType;
import soot.Type;

import static intr.Intrinsics.*;

public class ArraysInAI {
	public static class Application {
		public int[] getArray() {
			final int i = getPositiveBound();
			return new int[i];
		}

		public int[] getBoundedArray() {
			return new int[4];
		}

		protected int getPositiveBound() {
			int i = 0;
			while(nondet() == 0) {
				i++;
			}
			return i;
		}

		public ApplicationIntf[] getObjectArray() {
			return new ApplicationIntf[4];
		}
	}

	public interface ApplicationIntf { }

	public static class Application1 implements ApplicationIntf { }

	public static class Application2 implements ApplicationIntf { }

	public static class Application3 implements ApplicationIntf { }

	public static class Application4 implements ApplicationIntf { }

	public void testNondetIndexingInConcrete() {
		final Application app = allocateType(Application.class);
		final int[] k = app.getArray();
		k[3] = 34;
		assertEqual(k[3], lift(0, 34));
		for(int i = 0; i < k.length; i++) {
			k[i] = 35;
		}
		assertEqual(k[3], app.getPositiveBound());
	}

	public void testCombinedTypeChecking() {
		final Application app = allocateType(Application.class);
		final int[] k = app.getBoundedArray();
		final Object o;
		if(nondet() == 0) {
			o = this;
		} else if(nondet() == 3) {
			o = new int[3];
		} else {
			o = k;
		}
		assertEqual(o instanceof int[], lift(true, false));
		final int[] a = (int[])o;
		a[1] = 3;
		assertEqual(a[1], lift(0, 3));
	}

	public void testOnlyAIIsArray() {
		final Application app = allocateType(Application.class);
		final int[] k = app.getBoundedArray();
		final Object o;
		if(nondet() == 0) {
			o = this;
		} else {
			o = k;
		}
		assertEqual(o instanceof int[], lift(true, false));
		final int[] a = (int[])o;
		a[1] = 3;
		assertEqual(a[1], 3);
	}

	public void testOnlyConcreteIsArray() {
		final Application app = allocateType(Application.class);
		final int[] k = app.getBoundedArray();
		final Object o;
		if(nondet() == 0) {
			o = new int[4];
		} else {
			o = this;
		}
		assertEqual(o instanceof int[], lift(false, true));
		final int[] a = (int[])o;
		a[1] = 3;
		assertEqual(a[1], 3);
	}


	public void testConcreteArrays() {
		final ApplicationIntf[] a = new ApplicationIntf[3];
		combinedArrayTest(a);
	}

	public void testAbstractArrays() {
		combinedArrayTest(allocateType(Application.class).getObjectArray());
	}

	public void testCombinedArrays() {
		final ApplicationIntf[] a;
		if(nondet() == 0) {
			a = allocateType(Application.class).getObjectArray();
		} else {
			a = new ApplicationIntf[3];
		}
		combinedArrayTest(a);
	}

	public static class ApplicationVariantChecker implements AssertionChecker {
		public static Class<?> TARGET_AI = null;
		private static final Class<?>[] variantClasses = new Class[]{
			Application1.class, Application2.class, Application3.class, Application4.class
		};
		@Override public void assertValue(final String k, final IValue v, final AssertF assertF) {
			assertF.f(v.isEmbedded(), "wrong rtt");
			final Stream<Type> reachingTypes;
			if(TARGET_AI == ArrayBoundsChecker.class) {
				final PValue pv = (PValue) v.aVal.value;
				reachingTypes = pv.address.toStream().map(al -> al.type);
			} else if(TARGET_AI == OptimisticInformationFlow.class || TARGET_AI == BasicInterpreter.class) {
				reachingTypes = ((JValue)v.aVal.value).addressSet.toStream().map(al -> al.t);
			} else {
				throw new UnsupportedOperationException();
			}
			for(final Class<?> klass : variantClasses) {
				assertF.f(reachingTypes.exists(rt -> rt instanceof NullType || ((RefType)rt).getSootClass().getName().equals(klass.getName())), "Did not find " + klass.getName());
			}
		}
	}


	protected void combinedArrayTest(final ApplicationIntf[] a) {
		dumpIR();
		populateArray(a);
		final int[] cIntArray = new int[4];
		final int[] aIntArray = allocateType(Application.class).getBoundedArray();
		final Object o = nondet() == 1 ? cIntArray : nondet() == 0 ? aIntArray : a;
		assertEqual(o instanceof ApplicationIntf[], lift(true,false));
		assertEqual(o instanceof int[], lift(true, false));
		final int[] k = (int[])o;
		k[0] = 3;
		final ApplicationIntf[] casted = (ApplicationIntf[]) o;
		casted[nondet()] = allocateType(Application4.class);
		final ApplicationIntf read = casted[nondet()];
		customAssert(read, "variant", ApplicationVariantChecker.class);
	}

	private void populateArray(final ApplicationIntf[] i) {
		i[0] = allocateType(Application1.class);
		i[1] = allocateType(Application2.class);
		i[2] = allocateType(Application3.class);
	}

	public void main() {
		this.testAbstractArrays();
	}
}
