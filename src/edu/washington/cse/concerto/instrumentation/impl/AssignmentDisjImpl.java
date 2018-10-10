package edu.washington.cse.concerto.instrumentation.impl;

import java.util.List;

import edu.washington.cse.concerto.instrumentation.AssignmentDisjunctionSelector;
import edu.washington.cse.concerto.instrumentation.AssignmentSelector;
import edu.washington.cse.concerto.instrumentation.actions.AssignmentAction;
import edu.washington.cse.concerto.instrumentation.filter.ValueFilter;
import edu.washington.cse.concerto.instrumentation.forms.Assignment;
import fj.F;
import fj.data.Seq;

public class AssignmentDisjImpl<AVal, AHeap, AState> extends DisjunctionChain<Assignment, AssignmentSelector<AssignmentDisjunctionSelector<AVal, AHeap, AState>>,
	AssignmentDisjunctionSelector<AVal, AHeap, AState>, AssignmentAction<AVal, AHeap, AState>>
	implements AssignmentDisjunctionSelector<AVal, AHeap, AState> {

	public AssignmentDisjImpl(final Seq<Seq<ValueFilter<Assignment>>> s, final List<FilterAndAction<Assignment, AssignmentAction<AVal, AHeap, AState>>> outList) {
		super(s, outList);
	}
	
	public AssignmentDisjImpl(final List<FilterAndAction<Assignment, AssignmentAction<AVal, AHeap, AState>>> outList) {
		super(outList);
	}

	@Override
	protected AssignmentSelector<AssignmentDisjunctionSelector<AVal, AHeap, AState>> makeCase(final F<Seq<ValueFilter<Assignment>>, AssignmentDisjunctionSelector<AVal, AHeap, AState>> f) {
		return new AssignmentSelectorImpl<>(f);
	}

	@Override
	protected AssignmentDisjunctionSelector<AVal, AHeap, AState> withSeq(final Seq<Seq<ValueFilter<Assignment>>> s, final List<FilterAndAction<Assignment, AssignmentAction<AVal, AHeap, AState>>> outList) {
		return new AssignmentDisjImpl<>(s, outList);
	}
}