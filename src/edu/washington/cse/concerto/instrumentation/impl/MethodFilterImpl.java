package edu.washington.cse.concerto.instrumentation.impl;

import edu.washington.cse.concerto.instrumentation.filter.MethodFilter;
import edu.washington.cse.concerto.instrumentation.filter.TypeFilter;
import edu.washington.cse.concerto.instrumentation.filter.TypeFilterBuilder;
import edu.washington.cse.concerto.instrumentation.filter.ValueFilter;
import edu.washington.cse.concerto.instrumentation.forms.MethodCall;
import fj.F;
import fj.data.Seq;
import soot.SootMethodRef;
import soot.Type;
import soot.toolkits.scalar.Pair;

public class MethodFilterImpl<T> implements MethodFilter<T> {

	private final FilterAcceptor<T, MethodCall> filterAcceptor;
	private final Seq<Pair<Integer, TypeFilter>> argFilters;
	private final Seq<TypeFilter> declaringFilters;
	private final Seq<TypeFilter> returnFilters;
	private final String subSig;
	private final String name;
	private final String sig;

	public MethodFilterImpl(final FilterAcceptor<T, MethodCall> filterAcceptor) {
		this.filterAcceptor = filterAcceptor;
		
		this.argFilters = Seq.empty();
		this.declaringFilters = Seq.empty();
		this.returnFilters = Seq.empty();
		this.name = null;
		this.subSig = null;
		this.sig = null;
	}
	
	public MethodFilterImpl(final FilterAcceptor<T, MethodCall> filterAcceptor,
		final Seq<Pair<Integer, TypeFilter>> argFilters, final Seq<TypeFilter> declaringFilters,
		final Seq<TypeFilter> returnFilters, final String name, final String subSig, final String sig) {
		this.filterAcceptor = filterAcceptor;
		this.argFilters = argFilters;
		this.declaringFilters = declaringFilters;
		this.returnFilters = returnFilters;
		this.name = name;
		this.subSig = subSig;
		this.sig = sig;
	}

	@Override
	public T build() {
		return filterAcceptor.accept(new ValueFilter<MethodCall>() {
			@Override
			public boolean test(final MethodCall value) {
				final SootMethodRef smr = value.getMethod();
				if(sig != null) {
					if(!smr.getSignature().equals(sig)) {
						return false;
					}
				}
				if(subSig != null) {
					if(!smr.getSubSignature().getString().equals(subSig)) {
						return false;
					}
				}
				if(name != null) {
					if(!smr.name().equals(name)) {
						return false;
					}
				}
				if(declaringFilters.isNotEmpty()) {
					if(!testTypes(smr.declaringClass().getType(), declaringFilters)) {
						return false;
					}
				}
				if(returnFilters.isNotEmpty()) {
					if(!testTypes(smr.returnType(), returnFilters)) {
						return false;
					}
				}
				if(argFilters.isNotEmpty()) {
					final boolean test = argFilters.toStream().forall(new F<Pair<Integer,TypeFilter>, Boolean>() {
						@Override
						public Boolean f(final Pair<Integer, TypeFilter> a) {
							final int pNum = a.getO1();
							if(pNum >= smr.parameterTypes().size()) {
								return false;
							}
							return a.getO2().accept(smr.parameterType(pNum));
						}
					});
					if(!test) {
						return false;
					}
				}
				return true;
			}

			private boolean testTypes(final Type t, final Seq<TypeFilter> typeFilter) {
				return typeFilter.toStream().forall(a -> a.accept(t));
			}
		});
	}

	@Override
	public MethodFilter<T> name(final String name) {
		return new MethodFilterImpl<>(filterAcceptor, argFilters, declaringFilters, returnFilters, name, subSig, sig);
	}

	@Override
	public TypeFilterBuilder<MethodFilter<T>> returnType() {
		return new TypeFilterBuilder<>(new F<TypeFilter, MethodFilter<T>>() {
			@Override
			public MethodFilter<T> f(final TypeFilter a) {
				return new MethodFilterImpl<>(filterAcceptor, argFilters, declaringFilters, returnFilters.cons(a), name, subSig, sig);
			}
		});
	}

	@Override
	public TypeFilterBuilder<MethodFilter<T>> declaringType() {
		return new TypeFilterBuilder<>(new F<TypeFilter, MethodFilter<T>>() {
			@Override
			public MethodFilter<T> f(final TypeFilter a) {
				return new MethodFilterImpl<>(filterAcceptor, argFilters, declaringFilters.cons(a), returnFilters, name, subSig, sig);
			}
			
		});
	}

	@Override
	public MethodFilter<T> is(final String sig) {
		return new MethodFilterImpl<>(filterAcceptor, argFilters, declaringFilters, returnFilters, name, subSig, sig);
	}
	
	@Override
	public MethodFilter<T> subSigIs(final String subSig) {
		return new MethodFilterImpl<>(filterAcceptor, argFilters, declaringFilters, returnFilters, name, subSig, sig);
	}


	@Override
	public TypeFilterBuilder<MethodFilter<T>> argType(final int i) {
		return new TypeFilterBuilder<>(new F<TypeFilter, MethodFilter<T>>() {
			@Override
			public MethodFilter<T> f(final TypeFilter a) {
				return new MethodFilterImpl<>(filterAcceptor, argFilters.cons(new Pair<>(i, a)), declaringFilters, returnFilters, name, subSig, sig);
			}
		});
	}

}
