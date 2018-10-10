package edu.washington.cse.concerto.instrumentation.impl;

import java.util.List;

import edu.washington.cse.concerto.instrumentation.FieldReadDisjunctionSelector;
import edu.washington.cse.concerto.instrumentation.FieldReadSelector;
import edu.washington.cse.concerto.instrumentation.actions.FieldReadAction;
import edu.washington.cse.concerto.instrumentation.filter.ValueFilter;
import edu.washington.cse.concerto.instrumentation.forms.HeapAccess;
import fj.F;
import fj.data.Seq;

public final class FieldReadDisjImpl<AVal, AHeap, AState> 
	extends DisjunctionChain<HeapAccess, FieldReadSelector<FieldReadDisjunctionSelector<AVal, AHeap, AState>>, FieldReadDisjunctionSelector<AVal, AHeap, AState>, FieldReadAction<AVal, AHeap, AState>>
	implements FieldReadDisjunctionSelector<AVal, AHeap, AState> {

	public FieldReadDisjImpl(final Seq<Seq<ValueFilter<HeapAccess>>> s, final List<FilterAndAction<HeapAccess, FieldReadAction<AVal, AHeap, AState>>> outList) {
		super(s, outList);
	}
	
	public FieldReadDisjImpl(final List<FilterAndAction<HeapAccess, FieldReadAction<AVal, AHeap, AState>>> outList) {
		super(outList);
	}

	@Override
	protected FieldReadSelector<FieldReadDisjunctionSelector<AVal, AHeap, AState>> makeCase(final F<Seq<ValueFilter<HeapAccess>>, FieldReadDisjunctionSelector<AVal, AHeap, AState>> f) {
		return new ReadSelectorImpl<>(f);
	}

	@Override
	protected FieldReadDisjunctionSelector<AVal, AHeap, AState> withSeq(final Seq<Seq<ValueFilter<HeapAccess>>> s, final List<FilterAndAction<HeapAccess, FieldReadAction<AVal, AHeap, AState>>> outList) {
		return new FieldReadDisjImpl<>(s, outList);
	}
}