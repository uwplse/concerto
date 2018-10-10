package edu.washington.cse.concerto;

public interface Function<Domain, Codomain> {
	public Codomain apply(Domain in);
}
