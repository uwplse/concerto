package edu.washington.cse.concerto.interpreter.ai.instantiation.pta;

import edu.washington.cse.concerto.interpreter.ai.UnitOrd;
import fj.Ord;
import fj.Ordering;
import soot.SootMethod;
import soot.Unit;

public class ReturnSite {
	public final SootMethod method;
	public final Unit unit;

	public ReturnSite(final SootMethod method, final Unit u) {
		this.method = method;
		this.unit = u;
	}

	public ReturnSite(final SootMethod method) {
		this(method, null);
	}

	public static Ord<ReturnSite> contiuationOrd = Ord.ord((a, b) -> {
		int sigCmp = a.method.getSignature().compareTo(b.method.getSignature());
		if(sigCmp != 0) {
			return Ordering.fromInt(sigCmp);
		}
		return UnitOrd.unitOrdering.compare(a.unit, b.unit);
	});

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((method == null) ? 0 : method.hashCode());
		result = prime * result + ((unit == null) ? 0 : unit.hashCode());
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
		final ReturnSite other = (ReturnSite) obj;
		if(method == null) {
			if(other.method != null) {
				return false;
			}
		} else if(!method.equals(other.method)) {
			return false;
		}
		if(unit == null) {
			if(other.unit != null) {
				return false;
			}
		} else if(!unit.equals(other.unit)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "ReturnSite [method=" + method + ", unit=" + unit + "]";
	}
}
