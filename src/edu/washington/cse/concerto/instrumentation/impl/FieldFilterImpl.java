package edu.washington.cse.concerto.instrumentation.impl;

import soot.Type;
import edu.washington.cse.concerto.instrumentation.filter.FieldFilter;
import edu.washington.cse.concerto.instrumentation.filter.TypeFilter;
import edu.washington.cse.concerto.instrumentation.filter.TypeFilterBuilder;
import edu.washington.cse.concerto.instrumentation.filter.ValueFilter;
import edu.washington.cse.concerto.instrumentation.forms.HeapAccess;
import edu.washington.cse.concerto.instrumentation.forms.HeapLocation;
import fj.F;
import fj.data.Seq;


public class FieldFilterImpl<Return> implements FieldFilter<Return> {
	private final FilterAcceptor<Return, HeapAccess> acceptor;
	private final Seq<TypeFilter> declaringFilters;
	private final Seq<TypeFilter> fieldFilters;
	private final String name;
	private final String sig;

	public FieldFilterImpl(final FilterAcceptor<Return, HeapAccess> acceptor,
			final Seq<TypeFilter> declFilter, final Seq<TypeFilter> fieldFilters,
			final String name, final String sig) {
		this.acceptor = acceptor;
		this.declaringFilters = declFilter;
		this.fieldFilters = fieldFilters;
		this.name = name;
		this.sig = sig;
	}
	
	public FieldFilterImpl(final FilterAcceptor<Return, HeapAccess> acceptor) {
		this.acceptor = acceptor;
		this.declaringFilters = Seq.empty();
		this.fieldFilters = Seq.empty();
		this.name = null;
		this.sig = null;
	}

	@Override
	public FieldFilterImpl<Return> name(final String name) {
		return new FieldFilterImpl<>(acceptor, declaringFilters, fieldFilters, name, sig);
	}
	
	@Override
	public TypeFilterBuilder<FieldFilter<Return>> declaringTypeFilter() {
		return new TypeFilterBuilder<>(new F<TypeFilter, FieldFilter<Return>>() {
			@Override
			public FieldFilter<Return> f(final TypeFilter a) {
				return new FieldFilterImpl<>(acceptor, declaringFilters.cons(a), fieldFilters, name, sig);
			}
		});
	}
	
	@Override
	public TypeFilterBuilder<FieldFilter<Return>> fieldTypeFilter() {
		return new TypeFilterBuilder<>(new F<TypeFilter, FieldFilter<Return>>() {
			@Override
			public FieldFilter<Return> f(final TypeFilter a) {
				return new FieldFilterImpl<>(acceptor, declaringFilters, fieldFilters.cons(a), name, sig);
			}
		});
	}
	
	@Override
	public FieldFilter<Return> is(final String sig) {
		return new FieldFilterImpl<>(acceptor, declaringFilters, fieldFilters, name, sig);
	}
	
	public FieldFilter<Return> isArray() {
		return new FieldFilterImpl<>(acceptor, declaringFilters, fieldFilters, "*", null);
	}

	@Override
	public Return build() {
		return this.acceptor.accept(new ValueFilter<HeapAccess>() {
			@Override
			public boolean test(final HeapAccess value) {
				final HeapLocation field = value.getField();
				if(sig != null) {
					if(!sig.equals(field.getSignature())) {
						return false;
					}
				}
				if(declaringFilters.isNotEmpty()) {
					final Type t = field.hostType();
					final boolean filterTest = declaringFilters.toStream().forall(new F<TypeFilter, Boolean>() {
						@Override
						public Boolean f(final TypeFilter a) {
							return a.accept(t);
						}
					});
					if(!filterTest) {
						return false;
					}
				}
				if(fieldFilters.isNotEmpty()) {
					final Type t = field.getResultType();
					final boolean filterTest = declaringFilters.toStream().forall(new F<TypeFilter, Boolean>() {
						@Override
						public Boolean f(final TypeFilter a) {
							return a.accept(t);
						}
					});
					if(!filterTest) {
						return false;
					}
				}
				if(name != null) {
					if(!name.equals(field.getName())) {
						return false;
					}
				}
				return true;
			}
		});
	}

}
