package edu.washington.cse.concerto.interpreter.ai.instantiation.pta;

import edu.washington.cse.concerto.interpreter.ai.MappedValueLatticeHelper;
import edu.washington.cse.concerto.interpreter.ai.ValueMonad;
import edu.washington.cse.concerto.interpreter.ai.ValueMonadLattice;
import edu.washington.cse.concerto.interpreter.ai.injection.NeedsMonads;
import edu.washington.cse.concerto.interpreter.lattice.Lattice;
import edu.washington.cse.concerto.interpreter.meta.Monads;
import fj.data.Option;
import fj.data.TreeMap;
import soot.AnySubType;
import soot.ArrayType;
import soot.RefType;
import soot.Type;
import soot.toolkits.scalar.Pair;

public class StupidHeap {
	public static ValueMonadLattice<StupidHeap> lattice = new ValueMonadLattice<StupidHeap>() {
		private final ValueMonadLattice<AbstractObject> objectLattice = new ValueMonadLattice<AbstractObject>() {
			private MappedValueLatticeHelper<String, Object> mapHelper;
			
			@Override
			public AbstractObject widen(final AbstractObject prev, final AbstractObject next) {
				if(prev == next) {
					return prev;
				}
				return new AbstractObject(mapHelper.widen(prev.fields, next.fields));
			}

			@Override
			public AbstractObject join(final AbstractObject first, final AbstractObject second) {
				if(first == second) {
					return first;
				}
				return new AbstractObject(mapHelper.join(first.fields, second.fields));
			}

			@Override
			public boolean lessEqual(final AbstractObject first, final AbstractObject second) {
				if(first == second) {
					return true;
				}
				return mapHelper.lessEqual(first.fields, second.fields);
			}

			@Override
			public void injectValueLattice(final Lattice<Object> m) {
				this.mapHelper = new MappedValueLatticeHelper<>(m);
			}
		};
		private MappedValueLatticeHelper<AbstractAddress, AbstractObject> latticeHelper;

		@Override
		public StupidHeap widen(final StupidHeap prev, final StupidHeap next) {
			return new StupidHeap(latticeHelper.widen(prev.mapping, next.mapping));
		}
		
		@Override
		public boolean lessEqual(final StupidHeap first, final StupidHeap second) {
			return latticeHelper.lessEqual(first.mapping, second.mapping);
		}
		
		@Override
		public StupidHeap join(final StupidHeap first, final StupidHeap second) {
			return new StupidHeap(latticeHelper.join(first.mapping, second.mapping));
		}
		
		@Override
		public void injectValueLattice(final Lattice<Object> m) {
			this.latticeHelper = new MappedValueLatticeHelper<>(this.objectLattice);
			objectLattice.injectValueLattice(m);
		}
	};

	public final fj.data.TreeMap<AbstractAddress, AbstractObject> mapping;
	public static final StupidHeap empty = new StupidHeap(TreeMap.<AbstractAddress, AbstractObject>empty(AbstractAddress.ADDRESS_ORDER));
	
	private static ValueMonad<JValue> vMonad;
	public static final NeedsMonads<JValue, StupidState> injector = new NeedsMonads<JValue, StupidState>() {
		@Override
		public void inject(final Monads<JValue, StupidState> monads) {
			StupidHeap.vMonad = monads.valueMonad;
		}
	};

	public StupidHeap(final TreeMap<AbstractAddress, AbstractObject> mapping) {
		this.mapping = mapping;
	}

	public StupidHeap set(final JValue baseValue, final String fName, final Object value) {
		final TreeMap<AbstractAddress, AbstractObject> updated = baseValue.addressSet.toStream().foldLeft((a, b) -> {
			if(b.equals(AbstractAddress.NULL_ADDRESS.f())) {
				return a;
			}
			return a.update(b, a1 -> a1.put(fName, value))._2();
		}, mapping);
		return new StupidHeap(updated);
	}

	public Object get(final JValue unwrap, final String name) {
		final Object value = unwrap.addressSet.toStream().foldLeft((a, b) -> {
			final Option<Object> value1 = mapping.get(b).bind(a1 -> a1.fields.get(name));
			if(value1.isNone()) {
				return a;
			} else if(a == null) {
				return value1.some();
			} else {
				return StupidHeap.vMonad.join(a, value1.some());
			}
		}, null);
		if(value == null) {
			return StupidHeap.vMonad.lift(JValue.bottom);
		} else {
			return value;
		}
	}

	public Pair<StupidHeap, JValue> alloc(final Type type) {
		final AbstractAddress addr = AbstractAddress.getAddress(type);
		return allocForAddress(addr);
	}

	private Pair<StupidHeap, JValue> allocForAddress(final AbstractAddress addr) {
		final JValue l = JValue.lift(addr);
		if(mapping.contains(addr)) {
			return new Pair<>(this, l);
		} else {
			final StupidHeap newHeap = new StupidHeap(mapping.set(addr, AbstractObject.empty));
			return new Pair<>(newHeap, l);
		}
	}
	
	public Pair<StupidHeap, JValue> allocUnknownType(final Type upperBound) {
		final AbstractAddress addr = AbstractAddress.getAddress(AnySubType.v((RefType) upperBound));
		return allocForAddress(addr);
	}

	public Pair<StupidHeap, JValue> alloc(final Type type, final int sizeCount) {
		if(sizeCount == 1) {
			return alloc(type);
		}
		assert type instanceof ArrayType;
		final ArrayType at = (ArrayType) type;
		assert at.numDimensions <= sizeCount;
		final Pair<StupidHeap, JValue> alloced = alloc(type);
		final Pair<StupidHeap, JValue> subAlloc = alloced.getO1().alloc(at.getElementType(), sizeCount - 1);
		
		final StupidHeap finalHeap = subAlloc.getO1().set(alloced.getO2(), "*", StupidHeap.vMonad.lift(subAlloc.getO2()));
		final JValue toReturn = alloced.getO2();
		return new Pair<>(finalHeap, toReturn);
	}
	
	@Override
	public String toString() {
		return mapping.toString();
	}

}
