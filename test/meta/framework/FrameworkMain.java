package meta.framework;

import edu.washington.cse.concerto.interpreter.ai.instantiation.pta.JValue;
import edu.washington.cse.concerto.interpreter.ai.test.AssertionChecker;
import edu.washington.cse.concerto.interpreter.meta.CombinedValue;
import edu.washington.cse.concerto.interpreter.value.IValue;
import meta.application.MixedAction;
import meta.application.SimpleAction;
import meta.framework.filter.Filter;
import meta.framework.response.NotFoundPayload;
import meta.framework.response.Payload;
import meta.framework.response.Result;
import soot.ArrayType;
import soot.IntType;

import static intr.Intrinsics.*;

public class FrameworkMain {
	public static final class ResultImpl implements Result {
		private Payload payload;
		public ResultImpl() {
			payload = null;
		}
		
		@Override
		public void setPayload(final Payload x) {
			this.payload = x;
		}
		
		@Override
		public Payload getPayload() {
			return payload;
		}
	}

	public void main() {
		final ObjectGraph og = initObjectGraph();
		final Dispatcher d = initDispatcher(og);
		final Filters filters = initFilters();
		Request req = getRequestObject(nondet());
		while(req.getTarget() >= 0) {
			final Result result = filters.handleRequest(d, req);
			final int[] response = result.getPayload().getBytes();
			for(int i = 0; i < response.length; i++) {
				write(response[i]);
			}
			req = getRequestObject(nondet());
		}
	}

	private ObjectGraph initObjectGraph() {
		final ObjectGraph toReturn = new ObjectGraph();
		toReturn.init();
		return toReturn;
	}

	private Filters initFilters() {
		final int numFilters = read();
		final FilterAndTarget[] fats = new FilterAndTarget[numFilters];
		for(int i = 0; i < fats.length; i++) {
			final int klass = read();
			final Filter f = allocateType(klass);
			final int numProps = read();
			for(int j = 0; j < numProps; j++) {
				final int propKey = read();
				final int propLen = read();
				final int[] propStream = new int[propLen];
				for(int k = 0; k < propLen; k++) {
					propStream[k] = read();
				}
				f.init(propKey, propStream);
			}
			final int numTargets = read();
			if(numTargets == -1) {
				fats[i] = new GlobFilter(f);
			} else {
				final int[] targets = new int[numTargets];
				for(int j = 0; j < targets.length; j++) {
					targets[j] = read();
				}
				fats[i] = new FilterAndLiteralTargets(f, targets);
			}
		}
		return new Filters(fats);
	}

	public void testMain() {
		final Dispatcher d = initDispatcher(initObjectGraph());
		final int k = read();
		assertEqual(k, 0);
		final int testSuite = read();
		final int numActions = read();
		for(int i = 0; i < numActions; i++) {
			final int target = read();
			final Request r = getRequestObject(target);
			final Result y = d.forward(target, r);
			if(testSuite == 0) {
				println(y);
				println(y.getPayload());
				println(y.getPayload().getBytes());
			} else if(testSuite == 1) {
				assertPtsTo(i, y);
			} else if(testSuite == 2) {
				assertPtsToIndirection(i, y);
			} else {
				fail("Unrecognized test suite");
			}
		}
	}

	private Request getRequestObject(final int target) {
		final ReqAttributes attr = new ReqAttributes();
		int paramCount = nondet();
		if(paramCount < 0) {
			paramCount = 0;
		}
		final int[] params = new int[paramCount];
		for(int i = 0; i < paramCount; i++) {
			params[i] = nondet();
		}
		return new FrameworkRequest(target, attr, params);
	}

	private Dispatcher initDispatcher(final ObjectGraph og) {
		final Action[] actionTable;
		{
			final int numActions = read();
			actionTable = new Action[numActions];
			for(int i = 0; i < numActions; i++ ) {
				actionTable[i] = og.getUnchecked(read());
			}
		}
		return getDispatcher(actionTable, og);
	}

	private Dispatcher getDispatcher(final Action[] actionTable, final ObjectGraph og) {
		return new Dispatcher() {
			@Override public ObjectGraph getObjectGraph() {
				return og;
			}

			@Override
			public Result forward(final int r, final Request req) {
				if(r < 0 || r >= actionTable.length) {
					return new NotFoundResult();
				}
				return actionTable[r].doAction(r, this, req);
			}

			@Override
			public void printInt(final int a) {
				println(a);
			}

			@Override
			public Forwarder dispatch(final int r) {
				final Dispatcher self = this;
				return new Forwarder() {
					@Override public Result forward(final Request req) {
						return actionTable[r].doAction(r, self, req);
					}
				};
			}

			@Override
			public int readNondet() {
				return nondet();
			}

			@Override
			public int[] getRegisteredChain() {
				return new int[]{1,3,4};
			}

			@Override
			public void writeDatabase(final int value) { }

			@Override
			public void writeDatabase(final Object attributes) { }
		};
	}

	public static class PtaChecker implements AssertionChecker {
		@Override public void assertValue(final String k, final IValue v, final AssertF assertF) {
			if(!k.equals("ind-result")) {
				assertF.f(v.isEmbedded(), "not embedded");
			}
			if(k.equals("payload-impl1")) {
				final JValue jv = (JValue) v.aVal.value;
				final Class<?> expectedClass = SimpleAction.PayloadImpl1.class;
				assertAddressIs(assertF, jv, expectedClass.getName());
			} else if(k.equals("int-array")) {
				final JValue jv = (JValue) v.aVal.value;
				assertAddressIs(assertF, jv, "int[]");
			} else if(k.equals("combined-result")) {
				assertF.f(v.aVal.value instanceof CombinedValue, "wrong value repr");
				final CombinedValue cv = (CombinedValue) v.aVal.value;
				final IValue iv = cv.concreteComponent;
				assertAddressIs(assertF, iv, ResultImpl.class);
				assertAddressIs(assertF, cv.abstractComponent, MixedAction.class.getName() + "$2");
			} else if(k.equals("combined-payload")) {
				assertF.f(v.aVal.value instanceof CombinedValue, "wrong value repr");
				final CombinedValue cv = (CombinedValue) v.aVal.value;
				final IValue iv = cv.concreteComponent;
				assertAddressIs(assertF, iv, NotFoundPayload.class);
				assertAddressIs(assertF, cv.abstractComponent, MixedAction.class.getName() + "$1");
			} else if(k.equals("combined-bytes")) {
				assertF.f(v.aVal.value instanceof CombinedValue, "wrong value repr");
				final CombinedValue cv = (CombinedValue) v.aVal.value;
				final IValue iv = cv.concreteComponent;
				assertF.f(iv.getTag() == IValue.RuntimeTag.ARRAY, "wrong runtime type");
				assertF.f(iv.getLocation().type instanceof ArrayType, "wrong heap type");
				assertF.f(iv.getLocation().type.equals(IntType.v().makeArrayType()), "wrong array type");
				assertAddressIs(assertF, cv.abstractComponent, "int[]");
			} else if(k.equals("ind-impl")) {
				assertAddressIs(assertF, v, ResultImpl.class);
			} else if(k.equals("ind-payload")) {
				assertAddressIs(assertF, v.aVal.value, SimpleAction.PayloadImpl1.class.getName());
			} else if(k.equals("ind-bytes")) {
				assertAddressIs(assertF, v.aVal.value, "int[]");
			}
		}

		protected void assertAddressIs(final AssertF assertF, final IValue iv, final Class<?> testKlass) {
			assertF.f(iv.getTag() == IValue.RuntimeTag.OBJECT, "wrong runtime tag: " + iv.getTag());
			assertF.f(iv.getSootClass().getName().equals(testKlass.getName()), "wrong runtime type: " + iv);
		}

		protected void assertAddressIs(final AssertF assertF, final Object aVal, final String name) {
			assertF.f(aVal instanceof JValue, "wrong abstract value");
			final JValue jv = (JValue) aVal;
			assertF.f(jv.addressSet.size() == 1, "wrong size");
			assertF.f(jv.addressSet.iterator().next().t.toString().equals(name), "wrong type");
		}
	}

	/*
		TODO: replace these with custom asserts
	 */
	private void assertPtsTo(final int i, final Result y) {
		if(i == 0) {
			customAssert(y.getPayload(), "payload-impl1", PtaChecker.class);
			customAssert(y.getPayload().getBytes(), "int-array", PtaChecker.class);
		} else if(i == 1) {
			customAssert(y, "combined-result", PtaChecker.class);
			customAssert(y.getPayload(), "combined-payload", PtaChecker.class);
			customAssert(y.getPayload().getBytes(), "combined-bytes", PtaChecker.class);
		} else {
			fail("Unrecognized test round");
		}
	}
	
	private void assertPtsToIndirection(final int i, final Result y) {
		if(i != 0) {
			println(i);
			fail("Unrecognized test round");
		}
		customAssert(y, "ind-result", PtaChecker.class);
		customAssert(y.getPayload(), "ind-paylaod", PtaChecker.class);
		customAssert(y.getPayload().getBytes(), "ind-bytes", PtaChecker.class);
	}

	private static class FrameworkRequest implements Request {
		private final int target;
		private final ReqAttributes attr;
		private final int[] requestData;

		public FrameworkRequest(final int target, final ReqAttributes attr, final int[] requestData) {
			this.target = target;
			this.attr = attr;
			this.requestData = requestData;
		}

		@Override
		public int getTarget() {
			return target;
		}

		@Override
		public int getRequestData(final int k) {
			if(k < 0 || k >= requestData.length) {
				fail("out of bounds");
			}
			return requestData[k];
		}

		@Override public int getParameterCount() {
			return requestData.length;
		}

		@Override
		public ReqAttributes getAttributes() {
			return attr;
		}

		@Override public Request withAttribute(final int k, final Object v) {
			return new FrameworkRequest(target, attr.withAttribute(k, v), requestData);
		}

		@Override public Request withAttribute(final int k, final int v) {
			return new FrameworkRequest(target, attr.withAttribute(k, v), requestData);
		}

		@Override public Request withAttributes(final ReqAttributes attr) {
			return new FrameworkRequest(target, attr, requestData);
		}

		@Override
		public Request withRequestData(final int[] requestData) {
			return new FrameworkRequest(target, attr, requestData);
		}
	}
}
