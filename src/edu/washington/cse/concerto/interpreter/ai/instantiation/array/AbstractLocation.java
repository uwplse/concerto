package edu.washington.cse.concerto.interpreter.ai.instantiation.array;

import com.google.common.annotations.VisibleForTesting;
import fj.F2;
import fj.Ord;
import fj.Ordering;
import soot.NullType;
import soot.Type;
import soot.Value;
import soot.jimple.NullConstant;
import soot.util.MapNumberer;
import soot.util.Numberer;

public class AbstractLocation {
	public static final Ord<AbstractLocation> LOCATION_ORDER = Ord.ord(new F2<AbstractLocation, AbstractLocation, Ordering>() {
		private final Numberer<Value> allocationNumberer = new MapNumberer<Value>();
		
		@Override
		public Ordering f(final AbstractLocation a, final AbstractLocation b) {
			if(a == NULL_LOCATION) {
				if(b == NULL_LOCATION) {
					return Ordering.EQ;
				} else {
					return Ordering.LT;
				}
			}
			if(b == NULL_LOCATION) {
				return Ordering.GT;
			}
			final int typeComp = a.type.toString().compareTo(b.type.toString());
			if(typeComp != 0) {
				return Ordering.fromInt(typeComp);
			}
			final int allocCmp = getOrAddValueNumber(a.allocationExpr) - getOrAddValueNumber(b.allocationExpr);
			if(allocCmp != 0) {
				return Ordering.fromInt(allocCmp);
			}
			return CallSite.SITE_ORDER.compare(a.allocContext, b.allocContext);
		}
		
		private int getOrAddValueNumber(final Value v) {
			allocationNumberer.add(v);
			return (int) allocationNumberer.get(v);
		}
	});
	
	@Override
	public String toString() {
		return "[" + type + "@" + allocationExpr + " in " + allocContext.method + "]";
	}
	
	public static final AbstractLocation NULL_LOCATION = new AbstractLocation(NullType.v(), null, NullConstant.v()) {
		@Override
		public String toString() {
			return "[NULL]";
		};
	};

	@VisibleForTesting
	public final Type type;
	private final Value allocationExpr;
	private final CallSite allocContext;

	public AbstractLocation(final Type t, final CallSite allocContext, final Value allocationExpr) {
		this.type = t;
		this.allocContext = allocContext;
		this.allocationExpr = allocationExpr;
	}

}
