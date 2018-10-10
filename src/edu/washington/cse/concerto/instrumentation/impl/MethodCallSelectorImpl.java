package edu.washington.cse.concerto.instrumentation.impl;

import edu.washington.cse.concerto.instrumentation.MethodCallSelector;
import edu.washington.cse.concerto.instrumentation.filter.MethodFilter;
import edu.washington.cse.concerto.instrumentation.filter.ValueFilter;
import edu.washington.cse.concerto.instrumentation.filter.ValuePredicate;
import edu.washington.cse.concerto.instrumentation.forms.MethodCall;
import edu.washington.cse.concerto.instrumentation.forms.RuntimeValue;
import fj.F;
import fj.data.Seq;

public class MethodCallSelectorImpl<T> implements MethodCallSelector<T> {
	private final Seq<ValueFilter<MethodCall>> filters;
	private final F<Seq<ValueFilter<MethodCall>>, T> finishCb;
	
	public MethodCallSelectorImpl(final F<Seq<ValueFilter<MethodCall>>, T> finishCb) {
		filters = Seq.empty();
		this.finishCb = finishCb;
	}
	
	public MethodCallSelectorImpl(final Seq<ValueFilter<MethodCall>> filters, final F<Seq<ValueFilter<MethodCall>>, T> finishCb) {
		this.filters = filters;
		this.finishCb = finishCb;
	}

	private MethodCallSelectorImpl<T> withFilter(final ValueFilter<MethodCall> f) {
		return new MethodCallSelectorImpl<>(filters.cons(f), this.finishCb);
	}

	@Override
	public MethodFilter<MethodCallSelector<T>> methodFilter() {
		return new MethodFilterImpl<>(new FilterAcceptor<MethodCallSelector<T>, MethodCall>() {
			@Override
			public MethodCallSelector<T> accept(final ValueFilter<MethodCall> filter) {
				return withFilter(filter);
			}
		});
	}

	@Override
	public T build() {
		return this.finishCb.f(filters);
	}

	@Override
	public ValuePredicate<MethodCallSelector<T>> basePointerFilter() {
		return new ValuePredicateImpl<>(new FilterAcceptor<MethodCallSelector<T>, RuntimeValue>() {
			@Override
			public MethodCallSelector<T> accept(final ValueFilter<RuntimeValue> accept) {
				return withFilter(new ValueFilter<MethodCall>() {
					@Override
					public boolean test(final MethodCall value) {
						return accept.test(value.getBasePointerValue());
					}
				});
			}
		});
	}

	@Override
	public ValuePredicate<MethodCallSelector<T>> argumentIs(final int num) {
		return new ValuePredicateImpl<>(new FilterAcceptor<MethodCallSelector<T>, RuntimeValue>() {
			@Override
			public MethodCallSelector<T> accept(final ValueFilter<RuntimeValue> accept) {
				return withFilter(new ValueFilter<MethodCall>() {
					@Override
					public boolean test(final MethodCall value) {
						return accept.test(value.getArgValue(num));
					}
				});
			}
		});
	}
}