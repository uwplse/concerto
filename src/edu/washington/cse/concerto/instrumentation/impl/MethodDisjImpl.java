package edu.washington.cse.concerto.instrumentation.impl;

import java.util.List;

import edu.washington.cse.concerto.instrumentation.MethodCallSelector;
import edu.washington.cse.concerto.instrumentation.MethodDisjunctionSelector;
import edu.washington.cse.concerto.instrumentation.actions.MethodCallAction;
import edu.washington.cse.concerto.instrumentation.filter.ValueFilter;
import edu.washington.cse.concerto.instrumentation.forms.MethodCall;
import fj.F;
import fj.data.Seq;

public class MethodDisjImpl<AVal, AHeap, AState> extends DisjunctionChain<MethodCall, MethodCallSelector<MethodDisjunctionSelector<AVal, AHeap, AState>>, MethodDisjunctionSelector<AVal, AHeap, AState>,
	MethodCallAction<AVal, AHeap, AState>> implements MethodDisjunctionSelector<AVal, AHeap, AState> {

	public MethodDisjImpl(final Seq<Seq<ValueFilter<MethodCall>>> s, final List<FilterAndAction<MethodCall, MethodCallAction<AVal, AHeap, AState>>> outList) {
		super(s, outList);
	}
	
	public MethodDisjImpl(final List<FilterAndAction<MethodCall, MethodCallAction<AVal, AHeap, AState>>> outList) {
		super(outList);
	}

	@Override
	protected MethodCallSelector<MethodDisjunctionSelector<AVal, AHeap, AState>> makeCase(final F<Seq<ValueFilter<MethodCall>>, MethodDisjunctionSelector<AVal, AHeap, AState>> f) {
		return new MethodCallSelectorImpl<>(f);
	}

	@Override
	protected MethodDisjunctionSelector<AVal, AHeap, AState> withSeq(final Seq<Seq<ValueFilter<MethodCall>>> s, final List<FilterAndAction<MethodCall, MethodCallAction<AVal, AHeap, AState>>> outList) {
		return new MethodDisjImpl<>(s, outList);
	}
}