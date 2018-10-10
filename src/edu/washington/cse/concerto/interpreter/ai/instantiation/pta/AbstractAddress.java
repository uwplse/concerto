package edu.washington.cse.concerto.interpreter.ai.instantiation.pta;

import fj.F0;
import fj.Ord;
import fj.Ordering;
import soot.NullType;
import soot.Type;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class AbstractAddress {
	public final Type t;
	public final int order;

	public static final Ord<AbstractAddress> ADDRESS_ORDER = Ord.<AbstractAddress>ord((final AbstractAddress a, final AbstractAddress b) -> Ordering.fromInt(a.order - b.order));
	private final static AtomicInteger atomInt = new AtomicInteger();
	private final static ConcurrentHashMap<Type, AbstractAddress> canonicalizationMap = new ConcurrentHashMap<>();
	private AbstractAddress(final Type type, final int order) {
		this.t = type;
		this.order = order;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((t == null) ? 0 : t.hashCode());
		return result;
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
		final AbstractAddress other = (AbstractAddress) obj;
		if(t == null) {
			if(other.t != null) {
				return false;
			}
		} else if(!t.equals(other.t)) {
			return false;
		}
		return true;
	}
	
	public static AbstractAddress getAddress(final Type t) {
		if(canonicalizationMap.containsKey(t)) {
			return canonicalizationMap.get(t);
		}
		final int order = atomInt.getAndIncrement();
		final AbstractAddress addr = new AbstractAddress(t, order);
		canonicalizationMap.putIfAbsent(t, addr);
		return canonicalizationMap.get(t);
	}

	public static final F0<AbstractAddress> NULL_ADDRESS = () -> AbstractAddress.getAddress(NullType.v());

	@Override
	public String toString() {
		return "{" + this.t + "}";
	}
	
	public static void reset() {
		canonicalizationMap.clear();
		atomInt.set(0);
	}
}