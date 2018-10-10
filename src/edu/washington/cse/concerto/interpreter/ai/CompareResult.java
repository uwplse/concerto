package edu.washington.cse.concerto.interpreter.ai;

import java.util.EnumSet;

import fj.Ordering;

public class CompareResult {
	public static final CompareResult LT = new CompareResult(EnumSet.of(Ordering.LT));
	public static final CompareResult LE = new CompareResult(EnumSet.of(Ordering.LT, Ordering.EQ));
	public static final CompareResult GT = new CompareResult(EnumSet.of(Ordering.GT));
	public static final CompareResult GE = new CompareResult(EnumSet.of(Ordering.GT, Ordering.EQ));
	public static final CompareResult EQ = new CompareResult(EnumSet.of(Ordering.EQ));
	public static final CompareResult NE = new CompareResult(EnumSet.of(Ordering.LT, Ordering.GT));
	public static final CompareResult NONDET = new CompareResult(EnumSet.allOf(Ordering.class));
	public static final CompareResult NO_RESULT = new CompareResult(EnumSet.noneOf(Ordering.class));
	private final EnumSet<Ordering> ord;
	
	private CompareResult(final EnumSet<Ordering> ord) {
		this.ord = ord;
	}

	public static CompareResult nondet() {
		return NONDET;
	}
	
	public boolean hasFlag(final Ordering o) {
		return ord.contains(o);
	}

	public static CompareResult fromInt(final int i) {
		if(i == 0) {
			return EQ; 
		} else if(i < 0) {
			return LT;
		} else {
			return GT;
		}
	}
	
	@Override
	public String toString() {
		return ord.toString();
	}
}
