package edu.washington.cse.concerto.interpreter.value;

public interface IValueAction {
	default public void nondet() { }
	public void accept(IValue v, boolean isMulti);
}
