package edu.washington.cse.concerto.interpreter.value;

import java.util.Set;

public interface ValueMerger {
	public static final int MAX_WIDEN_SIZE = 2;

	public IValue merge(final IValue v1, final IValue v2);
	public IValue mergeAll(Set<IValue> values);
	
	public static final ValueMerger STRICT_MERGE = new ValueMerger() {
		@Override
		public IValue merge(final IValue v1, final IValue v2) {
			return IValue.merge(v1, v2);
		}
		
		@Override
		public IValue mergeAll(final Set<IValue> values) {
			return IValue.lift(values);
		}
	}; 
	
	public static final ValueMerger WIDENING_MERGE = new ValueMerger() {
		@Override
		public IValue merge(final IValue v1, final IValue v2) {
			if(v1 == null) {
				return v2;
			} else if(v2 == null) {
				return v1;
			} else if(v1.equals(v2)) {
				return v1;
			} else if(v1.isEmbedded() && v2.isEmbedded()) {
				return IValue.lift(v1.aVal.monad.widen(v1.aVal.value, v2.aVal.value), v1.aVal.monad);
			} else if(v1.isEmbedded()) {
				return IValue.lift(v1.aVal.monad.widen(v1.aVal.value, v2), v1.aVal.monad);
			} else if(v2.isEmbedded()) {
				return IValue.lift(v2.aVal.monad.widen(v1, v2.aVal.value), v2.aVal.monad);
			} else if(v1.isHeapValue() || v2.isHeapValue() || v1.isMultiHeap() || v2.isMultiHeap()) {
				return IValue.merge(v1, v2);
			} else {
				final IValue v = IValue.merge(v1, v2);
				if(v.isMulti() && v.variantSize() > MAX_WIDEN_SIZE) {
					return IValue.nondet(); 
				} else {
					return v;
				}
			}
		}
		
		@Override
		public IValue mergeAll(final Set<IValue> values) {
			final IValue v = IValue.lift(values);
			if(v.isMultiHeap()) {
				return v;
			} else if(v.isMulti() && v.variantSize() > MAX_WIDEN_SIZE) {
				return IValue.nondet();
			} else {
				return v;
			}
		}
	};
}