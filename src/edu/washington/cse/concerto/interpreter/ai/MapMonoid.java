package edu.washington.cse.concerto.interpreter.ai;

public interface MapMonoid<Ret> extends Merger<Ret> {
	public Ret zero();
}
