package edu.washington.cse.concerto.interpreter.ai;

import edu.washington.cse.concerto.interpreter.lattice.Lattice;
import fj.data.Option;
import fj.data.TreeMap;

public class MappedValueLatticeHelper<K, V> implements Lattice<fj.data.TreeMap<K, V>> {
	private final Lattice<V> valueMonad;

	public MappedValueLatticeHelper(final Lattice<V> valueMonad) {
		this.valueMonad = valueMonad;
	}
	
	private fj.data.TreeMap<K, V> widenMap(final fj.data.TreeMap<K, V> prev, final fj.data.TreeMap<K, V> next) {
		return prev.toStream().foldLeft(
			(accum, prevKV) -> accum.update(prevKV._1(), a -> valueMonad.widen(prevKV._2(), a), prevKV._2()),
			next);
	}
	
	private fj.data.TreeMap<K, V> joinMap(final fj.data.TreeMap<K, V> first, final fj.data.TreeMap<K, V> second) {
		return first.toStream().foldLeft(
			(accum, kv) -> accum.update(kv._1(), a1 -> valueMonad.join(kv._2(), a1), kv._2()),
			second);
	}
	
	private boolean lessEqualMap(final fj.data.TreeMap<K, V> first, final fj.data.TreeMap<K, V> second) {
		return first.toStream().forall(a -> {
			final Option<V> bind = second.get(a._1());
			if(bind.isNone()) {
				return false;
			}
			return valueMonad.lessEqual(a._2(), bind.some());
		});
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
}
