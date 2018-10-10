package edu.washington.cse.concerto.interpreter.ai;

import edu.washington.cse.concerto.interpreter.BodyManager;
import edu.washington.cse.concerto.interpreter.ReflectionEnvironment;
import edu.washington.cse.concerto.interpreter.exception.FailedObjectLanguageAssertionException;
import edu.washington.cse.concerto.interpreter.exception.UnrecognizedIntrinsicException;
import edu.washington.cse.concerto.interpreter.meta.InstrumentedState;
import fj.F0;
import fj.data.Option;
import soot.SootClass;
import soot.SootMethodRef;
import soot.Value;
import soot.jimple.ClassConstant;
import soot.jimple.IntConstant;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.StringConstant;
import soot.util.NumberedString;

public abstract class IntrinsicHandler<AS, Context> {
	public static boolean ENABLE_PRINTLN = true;

	private final F0<? extends StateMonad<AS, ?>> stateMonadProvider;

	public IntrinsicHandler(final F0<? extends StateMonad<AS, ?>> monads) {
		this.stateMonadProvider = monads;
	}

	public EvalResult handleIntrinsic(final InstrumentedState currState, final StaticInvokeExpr sie, final Context context) {
		final SootMethodRef mref = sie.getMethodRef();
		final NumberedString sig = mref.getSubSignature();
		if(mref.name().equals("read") || mref.name().equals("nondet")) {
			return this.handleIO(currState, context);
		} else if(mref.name().equals("dumpIR")) {
			System.out.println(BodyManager.getHostMethod(sie).getActiveBody());
			return new EvalResult(currState, null);
		} else if(mref.name().equals("allocateType")) {
			if(sie.getArg(0) instanceof ClassConstant) {
				final String jvmRepr = ((ClassConstant)sie.getArg(0)).value;
				final String languageRepr = jvmRepr.substring(1, jvmRepr.length() - 1).replace('/', '.');
				return this.handleConstantAlloc(currState, sie, languageRepr, context);
			} else if(sie.getArg(0) instanceof IntConstant) {
				final IntConstant classKey = (IntConstant) sie.getArg(0);
				final Option<SootClass> resolvedClass = ReflectionEnvironment.v().resolve(classKey.value);
				if(resolvedClass.isSome()) {
					return this.handleConstantAlloc(currState, sie, resolvedClass.some().getName(), context);
				}
			} else if(sie.getArg(0) instanceof StringConstant) {
				return this.handleConstantAlloc(currState, sie, ((StringConstant) sie.getArg(0)).value, context);
			}
			return this.handleAllocate(currState, sie, context);
		} else if(mref.name().equals("println")) {
			final EvalResult res = handlePrintln(currState, sie, context);
			return new EvalResult(res.state, null);
		} else if(mref.name().equals("dumpState")) {
			if(mref.parameterTypes().size() == 1) {
				System.out.println(" >>>> " + sie.getArg(0) + "\n\n" + currState + "\n<<<");
			} else {
				System.out.println(currState);
			}
			return new EvalResult(currState, null);
		} else if(mref.name().equals("debug")) {
			System.out.println(((StringConstant)sie.getArg(0)).value);
			return new EvalResult(currState, null);
		} else if(mref.name().equals("invokeObj") || mref.name().equals("invokeInt")) {
			return this.handleInvoke(currState, sie, context);
		} else if(mref.name().equals("lift")) {
			return this.handleLift(currState, sie, context);
		} else if(mref.name().equals("assertFalse")) {
			final String msg = ((StringConstant)sie.getArg(0)).value;
			throw new FailedObjectLanguageAssertionException("Unreachable code reached: " + msg);
		} else if(mref.name().startsWith("assert") || mref.name().equals("customAssert")) {
			return this.handleAssert(currState, sie, context);
		} else if(mref.name().equals("fail")) {
			return handleFailure(currState, sie);
		} else if(mref.name().equals("abort")) {
			return this.handleAbort();
		} else if(mref.name().equals("checkAsserts")) {
			return this.handleCheckAsserts(currState, sie, context);
		} else if(mref.name().equals("getClass")) {
			final EvalResult res = this.eval(currState, sie.getArg(0), context);
			return this.handleGetClass(res.state, res.value, context);
		} else if(mref.name().equals("write")) {
			final EvalResult res = this.eval(currState, sie.getArg(0), context);
			return new EvalResult(res.state, null);
		} else {
			throw new UnrecognizedIntrinsicException(sig.toString());
		}
	}

	protected abstract EvalResult handleGetClass(final InstrumentedState state, final Object value, final Context context);

	protected EvalResult handlePrintln(final InstrumentedState currState, final StaticInvokeExpr sie, final Context context) {
		final EvalResult res = this.eval(currState, sie.getArg(0), context);
		if(ENABLE_PRINTLN) {
			System.out.println(res.value);
		}
		return new EvalResult(res.state, null);
	}

	protected EvalResult handleAbort() {
		System.exit(1);
		return null;
	}

	protected EvalResult handleCheckAsserts(final InstrumentedState currState, final StaticInvokeExpr sie, final Context context) {
		return new EvalResult(currState, null);
	}

	protected  EvalResult handleAssert(final InstrumentedState currState, final StaticInvokeExpr sie, final Context context) {
		return new EvalResult(currState, null);
	}

	protected abstract EvalResult handleFailure(final InstrumentedState currState, final StaticInvokeExpr sie);

	protected abstract EvalResult handleLift(final InstrumentedState currState, final StaticInvokeExpr sie, final Context context);
	protected abstract EvalResult handleInvoke(final InstrumentedState currState, final StaticInvokeExpr sie, final Context context);
	protected abstract EvalResult handleAllocate(final InstrumentedState currState, final StaticInvokeExpr sie, final Context context);
	protected abstract EvalResult handleConstantAlloc(final InstrumentedState currState, StaticInvokeExpr sie, String className, Context context);
	protected abstract EvalResult handleIO(final InstrumentedState currState, final Context context);
	protected abstract EvalResult eval(final InstrumentedState currState, final Value arg, final Context context);
}
