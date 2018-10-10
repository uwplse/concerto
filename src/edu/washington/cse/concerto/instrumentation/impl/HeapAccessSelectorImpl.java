package edu.washington.cse.concerto.instrumentation.impl;

import edu.washington.cse.concerto.instrumentation.filter.FieldFilter;
import edu.washington.cse.concerto.instrumentation.filter.ValueFilter;
import edu.washington.cse.concerto.instrumentation.filter.ValuePredicate;
import edu.washington.cse.concerto.instrumentation.forms.HeapAccess;
import edu.washington.cse.concerto.instrumentation.forms.RuntimeValue;
import fj.F;
import fj.data.Seq;

public abstract class HeapAccessSelectorImpl<T, Self> {
	private final Seq<ValueFilter<HeapAccess>> filters;
	private final F<Seq<ValueFilter<HeapAccess>>, T> finishCb;

	public HeapAccessSelectorImpl(final F<Seq<ValueFilter<HeapAccess>>, T> finishCb) {
		this.finishCb = finishCb;
		this.filters = Seq.<ValueFilter<HeapAccess>>empty();
	}
	
	public HeapAccessSelectorImpl(final Seq<ValueFilter<HeapAccess>> filters, final F<Seq<ValueFilter<HeapAccess>>, T> finishCb) {
		this.filters = filters;
		this.finishCb = finishCb;
	}
	
	protected abstract Self deriveSelf(Seq<ValueFilter<HeapAccess>> filters, F<Seq<ValueFilter<HeapAccess>>, T> finish);

	public FieldFilter<Self> fieldFilter() {
		return new FieldFilterImpl<>(new FilterAcceptor<Self, HeapAccess>() {
			@Override
			public Self accept(final ValueFilter<HeapAccess> accept) {
				return deriveSelf(filters.cons(accept), finishCb);
			}
		});
	}

	public ValuePredicate<Self> basePointerFilter() {
		return new ValuePredicateImpl<>(new FilterAcceptor<Self, RuntimeValue>() {
			@Override
			public Self accept(final ValueFilter<RuntimeValue> accept) {
				final ValueFilter<HeapAccess> baseFilter = new ValueFilter<HeapAccess>() {
					@Override
					public boolean test(final HeapAccess value) {
						if(!value.hasBasePointer()) {
							return false;
						}
						return accept.test(value.getBasePointerValue());
					}
				};
				return deriveSelf(filters.cons(baseFilter), finishCb);
			}
		});
	}

	public T build() {
		return this.finishCb.f(this.filters);
	}
}
