package edu.washington.cse.concerto.instrumentation;

import edu.washington.cse.concerto.Function;
import edu.washington.cse.concerto.instrumentation.actions.AssignmentAction;
import edu.washington.cse.concerto.instrumentation.actions.FieldAction;
import edu.washington.cse.concerto.instrumentation.actions.FieldReadAction;
import edu.washington.cse.concerto.instrumentation.actions.FieldWriteAction;
import edu.washington.cse.concerto.instrumentation.actions.MethodCallAction;
import edu.washington.cse.concerto.instrumentation.actions.MultiValueReader;
import edu.washington.cse.concerto.instrumentation.actions.MultiValueReplacement;
import edu.washington.cse.concerto.instrumentation.actions.ValueReplacement;
import edu.washington.cse.concerto.instrumentation.filter.ValueFilter;
import edu.washington.cse.concerto.instrumentation.filter.WrappedPredicate;
import edu.washington.cse.concerto.instrumentation.forms.Assignment;
import edu.washington.cse.concerto.instrumentation.forms.HeapAccess;
import edu.washington.cse.concerto.instrumentation.forms.HeapLocation;
import edu.washington.cse.concerto.instrumentation.forms.MethodCall;
import edu.washington.cse.concerto.instrumentation.impl.AssignmentDisjImpl;
import edu.washington.cse.concerto.instrumentation.impl.AssignmentSelectorImpl;
import edu.washington.cse.concerto.instrumentation.impl.FieldReadDisjImpl;
import edu.washington.cse.concerto.instrumentation.impl.FieldWriteDisjImpl;
import edu.washington.cse.concerto.instrumentation.impl.FilterAndAction;
import edu.washington.cse.concerto.instrumentation.impl.MethodCallSelectorImpl;
import edu.washington.cse.concerto.instrumentation.impl.MethodDisjImpl;
import edu.washington.cse.concerto.instrumentation.impl.ReadSelectorImpl;
import edu.washington.cse.concerto.instrumentation.impl.WriteSelectorImpl;
import edu.washington.cse.concerto.interpreter.EmbeddedState;
import edu.washington.cse.concerto.interpreter.ai.State;
import edu.washington.cse.concerto.interpreter.ai.StateMonad;
import edu.washington.cse.concerto.interpreter.ai.StateUpdater;
import edu.washington.cse.concerto.interpreter.ai.ValueMapper;
import edu.washington.cse.concerto.interpreter.ai.ValueMonad;
import edu.washington.cse.concerto.interpreter.ai.ValueStateTransformer;
import edu.washington.cse.concerto.interpreter.heap.Heap;
import edu.washington.cse.concerto.interpreter.lattice.Lattice;
import edu.washington.cse.concerto.interpreter.meta.InstrumentedState;
import edu.washington.cse.concerto.interpreter.meta.Monads;
import edu.washington.cse.concerto.interpreter.meta.TypeOracle;
import edu.washington.cse.concerto.interpreter.meta.TypeOracle.TypeOwner;
import edu.washington.cse.concerto.interpreter.state.ExecutionState;
import edu.washington.cse.concerto.interpreter.value.EmbeddedValue;
import edu.washington.cse.concerto.interpreter.value.IValue;
import fj.F;
import fj.F2;
import fj.Monoid;
import fj.P;
import fj.P2;
import fj.P4;
import fj.data.Option;
import fj.data.Seq;
import fj.function.Effect2;
import soot.SootMethodRef;
import soot.Type;
import soot.jimple.InvokeExpr;
import soot.toolkits.scalar.Pair;

import java.util.ArrayList;
import java.util.List;

public class InstrumentationManager<AVal, AHeap, AState> {
	private final List<FilterAndAction<MethodCall, MethodCallAction<AVal, AHeap, AState>>> methodActions = new ArrayList<>();
	private final List<FilterAndAction<HeapAccess, FieldWriteAction<AVal, AHeap, AState>>> fieldWriteActions = new ArrayList<>();
	private final List<FilterAndAction<HeapAccess, FieldReadAction<AVal, AHeap, AState>>> fieldReadActions = new ArrayList<>();
	private final List<FilterAndAction<Assignment, AssignmentAction<AVal, AHeap, AState>>> assignmentActions = new ArrayList<>();
	private final StateMonad<AState, AVal> stateMonad;
	private final State<AHeap, AState> stateTransformer;
	private final F<InstrumentedState, Pair<Heap, AState>> projector;
	private final ValueMonad<AVal> valueMonad;
	private final Lattice<AHeap> heapLattice;
	private final Lattice<AVal> valueLattice;
	private final TypeOracle oracle;
	
	public InstrumentationManager(final Monads<AVal, AState> monads, final State<AHeap, AState> stateTransformer, final F<InstrumentedState, Pair<Heap, AState>> proj,
			final Lattice<AHeap> heapLattice, final Lattice<AVal> valueLattice, final TypeOracle oracle) {
		this.stateMonad = monads.stateMonad;
		this.valueMonad = monads.valueMonad;
		this.stateTransformer = stateTransformer;
		this.projector = proj;
		this.heapLattice = heapLattice;
		this.valueLattice = valueLattice;
		this.oracle = oracle;
	}

	private class InPlaceReplace implements ValueReplacement<AVal, AHeap, AState> {
		public IValue newValue;
		public Heap h;
		public EmbeddedState<AHeap> foreignHeap;

		private InPlaceReplace(final EmbeddedState<AHeap> foreignHeap, final Heap heap, final IValue baseValue) {
			this.newValue = baseValue;
			this.h = heap;
			this.foreignHeap = foreignHeap;
		}

		@Override
		public void update(final ValueStateTransformer<AVal, AState> tr) {
			final P2<IValue, EmbeddedState<AHeap>> updated = doUpdate(tr, newValue, h, foreignHeap);
			foreignHeap = updated._2();
			newValue = updated._1();
		}

		@Override
		public ValueReader<AVal, AHeap, AState> getReader() {
			return getValueReader(newValue, h, foreignHeap);
		}
	}
	
	private P2<IValue, EmbeddedState<AHeap>> doUpdate(final ValueStateTransformer<AVal, AState> tr, final IValue v, final Heap h, final EmbeddedState<AHeap> fh) {
		final Heap copy = h.fork();
		final InstrumentedState st = getState(fh, copy);
		final Object[] byRef = new Object[1];
		final InstrumentedState updated = stateMonad.updateValue(st, valueMonad.lift(v), tr, new StateUpdater<AState>() {
			@Override
			public AState updateState(final AState state, final Object value) {
				byRef[0] = value;
				return state;
			}
		});
		final Pair<Heap, AState> f = projector.f(updated);
		final AHeap newHeap = stateTransformer.project(f.getO2());
		final IValue newValue;
		if(byRef[0] instanceof IValue) {
			newValue = (IValue) byRef[0];
		} else {
			newValue = new IValue(new EmbeddedValue(byRef[0], valueMonad));
		}
		h.applyHeap(copy);
		return P.p(newValue, new EmbeddedState<AHeap>(newHeap, fh.stateLattice));
	}

	private InstrumentedState getState(final EmbeddedState<AHeap> fh, final Heap copy) {
		if(fh == null) {
			return stateMonad.lift(stateTransformer.emptyState(), copy);
		} else {
			return stateMonad.lift(stateTransformer.inject(stateTransformer.emptyState(), fh.state), copy);
		}
	}
	
	private class InPlaceMulti implements MultiValueReplacement<AVal, AHeap, AState> {
		public InPlaceMulti(final EmbeddedState<AHeap> foreignHeap, final Heap heap, final IValue receiver, final List<IValue> args, final IValue returnSlot) {
			this.foreignHeap = foreignHeap;
			this.h = heap;
			this.receiver = receiver;
			this.args = args;
			this.returnSlot = returnSlot;
		}
		
		public EmbeddedState<AHeap> foreignHeap;
		public Heap h;
		public List<IValue> args = null;
		public IValue receiver = null;
		public IValue returnSlot = null;
		
		@Override
		public void update(final int i, final ValueStateTransformer<AVal, AState> tr) {
			checkIndex(i);
			final P2<IValue, EmbeddedState<AHeap>> update;
			if(i == RETURN_SLOT) {
				update = doUpdate(tr, returnSlot, h, foreignHeap);
				returnSlot = update._1();
			} else if(i == -1) {
				update = doUpdate(tr, receiver, h, foreignHeap);
				returnSlot = update._1();
			} else {
				update = doUpdate(tr, args.get(i), h, foreignHeap);
				args.set(i, update._1());
			}
			foreignHeap = update._2();
		}

		public void checkIndex(final int i) {
			if(i == RETURN_SLOT && returnSlot == null) {
				throw new IllegalArgumentException();
			}
			if(i >= args.size()) {
				throw new IllegalArgumentException();
			}
			if(i < -1) {
				throw new IllegalArgumentException();
			}
		}

		@Override
		public MultiValueReader<AVal, AHeap, AState> getReader() {
			return new MultiValueReader<AVal, AHeap, AState>() {
				@Override
				public <R> R read(final int i, final ValueMapper<AVal, AState, R> map) {
					checkIndex(i);
					final InstrumentedState st = getState(foreignHeap, h);
					return stateMonad.mapValue(st, this.readRaw(i), map);
				}

				@Override public Object readRaw(final int i) {
					if(i == RETURN_SLOT) {
						return valueMonad.lift(returnSlot);
					} else if(i == -1) {
						return valueMonad.lift(receiver);
					} else {
						return valueMonad.lift(args.get(i));
					}
				}
			};
		}
	}

	public InstrumentationSelector<AVal, AHeap, AState> selector() {
		return new InstrumentationSelector<AVal, AHeap, AState>() {
			
			private <FormType, ActionType> F<Seq<ValueFilter<FormType>>, ActionHandler<ActionType>> getContinuation(final List<FilterAndAction<FormType, ActionType>> outputList) {
				return new F<Seq<ValueFilter<FormType>>, ActionHandler<ActionType>>() {
					@Override
					public ActionHandler<ActionType> f(final Seq<ValueFilter<FormType>> filters) {
						return new ActionHandler<ActionType>() {
							@Override
							public void withAction(final ActionType action) {
								outputList.add(new FilterAndAction<>(filters, action));
							}
						};
					}
				};
			}
			
			@Override
			public MethodCallSelector<ActionHandler<MethodCallAction<AVal, AHeap, AState>>> methodCall() {
				return new MethodCallSelectorImpl<>(getContinuation(methodActions));
			}
			
			@Override
			public FieldWriteSelector<ActionHandler<FieldWriteAction<AVal, AHeap, AState>>> fieldWrite() {
				return new WriteSelectorImpl<>(getContinuation(fieldWriteActions));
			}
			
			@Override
			public FieldReadSelector<ActionHandler<FieldReadAction<AVal, AHeap, AState>>> fieldRead() {
				return new ReadSelectorImpl<>(getContinuation(fieldReadActions));
			}
			
			@Override
			public AssignmentSelector<ActionHandler<AssignmentAction<AVal, AHeap, AState>>> assignment() {
				return new AssignmentSelectorImpl<>(getContinuation(assignmentActions));
			}
			
			@Override
			public MethodDisjunctionSelector<AVal, AHeap, AState> methodCases() {
				return new MethodDisjImpl<>(methodActions);
			}

			@Override
			public AssignmentDisjunctionSelector<AVal, AHeap, AState> assignmentCases() {
				return new AssignmentDisjImpl<>(assignmentActions);
			}

			@Override
			public FieldWriteDisjunctionSelector<AVal, AHeap, AState> fieldWriteCases() {
				return new FieldWriteDisjImpl<>(fieldWriteActions);
			}

			@Override
			public FieldReadDisjunctionSelector<AVal, AHeap, AState> fieldReadCases() {
				return new FieldReadDisjImpl<>(fieldReadActions);
			}
		};
	}
	
	public IValue postBase(final ExecutionState<AHeap, ?> es, final IValue baseValue, final HeapLocation ref, final boolean isRead) {
		if(isRead) {
			return filterBasePointer(es, baseValue, ref, this.fieldReadActions);
		} else {
			return filterBasePointer(es, baseValue, ref, this.fieldWriteActions);
		}
	}

	private IValue filterBasePointer(final ExecutionState<AHeap, ?> es, final IValue baseValue, final HeapLocation ref,
			final List<? extends FilterAndAction<HeapAccess, ? extends FieldAction<AVal, AHeap, AState>>> actionFilters) {
		final HeapAccess toTest = new HeapAccess(baseValue, ref, es.heap, es.foreignHeap, valueMonad);
		final List<FieldAction<AVal, AHeap, AState>> actions = new ArrayList<>();
		outer_loop: for(final FilterAndAction<HeapAccess, ? extends FieldAction<AVal, AHeap, AState>> spec : actionFilters) {
			for(final ValueFilter<HeapAccess> f : spec.filters) {
				if(!f.test(toTest)) {
					continue outer_loop;
				}
			}
			actions.add(spec.action);
		}
		if(actions.isEmpty()) {
			return baseValue;
		}
		final InPlaceReplace valueReplacement = this.getValueReplacement(baseValue, es.heap, es.foreignHeap);
		for(final FieldAction<AVal, AHeap, AState> act : actions) {
			act.postBase(valueReplacement);
		}
		es.replaceHeap(valueReplacement.foreignHeap);
		return valueReplacement.newValue;
	}
	
	public IValue filterFieldOutput(final ExecutionState<AHeap, ?> es, final IValue baseValue, final HeapLocation ref, final boolean isRead, final IValue output) {
		if(isRead) {
			return filterFieldOutput(es, baseValue, ref, this.fieldReadActions, output,
				new Effect2<FieldReadAction<AVal, AHeap, AState>, ValueReplacement<AVal, AHeap, AState>>() {
					@Override
					public void f(final FieldReadAction<AVal, AHeap, AState> a, final ValueReplacement<AVal, AHeap, AState> b) {
						a.postRead(b);
					}
			});
		} else {
			return filterFieldOutput(es, baseValue, ref, this.fieldWriteActions, output,
				new Effect2<FieldWriteAction<AVal, AHeap, AState>, ValueReplacement<AVal, AHeap, AState>>() {
					@Override
					public void f(final FieldWriteAction<AVal, AHeap, AState> a, final ValueReplacement<AVal, AHeap, AState> b) {
						a.preWrite(b);
					}
			});
		}
	}
	
	private final Monoid<Option<AVal>> valMonoid = Monoid.<Option<AVal>>monoid(new F2<Option<AVal>, Option<AVal>, Option<AVal>>() {
		@Override
		public Option<AVal> f(final Option<AVal> a, final Option<AVal> b) {
			if(a.isNone()) {
				return b;
			} else if(b.isNone()) {
				return a;
			} else {
				return Option.some(valueLattice.join(a.some(), b.some()));
			}
		}
		
	}, Option.<AVal>none());
	
	public static class PreCallInstrumentation<AHeap> { 
		public final IValue receiver;
		public final List<IValue> arguments;
		public final Option<IValue> summary;
		public final EmbeddedState<AHeap> fh;
		
		public PreCallInstrumentation(final IValue receiver, final List<IValue> arguments, final Option<IValue> summary, final EmbeddedState<AHeap> fh) {
			this.receiver = receiver;
			this.arguments = arguments;
			this.summary = summary;
			this.fh = fh;
		}
	}
	
	public PreCallInstrumentation<AHeap> preCallCoop(final Heap h, final AHeap fh, final IValue baseValue, final List<IValue> arguments, final SootMethodRef target,
			final InvokeExpr op) {
		final EmbeddedState<AHeap> embeddedHeap = new EmbeddedState<>(fh, heapLattice);
		final List<MethodCallAction<AVal, AHeap, AState>> actions = getMethodActions(h, embeddedHeap, baseValue, arguments, target);
		if(actions.isEmpty()) {
			return new PreCallInstrumentation<>(baseValue, arguments, Option.<IValue>none(), embeddedHeap);
		}
		final InPlaceMulti replace = new InPlaceMulti(embeddedHeap, h, baseValue, arguments, null);
		Option<AVal> replacement = valMonoid.zero();
		for(final MethodCallAction<AVal, AHeap, AState> act : actions) {
			replacement = valMonoid.sum(act.preCall(replace, op, target), replacement);
		}
		final Option<IValue> summary;
		if(!replacement.isNone()) {
			checkTypeOwnership(target);
			summary = Option.some(new IValue(new EmbeddedValue(valueMonad.lift(replacement.some()), valueMonad)));
		} else {
			summary = Option.none();
		}
		return new PreCallInstrumentation<>(replace.receiver, replace.args, summary, replace.foreignHeap);
	}

	private void checkTypeOwnership(final SootMethodRef target) {
		final Type retType = target.returnType();
		final TypeOwner owner = oracle.classifyType(retType);
		if(owner == TypeOwner.FRAMEWORK) {
			throw new UnsupportedOperationException();
		}
	}
	
	private class AbstractValueReplacement implements MultiValueReplacement<AVal, AHeap, AState> {
		public AbstractValueReplacement(final AVal receiver, final InstrumentedState state, final List<Object> argCopy) {
			this.receiverAccum = receiver;
			this.stateAccum = state;
			this.argCopy = argCopy;
		}
		public AbstractValueReplacement(final AVal receiver, final InstrumentedState state, final List<Object> argCopy, final Object returnValue) {
			this.receiverAccum = receiver;
			this.stateAccum = state;
			this.argCopy = argCopy;
			this.returnSlot = returnValue;
		}
		
		List<Object> argCopy;
		AVal receiverAccum;
		InstrumentedState stateAccum;
		Object returnSlot;
		
		@Override
		public void update(final int i, final ValueStateTransformer<AVal, AState> tr) {
			if(i == -1) {
				final Object o = valueMonad.lift(receiverAccum);
				stateAccum = stateMonad.updateValue(stateAccum, o, tr, new StateUpdater<AState>() {
					@SuppressWarnings("unchecked")
					@Override
					public AState updateState(final AState state, final Object value) {
						receiverAccum = (AVal) value;
						return state;
					}
				});
			} else if(i >= 0 && i < argCopy.size()) {
				stateAccum = stateMonad.updateValue(stateAccum, argCopy.get(i), tr, new StateUpdater<AState>() {
					@Override
					public AState updateState(final AState state, final Object value) {
						argCopy.set(i, value);
						return state;
					}
				});
			} else if(i == MultiValueReplacement.RETURN_SLOT && returnSlot != null) {
				stateAccum = stateMonad.updateValue(stateAccum, argCopy.get(i), tr, new StateUpdater<AState>() {
					@Override
					public AState updateState(final AState state, final Object value) {
						returnSlot = value;
						return state;
					}
				});
			} else {
				throw new IllegalArgumentException();
			}
		}
		
		@Override
		public MultiValueReader<AVal, AHeap, AState> getReader() {
			return new MultiValueReader<AVal, AHeap, AState>() {
				@Override
				public <R> R read(final int i, final ValueMapper<AVal, AState, R> map) {
					if(i == -1) {
						return stateMonad.mapValue(stateAccum, valueMonad.lift(receiverAccum), map);
					} else if(i >= 0 && i < argCopy.size()) {
						return stateMonad.mapValue(stateAccum, argCopy.get(i), map);
					} else if(i == MultiValueReplacement.RETURN_SLOT && returnSlot != null) {
						return stateMonad.mapValue(stateAccum, returnSlot, map);
					} else {
						throw new IllegalArgumentException();
					}
				}

				@Override public Object readRaw(final int i) {
					if(i == -1) {
						return valueMonad.lift(receiverAccum);
					} else if(i >= 0 && i < argCopy.size()) {
						return argCopy.get(i);
					} else if(i == MultiValueReplacement.RETURN_SLOT && returnSlot != null) {
						return returnSlot;
					} else {
						throw new IllegalArgumentException();
					}

				}

			};
		}
	};
	
	public P4<AVal, List<Object>, InstrumentedState, Option<Object>> preCallAI(final InstrumentedState state, final AVal receiver, final List<Object> args,
			final SootMethodRef target, final InvokeExpr op) {
		if(methodActions.isEmpty()) {
			return P.p(receiver, args, state, Option.none());
		}
		final List<Object> argCopy = new ArrayList<>(args);
		final List<MethodCallAction<AVal, AHeap, AState>> actions = getMethodActions(state, receiver, target, argCopy);
		
		final AbstractValueReplacement repl = new AbstractValueReplacement(receiver, state, argCopy);
		Option<AVal> summary = valMonoid.zero();
		for(final MethodCallAction<AVal, AHeap, AState> act : actions) {
			summary = valMonoid.sum(act.preCall(repl, op, target), summary);
		}
		final Option<Object> toReturn;
		if(!summary.isNone()) {
			checkTypeOwnership(target);
			toReturn = summary.map(new F<AVal, Object>() {
				@Override
				public Object f(final AVal a) {
					return valueMonad.lift(a);
				}
			});
		} else {
			toReturn = Option.none();
		}
		return P.p(repl.receiverAccum, argCopy, repl.stateAccum, toReturn);
	}
		
	public Pair<Object, InstrumentedState> postCallAI(final InstrumentedState state, final AVal receiver, final List<Object> args, final Object returnVal, final SootMethodRef target,
			final InvokeExpr op) {
		if(methodActions.isEmpty()) {
			return new Pair<Object, InstrumentedState>(returnVal, state);
		}
		final List<Object> argCopy = new ArrayList<>(args);
		final List<MethodCallAction<AVal, AHeap, AState>> actions = getMethodActions(state, receiver, target, argCopy);
		
		final AbstractValueReplacement repl = new AbstractValueReplacement(receiver, state, argCopy, returnVal);
		for(final MethodCallAction<AVal, AHeap, AState> act : actions) {
			act.preCall(repl, op, target);
		}
		return new Pair<>(repl.returnSlot, repl.stateAccum);
	}
	
	private List<MethodCallAction<AVal, AHeap, AState>> getMethodActions(final InstrumentedState state,
			final AVal receiver, final SootMethodRef target, final List<Object> argCopy) {
		final EmbeddedState<AHeap> embeddedHeap = stateMonad.map(state, new Function<AState, EmbeddedState<AHeap>>() {
			@Override
			public EmbeddedState<AHeap> apply(final AState in) {
				return new EmbeddedState<AHeap>(stateTransformer.project(in), heapLattice);
			}
		});
		final Heap h = this.projector.f(state).getO1();
		final MethodCall mc = new MethodCall(valueMonad.lift(receiver), argCopy, h, embeddedHeap, target);
		
		final List<MethodCallAction<AVal, AHeap, AState>> actions = new ArrayList<>();
		outer_loop: for(final FilterAndAction<MethodCall, MethodCallAction<AVal, AHeap, AState>> spec : methodActions) {
			for(final ValueFilter<MethodCall> f : spec.filters) {
				if(!f.test(mc)) {
					continue outer_loop;
				}
			}
			actions.add(spec.action);
		}
		return actions;
	}
	
	public P2<IValue, EmbeddedState<AHeap>> postCallCoop(final Heap h, final AHeap fh, final IValue baseValue, final List<IValue> arguments, final IValue returnValue,
			final SootMethodRef target, final InvokeExpr op) {
		final EmbeddedState<AHeap> embeddedFH = new EmbeddedState<>(fh, heapLattice);
		final List<MethodCallAction<AVal, AHeap, AState>> actions = getMethodActions(h, embeddedFH, baseValue, arguments, target);
		if(actions.isEmpty()) {
			return P.p(returnValue, embeddedFH);
		}
		final InPlaceMulti replace = new InPlaceMulti(embeddedFH, h, baseValue, arguments, returnValue);
		for(final MethodCallAction<AVal, AHeap, AState> act : actions) {
			act.postCall(replace, op, target);
		}
		return P.p(replace.returnSlot, replace.foreignHeap);
	}

	private List<MethodCallAction<AVal, AHeap, AState>> getMethodActions(final Heap h, final EmbeddedState<AHeap> fh, final IValue baseValue, final List<IValue> arguments,
			final SootMethodRef target) {
		final MethodCall mc = new MethodCall(baseValue, arguments, h, fh, target, valueMonad);
		final List<MethodCallAction<AVal, AHeap, AState>> actions = new ArrayList<>();
		outer_loop: for(final FilterAndAction<MethodCall, MethodCallAction<AVal, AHeap, AState>> spec : this.methodActions) {
			for(final ValueFilter<MethodCall> f : spec.filters) {
				if(!f.test(mc)) {
					continue outer_loop;
				}
			}
			actions.add(spec.action);
		}
		return actions;
	}

	
	public WrappedPredicate wrap(final ValueMapper<AVal, AState, Boolean> pred) {
		return new WrappedPredicate() {
			@Override
			@SuppressWarnings("unchecked")
			public boolean accept(final Object v, final Heap h, final EmbeddedState<?> fh) {
				return stateMonad.mapValue(getState((EmbeddedState<AHeap>) fh, h), v, pred);
			}
		};
	}

	private <T> IValue filterFieldOutput(final ExecutionState<AHeap, ?> es, final IValue baseValue, final HeapLocation ref,
			final List<FilterAndAction<HeapAccess,T>> actionFilter, final IValue outputValue,
			final Effect2<T, ValueReplacement<AVal, AHeap, AState>> apply) {
		final HeapAccess toTest = new HeapAccess(baseValue, ref, es.heap, es.foreignHeap, valueMonad);
		final List<T> action = new ArrayList<>();
		outer_search: for(final FilterAndAction<HeapAccess, T> spec : actionFilter) {
			for(final ValueFilter<HeapAccess> f : spec.filters) {
				if(!f.test(toTest)) {
					continue outer_search;
				}
			}
			action.add(spec.action);
		}
		final InPlaceReplace replacement = this.getValueReplacement(outputValue, es.heap, es.foreignHeap);
		for(final T act : action) {
			apply.f(act, replacement);
		}
		es.replaceHeap(replacement.foreignHeap);
		return replacement.newValue;
	}
	
	private ValueReader<AVal, AHeap, AState> getValueReader(final IValue baseValue, final Heap heap, final EmbeddedState<AHeap> foreignHeap) {
		return new ValueReader<AVal, AHeap, AState>() {
			@Override
			public <R> R read(final ValueMapper<AVal, AState, R> mapper) {
				final InstrumentedState state = getState(foreignHeap, heap);
				return stateMonad.mapValue(state, valueMonad.lift(baseValue), mapper);
			}
		};
	}
	
	private InPlaceReplace getValueReplacement(final IValue baseValue, final Heap heap, final EmbeddedState<AHeap> foreignHeap) {
		return new InPlaceReplace(foreignHeap, heap, baseValue);
	}
}
