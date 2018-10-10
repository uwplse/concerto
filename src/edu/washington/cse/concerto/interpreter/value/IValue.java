package edu.washington.cse.concerto.interpreter.value;

import edu.washington.cse.concerto.interpreter.ai.Concretizable;
import edu.washington.cse.concerto.interpreter.ai.ValueMonad;
import edu.washington.cse.concerto.interpreter.ai.binop.BranchPropagator;
import edu.washington.cse.concerto.interpreter.ai.binop.DefaultBranchPropagator;
import edu.washington.cse.concerto.interpreter.ai.binop.ObjectIdentityResult;
import edu.washington.cse.concerto.interpreter.heap.Location;
import edu.washington.cse.concerto.interpreter.util.ImmutableIterator;
import edu.washington.cse.concerto.interpreter.util.SingletonIterator;
import fj.data.Stream;
import soot.AnySubType;
import soot.FastHierarchy;
import soot.Scene;
import soot.SootClass;
import soot.Type;
import soot.jimple.Constant;
import soot.jimple.IntConstant;
import soot.jimple.NullConstant;
import soot.jimple.StringConstant;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

final public class IValue implements Iterable<IValue> {
	public static enum RuntimeTag {
		OBJECT,
		ARRAY,
		INT,
		STRING,
		NULL,
		
		NONDET,
		MULTI_VALUE,
		EMBEDDED, BOUNDED_OBJECT
	}
	
	private final RuntimeTag tag;
	private final int intValue;
	private final String stringValue;
	private final Location location;
	private final SootClass runtimeClass;
	public final AnySubType boundedType;
	private final Set<IValue> values;
	public final EmbeddedValue aVal;
	
	private IValue(final int i) {
		this.intValue = i;
		this.stringValue = null;
		this.location = null;
		this.runtimeClass = null;
		this.values = null;
		this.aVal = null;
		this.boundedType = null;
		
		this.tag = RuntimeTag.INT;
	}
	
	private IValue(final String s) {
		this.intValue = 0;
		this.stringValue = s;
		this.location = null;
		this.runtimeClass = null;
		this.values = null;
		this.aVal = null;
		this.boundedType = null;
		
		this.tag = RuntimeTag.STRING;
	}
	
	
	public IValue(final SootClass c, final Location location) {
		this.location = location;
		this.runtimeClass = c;
		this.intValue = 0;
		this.stringValue = null;
		this.values = null;
		this.aVal = null;
		this.boundedType = null;
		
		this.tag = RuntimeTag.OBJECT;
	}
	
	public IValue(final Location location) {
		this.location = location;
		this.runtimeClass = null;
		this.intValue = 0;
		this.stringValue = null;
		this.values = null;
		this.aVal = null;
		this.boundedType = null;
		
		this.tag = RuntimeTag.ARRAY;
	}
	
	IValue(final Set<IValue> vSet) {
		this.location = null;
		this.runtimeClass = null;
		this.intValue = 0;
		this.stringValue = null;
		this.values = vSet;
		this.aVal = null;
		assert flatStructure(vSet);
		this.boundedType = null;
		
		this.tag = RuntimeTag.MULTI_VALUE;
	}
	
	public IValue(final EmbeddedValue v) {
		this.location = null;
		this.runtimeClass = null;
		this.intValue = 0;
		this.stringValue = null;
		this.values = null;
		this.boundedType = null;
		
		this.aVal = v;
		this.tag = RuntimeTag.EMBEDDED;
	}
	
	private static boolean flatStructure(final Set<IValue> vSet) {
		for(final IValue v : vSet) {
			if(v.isMulti() || v.isEmbedded()) {
				return false;
			}
		}
		return true;
	}

	private IValue(final RuntimeTag t) {
		this.location = null;
		this.runtimeClass = null;
		this.intValue = 0;
		this.stringValue = null;
		this.values = null;
		this.aVal = null;
		this.boundedType = null;

		this.tag = t;
	}
	
	public IValue(final AnySubType bound, final Location location) {
		this.location = location;
		this.runtimeClass = null;
		this.intValue = 0;
		this.stringValue = null;
		this.values = null;
		this.aVal = null;
		this.boundedType = bound;

		this.tag = RuntimeTag.BOUNDED_OBJECT;
	}

	private static final IValue NULL = new IValue(RuntimeTag.NULL);
	private static final IValue NONDET = new IValue(RuntimeTag.NONDET);
	public static BranchPropagator<IValue> propagator = new DefaultBranchPropagator<IValue>() {
		@Override
		public IValue propagateNE(final IValue left, final IValue right) {
			if(left.isMulti() && right.getTag() == RuntimeTag.NULL && left.values.contains(IValue.NULL)) {
				final HashSet<IValue> filtered = new HashSet<>(left.values);
				filtered.remove(IValue.NULL);
				return new IValue(filtered);
			}
			return left;
		};
	};

	public RuntimeTag getTag() {
		return this.tag;
	}

	public static IValue nullConst() {
		return NULL;
	}
	
	public static IValue nondet() {
		return NONDET;
	}

	public static IValue lift(final Constant op) {
		if(op instanceof NullConstant) {
			return NULL;
		} else if(op instanceof StringConstant) {
			return new IValue(((StringConstant) op).value);
		} else if(op instanceof IntConstant) {
			return new IValue(((IntConstant) op).value);
		} else {
			throw new RuntimeException("Unrecognized constant form: " + op);
		}
	}

	public static IValue lift(final int iVal) {
		return new IValue(iVal);
	}
	
	public static IValue lift(final boolean b) {
		return new IValue(b ? 1 : 0);
	}
	
	public static IValue lift(final Collection<IValue> v) {
		return flattenToValue(v);
	}

	public int asInt() {
		assert tag == RuntimeTag.INT;
		return intValue;
	}
	
	public boolean asBoolean() {
		assert tag == RuntimeTag.INT;
		return intValue == 1;
	}

	public Location getLocation() {
		assert tag == RuntimeTag.NULL || tag == RuntimeTag.OBJECT || tag == RuntimeTag.ARRAY || tag == RuntimeTag.BOUNDED_OBJECT: tag;
		return location;
	}

	public String asString() {
		assert tag == RuntimeTag.STRING;
		return stringValue;
	}
	
	public SootClass getSootClass() {
		assert tag == RuntimeTag.OBJECT || tag == RuntimeTag.NULL;
		return runtimeClass;
	}
	
	@Override
	public boolean equals(final Object obj) {
		if(this == obj) {
			return true;
		}
		if(obj == null) {
			return false;
		}
		if(getClass() != obj.getClass()) {
			return false;
		}
		final IValue other = (IValue) obj;
		if(intValue != other.intValue) {
			return false;
		}
		if(location == null) {
			if(other.location != null) {
				return false;
			}
		} else if(!location.equals(other.location)) {
			return false;
		}
		if(runtimeClass == null) {
			if(other.runtimeClass != null) {
				return false;
			}
		} else if(!runtimeClass.equals(other.runtimeClass)) {
			return false;
		}
		if(stringValue == null) {
			if(other.stringValue != null) {
				return false;
			}
		} else if(!stringValue.equals(other.stringValue)) {
			return false;
		}
		if(tag != other.tag) {
			return false;
		}
		if(values == null) {
			if(other.values != null) {
				return false;
			}
		} else if(!values.equals(other.values)) {
			return false;
		}
		if(this.aVal == null) {
			if(other.aVal != null) {
				return false;
			}
		} else if(this.aVal != null) {
			if(other.aVal == null) {
				return false;
			} else if(this.aVal.monad != other.aVal.monad) {
				return false;
			} else {
				return this.aVal.monad.lessEqual(this.aVal.value, other.aVal.value) && this.aVal.monad.lessEqual(other.aVal.value, this.aVal.value);
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((aVal == null) ? 0 : aVal.hashCode());
		result = prime * result + intValue;
		result = prime * result + ((location == null) ? 0 : location.hashCode());
		result = prime * result + ((runtimeClass == null) ? 0 : runtimeClass.hashCode());
		result = prime * result + ((stringValue == null) ? 0 : stringValue.hashCode());
		result = prime * result + ((tag == null) ? 0 : tag.hashCode());
		result = prime * result + ((values == null) ? 0 : values.hashCode());
		return result;
	}

	public static IValue merge(final IValue v1, final IValue v2) {
		if(v1 == null) {
			return v2;
		} else if(v2 == null) {
			return v1;
		}
		if(v1.equals(v2)) {
			return v1;
		}
		if(v1.isEmbedded()) {
			return v1.mergeEmbedded(v2);
		} else if(v2.isEmbedded()) {
			return v2.mergeEmbedded(v1); 
		} else {
			final Set<IValue> flattened = new HashSet<>();
			flatten(flattened, v1);
			flatten(flattened, v2);
			return liftSet(flattened);
		}
	}
	
	public IValue mergeEmbedded(final IValue v1) {
		final Object toJoin;
		if(v1.isEmbedded()) {
			toJoin = v1.aVal.value;
		} else {
			toJoin = v1; 
		}
		final Object joined = this.aVal.monad.join(this.aVal.value, toJoin);
		return new IValue(new EmbeddedValue(joined, this.aVal.monad));
	}

	private static IValue flattenToValue(final Collection<IValue> v) {
		final Set<IValue> allVals = new HashSet<>();
		for(final IValue in : v) {
			if(in.isEmbedded()) {
				return liftValuesToEmbedded(in, v);
			}
		}
		for(final IValue in : v) {
			flatten(allVals, in);
		}
		return liftSet(allVals);
	}

	private static IValue liftValuesToEmbedded(final IValue in, final Collection<IValue> v) {
		Object accumulator = in.aVal.value;
		final ValueMonad<?> monad = in.aVal.monad;
		for(final IValue it : v) {
			if(it == in) {
				continue;
			} else if(it.isEmbedded()) {
				accumulator = monad.join(accumulator, it.aVal.value);
			} else {
				accumulator = monad.join(accumulator, it);
			}
		}
		return new IValue(new EmbeddedValue(accumulator, monad));
	}

	private static IValue liftSet(final Set<IValue> allVals) {
		if(allVals.isEmpty()) {
			return null;
		} else if(allVals.size() == 1) {
			return allVals.iterator().next();
		} else if(allVals.contains(nondet())) {
			return nondet();
		} else {
			return new IValue(allVals);
		}
	}
	
	private static void flatten(final Set<IValue> outSet, final IValue inValue) {
		if(inValue.isMulti()) {
			for(final IValue v : inValue.values) {
				assert !v.isMulti();
				outSet.add(v);
			}
		} else {
			outSet.add(inValue);
		}
	}

	public IValue mapValue(final IValueTransformer tr) {
		if(isMulti()) {
			final Set<IValue> mapped = new HashSet<>();
			for(final IValue v : values) {
				final IValue m = tr.transform(v, true);
				if(m != null) {
					mapped.add(m);
				}
			}
			return lift(mapped);
		} else if(isNonDet()) {
			return this;
		} else {
			return tr.transform(this, false);
		}
	}
	
	public boolean isNonDet() {
		return getTag() == RuntimeTag.NONDET;
	}
	
	public boolean isMulti() {
		return getTag() == RuntimeTag.MULTI_VALUE;
	}
	
	public boolean isDeterministic() {
		return !isMulti() && !isNonDet();
	}

	public void forEach(final IValueAction vt) {
		if(isNonDet()) {
			vt.nondet();
		} else if(isMulti()) {
			for(final IValue v : values) {
				vt.accept(v, true);
			}
		} else {
			vt.accept(this, false);
		}
	}

	public Iterator<IValue> variants() {
		assert isMulti();
		final Iterator<IValue> wrapped = this.values.iterator();
		return new ImmutableIterator<IValue>(wrapped);
	}
	
	@Override
	public String toString() {
		switch(tag) {
		case ARRAY:
			return "[ARRAY@"+location+"]";
		case INT:
			return intValue + "";
		case MULTI_VALUE:
			final StringBuilder sb = new StringBuilder();
			sb.append("[");
			final Iterator<IValue> variants = variants();
			sb.append(variants.next());
			while(variants.hasNext()) {
				sb.append(",").append(variants.next());
			}
			sb.append("]");
			return sb.toString();
		case NONDET:
			return "*";
		case NULL:
			return "NULL";
		case OBJECT:
			return "[CLASS:"+runtimeClass.getName()+"@"+location+"]";
		case STRING:
			return "\"" + stringValue + "\"";
		case EMBEDDED:
			return "V(" + this.aVal.value.toString() + ")";
		case BOUNDED_OBJECT:
			return "[?:"+boundedType + "@" + location + "]";
		}
		throw new RuntimeException("Unhandled value type: " + tag);
	}

	public boolean isHeapValue() {
		return tag == RuntimeTag.ARRAY || tag == RuntimeTag.OBJECT || tag == RuntimeTag.BOUNDED_OBJECT;
	}

	public int variantSize() {
		assert this.isMulti();
		return values.size();
	}

	public boolean isMultiHeap() {
		if(!this.isMulti()) {
			return false;
		}
		final IValue sample = variants().next();
		return sample.isHeapValue() || sample.getTag() == RuntimeTag.NULL;
	}
	
	// This this lessEqual to the second
	public boolean lessEqual(final IValue second) {
		if(second == null) {
			return false;
		}
		if(second == this) {
			return true;
		}
		if(this.equals(second)) {
			return true;
		}
		if(second.tag == RuntimeTag.EMBEDDED) {
			if(this.tag == RuntimeTag.EMBEDDED) {
				return second.aVal.monad.lessEqual(this.aVal.value, second.aVal.value);
			}
			return second.aVal.monad.lessEqual(this, second.aVal.value);
		}
		if(this.tag != RuntimeTag.MULTI_VALUE && second.tag == RuntimeTag.MULTI_VALUE) {
			return second.values.contains(this);
		}
		switch(this.tag) {
		case INT:
			if(second.getTag() == RuntimeTag.NONDET) {
				return true;
			} else {
				return false;
			}
		case MULTI_VALUE:
			for(final IValue v : values) {
				if(!v.lessEqual(second)) {
					return false;
				}
			}
			return true;
		case NONDET:
			return false;
		case NULL:
			return false;
		case ARRAY:
		case OBJECT:
		case BOUNDED_OBJECT:
			return false;
		case STRING:
			throw new RuntimeException("Not supported yet");
		case EMBEDDED:
			return this.aVal.monad.lessEqual(this.aVal.value, second);
		default:
			break;
		}
		throw new RuntimeException("Ooops");
	}

	public boolean isEmbedded() {
		return this.tag == RuntimeTag.EMBEDDED;
	}

	// TODO: make this more generally available
	public static IValue concretize(final Concretizable sz) {
		if(!sz.concretizable()) {
			return nondet();
		}
		return liftNative(sz.concretize());
	}

	public static IValue lift(final Object join, final ValueMonad<?> monad) {
		if(join instanceof IValue) {
			return (IValue) join;
		} else {
			return new IValue(new EmbeddedValue(join, monad));
		}
	}
	
	public static IValue liftNative(final Iterable<Object> toLift) {
		final Set<IValue> lifted = new HashSet<>();
		for(final Object o : toLift) {
			lifted.add(IValue.liftNative(o));
		}
		return IValue.lift(lifted);
	}

	public static IValue liftNative(final Object o) {
		if(o instanceof Integer) {
			return IValue.lift((Integer)o);
		} else if(o instanceof Boolean) {
			return IValue.lift((Boolean)o);
		} else {
			throw new UnsupportedOperationException("Don't know how to lift " + o + " of type " + o.getClass());
		}
	}

	public boolean isPrimitive() {
		if(this.tag == RuntimeTag.INT || this.tag == RuntimeTag.NULL) {
			return true;
		} else if(this.tag == RuntimeTag.MULTI_VALUE) {
			for(final IValue v : values) {
				if(!v.isPrimitive()) {
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}
	
	public Stream<IValue> valueStream() {
		if(this.tag == RuntimeTag.MULTI_VALUE) {
			return Stream.iterableStream(values);
		} else {
			return Stream.single(this);
		}
	}

	public boolean isDefinitelyNotEqual(final IValue b) {
		assert !b.isEmbedded() && !this.isEmbedded();
		if(this.isMulti() && b.isMulti()) {
			for(final IValue v : this.values) {
				if(b.values.contains(v)) {
					return false;
				}
			}
			return true;
		} else if(this.isMulti()) {
			return !this.values.contains(b);
		} else if(b.isMulti()) {
			return !b.values.contains(this);
		} else {
			return !this.equals(b);
		}
	}

	public ObjectIdentityResult isInstanceOf(final Type t) {
		return valueStream().map(v -> {
			if(v.getTag() == RuntimeTag.NULL) {
				return ObjectIdentityResult.MUST_BE;
			} else if(v.getTag() == RuntimeTag.OBJECT || v.getTag() == RuntimeTag.ARRAY) {
				final Location l = v.location;
				if(Scene.v().getOrMakeFastHierarchy().canStoreType(l.type, t)) {
					return ObjectIdentityResult.MUST_BE;
				} else {
					return ObjectIdentityResult.MUST_NOT_BE;
				}
			} else {
				final boolean canStore = Scene.v().getOrMakeFastHierarchy().canStoreType(boundedType, t);
				if(!canStore) {
					return ObjectIdentityResult.MUST_NOT_BE;
				} else if(!boundedType.getBase().getSootClass().isInterface()) {
					return ObjectIdentityResult.MUST_BE;
				} else {
					return ObjectIdentityResult.MAY_BE;
				}
			}
		}).foldLeft1(ObjectIdentityResult::join);
	}

	public IValue downCast(final Type castType) {
		assert getTag() != RuntimeTag.EMBEDDED;
		final FastHierarchy fh = Scene.v().getOrMakeFastHierarchy();
		if(isMultiHeap()) {
			final Set<IValue> toReturn = new HashSet<>();
			final Iterator<IValue> it = variants();
			while(it.hasNext()) {
				final IValue var = it.next();
				if(var.getTag() == RuntimeTag.NULL) {
					toReturn.add(var);
					continue;
				}
				assert var != null && var.getLocation() != null : var;
				final Type t = var.getLocation().type;
				if(!fh.canStoreType(t, castType)) {
					continue;
				}
				toReturn.add(var);
			}
			if(toReturn.isEmpty()) {
				return null;
			}
			return IValue.lift(toReturn);
		} else if(isHeapValue()) {
			final Type t = getLocation().type;
			if(!fh.canStoreType(t, castType)) {
				return null;
			}
			return this;
		} else {
			assert getTag() == RuntimeTag.NULL;
			return this;
		}
	}

	@Override
	public Iterator<IValue> iterator() {
		if(this.isMulti()) {
			return this.variants();
		} else {
			return new SingletonIterator<>(this);
		}
	}
}
