package edu.washington.cse.concerto.instrumentation;

public interface ActionHandler<T> {
	public void withAction(T action);
}
