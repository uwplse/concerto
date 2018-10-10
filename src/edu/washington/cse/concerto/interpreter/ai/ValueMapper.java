package edu.washington.cse.concerto.interpreter.ai;

public interface ValueMapper<AVal, AState, R> extends ValueTransfomer<AVal, AState, R, R, R, HeapReader<AState, AVal>>, Merger<R> {
	
}
