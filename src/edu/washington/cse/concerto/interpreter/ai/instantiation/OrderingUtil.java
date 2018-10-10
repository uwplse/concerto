package edu.washington.cse.concerto.interpreter.ai.instantiation;

import soot.util.MapNumberer;
import soot.util.Numberer;
import fj.F;
import fj.F2;
import fj.Ord;
import fj.Ordering;

public final class OrderingUtil {
	private OrderingUtil() { }
	
	public static <T> Ord<T> equalityOrdering() {
		final Numberer<T> numberer = new MapNumberer<T>();
		return Ord.<T>ord(new F2<T, T, Ordering>() {
			@Override
			public Ordering f(final T a, final T b) {
				if(a == b) {
					return Ordering.EQ;
				}
				return Ordering.fromInt(getNumber(a) - getNumber(b));
			}
			
			private int getNumber(final T l) {
				numberer.add(l);
				return (int) numberer.get(l);
			}
		});
	}
	
	public static <T> Ord<T> stringBasedOrdering(final F<T, String> toStringer) {
		return Ord.<T>ord(new F2<T, T, Ordering>() {
			@Override
			public Ordering f(final T a, final T b) {
				if(a == b) {
					return Ordering.EQ;
				}
				return Ordering.fromInt(toStringer.f(a).compareTo(toStringer.f(b)));
			}
		});
	}
	
	public static <T> Ord<T> stringBasedOrdering() {
		return stringBasedOrdering(new F<T, String>() {
			@Override
			public String f(final T a) {
				return a.toString();
			}
		});
	}
	
	public static Ordering tupled(final Ordering... ords) { 
		for(final Ordering ord: ords) {
			if(ord != Ordering.EQ) {
				return ord;
			}
		}
		return Ordering.EQ;
	}
}
