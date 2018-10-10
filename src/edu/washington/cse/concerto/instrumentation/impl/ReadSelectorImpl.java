package edu.washington.cse.concerto.instrumentation.impl;

import edu.washington.cse.concerto.instrumentation.FieldReadSelector;
import edu.washington.cse.concerto.instrumentation.filter.ValueFilter;
import edu.washington.cse.concerto.instrumentation.forms.HeapAccess;
import fj.F;
import fj.data.Seq;

public class ReadSelectorImpl<T> extends HeapAccessSelectorImpl<T, FieldReadSelector<T>> implements FieldReadSelector<T> {
	public ReadSelectorImpl(final F<Seq<ValueFilter<HeapAccess>>, T> finishCb) {
		super(finishCb);
	}
	
	public ReadSelectorImpl(final Seq<ValueFilter<HeapAccess>> filters, final F<Seq<ValueFilter<HeapAccess>>, T> finishCb) {
		super(filters, finishCb);
	}

	@Override
	protected ReadSelectorImpl<T> deriveSelf(final Seq<ValueFilter<HeapAccess>> filters, final F<Seq<ValueFilter<HeapAccess>>, T> finish) {
		return new ReadSelectorImpl<>(filters, finish);
	}
}