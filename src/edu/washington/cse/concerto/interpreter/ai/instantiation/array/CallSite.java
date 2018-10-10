package edu.washington.cse.concerto.interpreter.ai.instantiation.array;

import fj.F2;
import fj.Ord;
import fj.Ordering;
import soot.SootMethod;
import soot.jimple.InvokeExpr;
import soot.util.MapNumberer;
import soot.util.Numberer;

public class CallSite {
	private static final CallSite INIT_CONTEXT = new CallSite(null, null);
	protected static final Ord<CallSite> SITE_ORDER = Ord.ord(new F2<CallSite, CallSite, Ordering>() {
		private final Numberer<InvokeExpr> invokeNumberer = new MapNumberer<InvokeExpr>();
		@Override
		public Ordering f(final CallSite a, final CallSite b) {
			if(a == INIT_CONTEXT && b == INIT_CONTEXT) {
				return Ordering.EQ;
			} else if(a == INIT_CONTEXT) {
				return Ordering.LT;
			} else if(b == INIT_CONTEXT) {
				return Ordering.GT;
			}
			final int cmp = a.method.getSignature().compareTo(b.method.getSignature());
			if(cmp != 0) {
				return Ordering.fromInt(cmp);
			}
			if((a.invokeExpr == null) != (b.invokeExpr == null)) {
				if(a.invokeExpr != null) {
					return Ordering.LT;
				} else {
					return Ordering.GT;
				}
			}
			if(a.invokeExpr == null) {
				return Ordering.EQ;
			}
			assert b.invokeExpr != null;
			return Ordering.fromInt(getOrAddInvokeNumber(a.invokeExpr) - getOrAddInvokeNumber(b.invokeExpr));
		}
		private int getOrAddInvokeNumber(final InvokeExpr v) {
			invokeNumberer.add(v);
			return (int) invokeNumberer.get(v);
		}
		
	});
	
	@Override
	public String toString() {
		return "CallSite [method=" + method + ", invokeExpr=" + invokeExpr + "]";
	}

	public final SootMethod method;
	public final InvokeExpr invokeExpr;

	public CallSite(final SootMethod targetMethod, final InvokeExpr hostUnit) {
		this.method = targetMethod;
		this.invokeExpr = hostUnit;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((invokeExpr == null) ? 0 : invokeExpr.hashCode());
		result = prime * result + ((method == null) ? 0 : method.hashCode());
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
		final CallSite other = (CallSite) obj;
		if(invokeExpr == null) {
			if(other.invokeExpr != null) {
				return false;
			}
		} else if(!invokeExpr.equals(other.invokeExpr)) {
			return false;
		}
		if(method == null) {
			if(other.method != null) {
				return false;
			}
		} else if(!method.equals(other.method)) {
			return false;
		}
		return true;
	}

	public static CallSite initContext() {
		return INIT_CONTEXT;
	}
}
