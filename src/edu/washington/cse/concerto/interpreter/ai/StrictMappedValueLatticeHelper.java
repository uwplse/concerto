package edu.washington.cse.concerto.interpreter.ai;

import edu.washington.cse.concerto.interpreter.lattice.Lattice;
import fj.data.Option;
import fj.data.TreeMap;

public class StrictMappedValueLatticeHelper<K, V> implements Lattice<TreeMap<K, V>> {
	private final Lattice<V> valueMonad;
	private final V implicitMapping;

	public StrictMappedValueLatticeHelper(final Lattice<V> valueMonad, final V implicitMapping) {
		this.valueMonad = valueMonad;
		this.implicitMapping = implicitMapping;
	}

	@Override
	public TreeMap<K, V> widen(final TreeMap<K, V> prev, final TreeMap<K, V> next) {
		return this.widenMap(prev, next);
	}

	@Override
	public TreeMap<K, V> join(final TreeMap<K, V> first, final TreeMap<K, V> second) {
		return this.joinMap(first, second);
	}
	
	@Override
	public boolean lessEqual(final TreeMap<K, V> first, final TreeMap<K, V> second) {
		return this.lessEqualMap(first, second);
	}
	
	private TreeMap<K, V> widenMap(final TreeMap<K, V> prev, final TreeMap<K, V> next) {
		final TreeMap<K, V> foldedPrev = prev.toStream().foldLeft(
			(accum, prevKV) -> accum.update(prevKV._1(), a -> valueMonad.widen(prevKV._2(), a), valueMonad.join(prevKV._2(), implicitMapping)),
			next);
		return next.toStream().filter(kv -> !prev.contains(kv._1())).foldLeft((accum, nextKV) -> accum.set(nextKV._1(), valueMonad.widen(implicitMapping, nextKV._2())), foldedPrev);
	}

	private TreeMap<K, V> joinMap(final TreeMap<K, V> first, final TreeMap<K, V> second) {
		final TreeMap<K, V> foldedPrev = first.toStream().foldLeft(
				(accum, prevKV) -> accum.update(prevKV._1(), a -> valueMonad.join(a, prevKV._2()), valueMonad.join(implicitMapping, prevKV._2())),
				second);
		return second.toStream().filter(kv -> !first.contains(kv._1())).foldLeft((accum, nextKV) -> accum.set(nextKV._1(), valueMonad.join(nextKV._2(), implicitMapping)), foldedPrev);
	}

	private boolean lessEqualMap(final TreeMap<K, V> first, final TreeMap<K, V> second) {
		return first.toStream().forall(a -> {
			final Option<V> bind = second.get(a._1());
			if(bind.isNone()) {
				return valueMonad.lessEqual(a._2(), implicitMapping);
			}
			return valueMonad.lessEqual(a._2(), bind.some());
		}) && second.toStream().filter(kv -> !first.contains(kv._1())).forall(kv ->
			valueMonad.lessEqual(implicitMapping, kv._2())
		);
	}
}
