package edu.washington.cse.concerto.interpreter.ai;

import fj.F2;
import fj.Ord;
import fj.Ordering;
import soot.Scene;
import soot.Unit;
import soot.util.Numberer;

public class UnitOrd {
	public static final Ord<Unit> unitOrdering = Ord.ord(new F2<Unit, Unit, Ordering>() {
		Numberer<Unit> unitNumberer = Scene.v().getUnitNumberer();
		@Override public Ordering f(final Unit a, final Unit b) {
			unitNumberer.add(a);
			unitNumberer.add(b);
			return Ordering.fromInt((int) (unitNumberer.get(a) - unitNumberer.get(b)));
		}
	});
}
