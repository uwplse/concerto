package edu.washington.cse.concerto.interpreter.ai.instantiation.pta;

import edu.washington.cse.concerto.interpreter.ai.BottomAware;
import edu.washington.cse.concerto.interpreter.ai.binop.ObjectIdentityResult;
import edu.washington.cse.concerto.interpreter.ai.instantiation.optiflow.PV;
import edu.washington.cse.concerto.interpreter.lattice.Lattice;
import fj.F;
import fj.Ordering;
import fj.data.Set;
import soot.AnySubType;
import soot.Scene;

import java.util.Objects;

public class JValue implements BottomAware, PV {
	public JValue(final Set<AbstractAddress> aSet) {
		this.addressSet = aSet;
	}
	
	public static final Set<AbstractAddress> emptySet = Set.empty(AbstractAddress.ADDRESS_ORDER);
	public final Set<AbstractAddress> addressSet;

	public static final JValue bottom = new JValue(emptySet);
	public static final Lattice<JValue> lattice = new Lattice<JValue>() {
		
		@Override
		public JValue widen(final JValue prev, final JValue next) {
			return join(prev, next);
		}
		
		@Override
		public boolean lessEqual(final JValue first, final JValue second) {
			final boolean compare = first.addressSet.subsetOf(second.addressSet);
			return compare;
		}
		
		@Override
		public JValue join(final JValue first, final JValue second) {
			return new JValue(first.addressSet.union(second.addressSet));
		}
	};
	
	public static JValue lift(final AbstractAddress address) {
		return new JValue(emptySet.insert(address));
	}
	
	@Override
	public String toString() {
		return addressSet.toString();
	}

	public ObjectIdentityResult isNull() {
		if(this.addressSet.member(AbstractAddress.NULL_ADDRESS.f())) {
			if(this.addressSet.size() == 1) {
				return ObjectIdentityResult.MUST_BE;
			} else {
				return ObjectIdentityResult.MAY_BE;
			}
		} else {
			return ObjectIdentityResult.MUST_NOT_BE;
		}
	}

	public boolean compatibleWith(final AbstractAddress receiverAddr) {
		if(addressSet.member(receiverAddr)) {
			return true;
		}
		return addressSet.toStream().exists(a -> a.t instanceof AnySubType && Scene.v().getOrMakeFastHierarchy().canStoreType(receiverAddr.t, ((AnySubType) a.t).getBase()));
	}

	public JValue narrowToAddress(final AbstractAddress addr) {
		return new JValue(addressSet.filter(mem -> AbstractAddress.ADDRESS_ORDER.compare(mem, addr) == Ordering.EQ ||
				mem.t instanceof AnySubType && Scene.v().getOrMakeFastHierarchy().canStoreType(addr.t, ((AnySubType) mem.t).getBase())
		));
	}

	@Override public boolean equals(final Object o) {
		if(this == o)
			return true;
		if(o == null || getClass() != o.getClass())
			return false;
		final JValue jValue = (JValue) o;
		return Objects.equals(addressSet, jValue.addressSet);
	}

	@Override public int hashCode() {
		return Objects.hash(addressSet);
	}

	@Override

	public boolean isBottom() {
		return addressSet.isEmpty();
	}

	@Override public PV mapAddress(final F<Set<AbstractAddress>, Set<AbstractAddress>> mapper) {
		return new JValue(mapper.f(addressSet));
	}
}
