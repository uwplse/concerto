package edu.washington.cse.concerto.instrumentation.impl;

import edu.washington.cse.concerto.instrumentation.filter.ValueFilter;
import fj.data.Seq;

public class FilterAndAction<T, Act> {
	public final Seq<ValueFilter<T>> filters;
	public final Act action;

	public FilterAndAction(final Seq<ValueFilter<T>> f, final Act a) {
		this.filters = f;
		this.action = a;
	}
}
