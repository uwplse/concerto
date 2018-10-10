package edu.washington.cse.concerto.interpreter.lattice;

import edu.washington.cse.concerto.interpreter.value.IValue;
import edu.washington.cse.concerto.interpreter.value.ValueMerger;

public class IValueLattice implements Lattice<IValue> {
	@Override
	public IValue widen(final IValue prev, final IValue next) {
		return ValueMerger.WIDENING_MERGE.merge(prev, next);
	}

	@Override
	public IValue join(final IValue first, final IValue second) {
		return ValueMerger.STRICT_MERGE.merge(first, second);
	}

	@Override
	public boolean lessEqual(final IValue first, final IValue second) {
		return first.lessEqual(second);
	}
}
