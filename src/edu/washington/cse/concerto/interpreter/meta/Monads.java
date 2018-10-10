package edu.washington.cse.concerto.interpreter.meta;

import edu.washington.cse.concerto.Function;
import edu.washington.cse.concerto.interpreter.ai.AbstractInterpretation;
import edu.washington.cse.concerto.interpreter.ai.AbstractionFunction;
import edu.washington.cse.concerto.interpreter.ai.EvalResult;
import edu.washington.cse.concerto.interpreter.ai.HeapMutator;
import edu.washington.cse.concerto.interpreter.ai.HeapReader;
import edu.washington.cse.concerto.interpreter.ai.HeapUpdateResult;
import edu.washington.cse.concerto.interpreter.ai.Lattices;
import edu.washington.cse.concerto.interpreter.ai.MapMonoid;
import edu.washington.cse.concerto.interpreter.ai.Merger;
import edu.washington.cse.concerto.interpreter.ai.MethodResult;
import edu.washington.cse.concerto.interpreter.ai.MethodResultMonad;
import edu.washington.cse.concerto.interpreter.ai.MonoidalValueMapper;
import edu.washington.cse.concerto.interpreter.ai.RecursiveStateTransformer;
import edu.washington.cse.concerto.interpreter.ai.RecursiveTransformer;
import edu.washington.cse.concerto.interpreter.ai.State;
import edu.washington.cse.concerto.interpreter.ai.StateMonad;
import edu.washington.cse.concerto.interpreter.ai.StateUpdater;
import edu.washington.cse.concerto.interpreter.ai.UpdateResult;
import edu.washington.cse.concerto.interpreter.ai.ValueMapper;
import edu.washington.cse.concerto.interpreter.ai.ValueMonad;
import edu.washington.cse.concerto.interpreter.ai.ValueStateTransformer;
import edu.washington.cse.concerto.interpreter.heap.Heap;
import edu.washington.cse.concerto.interpreter.heap.HeapAccessResult;
import edu.washington.cse.concerto.interpreter.heap.HeapFieldAction;
import edu.washington.cse.concerto.interpreter.heap.HeapObject;
import edu.washington.cse.concerto.interpreter.heap.HeapReadResult;
import edu.washington.cse.concerto.interpreter.heap.Location;
import edu.washington.cse.concerto.interpreter.lattice.Lattice;
import edu.washington.cse.concerto.interpreter.value.EmbeddedValue;
import edu.washington.cse.concerto.interpreter.value.IValue;
import edu.washington.cse.concerto.interpreter.value.IValueAction;
import edu.washington.cse.concerto.interpreter.value.ValueMerger;
import fj.F2;
import fj.data.Stream;
import soot.SootFieldRef;
import soot.toolkits.scalar.Pair;

import java.util.HashSet;

public class Monads<AVal, AS> {
	public final ValueMonad<AVal> valueMonad;
	public final StateMonad<AS, AVal> stateMonad;
	public final MethodResultMonad<AVal, AS> methodResultMonad;
	
	public Monads(final ValueMonad<AVal> vm, final StateMonad<AS, AVal> stateMonad, final MethodResultMonad<AVal, AS> mrm) {
		this.valueMonad = vm;
		this.stateMonad = stateMonad;
		this.methodResultMonad = mrm;
	}
	
	@SuppressWarnings("unchecked")
	private static <AS, AHeap, AVal> ValueMonad<AVal> makeValueMonad(final AbstractInterpretation<AVal, AHeap, AS, ?> ai) {
		final Lattices<AVal, ?, AS> lattices = ai.lattices();
		final AbstractionFunction<AVal> alpha = ai.alpha();
		final Lattice<AVal> valueLattice = lattices.valueLattice();
		final ValueMonad<AVal> valueMonad = new ValueMonad<AVal>() {
			@Override
			public Object join(final Object o1, final Object o2) {
				assert isNotEmbeddedIValue(o1) && isNotEmbeddedIValue(o2);
				if(o1 instanceof CombinedValue) {
					final CombinedValue combinedValue = (CombinedValue) o1;
					return joinCombined(combinedValue, o2, valueLattice);
				} else if(o2 instanceof CombinedValue) {
					return joinCombined((CombinedValue) o2, o1, valueLattice);
				} else if(o1 instanceof IValue) {
					final IValue v1 = (IValue) o1;
					return joinConcreteValue(valueLattice, v1, o2, alpha);
				} else if(o2 instanceof IValue) {
					return joinConcreteValue(valueLattice, (IValue) o2, o1, alpha);
				} else {
					final AVal a1 = (AVal) o1;
					final AVal a2 = (AVal) o2;
					return valueLattice.join(a1, a2);
				}
			}
	
			public Object joinConcreteValue(final Lattice<AVal> valueLattice, final IValue v1, final Object o2, final AbstractionFunction<AVal> alpha) {
				assert !v1.isEmbedded();
				if(o2 instanceof IValue) {
					return ValueMerger.STRICT_MERGE.merge(v1, (IValue)o2);
				} else {
					if(Monads.isCombinable(v1)) {
						return new CombinedValue(v1, o2);
					} else {
						final AVal val = alpha.lift(v1);
						return valueLattice.join(val, (AVal) o2);
					}
				}
			}
	
			private Object joinCombined(final CombinedValue c1, final Object o2, final Lattice<AVal> valueLattice) {
				if(o2 instanceof CombinedValue) {
					final CombinedValue c2 = (CombinedValue) o2;
					final AVal joinedAbs = valueLattice.join((AVal)c1.abstractComponent, (AVal)c2.abstractComponent);
					final IValue joinedConcrete = ValueMerger.STRICT_MERGE.merge(c1.concreteComponent, c2.concreteComponent);
					return new CombinedValue(joinedConcrete, joinedAbs);
				} else if(o2 instanceof IValue) {
					final IValue joined = ValueMerger.STRICT_MERGE.merge(c1.concreteComponent, (IValue) o2);
					if(joined.equals(c1.concreteComponent)) {
						return c1;
					}
					return new CombinedValue(joined, c1.abstractComponent);
				} else {
					final AVal joinedAbs = valueLattice.join((AVal) c1.abstractComponent, (AVal)o2);
					if(valueLattice.lessEqual(joinedAbs, (AVal) c1.abstractComponent) && valueLattice.lessEqual((AVal) c1.abstractComponent, joinedAbs)) {
						return c1;
					}
					return new CombinedValue(c1.concreteComponent, joinedAbs);
				}
			}
	
			@Override
			public Object widen(final Object prev, final Object next) {
				assert isNotEmbeddedIValue(prev) && isNotEmbeddedIValue(next);
				if(prev instanceof CombinedValue) {
					final CombinedValue cPrev = (CombinedValue) prev;
					final AVal prevAbs = (AVal)cPrev.abstractComponent;
					if(next instanceof CombinedValue) {
						final CombinedValue cNext = (CombinedValue) next;
						return new CombinedValue(
							ValueMerger.WIDENING_MERGE.merge(cPrev.concreteComponent, cNext.concreteComponent),
							valueLattice.widen(prevAbs, (AVal)cNext.abstractComponent));
					} else if(next instanceof IValue) {
						final IValue w = ValueMerger.WIDENING_MERGE.merge(cPrev.concreteComponent, (IValue) next);
						if(cPrev.concreteComponent.equals(w)) {
							return cPrev;
						}
						return new CombinedValue(w, cPrev.abstractComponent);
					} else {
						final AVal widened = valueLattice.widen(prevAbs, (AVal)next);
						if(valueLattice.lessEqual(widened, prevAbs) && valueLattice.lessEqual(prevAbs, widened)) {
							return cPrev;
						}
						return new CombinedValue(cPrev.concreteComponent, widened);
					}
				} else if(next instanceof CombinedValue) {
					final CombinedValue cNext = (CombinedValue) next;
					if(prev instanceof IValue) {
						final IValue w = ValueMerger.WIDENING_MERGE.merge((IValue) prev, cNext.concreteComponent);
						if(w.equals(cNext.concreteComponent)) {
							return cNext;
						}
						return new CombinedValue(w, cNext.abstractComponent);
					} else {
						final AVal aPrev = (AVal) prev;
						final AVal aNext = (AVal) cNext.abstractComponent;
						final AVal widened = valueLattice.widen(aPrev, aNext);
						if(valueLattice.lessEqual(widened, aNext) && valueLattice.lessEqual(aNext, widened)) {
							return cNext;
						}
						return new CombinedValue(cNext.concreteComponent, widened);
					}
				} else if(prev instanceof IValue) {
					final IValue cPrev = (IValue)prev;
					if(next instanceof IValue) {
						return ValueMerger.WIDENING_MERGE.merge(cPrev, (IValue) next);
					} else if(Monads.isCombinable(cPrev)) {
						return new CombinedValue(cPrev, next);
					} else {
						final AVal aPrev = alpha.lift(cPrev);
						return valueLattice.widen(aPrev, (AVal) next);
					}
				} else if(next instanceof IValue) {
					final IValue cNext = (IValue)next;
					if(prev instanceof IValue) {
						return ValueMerger.WIDENING_MERGE.merge((IValue) prev, cNext);
					} else if(Monads.isCombinable(cNext)) {
						return new CombinedValue(cNext, prev);
					} else {
						final AVal aNext = alpha.lift(cNext);
						return valueLattice.widen((AVal) prev, aNext);
					}
				} else {
					final AVal aPrev = (AVal) prev;
					final AVal aNext = (AVal) next;
					return valueLattice.widen(aPrev, aNext);
				}
			}
	
			@Override
			public boolean lessEqual(final Object o1, final Object o2) {
				assert isNotEmbeddedIValue(o1) && isNotEmbeddedIValue(o2);
				if(o1 instanceof CombinedValue) {
					final CombinedValue c1 = (CombinedValue) o1;
					if(!(o2 instanceof CombinedValue)) {
						return false;
					}
					final CombinedValue c2 = (CombinedValue) o2;
					return c1.concreteComponent.lessEqual(c2.concreteComponent) &&
						valueLattice.lessEqual((AVal)c1.abstractComponent, (AVal)c2.abstractComponent);
				} else if(o2 instanceof CombinedValue) {
					if(o1 instanceof IValue) {
						return ((IValue) o1).lessEqual(((CombinedValue) o2).concreteComponent);
					} else {
						final AVal a1 = (AVal) o1;
						return valueLattice.lessEqual(a1, (AVal) ((CombinedValue) o2).abstractComponent);
					}
				} else if(o2 instanceof IValue) {
					if(o1 instanceof IValue) {
						return ((IValue) o1).lessEqual((IValue) o2); 
					} else {
						return false;
					}
				} else {
					if(o1 instanceof IValue) {
						final IValue v1 = (IValue) o1;
						if(Monads.isCombinable(v1)) {
							return false;
						} else {
							return valueLattice.lessEqual(alpha.lift(v1), (AVal)o2);
						}
					} else {
						return valueLattice.lessEqual((AVal) o1, (AVal) o2);
					}
				}
			}

			@Override
			public Object lift(final AVal toLift) {
				return toLift;
			}

			@Override
			public String toString(final Object a) {
				if(a instanceof CombinedValue) {
					final CombinedValue cVal = (CombinedValue) a;
					return "[C: " + cVal.concreteComponent + " & A: " + cVal.abstractComponent + "]";
				} else {
					return a.toString();
				}
			}

			@Override
			public AVal alpha(final Object o) {
				if(o instanceof CombinedValue || (o instanceof IValue && Monads.isCombinable((IValue) o))) {
					throw new UnsupportedOperationException();
				} else if(o instanceof IValue) {
					return alpha.lift((IValue) o);
				} else {
					return (AVal) o;
				}
			}

			@Override
			public Object lift(final IValue toLift) {
				if(toLift.isEmbedded() && toLift.aVal.monad == this) {
					return toLift.aVal.value;
				}
				return toLift;
			}
		};
		return valueMonad;
	}

	private static boolean isNotEmbeddedIValue(final Object o) {
		if(!(o instanceof IValue)) {
			return true;
		}
		final IValue iv = (IValue) o;
		return iv.getTag() != IValue.RuntimeTag.EMBEDDED;
	}
	
	protected static class InstrumentedStateImpl<AS> extends InstrumentedState {
		public final Heap concreteHeap;
		public final AS state;

		public InstrumentedStateImpl(final AS state, final Heap concreteHeap) {
			super(PermissionToken.TokenManager.tok);
			this.state = state;
			this.concreteHeap = concreteHeap;
		}
		
		@Override
		public String toString() {
			return "[CONCRETE: " + this.concreteHeap.toStringFull() + "||ABS: " + this.state + "]";
		}
	}
	
	protected static class PlainStateImpl<AS> extends InstrumentedState {
		public final AS state;

		public PlainStateImpl(final AS state) {
			super(PermissionToken.TokenManager.tok);
			this.state = state;
		}
		
		@Override
		public String toString() {
			return this.state.toString();
		}
	}

	@SuppressWarnings("unchecked")
	private static <AVal, AHeap, AS> StateMonad<AS, AVal> makeStateMonad(final AbstractInterpretation<AVal, AHeap, AS, ?> ai, final ValueMonad<AVal> vMonad) {
		final Lattice<AS> stateLattice = ai.lattices().stateLattice();
		return new StateMonad<AS, AVal>() {
			private final HeapMutator mutator = new HeapMutator() {
				@Override
				public <AState> HeapUpdateResult<AState> updateAtIndex(final AState s, final Heap h, final IValue base, final IValue indexValue, final Object toSet) {
					final IValue concretized = concretize(toSet);
					return updateArray(s, h.fork(), base, indexValue, concretized);
				}

				@Override
				public <AState> HeapUpdateResult<AState> updateNondetIndex(final AState s, final Heap h, final IValue base, final Object toSet) {
					final Heap toMutate = h.fork();
					final IValue concretized = concretize(toSet);
					final IValue index = IValue.nondet();
					return updateArray(s, toMutate, base, index, concretized);
				}

				private <AState> HeapUpdateResult<AState> updateArray(final AState s, final Heap toMutate, final IValue base, final IValue index, final IValue toSet) {
					final HeapAccessResult stat = toMutate.putArray(base, index, toSet);
					return new HeapUpdateResult<AState>(s, toMutate.popHeap(), base, stat);
				}

				public IValue concretize(final Object toSet) {
					final IValue concretized;
					if(toSet instanceof IValue) {
						concretized = (IValue) toSet;
					} else {
						concretized = new IValue(new EmbeddedValue(toSet, vMonad));
					}
					return concretized;
				}

				@Override
				public <AState> HeapUpdateResult<AState> updateField(final AState s, final Heap h, final IValue b, final SootFieldRef field, final Object toSet) {
					final IValue fieldValue = concretize(toSet);
					final Heap toMutate = h.fork();
					final HeapAccessResult status = toMutate.putField(b, field, fieldValue);
					return new HeapUpdateResult<AState>(s, toMutate.popHeap(), b, status);
				}
			};
			
			private final HeapReader<AS, AVal> access = new HeapReader<AS, AVal>() {
				@Override
				public HeapReadResult<Object> readNondetIndex(final Heap h, final IValue base) {
					final IValue index = IValue.nondet();
					return doArrayRead(h, base, index);
				}

				private HeapReadResult<Object> doArrayRead(final Heap h, final IValue base, final IValue index) {
					final HeapReadResult<IValue> read = h.getArray(base, index);
					return liftToInstrumented(read);
				}

				private Object abstraction(final IValue lifted) {
					if(lifted.isEmbedded()) {
						return lifted.aVal.value;
					} else {
						return vMonad.lift(lifted);
					}
				}

				@Override
				public HeapReadResult<Object> readIndex(final Heap h, final IValue base, final IValue indexValue) {
					return doArrayRead(h, base, indexValue);
				}
				
				@Override
				public HeapReadResult<Object> readField(final Heap h, final IValue b, final SootFieldRef field) {
					final HeapReadResult<IValue> val = h.getField(b, field);
					return liftToInstrumented(val);
				}

				private HeapReadResult<Object> liftToInstrumented(final HeapReadResult<IValue> val) {
					if(val.value == null) {
						return (HeapReadResult<Object>)(Object)val;
					}
					return new HeapReadResult<Object>(abstraction(val.value), val);
				}

				@Override
				public <Ret> Ret forEachField(final IValue v, final AS state, final Heap h, final RecursiveTransformer<AS, Ret> recursor, final Merger<Ret> m) {
					if(m instanceof MapMonoid) {
						final MapMonoid<Ret> mMonoid = (MapMonoid<Ret>) m;
						assert v.isMultiHeap() || v.isHeapValue();
						final Ret[] accum = (Ret[]) new Object[]{mMonoid.zero()};
						v.forEach(new IValueAction() {
							@Override
							public void nondet() { }
							
							@Override
							public void accept(final IValue base, final boolean isMulti) {
								final Location l = base.getLocation();
								final HeapObject obj = h.findObject(l);
								obj.forEachField(new HeapFieldAction() {
									@Override
									public void accept(final String fieldName, final IValue value) {
										accum[0] = m.merge(accum[0], recursor.mapValue(state, h, vMonad.lift(value)));
									}
								});
							}
						});
						return accum[0];
					} else {
						return v.valueStream().bind(iv -> {
							if(!iv.isHeapValue()) {
								return Stream.nil();
							} else {
								return h.findObject(iv.getLocation()).fieldValueStream().map(fVal ->
									recursor.mapValue(state, h, vMonad.lift(fVal))
								);
							}
						}).foldLeft1(m::merge);
					}
				}
			};
			
			@Override
			public final InstrumentedState join(final InstrumentedState o1, final InstrumentedState o2) {
				final InstrumentedStateImpl<AS> s1 = (InstrumentedStateImpl<AS>) o1;
				final InstrumentedStateImpl<AS> s2 = (InstrumentedStateImpl<AS>) o2;
				final InstrumentedStateImpl<AS> toReturn = new InstrumentedStateImpl<>(stateLattice.join(s1.state, s2.state), Heap.fullJoin(s1.concreteHeap, s2.concreteHeap));
				return toReturn;
			}
			
			@Override
			public InstrumentedState lift(final AS state, final Heap h) {
				return new InstrumentedStateImpl<>(state, h);
			}

			@Override
			public InstrumentedState widen(final InstrumentedState prev, final InstrumentedState next) {
				final InstrumentedStateImpl<AS> prev_ = (InstrumentedStateImpl<AS>) prev;
				final InstrumentedStateImpl<AS> next_ = (InstrumentedStateImpl<AS>) next;
				return new InstrumentedStateImpl<>(
						stateLattice.widen(prev_.state, next_.state),
						Heap.fullWiden(prev_.concreteHeap,  next_.concreteHeap)
				);
			}

			@Override
			public boolean lessEqual(final InstrumentedState o1, final InstrumentedState o2) {
				final InstrumentedStateImpl<AS> s1 = (InstrumentedStateImpl<AS>) o1;
				final InstrumentedStateImpl<AS> s2 = (InstrumentedStateImpl<AS>) o2;
				return stateLattice.lessEqual(s1.state, s2.state) && s1.concreteHeap.lessEqualFull(s2.concreteHeap);
			}

			@Override
			public <R> R map(final InstrumentedState state, final Function<AS, R> mapper) {
				final InstrumentedStateImpl<AS> state_ = (InstrumentedStateImpl<AS>) state;
				return mapper.apply(state_.state);
			}

			@Override
			public <R> R mapValue(final InstrumentedState state, final Object value, final ValueMapper<AVal, AS, R> mapper) {
				final InstrumentedStateImpl<AS> state_ = (InstrumentedStateImpl<AS>) state;
				final R zeroValue;
				final HashSet<Object> visited;
				if(mapper instanceof MonoidalValueMapper) {
					visited = new HashSet<>();
					zeroValue = ((MonoidalValueMapper<?, ?, R>) mapper).zero();
				} else {
					visited = null;
					zeroValue = null;
				}
				final RecursiveTransformer<AS, R> recursor = new RecursiveTransformer<AS, R>() {
					@Override
					public R mapValue(final AS state, final Heap h, final Object o) {
						if(visited != null && !visited.add(o)) {
							return zeroValue;
						}
						if(o instanceof IValue && isCombinable((IValue) o)) {
							return mapper.mapConcrete((IValue) o, state, h, access, this);
						} else if(o instanceof IValue) {
							final IValue concrete = (IValue) o;
							if(concrete.isEmbedded()) {
								return this.mapValue(state, h, concrete.aVal.value);
							} else {
								return mapper.mapAbstract(vMonad.alpha(concrete), state, h, this);
							}
						} else if(o instanceof CombinedValue) {
							final CombinedValue cVal = (CombinedValue) o;
							return mapper.merge(
								mapper.mapAbstract((AVal) cVal.abstractComponent, state, h, this),
								mapper.mapConcrete(cVal.concreteComponent, state, h, access, this)
							);
						} else {
							return mapper.mapAbstract((AVal) o, state, h, this);
						}
					}
				};
				return recursor.mapValue(state_.state, state_.concreteHeap, value);
			}
			
			@Override
			public InstrumentedState updateValue(final InstrumentedState state, final Object value, final ValueStateTransformer<AVal, AS> mapper, final StateUpdater<AS> updater) {
				final InstrumentedStateImpl<AS> state_ = (InstrumentedStateImpl<AS>) state;
				
				final HashSet<Object> visited = new HashSet<>();
				final RecursiveStateTransformer<AS> recursive = new RecursiveStateTransformer<AS>() {
					@Override
					public UpdateResult<AS, ?> mapValue(final AS state, final Heap h, final Object value) {
						if(!visited.add(value)) {
							return new UpdateResult<AS, Object>(state, h, value);
						}
						if(value instanceof IValue && isCombinable((IValue) value)) {
							final IValue concrete = (IValue) value;
							assert !concrete.isEmbedded();
							return mapper.mapConcrete(concrete, state, h, mutator, this);
						} else if(value instanceof IValue) {
							final IValue concrete = (IValue) value;
							if(concrete.isEmbedded()) {
								return this.mapValue(state, h, concrete.aVal.value);
							} else {
								return mapper.mapAbstract(vMonad.alpha(concrete), state, h, this);
							}
						} else if(value instanceof CombinedValue) {
							final CombinedValue combined = (CombinedValue) value;
							final UpdateResult<AS, AVal> newAbs = mapper.mapAbstract((AVal) combined.abstractComponent, state, h, this);
							final UpdateResult<AS, IValue> newConcrete = mapper.mapConcrete(combined.concreteComponent, newAbs.state, newAbs.heap, mutator, this);
							return new UpdateResult<>(newConcrete.state, newConcrete.heap, new CombinedValue(newConcrete.value, newAbs.value));
						} else {
							return mapper.mapAbstract((AVal) value, state, h, this);
						}
					}
				};
				final UpdateResult<AS, ?> subUpdated = recursive.mapValue(state_.state, state_.concreteHeap, value);
				final AS updated = updater.updateState(subUpdated.state, subUpdated.value);
				return new InstrumentedStateImpl<>(updated, subUpdated.heap);
			}

			@Override
			public InstrumentedState mapState(final InstrumentedState state, final Function<AS, AS> mapper) {
				final InstrumentedStateImpl<AS> state_ = (InstrumentedStateImpl<AS>) state;
				return new InstrumentedStateImpl<AS>(mapper.apply(state_.state), state_.concreteHeap);
			}

			@Override
			public <V> InstrumentedState iterateState(final InstrumentedState startState, final Iterable<V> iter, final F2<InstrumentedState, V, InstrumentedState> mapper) {
				InstrumentedState accum = startState;
				for(final V item : iter) { 
					accum = mapper.f(accum, item);
				}
				return accum;
			}

			@Override
			public String toString(final InstrumentedState newState) {
				return newState.toString();
			}

			@Override
			public EvalResult mapToResult(final InstrumentedState state, final Function<AS, Pair<AS, Object>> result) {
				final InstrumentedStateImpl<AS> state_ = (InstrumentedStateImpl<AS>) state;
				final Pair<AS, Object> updated = result.apply(state_.state);
				return new EvalResult(new InstrumentedStateImpl<AS>(updated.getO1(), state_.concreteHeap), updated.getO2());
			}

			@Override
			public InstrumentedState mapInHeaps(final InstrumentedState srcState, final InstrumentedState destState) {
				final InstrumentedStateImpl<AS> srcState_ = (InstrumentedStateImpl<AS>) srcState;
				final InstrumentedStateImpl<AS> destState_ = (InstrumentedStateImpl<AS>) destState;
				
				final State<AHeap, AS> stateTr = ai.stateTransformer();
				final AS updatedAbsState = stateTr.inject(destState_.state, stateTr.project(srcState_.state));
				return new InstrumentedStateImpl<>(updatedAbsState, srcState_.concreteHeap);
			}
		};
	}
	
	@SuppressWarnings("unchecked")
	public static <Value> ValueMonad<Value> unsafeLift(final Lattice<Value> l) {
		return new ValueMonad<Value>() {

			@Override
			public Object join(final Object o1, final Object o2) {
				return l.join((Value)o1, (Value)o2);
			}

			@Override
			public Object widen(final Object prev, final Object next) {
				return l.widen((Value)prev, (Value)next);
			}

			@Override
			public boolean lessEqual(final Object o1, final Object o2) {
				return l.lessEqual((Value)o1, (Value)o2);
			}
			
			@Override
			public Value alpha(final Object o) {
				return (Value)o;
			}

			@Override
			public Object lift(final Value toLift) {
				return toLift;
			}
			
			@Override
			public String toString(final Object a) {
				return a.toString();
			}

			@Override
			public Object lift(final IValue downCast) {
				throw new UnsupportedOperationException();
			}
		};
	}
	
	private static <AVal, AS> MethodResultMonad<AVal, AS> makeMethodResultMonad(final ValueMonad<AVal> val, final StateMonad<AS, AVal> state) {
		return new MethodResultMonad<AVal, AS>() {
			@Override
			public MethodResult join(final MethodResult o1, final MethodResult o2) {
				assert (o1.getReturnValue() == null) == (o2.getReturnValue() == null);
				final Object newVal;
				if(o1.getReturnValue() != null) {
					newVal = val.join(o1.getReturnValue(), o2.getReturnValue());
				} else {
					newVal = null;
				}
				final InstrumentedState newState = state.join(o1.getState(), o2.getState());
				return new MethodResult(newState, newVal);
			}

			@Override
			public MethodResult widen(final MethodResult prev, final MethodResult next) {
				assert (prev.getReturnValue() == null) == (next.getReturnValue() == null);
				final Object newVal;
				if(prev.getReturnValue() != null) {
					newVal = val.widen(prev.getReturnValue(), next.getReturnValue());
				} else {
					newVal = null;
				}
				final InstrumentedState newState = state.widen(prev.getState(), next.getState());
				return new MethodResult(newState, newVal);
			}

			@Override
			public boolean lessEqual(final MethodResult o1, final MethodResult o2) {
				assert (o1.getReturnValue() == null) == (o2.getReturnValue() == null);
				if(o1.getReturnValue() != null) {
					if(!val.lessEqual(o1.getReturnValue(), o2.getReturnValue())) {
						return false;
					}
				}
				return state.lessEqual(o1.getState(), o2.getState());
			}
		};
	}
	
	public static <AVal, AS> Monads<AVal, AS> makeMonads(final AbstractInterpretation<AVal, ?, AS, ?> ai) {
		final ValueMonad<AVal> valueMonad = makeValueMonad(ai);
		final StateMonad<AS, AVal> stateMonad = makeStateMonad(ai, valueMonad);
		final MethodResultMonad<AVal, AS> resultMonad = makeMethodResultMonad(valueMonad, stateMonad);
		return new Monads<>(valueMonad, stateMonad, resultMonad);
	}

	public static boolean isCombinable(final IValue cNext) {
		return cNext.isHeapValue() || cNext.isMultiHeap();
	}

	public static <AVal, AS> Monads<AVal, AS> makeNullMonads(final AbstractInterpretation<AVal, ?, AS, ?> ai) {
		final Lattices<AVal, ?, AS> lattices = ai.lattices();
		final ValueMonad<AVal> vm = new ValueMonad<AVal>() {

			@SuppressWarnings("unchecked")
			@Override
			public Object widen(final Object prev, final Object next) {
				return lattices.valueLattice().widen((AVal)prev, (AVal)next);
			}

			@SuppressWarnings("unchecked")
			@Override
			public Object join(final Object first, final Object second) {
				return lattices.valueLattice().join((AVal)first, (AVal)second);
			}

			@SuppressWarnings("unchecked")
			@Override
			public boolean lessEqual(final Object first, final Object second) {
				return lattices.valueLattice().lessEqual((AVal)first, (AVal)second);
			}

			@SuppressWarnings("unchecked")
			@Override
			public AVal alpha(final Object o) {
				return (AVal) o;
			}

			@Override
			public Object lift(final AVal toLift) {
				return toLift;
			}

			@Override
			public Object lift(final IValue downCast) {
				throw new UnsupportedOperationException();
			}

			@Override
			public String toString(final Object a) {
				return a.toString();
			}
		};
		final StateMonad<AS, AVal> stateMonad = new StateMonad<AS, AVal>() {

			private final Lattice<AS> stateLattice = lattices.stateLattice();

			@Override
			public InstrumentedState join(final InstrumentedState o1, final InstrumentedState o2) {
				return wrap(stateLattice.join(this.unwrap(o1), this.unwrap(o2)));
			}

			@Override
			public InstrumentedState widen(final InstrumentedState prev, final InstrumentedState next) {
				return wrap(stateLattice.widen(unwrap(prev), unwrap(next)));
			}

			@Override
			public boolean lessEqual(final InstrumentedState o1, final InstrumentedState o2) {
				return stateLattice.lessEqual(unwrap(o1), unwrap(o2));
			}

			@Override
			public InstrumentedState lift(final AS state, final Heap h) {
				throw new UnsupportedOperationException();
			}

			@Override
			public <R> R map(final InstrumentedState state, final Function<AS, R> mapper) {
				return mapper.apply(this.unwrap(state));
			}
			
			@SuppressWarnings("unchecked")
			private AS unwrap(final InstrumentedState state) {
				return ((PlainStateImpl<AS>)state).state;
			}

			@Override
			public InstrumentedState mapState(final InstrumentedState state, final Function<AS, AS> mapper) {
				return wrap(mapper.apply(this.unwrap(state)));
			}

			private PlainStateImpl<AS> wrap(final AS toReturn) {
				return new PlainStateImpl<>(toReturn);
			}

			@Override
			public <R> R mapValue(final InstrumentedState state, final Object value, final ValueMapper<AVal, AS, R> mapper) {
				final R zeroValue;
				final HashSet<Object> visited;
				if(mapper instanceof MonoidalValueMapper) {
					visited = new HashSet<>();
					zeroValue = ((MonoidalValueMapper<?, ?, R>) mapper).zero();
				} else {
					visited = null;
					zeroValue = null;
				}
				final RecursiveTransformer<AS, R> recursor = new RecursiveTransformer<AS, R>() {
					@SuppressWarnings("unchecked")
					@Override
					public R mapValue(final AS state, final Heap h, final Object o) {
						if(visited != null && !visited.add(o)) {
							return zeroValue;
						}
						return mapper.mapAbstract((AVal) o, state, h, this);
					}
				};
				return recursor.mapValue(unwrap(state), null, value);
			}

			@Override
			public InstrumentedState updateValue(final InstrumentedState state, final Object value, final ValueStateTransformer<AVal, AS> mapper, final StateUpdater<AS> updater) {
				final HashSet<Object> visited = new HashSet<>();
				final RecursiveStateTransformer<AS> recursive = new RecursiveStateTransformer<AS>() {
					@SuppressWarnings("unchecked")
					@Override
					public UpdateResult<AS, ?> mapValue(final AS state, final Heap h, final Object value) {
						if(!visited.add(value)) {
							return new UpdateResult<AS, Object>(state, h, value);
						}
						assert h == null;
						return mapper.mapAbstract((AVal) value, state, null, this);
					}
				};
				final UpdateResult<AS, ?> subUpdated = recursive.mapValue(unwrap(state), null, value);
				final AS updated = updater.updateState(subUpdated.state, subUpdated.value);
				assert subUpdated.heap == null;
				return wrap(updated);
			}

			@Override
			public String toString(final InstrumentedState newState) {
				return newState.toString();
			}

			@Override
			public <V> InstrumentedState iterateState(final InstrumentedState startState, final Iterable<V> iter, final F2<InstrumentedState, V, InstrumentedState> mapper) {
				InstrumentedState accum = startState;
				for(final V item : iter) { 
					accum = mapper.f(accum, item);
				}
				return accum;
			}

			@Override
			public EvalResult mapToResult(final InstrumentedState state, final Function<AS, Pair<AS, Object>> result) {
				final Pair<AS, Object> updated = result.apply(unwrap(state));
				return new EvalResult(new PlainStateImpl<AS>(updated.getO1()), updated.getO2());
			}

			@Override
			public InstrumentedState mapInHeaps(final InstrumentedState srcState, final InstrumentedState destState) {
				return mapHeapImpl(ai.stateTransformer(), srcState, destState);
			}
			
			private <HType> InstrumentedState mapHeapImpl(final State<HType, AS> stateTr,
					final InstrumentedState srcState, final InstrumentedState destState) {
				final AS updatedAbsState = stateTr.inject(unwrap(destState), stateTr.project(unwrap(srcState)));
				return wrap(updatedAbsState);
			}
		};
		final MethodResultMonad<AVal, AS> mrm = makeMethodResultMonad(vm, stateMonad);
		return new Monads<>(vm, stateMonad, mrm);
	}
}
