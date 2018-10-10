package edu.washington.cse.concerto.interpreter.heap;

import edu.washington.cse.concerto.interpreter.exception.NullPointerException;
import edu.washington.cse.concerto.interpreter.exception.PruneExecutionException;
import edu.washington.cse.concerto.interpreter.meta.EmbeddedContext;
import edu.washington.cse.concerto.interpreter.meta.MetaInterpreter;
import edu.washington.cse.concerto.interpreter.state.StateReader;
import edu.washington.cse.concerto.interpreter.value.Copyable;
import edu.washington.cse.concerto.interpreter.value.IValue;
import edu.washington.cse.concerto.interpreter.value.IValue.RuntimeTag;
import edu.washington.cse.concerto.interpreter.value.IValueAction;
import edu.washington.cse.concerto.interpreter.value.IValueTransformer;
import edu.washington.cse.concerto.interpreter.value.ValueMerger;
import fj.F;
import fj.Ord;
import fj.P;
import fj.P2;
import fj.P3;
import fj.data.Option;
import fj.data.TreeMap;
import soot.AnySubType;
import soot.ArrayType;
import soot.RefLikeType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootFieldRef;
import soot.Type;
import soot.Value;
import soot.grimp.Grimp;
import soot.jimple.ArrayRef;
import soot.jimple.IntConstant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Heap {
	private static int HEAP_ID_COUNTER = 0;
	
	protected final HashMap<Location, HeapObject> heap = new HashMap<>();
	public StateReader stateReader;
	public TreeMap<Integer, Integer> locationCounter;
	private static final ConcurrentHashMap<P3<Object, Value, Type>, Integer> canonContextMap = new ConcurrentHashMap<>();
	private static final AtomicInteger contextNumberer = new AtomicInteger(0);
	

	public static void resetCounters() {
		HEAP_ID_COUNTER = 0;
		contextNumberer.set(0);
		canonContextMap.clear();
	}
	
	public final Heap parentHeap;
	protected final int id;
	
	public Heap() {
		this.id = HEAP_ID_COUNTER++;
		this.parentHeap = null;
		locationCounter = TreeMap.empty(Ord.intOrd);
		this.stateReader = new StateReader(0, 0);
	}
	
	protected Heap(final Heap parent, final TreeMap<Integer, Integer> locationCounter, final StateReader reader) {
		this.id = HEAP_ID_COUNTER++;
		this.parentHeap = parent;
		assert locationCounter != null;
		this.locationCounter = locationCounter;
		this.stateReader = reader;
	}
	
	protected Heap(final Heap parentHeap, final HashMap<Location, HeapObject> heap, final TreeMap<Integer, Integer> locCounter, final StateReader reader) {
		this.parentHeap = parentHeap;
		this.id = HEAP_ID_COUNTER++;
		deepCopy(this.heap, heap);
		assert locCounter != null;
		this.locationCounter = locCounter;
		this.stateReader = reader;
	}
	
	private static <K, V extends Copyable<V>> void deepCopy(final Map<K, V> out, final Map<K, V> in) {
		for(final Map.Entry<K, V> kv : in.entrySet()) {
			out.put(kv.getKey(), kv.getValue().copy());
		}
	}

	public Heap fork() {
		return new Heap(this, this.locationCounter, stateReader.fork());
	}
	
	public Heap copy() {
		return this.newDerivedHeap(parentHeap, this.heap, locationCounter, stateReader.copy());
	}
	
	protected Location makeFreshLocation(final Value allocationSite, final Type t, final Object allocationContext) {
		assert allocationContext != null;
		assert t != null;
		assert allocationSite != null;
		final int contextNumber = Heap.getContextNumber(allocationSite, t, allocationContext);
		if(this.isSummarizedContext(contextNumber)) {
			return new Location(contextNumber, t, -1, true);
		} else {
			final int id = this.getNextLocation(contextNumber, allocationContext);
			return new Location(contextNumber, t, id, false);
		}
	}
	
	public void assertSaneStructure() {
		final TreeMap<Integer, Integer> locs = this.locationCounter;
		Heap it = this;
		while(it != null) {
			final HashMap<Location, HeapObject> bindings = it.heap;
			for(final Location l : bindings.keySet()) {
				assert locs.contains(l.contextNumber) : l + " " + this + " " + locs;
				bindings.get(l).forEachField((n, hf) -> {
					hf.valueStream().forEach(iv -> {
						if(iv.isHeapValue()) {
							assert this.findObject(iv.getLocation()) != null : iv.getLocation() + " " + l + " " +  n + " " + this.toStringFull();
						}
					});
				});
				if(l.isSummary) {
					assert this.isSummarizedContext(l.contextNumber) : this + " " + locs;
				}
				if(l.id != -1) {
					assert (l.id <= locs.get(l.contextNumber).some()) || locs.get(l.contextNumber).some() == -1 : l + " " + this + " " + locs;
				}
			}
			it = it.parentHeap;
		}
	}
	
	private static final F<Integer, Integer> incrementer = new F<Integer, Integer>() {
		@Override
		public Integer f(final Integer a) {
			assert a != -1;
			return a + 1;
		}
	};

	private int getNextLocation(final int contextNumber, final Object allocationContext) {
		assert !this.isSummarizedContext(contextNumber);
		if(locationCounter.contains(contextNumber) && (allocationContext instanceof EmbeddedContext)) {
			locationCounter = locationCounter.set(contextNumber, -1);
			return -1;
		}
		locationCounter = locationCounter.update(contextNumber, incrementer, 0);
		return locationCounter.get(contextNumber).some();
	}

	private static int getContextNumber(final Value allocationSite, final Type t, final Object allocationContext) {
		final P3<Object, Value, Type> context = P.p(allocationContext, allocationSite, t);
		if(!canonContextMap.containsKey(context)) {
			canonContextMap.putIfAbsent(context, contextNumberer.getAndIncrement());
		}
		return canonContextMap.get(context);
	}

	private boolean isSummarizedContext(final int allocationContext) {
		return locationCounter.get(allocationContext).orSome(0) == -1;
	}
	
	public HeapAccessResult putArray(final IValue arrayRef, final IValue index, final IValue rhs, final boolean isWeakWrite) {
		if(arrayRef.isEmbedded()) {
			throw new UnsupportedOperationException("Found array read with abstract array reference " + arrayRef);
		}
		final HeapFaultStatus[] flags = new HeapFaultStatus[]{
			HeapFaultStatus.BOTTOM,
			HeapFaultStatus.BOTTOM
		};
		final IValue toWrite;
		toWrite = toConcreteInt(index);
		filterForAccess(arrayRef).forEach(new IValueAction() {
			
			@Override
			public void nondet() { }
			
			@Override
			public void accept(final IValue v, final boolean isPtrMulti) {
				if(v.getTag() == RuntimeTag.NULL) {
					flags[0] = flags[0].mark();
					return;
				}
				flags[0] = flags[0].unmark();
				final Location loc = v.getLocation();
				final HeapObject h = findAndCloneObject(loc);
				if(h.isNondetSize || toWrite.isNonDet()) {
					h.nondetContents = IValue.merge(h.nondetContents, rhs);
				} else {
					toWrite.forEach(new IValueAction() {
						@Override
						public void nondet() {
							throw new RuntimeException("Invariant violation");
						}
						
						@Override
						public void accept(final IValue v, final boolean isIndexMulti) {
							final int indexValue = v.asInt();
							if(checkOutOfBounds(h, indexValue)) {
								flags[1] = flags[1].mark();
								return;
							}
							flags[1] = flags[1].unmark();
							if(isIndexMulti || isPtrMulti || loc.isSummary || isWeakWrite) {
								h.arrayField[indexValue] = IValue.merge(h.arrayField[indexValue], rhs);
							} else {
								h.arrayField[indexValue] = rhs;
							}
						}
					});
				}
			}
		});
		return new HeapAccessResult(flags[0], flags[1]);
	}

	public HeapAccessResult putArray(final IValue arrayRef, final IValue index, final IValue rhs) {
		return this.putArray(arrayRef, index, rhs, false);
	}

	private HeapObject findAndCloneObject(final Location location) {
		if(location == null) {
			throw new NullPointerException();
		}
		if(heap.containsKey(location)) {
			return heap.get(location);
		}
		final HeapObject o = parentHeap.findObject(location);
		if(o == null) {
			return null;
		} else {
			final HeapObject copy = o.copy();
			heap.put(location, copy);
			return copy;
		}
	}
	
	public HeapObject findObject(final Location location) {
		if(location == null) {
			throw new NullPointerException();
		}
		if(heap.containsKey(location)) {
			return heap.get(location);
		} else if(parentHeap == null) {
			return null;
		} else {
			return parentHeap.findObject(location);
		}
	}

	public HeapAccessResult putField(final IValue base, final SootFieldRef fieldRef, final IValue rhs) {
		if(base.isEmbedded()) {
			throw new UnsupportedOperationException("Found write to abstract base value " + base);
		}
		final String fieldName = fieldRef.name();
		final HeapFaultStatus[] npeFlag = new HeapFaultStatus[]{HeapFaultStatus.BOTTOM};
		filterForAccess(base).forEach(new IValueAction() {
			@Override
			public void nondet() { }
			
			@Override
			public void accept(final IValue v, final boolean weakUpdate) {
				if(v.getTag() == RuntimeTag.NULL) {
					npeFlag[0] = npeFlag[0].mark();
					return;
				}
				npeFlag[0] = npeFlag[0].unmark();
				final HeapObject h = findAndCloneObject(v.getLocation());
				final Map<String, IValue> fields = h.fieldMap;
				if(weakUpdate || v.getLocation().isSummary) {
					fields.put(fieldName, IValue.merge(rhs, fields.get(fieldName)));
				} else {
					fields.put(fieldName, rhs);
				}
			}
		});
		return new HeapAccessResult(npeFlag[0], HeapFaultStatus.MUST_NOT);
	}

	public IValue filterForAccess(final IValue base) {
		final IValue filtered = base.mapValue(new IValueTransformer() {
			@Override
			public IValue transform(final IValue v, final boolean isMulti) {
				if(v.isHeapValue() && findObject(v.getLocation()) == null) {
					return null;
				}
				return v;
			}
		});
		if(filtered == null) {
			throw new PruneExecutionException();
		}
		return filtered;
	}

	public HeapReadResult<IValue> getArray(final IValue base, final IValue index) {
		if(base.isEmbedded()) {
			throw new UnsupportedOperationException("Found array read with abstract array reference " + base);
		}
		final HeapFaultStatus[] flags = new HeapFaultStatus[]{
			HeapFaultStatus.BOTTOM,
			HeapFaultStatus.BOTTOM
		};
		final IValue toRead;
		toRead = toConcreteInt(index);
		final Set<IValue> toReturn = new HashSet<>();
		filterForAccess(base).forEach(new IValueAction() {
			
			@Override
			public void nondet() { throw new RuntimeException(); }
			
			@Override
			public void accept(final IValue v, final boolean isMulti) {
				if(v.getTag() == RuntimeTag.NULL) { 
					flags[0] = flags[0].mark();
					return;
				}
				flags[0] = flags[0].unmark();
				final Location location = v.getLocation();
				final HeapObject h = findObject(location);
				if(h.isNondetSize) {
					flags[1] = flags[1].unmark();
					toReturn.add(h.nondetContents);
					return;
				}
				if(h.nondetContents != null) {
					toReturn.add(h.nondetContents);
				}
				toRead.forEach(new IValueAction() {
					@Override
					public void nondet() {
						collectArrayValues(h, toReturn);
					}
					
					@Override
					public void accept(final IValue indexV, final boolean isMulti) {
						final int index = indexV.asInt();
						if(checkOutOfBounds(h, index)) {
							flags[1] = flags[1].mark();
							return;
						}
						flags[1] = flags[1].unmark();
						toReturn.add(h.arrayField[index]);
					}
				});
			}
		});
		if(toReturn.isEmpty()) {
			assert flags[0] == HeapFaultStatus.MUST || flags[1] == HeapFaultStatus.MUST;
			return new HeapReadResult<>(flags);
		} else {
			return new HeapReadResult<>(IValue.lift(toReturn), flags);
		}
	}

	public IValue toConcreteInt(final IValue index) {
		final IValue toRead;
		if(index.isEmbedded()) {
			toRead = MetaInterpreter.concretize(index.aVal.value);
		} else {
			toRead = index;
		}
		return toRead;
	}

	public boolean checkOutOfBounds(final HeapObject h, final int indexV) {
		return indexV >= h.arrayLength || indexV < 0;
	}

	public HeapReadResult<IValue> getField(final IValue base, final SootFieldRef fieldRef) {
		if(base.isEmbedded()) {
			throw new UnsupportedOperationException("Found field read with abstract base pointer " + base);
		}
		final HeapFaultStatus[] npeFlag = new HeapFaultStatus[]{HeapFaultStatus.BOTTOM};
		final Set<IValue> read = new HashSet<>();
		filterForAccess(base).forEach(new IValueAction() {
			
			@Override
			public void nondet() { throw new RuntimeException(); }
			
			@Override
			public void accept(final IValue v, final boolean isMulti) {
				if(v.getTag() == RuntimeTag.NULL) {
					npeFlag[0] = npeFlag[0].mark();
					return;
				}
				npeFlag[0] = npeFlag[0].unmark();
				final Location location = v.getLocation();
				final HeapObject h = findObject(location);
				final String fieldName = fieldRef.name();
				if(h.fieldMap.get(fieldName) == null) {
					throw new PruneExecutionException();
				}
				read.add(h.fieldMap.get(fieldName));
			}
		});
		return new HeapReadResult<>(IValue.lift(read), npeFlag[0], HeapFaultStatus.MUST_NOT);
	}
	
	public IValue getLength(final IValue base) {
		return base.valueStream().map(IValue::getLocation).map(this::findObject).map(h -> {
			if(h.isNondetSize) {
				return IValue.nondet();
			} else {
				return IValue.lift(h.arrayLength);
			}
		}).foldLeft1(ValueMerger.STRICT_MERGE::merge);
	}
	
	private void updateLocation(final Location location, final HeapObject object) {
		if(location.isSummary) {
			final HeapObject existing = findObject(location);
			if(existing != null) {
				heap.put(location, mergeObjects(existing, object, ValueMerger.STRICT_MERGE));
			} else {
				heap.put(location, object);
			}
		} else {
			assert findObject(location) == null : this + " " + location + " " + object + " " + findObject(location) + " " + this.locationCounter;
			heap.put(location, object);
		}
	}

	public IValue allocateArray(final Type arrayType, final IValue sz, final Value allocationSite, final Object allocationContext) {
		return this.allocateArray(arrayType, sz, allocationSite, allocationContext, false);
	}
	
	private IValue allocateArray(final Type arrayType, final IValue sz_, final Value allocationSite, final Object allocationContext, final boolean isSummaryArray) {
		final Location location = makeFreshLocation(allocationSite, arrayType, allocationContext);
		final IValue sz;
		sz = toConcreteInt(sz_);
		final HeapObject h;
		final Type baseType = ((ArrayType)arrayType).getElementType();
		final IValue dVal = getDefaultJVMValue(baseType);
		if(!sz.isDeterministic()) {
			h = new HeapObject(isSummaryArray, dVal);
		} else {
			final int len = sz.asInt();
			h = new HeapObject(len, dVal);
		}
		updateLocation(location, h);
		return new IValue(location);
	}

	public static IValue getDefaultJVMValue(final Type baseType) {
		final IValue dVal;
		if(baseType instanceof RefLikeType) {
			dVal = IValue.nullConst();
		} else {
			dVal = IValue.lift(0);
		}
		return dVal;
	}
	
	public IValue allocate(final SootClass sootClass, final Value allocationSite, final Object allocContext) {
		Scene.v().loadClass(sootClass.getName(), SootClass.BODIES);
		final Location location = makeFreshLocation(allocationSite, sootClass.getType(), allocContext);
		final IValue v = new IValue(sootClass, location);
		final HeapObject object = HeapObject.forClass(sootClass);
		updateLocation(location, object);
		return v;
	}
	
	public IValue allocateBoundedType(final RefType upperBound, final Value allocationSite, final Object allocContext) {
		this.assertSaneStructure();
		final AnySubType bound = AnySubType.v(upperBound);
		final Location location = makeFreshLocation(allocationSite, bound, allocContext);
		final IValue v = new IValue(bound, location);
		final HeapObject  object= HeapObject.forTypeBound(bound);
		updateLocation(location, object);
		this.assertSaneStructure();
		return v;
		
	}
	
	public IValue allocateArray(final Type type, final List<IValue> sz, final Value allocationSite, final Object allocationContext) {
		final IValue firstSize = toConcreteInt(sz.get(0));
		if(sz.size() == 1) {
			return allocateArray(type, firstSize, allocationSite, allocationContext);
		}
		final ArrayType arrayType = (ArrayType) type;
		if(!firstSize.isDeterministic()) {
			final IValue head = allocateArray(type, firstSize, allocationSite, allocationContext);
			Type tAccum = type;
			IValue pieceAccum = head;
			for(int i = 1; i < sz.size(); i++) {
				tAccum = ((ArrayType)tAccum).getElementType();
				final IValue nextPiece = allocateArray(tAccum, IValue.nondet(), allocationSite, allocationContext, true);
				writeNondetField(pieceAccum.getLocation(), nextPiece);
				pieceAccum = nextPiece;
			}
			return head;
		} else {
			final List<IValue> remainder = sz.subList(1, sz.size());
			final int dim = firstSize.asInt();
			final IValue[] arr = new IValue[dim];
			for(int i = 0; i < dim; i++) {
				arr[i] = allocateArray(arrayType.getElementType(), remainder, allocationSite, allocationContext);
			}
			final Location location = makeFreshLocation(allocationSite, type, allocationContext);
			updateLocation(location, new HeapObject(arr, null));
			return new IValue(location);
		}
	}
	
	private void writeNondetField(final Location location, final IValue nextPiece) {
		if(location.isSummary) {
			final HeapObject toWrite = findAndCloneObject(location);
			if(toWrite.nondetContents == null) {
				toWrite.nondetContents = nextPiece;
			} else {
				toWrite.nondetContents = ValueMerger.STRICT_MERGE.merge(toWrite.nondetContents, nextPiece);
			}
		} else {
			findAndCloneObject(location).nondetContents = nextPiece;
		}
	}

	public static Map<Value, Value> canonicalSubMap = new HashMap<>();
	
	protected Value getChildAllocation(final Value e) {
		if(canonicalSubMap.containsKey(e)) {
			return canonicalSubMap.get(e);
		}
		final ArrayRef toRet = Grimp.v().newArrayRef(e, IntConstant.v(0));
		canonicalSubMap.put(e, toRet);
		return toRet;
	}
	
	private static HeapObject mergeObjects(final HeapObject obj1, final HeapObject obj2, final ValueMerger vm) {
		if(obj1.fieldMap != null) {
			assert obj2.fieldMap != null;
			final Map<String, IValue> fMap = new HashMap<>();
			final Set<String> keys = new HashSet<>(obj1.fieldMap.keySet());
			keys.addAll(obj2.fieldMap.keySet());
			for(final String s : keys) {
				fMap.put(s, vm.merge(obj1.fieldMap.get(s), obj2.fieldMap.get(s)));
			}
			return new HeapObject(fMap);
		} else if(obj1.isNondetSize && obj2.isNondetSize) {
			return new HeapObject(obj1.isSummaryObject || obj2.isSummaryObject, vm.merge(obj1.nondetContents, obj2.nondetContents));
		} else if(obj1.isNondetSize) {
			return mergeIntoNondetArray(obj1, obj2, vm);
		} else if(obj2.isNondetSize) {
			return mergeIntoNondetArray(obj2, obj1, vm);
		} else if(obj1.arrayLength == obj2.arrayLength) {
			return mergeArrays(obj1, obj2, vm);
		} else {
			return collapseArrays(obj1, obj2, vm);
		}
	}
	
	private static HeapObject collapseArrays(final HeapObject obj1, final HeapObject obj2, final ValueMerger vm) {
		final Set<IValue> allVals = new HashSet<>();
		collectArrayValues(obj1, allVals);
		collectArrayValues(obj2, allVals);
		return new HeapObject(obj1.isSummaryObject || obj2.isSummaryObject, vm.mergeAll(allVals));
	}

	public static void collectArrayValues(final HeapObject arrayObj, final Set<IValue> allVals) {
		for(final IValue v : arrayObj.arrayField) {
			allVals.add(v);
		}
		if(arrayObj.nondetContents != null) {
			allVals.add(arrayObj.nondetContents);
		}
	}

	private static HeapObject mergeArrays(final HeapObject obj1, final HeapObject obj2, final ValueMerger vm) {
		assert obj1.arrayField != null && obj2.arrayField != null && obj1.arrayLength == obj2.arrayLength;
		final IValue[] merged = new IValue[obj2.arrayLength];
		for(int i = 0; i < obj2.arrayLength; i++) {
			merged[i] = vm.merge(obj1.arrayField[i], obj2.arrayField[i]);
		}
		final IValue nondetContents;
		if(obj1.nondetContents != null && obj2.nondetContents != null) {
			nondetContents = vm.merge(obj1.nondetContents, obj2.nondetContents);
		} else if(obj1.nondetContents != null) {
			nondetContents = obj1.nondetContents;
		} else if(obj2.nondetContents != null) {
			nondetContents = obj2.nondetContents;
		} else {
			nondetContents = null;
		}
		return new HeapObject(merged, nondetContents);
	}

	private static HeapObject mergeIntoNondetArray(final HeapObject obj1, final HeapObject obj2, final ValueMerger vm) {
		final Set<IValue> allVals = new HashSet<>();
		allVals.add(obj1.nondetContents);
		collectArrayValues(obj2, allVals);
		return new HeapObject(obj1.isSummaryObject || obj2.isSummaryObject, vm.mergeAll(allVals));
	}

	public Heap popHeap() { 
		if(parentHeap == null) {
			throw new UnsupportedOperationException();
		}
		final Heap toReturn = new Heap(parentHeap.parentHeap, this.locationCounter, this.stateReader);
		// XXX: do we need a deep copy?
		toReturn.heap.putAll(heap);
		mergeParentHeap(toReturn, this.parentHeap);
		return toReturn;
	}

	protected static void mergeParentHeap(final Heap targetHeap, final Heap parentHeap) {
		for(final Location l : parentHeap.heap.keySet()) {
			if(targetHeap.heap.containsKey(l)) {
				continue;
			}
			targetHeap.heap.put(l, parentHeap.heap.get(l).copy());
		}
	}
	
	public void mergeHeap(final Heap other) {
		assert other.parentHeap == this.parentHeap;
		mergeLoop(this, this, other, ValueMerger.STRICT_MERGE);
		locationCounter = mergeCounters(locationCounter, other.locationCounter, STRICT_MERGE);
		stateReader = this.stateReader.joinWith(other.stateReader);
	}
	
	public void mergeAndPopHeap(final Heap other) {
		assert this == other.parentHeap;
		this.heap.putAll(other.heap);
		this.locationCounter = other.locationCounter;
		this.stateReader = other.stateReader;
	}

	private interface CounterMerge {
		Integer getMerged(int a, int b);
		Integer handleMissing(int a);
	}
	
	private static final CounterMerge WIDENING_MERGE = new CounterMerge() {
		@Override
		public Integer getMerged(final int prev, final int next) {
			if(next > prev) {
				return -1;
			} else {
				return prev;
			}
		}

		@Override public Integer handleMissing(final int a) {
			return -1;
		}
	};
	
	private static final CounterMerge STRICT_MERGE = new CounterMerge() {
		@Override
		public Integer getMerged(final int a, final int b) {
			if(a == -1) {
				return -1;
			} else if(b == -1) {
				return -1;
			} else {
				return Math.max(a, b);
			}
		}

		@Override public Integer handleMissing(final int a) {
			return a;
		}
	};

	public void mergeAndPopHeaps(final Heap child1, final Heap child2) {
		assert child1.parentHeap == this;
		assert child2.parentHeap == this;
		mergeLoop(this, child1, child2, ValueMerger.STRICT_MERGE);
		this.locationCounter = mergeCounters(child1.locationCounter, child2.locationCounter, STRICT_MERGE);
		this.stateReader = child1.stateReader.joinWith(child2.stateReader);
	}
	
	private static TreeMap<Integer, Integer> mergeCounters(final TreeMap<Integer, Integer> lc1, final TreeMap<Integer, Integer> lc2, final CounterMerge strictMerge) {
		return lc2.toStream().foldLeft((a, b) -> a.update(b._1(), currValue -> {
			if(!currValue.equals(b._2())) {
				return strictMerge.getMerged(currValue, b._2());
			} else {
				return currValue;
			}
		}, strictMerge.handleMissing(b._2())), lc1);
	}

	private static void mergeLoop(final Heap targetHeap, final Heap s1, final Heap s2, final ValueMerger vm) {
		final Set<Location> locations = new HashSet<>();
		locations.addAll(s1.heap.keySet());
		locations.addAll(s2.heap.keySet());
		for(final Location loc : locations) {
			if(s1.heap.containsKey(loc) && s2.heap.containsKey(loc)) {
				targetHeap.heap.put(loc, mergeObjects(s1.heap.get(loc), s2.heap.get(loc), vm));
			} else if(s1.heap.containsKey(loc)) {
				// lookup the mapping in the s2 heap, consulting parent heaps
				// (s1 effectively gets the old value of the binding)
				final HeapObject o = s2.findObject(loc);
				// s1 mapping only exists in s1 heap
				if(o == null) {
					targetHeap.heap.put(loc, s1.heap.get(loc).copy());
					continue;
				}
				targetHeap.heap.put(loc, mergeObjects(o, s1.heap.get(loc), vm));
			} else {
				assert s2.heap.containsKey(loc);
				final HeapObject o = s1.findObject(loc);
				if(o == null) {
					targetHeap.heap.put(loc, s2.heap.get(loc).copy());
					continue;
				}
				targetHeap.heap.put(loc, mergeObjects(o, s2.heap.get(loc), vm));
			}
		}
	}
	
	@Override
	public String toString() {
		final List<Heap> heaps = new ArrayList<>();
		Heap it = parentHeap;
		while(it != null) {
			heaps.add(it);
			it = it.parentHeap;
		}
		final StringBuilder sb = new StringBuilder();
		for(int i = heaps.size() - 1; i >= 0; i--) {
			final Heap currHeap = heaps.get(i);
			sb.append(currHeap.id).append(":[").append(currHeap.heap).append("] o ");
		}
		sb.append(id).append(":");
		sb.append("[").append(heap.toString()).append("]");
		return sb.toString();
	}
	
	public String toStringFull() {
		return "{" + toString() + "X" + this.locationCounter + "}";
	}
	
	public static Heap join(final Heap first, final Heap second) {
		assert first.parentHeap == second.parentHeap;
		final TreeMap<Integer, Integer> newCounts = mergeCounters(first.locationCounter, second.locationCounter, STRICT_MERGE);
		final Heap toReturn = new Heap(first.parentHeap, newCounts, first.stateReader.joinWith(second.stateReader));
		mergeLoop(toReturn, first, second, ValueMerger.STRICT_MERGE);
		return toReturn;
	}
	
	public static Heap widen(final Heap prev, final Heap next) {
		assert next.parentHeap == prev.parentHeap;
		final TreeMap<Integer, Integer> newCounts = mergeCounters(prev.locationCounter, next.locationCounter, WIDENING_MERGE);
		final Heap toReturn = new Heap(next.parentHeap, newCounts, prev.stateReader.widenWith(next.stateReader));
		mergeLoop(toReturn, prev, next, ValueMerger.WIDENING_MERGE);
		return toReturn;
	}

	protected Heap newDerivedHeap(final Heap parentHeap, final HashMap<Location, HeapObject> heap, final TreeMap<Integer, Integer> locationCounter, final StateReader reader) {
		return new Heap(parentHeap, heap, locationCounter, reader);
	}

	public IValue getCollapsedArray(final Location location) {
		final HeapObject o = findObject(location);
		final Set<IValue> v = new HashSet<>();
		collectArrayValues(o, v);
		return IValue.lift(v);
	}

	public void applyHeap(final Heap h) {
		this.assertSaneStructure();
		h.assertSaneStructure();
		this.heap.putAll(h.heap);
		h.heap.values().forEach(ho ->
			ho.forEachField((n, iv) -> 
				iv.valueStream().forEach(iv_ -> {
					if(iv_.isHeapValue() && findObject(iv_.getLocation()) == null) {
						this.heap.put(iv_.getLocation(), h.findObject(iv_.getLocation()));
					}
				})
			)
		);
		this.locationCounter = h.locationCounter;
		this.stateReader = h.stateReader;
		this.assertSaneStructure();
	}

	public Heap popTo(final Heap targetHeap) {
		if(targetHeap == parentHeap) {
			return this.copy();
		}
		final Heap toReturn = new Heap(targetHeap, this.locationCounter, this.stateReader);
		toReturn.heap.putAll(this.heap);
		Heap parent = this.parentHeap;
		while(parent != targetHeap) {
			assert parent != null;
			mergeParentHeap(toReturn, parent);
			parent = parent.parentHeap;
		}
		return toReturn;
	}

	public void dumpObject(final Location location) {
		System.out.println(findObject(location));
	}

	public boolean isSummary(final Location location) {
		if(location == null) {
			return false;
		}
		final HeapObject obj = findObject(location);
		if(obj == null) {
			return false;
		}
		return obj.isSummaryObject;
	}

	public boolean noWidenedLocations() {
		return true;
	}

	public boolean lessEqual(final Heap second) {
		assert this.parentHeap == second.parentHeap;
		if(!lessEqualCounters(this, second)) {
			return false;
		}
		for(final Location l : this.heap.keySet()) {
			if(!this.heap.get(l).lessEqual(second.findObject(l))) {
				return false;
			}
		}
		return true;
	}
	
	public boolean lessEqualFull(final Heap second) {
		assert this.depth() == second.depth();
		if(this.parentHeap == second.parentHeap) {
			return this.lessEqual(second);
		}
		if(!lessEqualCounters(this, second)) {
			return false;
		}
		return lessEqualLoop(this, second);
	}

	private static boolean lessEqualCounters(final Heap h1, final Heap h2) {
		return h1.locationCounter.toStream().forall(new F<P2<Integer,Integer>, Boolean>() {
			@Override
			public Boolean f(final P2<Integer, Integer> a) {
				final int k = a._1();
				final int v = a._2();
				final Option<Integer> otherV = h2.locationCounter.get(k);
				if(otherV.isNone()) {
					return false;
				}
				final int unwrapped = otherV.some();
				if(unwrapped == -1) {
					return true;
				}
				return v <= unwrapped;
			}
		});
	}

	private static boolean lessEqualLoop(final Heap first, final Heap second) {
		final Set<Location> visitedLocSet = new HashSet<>();
		Heap it = first;
		while(it != null) {
			for(final Location l : it.heap.keySet()) {
				if(!visitedLocSet.add(l)) {
					continue;
				}
				if(!it.heap.get(l).lessEqual(second.findObject(l))) {
					return false;
				}
			}
			it = it.parentHeap;
		}
		return true;
	}

	public boolean descendantOf(final Heap parent) {
		Heap h = this.parentHeap;
		while(h != null) {
			if(h == parent) {
				return true;
			}
			h = h.parentHeap;
		}
		return false;
	}

	public void computeBackup(final Heap other) {
		for(final Location l : other.heap.keySet()) {
			if(this.heap.containsKey(l)) {
				continue;
			} else {
				this.findAndCloneObject(l);
			}
		}
	}
	
	private static Heap assertAndReturn(final Heap h) {
		h.assertSaneStructure();
		return h;
	}

	public Heap collapseToSingle() {
		// this is a gross little trick
		return assertAndReturn(this.popTo(null));
	}

	public int depth() {
		int i = 1;
		Heap it = parentHeap;
		while(it != null) {
			i++;
			it = it.parentHeap;
		}
		return i;
	}
	
	public static Heap fullWiden(final Heap ch1, final Heap ch2) {
		assert ch1.depth() == ch2.depth();
		if(ch1.parentHeap == ch2.parentHeap) {
			return widen(ch1, ch2);
		}
		final Heap parentJoin = fullWiden(ch1.parentHeap, ch2.parentHeap);
		final TreeMap<Integer, Integer> newCounts = mergeCounters(ch1.locationCounter, ch2.locationCounter, WIDENING_MERGE);
		final Heap toReturn = new Heap(parentJoin, newCounts, ch1.stateReader.widenWith(ch2.stateReader));
		mergeLoop(toReturn, ch1, ch2, ValueMerger.WIDENING_MERGE);
		return toReturn;
	}

	public static Heap fullJoin(final Heap ch1, final Heap ch2) {
		assert ch1.depth() == ch2.depth();
		if(ch1.parentHeap == ch2.parentHeap) {
			return join(ch1, ch2);
		}
		final Heap parentJoin = fullJoin(ch1.parentHeap, ch2.parentHeap);
		final TreeMap<Integer, Integer> newCounts = mergeCounters(ch1.locationCounter, ch2.locationCounter, STRICT_MERGE);
		final Heap toReturn = new Heap(parentJoin, newCounts, ch1.stateReader.joinWith(ch2.stateReader));
		mergeLoop(toReturn, ch1, ch2, ValueMerger.STRICT_MERGE);
		return toReturn;
	}
}
