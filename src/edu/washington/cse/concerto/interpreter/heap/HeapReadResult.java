package edu.washington.cse.concerto.interpreter.heap;

import fj.F2;

public class HeapReadResult<V> extends HeapAccessResult {
	public final V value;
	public HeapReadResult(final V res, final HeapFaultStatus npe, final HeapFaultStatus oob) {
		super(npe, oob);
		this.value = res;
	}
	public HeapReadResult(final HeapFaultStatus[] flags) {
		this(null, flags[0], flags[1]);
	}
	public HeapReadResult(final V lift, final HeapFaultStatus[] flags) {
		this(lift, flags[0], flags[1]);
	}
	public HeapReadResult(final V value, final HeapAccessResult status) {
		this(value, status.npe, status.oob);
	}

	public HeapReadResult<V> merge(final HeapReadResult<V> other, final F2<V, V, V> join) {
		final HeapFaultStatus joinedNpe = other.npe.joinWith(this.npe);
		final HeapFaultStatus joinedOob = other.oob.joinWith(this.oob);
		if(this.value == null) {
			assert joinedNpe == HeapFaultStatus.MUST || joinedOob == HeapFaultStatus.MUST || other.value != null;
			return new HeapReadResult<>(other.value, joinedNpe, joinedOob);
		} else if(other.value == null) {
			assert joinedNpe == HeapFaultStatus.MUST || joinedOob == HeapFaultStatus.MUST || this.value != null;
			return new HeapReadResult<>(this.value, joinedNpe, joinedOob);
		} else {
			assert this.value != null && other.value != null;
			return new HeapReadResult<>(join.f(this.value, other.value), joinedNpe, joinedOob);
		}
	}
}
