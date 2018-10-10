package edu.washington.cse.concerto.interpreter.value;

public interface IValueTransformer {
	public IValue transform(IValue v, boolean isMulti);
}