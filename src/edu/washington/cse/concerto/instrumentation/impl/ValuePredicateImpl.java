package edu.washington.cse.concerto.instrumentation.impl;

import soot.Type;
import edu.washington.cse.concerto.instrumentation.filter.TypeFilter;
import edu.washington.cse.concerto.instrumentation.filter.TypeFilterBuilder;
import edu.washington.cse.concerto.instrumentation.filter.ValueFilter;
import edu.washington.cse.concerto.instrumentation.filter.ValuePredicate;
import edu.washington.cse.concerto.instrumentation.filter.WrappedPredicate;
import edu.washington.cse.concerto.instrumentation.forms.RuntimeValue;
import fj.F;


public class ValuePredicateImpl<T> implements ValuePredicate<T> {
	private final FilterAcceptor<T, RuntimeValue> returnPoint;
	public final TypeFilter tf;
	public final WrappedPredicate wp;

	public ValuePredicateImpl(final FilterAcceptor<T, RuntimeValue> ret) {
		this.returnPoint = ret;
		this.wp = null;
		this.tf= null;
	}
	
	public ValuePredicateImpl(final FilterAcceptor<T, RuntimeValue> ret, final TypeFilter tf, final WrappedPredicate wp) {
		this.returnPoint = ret;
		this.wp = wp;
		this.tf = tf;
	}

	@Override
	public T build() {
		return returnPoint.accept(new ValueFilter<RuntimeValue>() {
			@Override
			public boolean test(final RuntimeValue value) {
				final Type t = value.getType();
				if(tf != null) {
					final boolean res = tf.accept(t);
					if(!res) {
						return false;
					}
				}
				if(wp != null) {
					if(!value.hasValue()) {
						return false;
					}
					return wp.accept(value.getValue(), value.getHeap(), value.getForeignHeap());
				}
				return true;
			}
		});
	}

	@Override
	public TypeFilterBuilder<ValuePredicate<T>> typeFilter() {
		return new TypeFilterBuilder<>(new F<TypeFilter, ValuePredicate<T>>() {
			@Override
			public ValuePredicate<T> f(final TypeFilter tf) {
				return new ValuePredicateImpl<>(returnPoint, tf, wp);
			}
		});
	}

	@Override
	public ValuePredicate<T> filter(final WrappedPredicate wp) {
		return new ValuePredicateImpl<>(returnPoint, tf, wp);
	}
}
