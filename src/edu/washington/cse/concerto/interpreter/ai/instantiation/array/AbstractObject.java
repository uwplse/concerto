package edu.washington.cse.concerto.interpreter.ai.instantiation.array;

import edu.washington.cse.concerto.interpreter.ai.MappedValueLatticeHelper;
import edu.washington.cse.concerto.interpreter.ai.MonadicLattice;
import edu.washington.cse.concerto.interpreter.ai.ValueMonad;
import edu.washington.cse.concerto.interpreter.ai.injection.NeedsMonads;
import edu.washington.cse.concerto.interpreter.meta.Monads;
import fj.F;
import fj.F2;
import fj.Ord;
import fj.P2;
import fj.data.Option;
import fj.data.Seq;
import fj.data.Stream;
import fj.data.TreeMap;
import soot.RefLikeType;
import soot.Scene;
import soot.Type;

public class AbstractObject {
	private static ValueMonad<PValue> vMonad;
	public static final NeedsMonads<PValue, ARState> injector = new NeedsMonads<PValue, ARState>() {
		@Override
		public void inject(final Monads<PValue, ARState> monads) {
			vMonad = monads.valueMonad;
		}
	};
	
	private static final F2<Object, Object, Object> pairwiseJoin = new F2<Object, Object, Object>() {
		@Override
		public Object f(final Object a, final Object b) {
			return vMonad.join(a, b);
		}
	};
	protected static final MonadicLattice<AbstractObject, PValue, ARState> lattice = new MonadicLattice<AbstractObject, PValue, ARState>() {
		MappedValueLatticeHelper<String, Object> fieldMapLattice;
		@Override
		public AbstractObject widen(final AbstractObject prev, final AbstractObject next) {
			assert (prev.fields == null) == (next.fields == null);
			if(prev.fields != null) {
				return new AbstractObject(fieldMapLattice.widen(prev.fields, next.fields));
			} else if(prev.discreteArrayField != null) {
				if(next.discreteArrayField != null) {
					if(prev.discreteArrayField.length() != next.discreteArrayField.length()) {
						return new AbstractObject(vMonad.widen(prev.collapseArray(), next.collapseArray()));
					}
					final Stream<Object> widenedSeq = prev.discreteArrayField.toStream().zipWith(next.discreteArrayField.toStream(), new F2<Object, Object, Object>() {
						@Override
						public Object f(final Object a, final Object b) {
							return vMonad.widen(a, b);
						}
					});
					return new AbstractObject(Seq.iterableSeq(widenedSeq));
				} else {
					assert next.nondetArrayField != null;
					return new AbstractObject(vMonad.widen(prev.collapseArray(), next.nondetArrayField));
				}
			} else {
				assert prev.nondetArrayField != null;
				if(next.nondetArrayField != null) {
					return new AbstractObject(vMonad.widen(prev.nondetArrayField, next.nondetArrayField));
				} else {
					return new AbstractObject(vMonad.widen(prev.nondetArrayField, next.collapseArray()));
				}
			}
		}

		@Override
		public AbstractObject join(final AbstractObject first, final AbstractObject second) {
			assert (first.fields == null) == (second.fields == null);
			if(first.fields != null) {
				return new AbstractObject(fieldMapLattice.join(first.fields, second.fields));
			} else if(first.nondetArrayField != null) {
				return joinWithNondet(first, second);
			} else if(second.nondetArrayField != null) {
				return joinWithNondet(second, first);
			} else {
				return joinFull(first, second);
			}
		}

		private AbstractObject joinWithNondet(final AbstractObject first, final AbstractObject second) {
			if(second.nondetArrayField != null) {
				return new AbstractObject(vMonad.join(first.nondetArrayField, second.nondetArrayField));
			} else {
				final Object collapsed = second.collapseArray();
				return new AbstractObject(vMonad.join(collapsed, first.nondetArrayField));
			}
		}

		private AbstractObject joinFull(final AbstractObject first, final AbstractObject second) {
			if(first.discreteArrayField.length() != second.discreteArrayField.length()) {
				return new AbstractObject(vMonad.join(first.collapseArray(), second.collapseArray()));
			} else {
				final Seq<Object> joinedSeq = Seq.iterableSeq(first.discreteArrayField.toStream().zipWith(second.discreteArrayField.toStream(), pairwiseJoin));
				return new AbstractObject(joinedSeq);
			}
		}

		@Override
		public boolean lessEqual(final AbstractObject first, final AbstractObject second) {
			assert (first.fields == null) == (second.fields == null);
			if(first.fields != null) {
				return fieldMapLattice.lessEqual(first.fields, second.fields);
			} else if(first.discreteArrayField != null) {
				if(second.discreteArrayField != null) {
					if(second.discreteArrayField.length() != first.discreteArrayField.length()) {
						return false;
					}
					return first.discreteArrayField.toStream().zip(second.discreteArrayField.toStream()).forall(new F<P2<Object,Object>, Boolean>() {
						@Override
						public Boolean f(final P2<Object, Object> a) {
							return vMonad.lessEqual(a._1(), a._2());
						}
					});
				} else {
					final Object joined = second.nondetArrayField;
					return first.discreteArrayField.toStream().forall(new F<Object, Boolean>() {
						@Override
						public Boolean f(final Object a) {
							return vMonad.lessEqual(a, joined);
						}
					});
				}
			} else {
				assert first.nondetArrayField!= null;
				if(second.discreteArrayField != null) {
					return false;
				}
				return vMonad.lessEqual(first.nondetArrayField, second.nondetArrayField);
			}
		}

		@Override
		public void inject(final Monads<PValue, ARState> monads) {
			fieldMapLattice = new MappedValueLatticeHelper<>(monads.valueMonad);
		}
	};
	public final TreeMap<String, Object> fields;
	public final Object nondetArrayField;
	public final Seq<Object> discreteArrayField;

	public AbstractObject(final TreeMap<String, Object> fields) {
		this.fields = fields;
		this.nondetArrayField = null;
		this.discreteArrayField = null;
	}
	
	public AbstractObject(final Object nondetContents) {
		this.nondetArrayField = nondetContents;
		this.fields = null;
		this.discreteArrayField = null;
	}
	
	public AbstractObject(final Seq<Object> discreteArrayField) {
		this.nondetArrayField = null;
		this.fields = null;
		this.discreteArrayField = discreteArrayField;
	}

	public AbstractObject() {
		this.nondetArrayField = null;
		this.discreteArrayField = null;
		this.fields = TreeMap.empty(Ord.stringOrd);
	}

	public Option<AbstractObject> updateArray(final PValue ind, final Object toSet, final boolean strong) {
		assert ind.isInterval();
		assert nondetArrayField != null || discreteArrayField != null;
		assert fields == null;
		if(nondetArrayField != null) {
			return Option.none();
		}
		if(ind.singleton()) {
			final int index = ind.asInt();
			if(index < discreteArrayField.length() && index >= 0) {
				if(strong) {
					return Option.some(new AbstractObject(discreteArrayField.update(index, toSet)));
				} else {
					return Option.some(new AbstractObject(joinWithIndex(discreteArrayField, index, toSet)));
				}
			} else {
				return Option.none();
			}
		} else {
			if(!checkBounds(ind)) {
				return Option.none();
			}
			Seq<Object> accum = discreteArrayField;
			for(int i = ind.lowerBound(); i <= ind.upperBound(); i++) {
				accum = joinWithIndex(accum, i, toSet);
			}
			return Option.some(new AbstractObject(accum));
		}
	}
	
	public AbstractObject updateArraySafe(final PValue ind, final Object toSet, final boolean strong) {
		assert ind.isInterval();
		assert nondetArrayField != null || discreteArrayField != null;
		assert fields == null;
		if(nondetArrayField != null) {
			return new AbstractObject(vMonad.join(toSet, nondetArrayField));
		}
		if(ind.singleton()) {
			final int index = ind.asInt();
			if(index < discreteArrayField.length()) {
				if(strong) {
					return new AbstractObject(discreteArrayField.update(index, toSet));
				} else {
					return new AbstractObject(joinWithIndex(discreteArrayField, index, toSet));
				}
			} else {
				throw new OutOfBoundsArrayAccess();
			}
		} else {
			Seq<Object> accum = discreteArrayField;
			final int upperBound = computeSafeUpperBound(ind);
			final int lowerBound = computeSafeLowerBound(ind);
			for(int i = lowerBound; i <= upperBound; i++) {
				accum = joinWithIndex(accum, i, toSet);
			}
			return new AbstractObject(accum);
		}
	}


	private boolean checkBounds(final PValue ind) {
		if(!ind.isFinite()) {
			return false;
		}
		if(ind.lowerBound() < 0 || ind.lowerBound() > discreteArrayField.length()) {
			return false;
		} else if(ind.upperBound() >= discreteArrayField.length() || ind.upperBound() < 0) {
			return false;
		}
		return true;
	}
	
	private Object collapseArray() {
		if(this.discreteArrayField.isEmpty()) {
			return vMonad.lift(PValue.bottom());
		}
		return this.discreteArrayField.toStream().foldLeft1(pairwiseJoin);
	}
	
	public Option<Object> getArray(final PValue ind) {
		if(nondetArrayField != null) {
			return Option.none();
		} else if(checkBounds(ind)) {
			if(ind.singleton()) {
				return Option.some(discreteArrayField.index(ind.asInt()));
			} else {
				return Option.some(discreteArrayField.drop(ind.lowerBound()).take(ind.upperBound() - ind.lowerBound() + 1).toStream().foldLeft1(pairwiseJoin));
			}
		} else {
			return Option.none();
		}
	}
	
	public Option<Object> getArraySafe(final PValue ind) {
		if(nondetArrayField != null) {
			return Option.some(nondetArrayField);
		} else {
			if(ind.singleton()) {
				if(!checkBounds(ind)) {
					return Option.none();
				}
				return Option.some(discreteArrayField.index(ind.asInt()));
			} else {
				if(ind.interval.min != null && ind.lowerBound() >= discreteArrayField.length()) {
					return Option.none();
				}
				if(discreteArrayField.isEmpty()) {
					return Option.none();
				}
				final int upperBound = computeSafeUpperBound(ind);
				final int lowerBound = computeSafeLowerBound(ind);
				assert lowerBound <= upperBound : ind + " " + discreteArrayField + " " + upperBound + " " + lowerBound;
				return Option.some(discreteArrayField.drop(lowerBound).take(upperBound - lowerBound + 1).toStream().foldLeft1(pairwiseJoin));
			}
		}
	}

	private int computeSafeUpperBound(final PValue ind) {
		return ind.interval.max == null ? discreteArrayField.length() - 1 : Math.min(discreteArrayField.length() - 1, ind.interval.max);
	}

	private int computeSafeLowerBound(final PValue ind) {
		return ind.interval.min == null ? 0 : Math.min(Math.max(0, ind.interval.min), discreteArrayField.length() - 1);
	}

	
	private void checkLowerBound(final PValue ind) {
		if(ind.interval.min == null) {
			throw new OutOfBoundsArrayAccess("Infinite lower bound");
		} else if(ind.interval.min < 0) {
			throw new OutOfBoundsArrayAccess("Negative lower bound " + ind);
		} else if(ind.interval.min >= discreteArrayField.length()) {
			throw new OutOfBoundsArrayAccess("Badly specified lower bound " + ind);
		}
	}

	public AbstractObject updateField(final String field, final Object toSet, final boolean strong) {
		assert this.fields != null;
		if(strong) {
			return new AbstractObject(fields.set(field, toSet));
		} else {
			return new AbstractObject(fields.update(field, new F<Object, Object>() {
				@Override
				public Object f(final Object a) {
					return vMonad.join(a, toSet);
				} 
			}, toSet));
		}
	}
	
	public Object getField(final String field) {
		assert fields != null;
		final Option<Object> optionalValue = fields.get(field);
		if(optionalValue.isNone()) {
			final Type fieldType = Scene.v().getField(field).getType();
			if(fieldType instanceof RefLikeType) {
				return vMonad.lift(PValue.nullPtr());
			} else {
				return vMonad.lift(PValue.lift(0));
			}
		}
		return optionalValue.orSome(vMonad.lift(PValue.bottom()));
	}
	
	public boolean isObject() {
		return fields != null;
	}
	
	public PValue getLength() {
		assert fields == null;
		if(nondetArrayField != null) {
			return PValue.positiveInterval();
		} else {
			return PValue.lift(discreteArrayField.length());
		}
	}

	private Seq<Object> joinWithIndex(final Seq<Object> accum, final int index, final Object toSet) {
		return accum.update(index, vMonad.join(toSet, accum.index(index)));
	}
	
	@Override
	public String toString() {
		if(fields != null) {
			return fields.toString();
		} else if(discreteArrayField != null) {
			return discreteArrayField.toString();
		} else {
			return nondetArrayField.toString();
		}
	}

	public Object collapseToValue() {
		if(nondetArrayField != null) {
			return nondetArrayField;
		} else if(discreteArrayField != null) { 
			return this.collapseArray();
		} else {
			throw new UnsupportedOperationException();
		}
	}
}
