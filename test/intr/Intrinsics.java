package intr;

import edu.washington.cse.concerto.interpreter.ai.test.AssertionChecker;

@SuppressWarnings("unchecked")
final public class Intrinsics {
	private Intrinsics() { }
	
	public static void println(final int v) { }
	public static <T> void println(final T toPrint) { }
	public static int read() { return 3; }
	public static int nondet() { return 4; }
	public static void write(final int v) { }
	
	public static int lift(final int... l) { return 0; }
	public static <T> T lift(final T[] l) { return null; }
	public static boolean lift(final boolean... a) { return false; }
	
	public static <T> void assertEqual(final T a, final T b) { }
	public static void assertEqual(final int a, final int b) { }
	public static void assertEqual(final boolean a, final boolean b) { }
	
	public static <T> void assertNotEqual(final T a, final T b) { }
	public static void assertNotEqual(final int a, final int b) { }
	public static void assertNotEqual(final boolean a, final boolean b) { }
	public static void assertFalse(final String msg) { }
	
	public static void dumpState() { }
	public static void dumpState(final String key) { }
	public static void dumpObjectState(final Object o) { }
	public static void dumpIR() { }
	
	public static void debug(final String s) { }
	public static void abort() { }
	public static void fail(final String msg) { }
	public static void printStackTrace() { }
	
	public static ForeignLocation liftLocation(final String s) { return null; }
	public static int get(final ForeignLocation fl) { return 0; }
	public static void put(final ForeignLocation fl, final int s) { }
	
	public static <T> T allocateType(final String s) { return (T) new Object(); }
	public static <T> T allocateType(final int k) { return (T)new Object(); }
	public static <T> T allocateType(final Class<T> klass) { return (T)new Object(); }
	public static Object invokeObj(final Object receiver, final int klassKey, final int method, final Object argument) {
		return new Object();
	}

	public static <T> int getClass(final T obj) { return 0; }
	
	public static Object invokeObj(final Object receiver, final int klassKey, final int method, final int argument) {
		return new Object();
	}
	
	public static int invokeInt(final Object receiver, final int klassKey, final int method, final Object argument) {
		return 0;
	}
	public static int invokeInt(final Object receiver, final int klassKey, final int method, final int argument) {
		return 0;
	}

	public static int invokeInt(final Object receiver, final int klassKey, final int method) {
		return 0;
	}

	public static Object invokeObj(final Object receiver, final int klassKey, final int method) {
		return new Object();
	}

	public static void assertToStringEquals(final Object o, final String eq) { }
	public static void assertToStringEquals(final int o, final String eq) { }
	public static void customAssert(final Object o, final String key, final Class<? extends AssertionChecker> clazz) { }
	
	/*
	 * This is a gross hack
	 */
	public static void checkAsserts() { }
}
