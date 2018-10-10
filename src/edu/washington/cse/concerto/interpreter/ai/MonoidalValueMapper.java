package edu.washington.cse.concerto.interpreter.ai;

public interface MonoidalValueMapper<AVal, AState, R> extends ValueMapper<AVal, AState, R>, MapMonoid<R> {
	
}
