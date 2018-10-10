package edu.washington.cse.concerto.instrumentation.filter;

public interface MethodFilter<Return> {
	public Return build();
	public TypeFilterBuilder<MethodFilter<Return>> returnType();
	public TypeFilterBuilder<MethodFilter<Return>> declaringType();
	public TypeFilterBuilder<MethodFilter<Return>> argType(int i);
	public MethodFilter<Return> is(String sig);
	public MethodFilter<Return> subSigIs(String subSig);
	MethodFilter<Return> name(String name);
}
