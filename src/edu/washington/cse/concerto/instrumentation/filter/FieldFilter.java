package edu.washington.cse.concerto.instrumentation.filter;

public interface FieldFilter<Return> {
	public Return build();

	public FieldFilter<Return> name(String name);
	public FieldFilter<Return> is(String sig);
	
	public TypeFilterBuilder<FieldFilter<Return>> fieldTypeFilter();
	public TypeFilterBuilder<FieldFilter<Return>> declaringTypeFilter();
}
