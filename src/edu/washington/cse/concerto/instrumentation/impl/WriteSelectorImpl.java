package edu.washington.cse.concerto.instrumentation.impl;

import edu.washington.cse.concerto.instrumentation.FieldWriteSelector;
import edu.washington.cse.concerto.instrumentation.filter.ValueFilter;
import edu.washington.cse.concerto.instrumentation.forms.HeapAccess;
import fj.F;
import fj.data.Seq;

public class WriteSelectorImpl<T> extends HeapAccessSelectorImpl<T, FieldWriteSelector<T>> implements FieldWriteSelector<T> {
	public WriteSelectorImpl(final F<Seq<ValueFilter<HeapAccess>>, T> finishCb) {
		super(finishCb);
	}
	
	public WriteSelectorImpl(final Seq<ValueFilter<HeapAccess>> filters, final F<Seq<ValueFilter<HeapAccess>>, T> finishCb) {
		super(filters, finishCb);
	}

	@Override
	protected WriteSelectorImpl<T> deriveSelf(final Seq<ValueFilter<HeapAccess>> filters, final F<Seq<ValueFilter<HeapAccess>>, T> finish) {
		return new WriteSelectorImpl<>(filters, finish);
	}
}