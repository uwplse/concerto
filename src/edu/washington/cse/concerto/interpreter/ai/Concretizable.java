package edu.washington.cse.concerto.interpreter.ai;

public interface Concretizable {
	public boolean concretizable();
	// return a stream of native representations of strings or integers
	public Iterable<Object> concretize(); 
}
