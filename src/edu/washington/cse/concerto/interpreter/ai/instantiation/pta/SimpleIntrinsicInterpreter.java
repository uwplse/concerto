package edu.washington.cse.concerto.interpreter.ai.instantiation.pta;

import edu.washington.cse.concerto.interpreter.ai.CallHandler;
import edu.washington.cse.concerto.interpreter.ai.EvalResult;
import edu.washington.cse.concerto.interpreter.ai.HeapReader;
import edu.washington.cse.concerto.interpreter.ai.IntrinsicHandler;
import edu.washington.cse.concerto.interpreter.ai.RecursiveTransformer;
import edu.washington.cse.concerto.interpreter.ai.StateMonad;
import edu.washington.cse.concerto.interpreter.ai.ValueMapper;
import edu.washington.cse.concerto.interpreter.ai.ValueMonad;
import edu.washington.cse.concerto.interpreter.heap.Heap;
import edu.washington.cse.concerto.interpreter.meta.InstrumentedState;
import edu.washington.cse.concerto.interpreter.value.IValue;
import fj.F0;
import soot.Value;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.StringConstant;

import java.util.ArrayList;
import java.util.List;

public abstract class SimpleIntrinsicInterpreter<AS, Context, AVal> extends IntrinsicHandler<AS, Context> {
	private final F0<? extends StateMonad<AS, AVal>> stateMonad;
	private final F0<CallHandler<Context>> handlerProvider;
	private final F0<? extends ValueMonad<AVal>> valueMonad;

	public SimpleIntrinsicInterpreter(final F0<? extends StateMonad<AS, AVal>> stateMonad, final F0<? extends ValueMonad<AVal>> valueMonad, final F0<CallHandler<Context>> handlerProvider) {
		super(stateMonad);
		this.stateMonad = stateMonad;
		this.valueMonad = valueMonad;
		this.handlerProvider = handlerProvider;
	}

	@Override protected EvalResult handleLift(final InstrumentedState currState, final StaticInvokeExpr sie, final Context context) {
		final EvalResult liftResult = this.eval(currState, sie.getArg(0), context);
		final Object lifted = stateMonad.f().mapValue(liftResult.state, liftResult.value, new ValueMapper<AVal, AS, Object>() {
			@Override public Object merge(final Object v1, final Object v2) {
				return valueMonad.f().join(v1, v2);
			}

			@Override public Object mapAbstract(final AVal aVal, final AS as, final Heap h, final RecursiveTransformer<AS, Object> recursor) {
				return readAllFields(as, aVal);
			}

			@Override public Object mapConcrete(final IValue v, final AS as, final Heap h, final HeapReader<AS, AVal> heapAccessor, final RecursiveTransformer<AS, Object> recursor) {
				return heapAccessor.readNondetIndex(h, v).value;
			}
		});
		return new EvalResult(liftResult.state, lifted);
	}

	@Override protected EvalResult handleInvoke(final InstrumentedState currState, final StaticInvokeExpr sie, final Context context) {
		final List<Object> argVals = new ArrayList<>();
		InstrumentedState s = currState;
		for(final Value v : sie.getArgs()) {
			final EvalResult argResult = this.eval(s, v, context);
			s = argResult.state;
			argVals.add(argResult.value);
		}
		return this.handlerProvider.f().handleInvoke(context, sie, argVals, s).map(EvalResult::new).orSome(this::handleMissingSummary);
	}

	@Override protected EvalResult handleAllocate(final InstrumentedState currState, final StaticInvokeExpr sie, final Context context) {
		if(sie.getArg(0) instanceof StringConstant) {
			final String typeName = ((StringConstant) sie.getArg(0)).value;
			return this.handlerProvider.f().allocType(typeName,currState,sie, context).orSome(this::handleMissingSummary);
		} else {
			final EvalResult argResult = this.eval(currState, sie.getArg(0), context);
			return this.handlerProvider.f().allocUnknownType(argResult.state, sie, context).orSome(this::handleMissingSummary);
		}
	}

	@Override protected EvalResult handleIO(final InstrumentedState currState, final Context context) {
		return new EvalResult(currState, valueMonad.f().lift(this.nondetInt()));
	}

	protected abstract AVal nondetInt();
	protected abstract Object readAllFields(AS state, AVal arrayRef);
	protected abstract EvalResult handleMissingSummary();

	@Override protected EvalResult handleConstantAlloc(final InstrumentedState currState, final StaticInvokeExpr sie, final String className, final Context context) {
		return handlerProvider.f().allocType(className, currState, sie, context).orSome(this::handleMissingSummary);
	}

	@Override protected EvalResult handleGetClass(final InstrumentedState state, final Object value, final Context context) {
		return new EvalResult(state, valueMonad.f().lift(this.nondetInt()));
	}
}
