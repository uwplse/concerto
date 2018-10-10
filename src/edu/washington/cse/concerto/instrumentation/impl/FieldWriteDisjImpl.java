package edu.washington.cse.concerto.instrumentation.impl;

import java.util.List;

import edu.washington.cse.concerto.instrumentation.FieldWriteDisjunctionSelector;
import edu.washington.cse.concerto.instrumentation.FieldWriteSelector;
import edu.washington.cse.concerto.instrumentation.actions.FieldWriteAction;
import edu.washington.cse.concerto.instrumentation.filter.ValueFilter;
import edu.washington.cse.concerto.instrumentation.forms.HeapAccess;
import fj.F;
import fj.data.Seq;

public class FieldWriteDisjImpl<AVal, AHeap, AState> extends DisjunctionChain<HeapAccess, FieldWriteSelector<FieldWriteDisjunctionSelector<AVal, AHeap, AState>>,
	FieldWriteDisjunctionSelector<AVal, AHeap, AState>, FieldWriteAction<AVal, AHeap, AState>>
	implements FieldWriteDisjunctionSelector<AVal, AHeap, AState> {

	public FieldWriteDisjImpl(final Seq<Seq<ValueFilter<HeapAccess>>> s, final List<FilterAndAction<HeapAccess, FieldWriteAction<AVal, AHeap, AState>>> outList) {
		super(s, outList);
	}
	
	public FieldWriteDisjImpl(final List<FilterAndAction<HeapAccess, FieldWriteAction<AVal, AHeap, AState>>> outList) {
		super(outList);
	}

	@Override
	protected FieldWriteSelector<FieldWriteDisjunctionSelector<AVal, AHeap, AState>> makeCase(final F<Seq<ValueFilter<HeapAccess>>, FieldWriteDisjunctionSelector<AVal, AHeap, AState>> f) {
		return new WriteSelectorImpl<>(f);
	}

	@Override
	protected FieldWriteDisjunctionSelector<AVal, AHeap, AState> withSeq(final Seq<Seq<ValueFilter<HeapAccess>>> s, final List<FilterAndAction<HeapAccess, FieldWriteAction<AVal, AHeap, AState>>> outList) {
		return new FieldWriteDisjImpl<>(s, outList);
	}
	
}