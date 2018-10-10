package edu.washington.cse.concerto.instrumentation.impl;

import java.util.List;

import edu.washington.cse.concerto.instrumentation.filter.ValueFilter;
import fj.F;
import fj.data.Seq;
import fj.function.Effect1;

abstract class DisjunctionChain<ValueForm, ReturnValue, SelfType, ActionType> {
	public Seq<Seq<ValueFilter<ValueForm>>> chain;
	private final List<FilterAndAction<ValueForm, ActionType>> l;
	
	protected DisjunctionChain(final List<FilterAndAction<ValueForm, ActionType>> l) {
		this.l = l;
		this.chain = Seq.empty();
	}
	
	protected DisjunctionChain(final Seq<Seq<ValueFilter<ValueForm>>> chain, final List<FilterAndAction<ValueForm, ActionType>> l) {
		this.l = l;
		this.chain = chain;
	}
	
	public void withAction(final ActionType o) {
		chain.toStream().foreachDoEffect(new Effect1<Seq<ValueFilter<ValueForm>>>() {
			@Override
			public void f(final Seq<ValueFilter<ValueForm>> a) {
				l.add(new FilterAndAction<>(a, o));
			}
		});
	}
	
	public ReturnValue cases() {
		return makeCase(new F<Seq<ValueFilter<ValueForm>>, SelfType>() {
			@Override
			public SelfType f(final Seq<ValueFilter<ValueForm>> a) {
				return withSeq(chain.cons(a), l);
			}
		});
	}

	protected abstract ReturnValue makeCase(F<Seq<ValueFilter<ValueForm>>, SelfType> f);
	protected abstract SelfType withSeq(Seq<Seq<ValueFilter<ValueForm>>> s, List<FilterAndAction<ValueForm, ActionType>> outList);
}