package edu.washington.cse.concerto.interpreter.ai.instantiation.array;

import edu.washington.cse.concerto.interpreter.ai.BottomAware;
import edu.washington.cse.concerto.interpreter.ai.CompareResult;
import edu.washington.cse.concerto.interpreter.ai.Concretizable;
import edu.washington.cse.concerto.interpreter.ai.MonadicLattice;
import edu.washington.cse.concerto.interpreter.meta.Monads;
import fj.F;
import fj.data.Set;
import soot.FastHierarchy;
import soot.Scene;
import soot.Type;

import java.util.Iterator;

public class PValue implements Concretizable, BottomAware {
	private static final Set<AbstractLocation> EMPTY_LOCATIONS = Set.empty(AbstractLocation.LOCATION_ORDER);
	protected static MonadicLattice<PValue, PValue, ARState> lattice = new MonadicLattice<PValue, PValue, ARState>() {
		@Override
		public void inject(final Monads<PValue, ARState> monads) { }
		
		@Override
		public PValue widen(final PValue prev, final PValue next) {
			return new PValue(prev.address.union(next.address), Interval.widen(prev.interval, next.interval));
		}
		
		@Override
		public boolean lessEqual(final PValue first, final PValue second) {
			return first.address.subsetOf(second.address) &&
				Interval.lessEqual(first.interval, second.interval);
		}
		
		@Override
		public PValue join(final PValue first, final PValue second) {
			return new PValue(first.address.union(second.address), Interval.join(first.interval, second.interval));
		}
	};

	public static class Interval {
		public final Integer min;
		public final Integer max;
		
		public Interval(final Integer min, final Integer max) {
			if(min != null && min == 0 && max != null && max == 66) {
				throw new RuntimeException();
			}
			assert min == null || max == null || min <= max : min + " " + max;
			this.min = min;
			this.max = max;
		}

		public static Interval join(final Interval i1, final Interval i2) {
			if(i1 == null) {
				return i2;
			} else if(i2 == null) {
				return i1;
			}
			final Integer newMin = compareMin(i1.min, i2.min) < 0 ? i1.min : i2.min;
			final Integer newMax = compareMax(i1.max, i2.max) > 0 ? i1.max : i2.max;
			final Interval toReturn = new Interval(newMin, newMax);
			return toReturn;
		}

		public static boolean lessEqual(final Interval i1, final Interval i2) {
			if(i1 == null) {
				return true;
			} else if(i2 == null) {
				return false;
			}
			return compareMin(i2.min, i1.min) <= 0 && compareMax(i1.max, i2.max) <= 0;
		}

		public static Interval widen(final Interval prev, final Interval next) {
			if(prev == null) {
				return next;
			} else if(next == null) {
				return prev; // ? impossible?
			}
			// prev.min: a, next.min: c
			final Integer newMin = compareMin(next.min, prev.min) < 0 ? null : prev.min;
			final Integer newMax = compareMax(next.max, prev.max) > 0 ? null : prev.max;
			return new Interval(newMin, newMax);
		}

		public boolean isSingleton() {
			if(min == null || max == null) {
				return false;
			}
			return min == max;
		}

		public int asInt() {
			assert isSingleton();
			return min;
		}

		public boolean isFinite() {
			return min != null && max != null;
		}

		public boolean containedWithin(final Interval bInt) {
			final int minComp = compareMin(this.min, bInt.min);
			final int maxComp = compareMax(this.max, bInt.max);
			return minComp >= 0 && maxComp <= 0;
		}
		
		// <0 if a is smaller than b, >0 if a is bigger than b (a null => less, b null => greater)
		private static int compareMin(final Integer a, final Integer b) {
			if(a == b) {
				return 0;
			}
			if(a == null) {
				return -1;
			} else if(b == null) {
				return 1;
			} else {
				return a - b;
			}
		}
		
		// <0 if a is smaller than b, >0 if a is bigger than b (a null => larger, b null => less)
		private static int compareMax(final Integer a, final Integer b) {
			if(a == b) {
				return 0;
			}
			if(a == null) {
				return 1;
			} else if(b == null) {
				return -1;
			} else {
				return a - b;
			}
		}

		public static String toString(final Interval interval) {
			if(interval == null) {
				return "\u22A5";
			} else {
				return interval.toString();
			}
		}
		
		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder("[");
			if(this.min == null) {
				sb.append("-").append("\u221E");
			} else {
				sb.append(this.min);
			}
			sb.append(",");
			if(this.max == null) {
				sb.append("\u221E");
			} else {
				sb.append(this.max);
			}
			sb.append("]");
			return sb.toString();
		}

		public Interval intersectWith(final Interval right) {
			final Integer newMin;
			final Integer newMax;
			if(compareMin(min, right.min) > 0) {
				newMin = min;
			} else {
				newMin = right.min; 
			}
			if(compareMax(max, right.max) > 0) {
				newMax = right.max;
			} else {
				newMax = max;
			}
			return new Interval(newMin, newMax);
		}
	}
	
	public final Interval interval;
	public final Set<AbstractLocation> address;

	public PValue(final Set<AbstractLocation> loc, final Interval interval) {
		this.interval= interval;
		this.address = loc;
	}

	public PValue(final Interval interval) {
		this(EMPTY_LOCATIONS, interval);
	}

	public PValue(final Set<AbstractLocation> filter) {
		this(filter, null);
	}

	public static PValue fullInterval() {
		return new PValue(new Interval(null, null));
	}

	public PValue plus(final PValue b) {
		assert b.isInterval() && this.isInterval();
		final Interval bInterval = b.interval;
		final Interval aInterval = this.interval;
		final Interval toReturn = new Interval(addInt(bInterval.min, aInterval.min), addInt(bInterval.max, aInterval.max));
		return new PValue(toReturn);
	}
	

	public PValue minus(final PValue b) {
		final Interval bInterval = b.interval;
		final Interval aInterval = this.interval;
		final Interval toReturn = new Interval(subtractInt(aInterval.min, bInterval.max), subtractInt(aInterval.max, bInterval.min));
		return new PValue(toReturn);
	}

	private static Integer subtractInt(final Integer left, final Integer right) {
		if(left == null || right == null) {
				return null;
		}
		return left - right;
	}

	private static Integer addInt(final Integer a, final Integer b) {
		if(a == null || b == null) {
			return null;
		}
		return a + b;
	}

	public boolean isInterval() {
		return interval != null && address.isEmpty();
	}

	public boolean singleton() {
		if(isInterval()) {
			return interval.isSingleton();
		} else {
			return address.size() == 1;
		}
	}

	public int asInt() {
		assert isInterval() && this.singleton();
		return interval.asInt();
	}

	public int lowerBound() {
		return interval.min;
	}

	public int upperBound() {
		return interval.max;
	}

	public static PValue positiveInterval() {
		return new PValue(new Interval(0, null));
	}

	public static PValue lift(final int length) {
		return new PValue(new Interval(length, length));
	}

	public static PValue bottom() {
		return new PValue(EMPTY_LOCATIONS, null);
	}

	public CompareResult compare(final PValue b) {
		if(!b.isInterval() || !this.isInterval()) {
			return CompareResult.NO_RESULT;
		}
		if(this.singleton() && b.singleton()) {
			return CompareResult.fromInt(this.asInt() - b.asInt());
		}
		final Interval aInt = this.interval;
		final Interval bInt = b.interval;
		if(bInt.max != null && aInt.isSingleton() && bInt.max.equals(aInt.min)) {
			return CompareResult.GE;
		} else if(bInt.min != null && aInt.isSingleton() && bInt.min.equals(aInt.min)) {
			return CompareResult.LE;
		} else if(aInt.max != null && bInt.isSingleton() && aInt.max.equals(bInt.min)) {
			return CompareResult.LE;
		} else if(aInt.min != null && bInt.isSingleton() && aInt.min.equals(bInt.min)) {
			return CompareResult.GE;
		}
		if(aInt.containedWithin(bInt) || bInt.containedWithin(aInt)) {
			return CompareResult.nondet();
		}
		// Cannot be equal, or we would have containment
		if(Interval.compareMin(aInt.min, bInt.min) < 0) {
			/*
			 * two cases:
			 * 1) a c[....
			 *    b     [....
			 * OR
			 * 2) a -inf....
			 *    b     [....
			 * let's compare a's max to b's min, then
			 * null <=> c2 => impossible, we would have containment
			 * c2 <=> c3 => determines results
			 * c2 <=> null => impossible, by definition a-min < b-min, and b-min == -inf implies a-min >= b-min 
			 * null <=> null => impossible again, b's min cannot be -inf
			 */
			assert bInt.min != null;
			assert aInt.max != null;
			if(aInt.max < bInt.min) {
				return CompareResult.LT;
			} else if(aInt.max <= bInt.min) {
				return CompareResult.LE;
			} else {
				return CompareResult.nondet();
			}
		} else {
			// symmetric from above
			assert bInt.max != null;
			assert aInt.min != null;
			if(bInt.max < aInt.min) {
				return CompareResult.GT;
			} else if(bInt.max <= aInt.min) {
				return CompareResult.GE;
			} else {
				return CompareResult.nondet();
			}
		}
	}
	
	public static PValue interval(final int min, final int max) {
		return new PValue(new Interval(min, max));
	}

	public boolean isFinite() {
		if(interval != null) {
			return interval.isFinite();
		} else {
			return true;
		}
	}
	
	public Iterable<AbstractLocation> addresses() {
		return address;
	}

	public PValue filterType(final Type castType) {
		final FastHierarchy fh = Scene.v().getOrMakeFastHierarchy();
		return new PValue(address.filter(new F<AbstractLocation, Boolean>() {
			@Override
			public Boolean f(final AbstractLocation a) {
				final Type t = a.type;
				return fh.canStoreType(t, castType);
			}
		}));
	}

	@Override public boolean isBottom() {
		return address.isEmpty() && interval == null;
	}

	@Override
	public boolean concretizable() {
		return isInterval() && isFinite();
	}

	@Override
	public Iterable<Object> concretize() {
		return new Iterable<Object>() {
			
			@Override
			public Iterator<Object> iterator() {
				return new Iterator<Object>() {
					int curr = interval.min;
					final int end = interval.max;
					
					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
					
					@Override
					public Object next() {
						final int toReturn = curr++;
						return toReturn;
					}
					
					@Override
					public boolean hasNext() {
						return curr <= end;
					}
				};
			}
		};
	}
	
	@Override
	public String toString() {
		return "<" + address + "||" + Interval.toString(interval) + ">";
	}

	public static PValue lift(final AbstractLocation loc) {
		return new PValue(Set.single(AbstractLocation.LOCATION_ORDER, loc));
	}

	public PValue withMax(final Integer max) {
		if(this.interval.max == null || this.interval.max > max) {
			return new PValue(new Interval(this.interval.min, max));
		} else {
			return this;
		}
	}

	public PValue withMin(final int min) {
		if(this.interval.min == null || this.interval.min < min) {
			return new PValue(new Interval(min, this.interval.max));
		} else {
			return this;
		}
	}

	public PValue withBounds(final PValue right) {
		return new PValue(this.interval.intersectWith(right.interval));
	}

	public PValue intersectAddress(final PValue right) {
		return new PValue(address.intersect(right.address));
	}

	public PValue filterAddress(final PValue right) {
		return new PValue(address.minus(right.address));
	}

	public static PValue nullPtr() {
		return PValue.lift(AbstractLocation.NULL_LOCATION);
	}
	
	public boolean isPositive() {
		return this.interval.min != null && this.interval.min >= 0;
	}
	
	public boolean isStrictlyPositive() {
		return this.interval.min != null && this.interval.min > 0;
	}
	
	public boolean isNegative() {
		return this.interval.max != null && this.interval.max <= 0;
	}
	
	public boolean isStrictlyNegative() {
		return this.interval.max != null && this.interval.max < 0;
	}

	public boolean couldBeZero() {
		return this.interval.min != null && this.interval.min == 0;
	}
}
