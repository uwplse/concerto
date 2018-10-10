package edu.washington.cse.concerto.interpreter.ai;

import fj.Ord;
import fj.data.Option;
import fj.data.Set;

public class Continuation<C> {
	private final Set<Option<C>> returnSites;
	public final int version;

	public Continuation(final Set<Option<C>> returnSites, final int version) {
		this.returnSites = returnSites;
		this.version = version;
	}

	public static <C> boolean lessEqual(final Continuation<C> first, final Continuation<C> second) {
		return first.returnSites.subsetOf(second.returnSites) && first.version <= second.version;
	}

	public static <C> Continuation<C> join(final Continuation<C> first, final Continuation<C> second) {
		return new Continuation<>(first.returnSites.union(second.returnSites), Math.max(first.version, second.version));
	}

	public static <C> Continuation<C> widen(final Continuation<C> first, final Continuation<C> second) {
		return join(first, second);
	}

	public static <C> Continuation<C> bottom(final Ord<C> ord) {
		return new Continuation<>(Set.empty(Ord.optionOrd(ord)), 0);
	}

	@Override public String toString() {
		return "Continuation{" + "returnSites=" + returnSites + ", version=" + version + '}';
	}

	public Continuation<C> gtThan(final Continuation<C> cont) {
		return new Continuation<>(this.returnSites, cont.version + 1);
	}
}
