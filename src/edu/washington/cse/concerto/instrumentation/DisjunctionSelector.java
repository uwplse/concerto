package edu.washington.cse.concerto.instrumentation;

public interface DisjunctionSelector<Action> {
	public void withAction(Action o);
}
