package edu.washington.cse.concerto.interpreter.mock;

import com.google.common.base.Objects;

import edu.washington.cse.concerto.interpreter.EmbeddedState;
import edu.washington.cse.concerto.interpreter.ai.ValueMonad;
import edu.washington.cse.concerto.interpreter.lattice.Lattice;
import edu.washington.cse.concerto.interpreter.meta.Monads;
import edu.washington.cse.concerto.interpreter.value.EmbeddedValue;
import edu.washington.cse.concerto.interpreter.value.IValue;
import edu.washington.cse.concerto.interpreter.value.IValue.RuntimeTag;
import fj.F;
import fj.F2;
import fj.Ord;
import fj.P2;
import fj.data.Option;
import fj.data.TreeMap;

public class SimpleForeignHeap {
	private final static F2<TreeMap<String, LatticeInt>, P2<String, LatticeInt>, TreeMap<String, LatticeInt>> folder =
			new F2<TreeMap<String, LatticeInt>, P2<String, LatticeInt>, TreeMap<String, LatticeInt>>() {
			@Override
			public TreeMap<String, LatticeInt> f(final TreeMap<String, LatticeInt> a, final P2<String, LatticeInt> b) {
				return a.update(b._1(), new F<LatticeInt, LatticeInt>() {
					
					@Override
					public LatticeInt f(final LatticeInt a) {
						return a.join(b._2());
					}
				}, b._2());
			}
	};

	private static final Lattice<SimpleForeignHeap> lattice = new Lattice<SimpleForeignHeap>() {
		@Override
		public SimpleForeignHeap widen(final SimpleForeignHeap prev, final SimpleForeignHeap next) {
			return join(prev, next);
		}

		@Override
		public SimpleForeignHeap join(final SimpleForeignHeap first, final SimpleForeignHeap second) {
			return new SimpleForeignHeap(second.heap.toStream().foldLeft(folder, first.heap));
		}

		@Override
		public boolean lessEqual(final SimpleForeignHeap first, final SimpleForeignHeap second) {
			return first.heap.toStream().forall(new F<P2<String,LatticeInt>, Boolean>() {
				@Override
				public Boolean f(final P2<String, LatticeInt> a) {
					final Option<LatticeInt> value = second.heap.get(a._1());
					if(value.isNone()) {
						return false;
					}
					return value.some().lessEqual(a._2());
				}
			});
		}
		
	};
	private static final EmbeddedState<SimpleForeignHeap> empty = new EmbeddedState<>(new SimpleForeignHeap(TreeMap.<String, LatticeInt>empty(Ord.stringOrd)), lattice);

	private static ValueMonad<String> locationLattice = Monads.unsafeLift(new Lattice<String>() {
		@Override
		public String widen(final String prev, final String next) {
			return join(prev, next);
		}

		@Override
		public String join(final String first, final String second) {
			if(first == null || second == null) {
				return null;
			}
			if(!first.equals(second)) {
				return null;
			}
			return first;
		}

		@Override
		public boolean lessEqual(final String first, final String second) {
			if(second == null) {
				return true;
			} else {
				return Objects.equal(first, second);
			}
		}
	});
	private final TreeMap<String, LatticeInt> heap;
	
	private static class LatticeInt {
		private final int value;

		public LatticeInt(final int i) {
			this.value = i;
		}

		public boolean lessEqual(final LatticeInt other) {
			return this.equals(other) || other == top;
		}

		public static LatticeInt top = new LatticeInt(0) {
			@Override
			public boolean equals(final Object obj) {
				return this == obj;
			};
		};
		
		@Override
		public boolean equals(final Object obj) {
			if(obj == this) {
				return true;
			}
			if(obj == null) {
				return false;
			}
			if(obj.getClass() != LatticeInt.class) {
				return false;
			}
			return ((LatticeInt)obj).value == this.value;
		};
		
		public LatticeInt join(final LatticeInt other) {
			if(this.equals(other)) {
				return this;
			} else {
				return top;
			}
		}
	}
	
	public SimpleForeignHeap(final TreeMap<String, LatticeInt> treeMap) {
		this.heap = treeMap;
	}
	
	private SimpleForeignHeap put(final String s, final LatticeInt val) {
		return new SimpleForeignHeap(heap.set(s, val));
	}

	private IValue read(final String s) {
		if(!heap.contains(s)) {
			throw new RuntimeException("missed a location");
		}
		final LatticeInt value = heap.get(s).some();
		if(value == LatticeInt.top) {
			return IValue.nondet();
		} else {
			return IValue.lift(value.value);
		}
	}
	
	public static EmbeddedState<SimpleForeignHeap> put(EmbeddedState<SimpleForeignHeap> in, final IValue key, final IValue value) {
		if(in == null) {
			in = empty;
		}
		assert key.isEmbedded();
		final String val = (String) key.aVal.value;
		if(val == null) {
			throw new RuntimeException("write top key");
		}
		LatticeInt toPut;
		if(value.getTag() == RuntimeTag.INT) {
			toPut = new LatticeInt(value.asInt());
		} else {
			toPut = LatticeInt.top;
		}
		final SimpleForeignHeap updated = in.state.put(val, toPut);
		return new EmbeddedState<SimpleForeignHeap>(updated, in.stateLattice);
	}
	
	public static IValue get(final EmbeddedState<SimpleForeignHeap> in, final IValue key) {
		if(in == null) {
			throw new RuntimeException("No such state?!");
		}
		assert key.isEmbedded();
		final String val = (String) key.aVal.value;
		if(val == null) {
			return IValue.nondet();
		}
		return in.state.read(val);
	}
	
	public static IValue foreignLocation(final String val) {
		return new IValue(new EmbeddedValue(val, locationLattice));
	}
}
