package edu.washington.cse.concerto.instrumentation.impl;

import edu.washington.cse.concerto.instrumentation.AssignmentSelector;
import edu.washington.cse.concerto.instrumentation.filter.ValueFilter;
import edu.washington.cse.concerto.instrumentation.filter.ValuePredicate;
import edu.washington.cse.concerto.instrumentation.forms.Assignment;
import edu.washington.cse.concerto.instrumentation.forms.RuntimeValue;
import fj.F;
import fj.data.Seq;

public class AssignmentSelectorImpl<T> implements AssignmentSelector<T> {
	private final Seq<ValueFilter<Assignment>> filters;
	private final F<Seq<ValueFilter<Assignment>>, T> finishCb;
	
	public AssignmentSelectorImpl(final F<Seq<ValueFilter<Assignment>>, T> finishCb) {
		this.finishCb = finishCb;
		filters = Seq.empty();
	}
	
	public AssignmentSelectorImpl(final F<Seq<ValueFilter<Assignment>>, T> finishCb, final Seq<ValueFilter<Assignment>> filters) {
		this.finishCb = finishCb;
		this.filters = filters;
	}
	
	@Override
	public ValuePredicate<AssignmentSelector<T>> leftOp() {
		return new ValuePredicateImpl<AssignmentSelector<T>>(new FilterAcceptor<AssignmentSelector<T>, RuntimeValue>() {
			@Override
			public AssignmentSelector<T> accept(final ValueFilter<RuntimeValue> accept) {
				final ValueFilter<Assignment> toFilter = new ValueFilter<Assignment>() {
					@Override
					public boolean test(final Assignment value) {
						return accept.test(value.getLeftValue());
					}
				};
				return new AssignmentSelectorImpl<T>(finishCb, filters.cons(toFilter));
			}
		});
	}

	@Override
	public ValuePredicate<AssignmentSelector<T>> rightOp() {
		return new ValuePredicateImpl<AssignmentSelector<T>>(new FilterAcceptor<AssignmentSelector<T>, RuntimeValue>() {
			@Override
			public AssignmentSelector<T> accept(final ValueFilter<RuntimeValue> accept) {
				final ValueFilter<Assignment> toFilter = new ValueFilter<Assignment>() {
					@Override
					public boolean test(final Assignment value) {
						return accept.test(value.getRightValue());
					}
				};
				return new AssignmentSelectorImpl<T>(finishCb, filters.cons(toFilter));
			}
		});
	}

	@Override
	public T build() {
		return finishCb.f(filters);
	}

}
