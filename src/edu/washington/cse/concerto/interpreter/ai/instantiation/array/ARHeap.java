package edu.washington.cse.concerto.interpreter.ai.instantiation.array;

import edu.washington.cse.concerto.interpreter.ai.MappedValueLatticeHelper;
import edu.washington.cse.concerto.interpreter.ai.MonadicLattice;
import edu.washington.cse.concerto.interpreter.ai.ValueMonad;
import edu.washington.cse.concerto.interpreter.ai.injection.NeedsMonads;
import edu.washington.cse.concerto.interpreter.heap.HeapFaultStatus;
import edu.washington.cse.concerto.interpreter.meta.Monads;
import fj.F;
import fj.F2;
import fj.Ord;
import fj.P;
import fj.P2;
import fj.data.Option;
import fj.data.Seq;
import fj.data.Set;
import fj.data.Stream;
import fj.data.TreeMap;
import soot.AnySubType;
import soot.ArrayType;
import soot.RefType;
import soot.Type;
import soot.Value;
import soot.toolkits.scalar.Pair;

import java.util.Arrays;
import java.util.List;

import static edu.washington.cse.concerto.interpreter.ai.instantiation.array.AbstractHeapAccessResult.*;

public class ARHeap {
	private static final class JoinExistingObject implements F<AbstractObject, AbstractObject> {
		private final AbstractObject alloced;

		private JoinExistingObject(final AbstractObject alloced) {
			this.alloced = alloced;
		}

		@Override
		public AbstractObject f(final AbstractObject a) {
			return AbstractObject.lattice.join(a, alloced);
		}
	}

	protected final TreeMap<AbstractLocation, AbstractObject> map;
	protected final Set<AbstractLocation> weakLocations;
	private static ValueMonad<PValue> valueMonad;
	public static final NeedsMonads<PValue, ARState> injector = new NeedsMonads<PValue, ARState>() {
		@Override
		public void inject(final Monads<PValue, ARState> monads) {
			valueMonad = monads.valueMonad;
		}
	};

	protected static MonadicLattice<ARHeap, PValue, ARState> lattice = new MonadicLattice<ARHeap, PValue, ARState>() {
		private MappedValueLatticeHelper<AbstractLocation, AbstractObject> mapLattice;

		@Override
		public ARHeap widen(final ARHeap prev, final ARHeap next) {
			return new ARHeap(
					mapLattice.widen(prev.map, next.map),
					prev.weakLocations.union(next.weakLocations)
				);
		}

		@Override
		public ARHeap join(final ARHeap first, final ARHeap second) {
			return new ARHeap(
				mapLattice.join(first.map, second.map),
				first.weakLocations.union(second.weakLocations)
			);
		}

		@Override
		public boolean lessEqual(final ARHeap first, final ARHeap second) {
			return mapLattice.lessEqual(first.map, second.map) && first.weakLocations.subsetOf(second.weakLocations);
		}

		@Override
		public void inject(final Monads<PValue, ARState> monads) {
			AbstractObject.lattice.inject(monads);
			this.mapLattice = new MappedValueLatticeHelper<>(AbstractObject.lattice);
		}
	};
	
	public static ARHeap empty = new ARHeap(
			TreeMap.<AbstractLocation, AbstractObject>empty(AbstractLocation.LOCATION_ORDER),
			Set.empty(AbstractLocation.LOCATION_ORDER));
	
	private ARHeap(final TreeMap<AbstractLocation, AbstractObject> map, final Set<AbstractLocation> weakLocations) {
		this.map = map;
		this.weakLocations = weakLocations;
	}

	private interface ObjectMutation {
		AbstractHeapUpdateResult<AbstractObject> mutate(AbstractObject toMutate, boolean strong);
	}

	private interface FieldMutation extends ObjectMutation {
		@Override default AbstractHeapUpdateResult<AbstractObject> mutate(final AbstractObject toMutate, final boolean strong) {
			if(!toMutate.isObject()) {
				return AbstractHeapUpdateResult.lift(INFEASIBLE);
			}
			return AbstractHeapUpdateResult.of(mutateField(toMutate, strong));
		}

		AbstractObject mutateField(AbstractObject toMutate, boolean strong);
	}

	private interface ArrayMutation extends ObjectMutation {
		@Override default AbstractHeapUpdateResult<AbstractObject> mutate(final AbstractObject toMutate, final boolean strong) {
			if(toMutate.isObject()) {
				return AbstractHeapUpdateResult.lift(INFEASIBLE);
			}
			return mutateArray(toMutate, strong).map(AbstractHeapUpdateResult::of).orSome(() -> AbstractHeapUpdateResult.lift(OOB));
		}

		Option<AbstractObject> mutateArray(AbstractObject toMutate, boolean strong);
	}

	private P2<ARHeap, AbstractHeapAccessResult> doMutation(final PValue val, final ObjectMutation mutator) {
		if(val.singleton()) {
			final AbstractLocation updateAddress = val.addresses().iterator().next();
			if(updateAddress == AbstractLocation.NULL_LOCATION) {
				return P.p(this, NPE);
			}
			if(!map.contains(updateAddress)) {
				return P.p(this, INFEASIBLE);
			}
			final AbstractHeapUpdateResult<AbstractObject> mutated = mutator.mutate(map.get(updateAddress).some(), !weakLocations.member(updateAddress));
			return mutated.foldAndExtract((h, v) -> h.withBinding(updateAddress, v), this);
		} else {
			final P2<TreeMap<AbstractLocation, AbstractObject>, AbstractHeapAccessResult> updated = Stream.iterableStream(val.addresses()).foldLeft((accum, b) -> {
				if(b == AbstractLocation.NULL_LOCATION) {
					return accum.map2(AbstractHeapAccessResult::npe);
				}
				if(!accum._1().contains(b)) {
					return accum;
				}
				final AbstractObject o = accum._1().get(b).some();
				final AbstractHeapUpdateResult<AbstractObject> mutated = mutator.mutate(o, false);
				return mutated.extractAndMerge(ao -> accum._1().set(b, ao), accum::_1, accum._2());
			}, P.p(map, INFEASIBLE));
			return updated.map1(tr -> new ARHeap(tr, weakLocations));
		}
	}

	private ARHeap withBinding(final AbstractLocation updateAddress, final AbstractObject v) {
		return new ARHeap(map.set(updateAddress, v), weakLocations);
	}

	public P2<ARHeap, AbstractHeapAccessResult> setArray(final PValue val, final PValue index, final Object toSet) {
		return doMutation(val, (ArrayMutation) (toMutate, strong) -> toMutate.updateArray(index, toSet, strong));
	}
	
	public P2<ARHeap, AbstractHeapAccessResult> setArraySafe(final PValue val, final PValue index, final Object toSet) {
		return doMutation(val, (toMutate, strong) -> {
			if(toMutate.isObject()) {
				return AbstractHeapUpdateResult.lift(INFEASIBLE);
			}
			return AbstractHeapUpdateResult.of(toMutate.updateArraySafe(index, toSet, strong));
		});
	}


	public P2<ARHeap, AbstractHeapAccessResult> setField(final PValue baseValue, final String signature, final Object newValue) {
		return doMutation(baseValue, (FieldMutation) (toMutate, strong) -> toMutate.updateField(signature, newValue, strong));
	}

	private AbstractHeapUpdateResult<Object> empty() {
		return new AbstractHeapUpdateResult<>(null, HeapFaultStatus.BOTTOM, HeapFaultStatus.BOTTOM, true);
	}

	public AbstractHeapUpdateResult<Object> readField(final PValue v, final String signature) {
		return Stream.iterableStream(v.addresses()).foldLeft(new F2<AbstractHeapUpdateResult<Object>, AbstractLocation, AbstractHeapUpdateResult<Object>>() {
			@Override
			public AbstractHeapUpdateResult<Object> f(final AbstractHeapUpdateResult<Object> accum, final AbstractLocation b) {
				if(b == AbstractLocation.NULL_LOCATION) {
					return accum.npe();
				}
				final Option<AbstractObject> objectOpt = map.get(b);
				if(!objectOpt.isSome()) {
					return accum.infeasible();
				}
				final AbstractObject o = objectOpt.some();
				if(!o.isObject()) {
					return accum.infeasible();
				}
				return accum.fold(valueMonad::join, o.getField(signature));
			}
		}, empty());
	}

	public AbstractHeapUpdateResult<Object> readArray(final PValue val, final PValue ind) {
		return doArrayRead(val, ind, AbstractObject::getArray);
	}

	private AbstractHeapUpdateResult<Object> doArrayRead(final PValue val, final PValue ind, final F2<AbstractObject, PValue, Option<Object>> reader) {
		return Stream.iterableStream(val.addresses()).foldLeft((AbstractHeapUpdateResult<Object> accum, AbstractLocation b) -> {
			if(b == AbstractLocation.NULL_LOCATION) {
				return accum.npe();
			}
			final Option<AbstractObject> objectOpt = map.get(b);
			if(!objectOpt.isSome()) {
				return accum.infeasible();
			}
			final AbstractObject o = objectOpt.some();
			if(o.isObject()) {
				return accum.infeasible();
			}
			final Option<Object> result = reader.f(o, ind);
			if(result.isNone()) {
				return accum.oob();
			} else {
				return accum.fold(valueMonad::join, result.some());
			}
		}, empty());
	}
	

	public AbstractHeapUpdateResult<Object> readArraySafe(final PValue val, final PValue ind) {
		return this.doArrayRead(val, ind, (ao, i) -> ao.getArraySafe(i));
	}

	public Pair<ARHeap, PValue> allocate(final RefType t, final CallSite allocContext, final Value allocationExpr) {
		final AbstractLocation loc = new AbstractLocation(t, allocContext, allocationExpr);
		final AbstractObject allocedObject = allocForType(t);
		return this.addAllocedObject(loc, allocedObject);
	}
	
	public Pair<ARHeap, PValue> allocateUnknownType(final RefType upperBound, final CallSite allocContext, final Value allocationExpr) {
		final AbstractLocation loc = new AbstractLocation(AnySubType.v(upperBound), allocContext, allocationExpr);
		final AbstractObject ao = new AbstractObject();
		return this.addAllocedObject(loc, ao);
	}

	private AbstractObject allocForType(final RefType t) {
		return new AbstractObject(TreeMap.empty(Ord.stringOrd));
	}

	public Pair<ARHeap, PValue> allocate(final ArrayType t, final CallSite allocContext, final List<PValue> sizes, final Value allocExpr) {
		if(sizes.size() == 1) {
			return this.allocSingle(t, allocContext, sizes.get(0), allocExpr);
		}
		PValue prev = null;
		ARHeap accum = this;
		ArrayType typeAccum = t;
		PValue toReturn = null;
		for(int i = 0; i < sizes.size(); i++) {
			final Pair<ARHeap, PValue> allocSingle = accum.allocSingle(typeAccum, allocContext, sizes.get(i), allocExpr);
			final ARHeap newHeap = allocSingle.getO1();
			final PValue newPtr = allocSingle.getO2();
			
			if(toReturn == null) {
				toReturn = newPtr;
			}
			if(prev != null) {
				accum = newHeap.setArraySafe(prev, PValue.lift(0), valueMonad.lift(newPtr))._1();
			} else {
				accum = newHeap;
			}
			prev = newPtr;
			typeAccum = (ArrayType)typeAccum.getElementType();
		}
		return new Pair<>(accum, toReturn);
	}

	private Pair<ARHeap, PValue> allocSingle(final ArrayType t, final CallSite allocContext, final PValue pValue, final Value allocExpr) {
		final Type eType = t.getElementType();
		final AbstractLocation loc = new AbstractLocation(t, allocContext, allocExpr);
		final ARHeap newHeap;
		final Object defaultContents = eType instanceof RefType ? valueMonad.lift(PValue.nullPtr()) : valueMonad.lift(PValue.lift(0));
		if(eType instanceof ArrayType || !pValue.singleton() || !pValue.isInterval()) {
			final AbstractObject alloced = new AbstractObject(defaultContents);
			newHeap = new ARHeap(map.update(loc, new JoinExistingObject(alloced), alloced), weakLocations.insert(loc));
			return new Pair<>(newHeap, PValue.lift(loc));
		} else {
			final int size = pValue.asInt();
			final Object[] contents = new Object[size];
			Arrays.fill(contents, defaultContents);
			final AbstractObject newArray = new AbstractObject(Seq.arraySeq(contents));
			return this.addAllocedObject(loc, newArray);
		}
	}

	private Pair<ARHeap, PValue> addAllocedObject(final AbstractLocation loc, final AbstractObject allocedObject) {
		if(this.map.contains(loc)) {
			final ARHeap newHeap = new ARHeap(map.update(loc, new JoinExistingObject(allocedObject))._2(), weakLocations.insert(loc));
			return new Pair<>(newHeap, PValue.lift(loc));
		} else {
			final ARHeap newHeap = new ARHeap(map.set(loc, allocedObject), weakLocations);
			return new Pair<>(newHeap, PValue.lift(loc));
		}
	}

	public PValue getLength(final AbstractLocation b) {
		return map.get(b).map(AbstractObject::getLength).orSome(PValue.bottom());
	}

	
	@Override
	public String toString() {
		return "[M: " + map.toString() + "|WEAK: " + weakLocations + "]";
	}

}
