package edu.washington.cse.concerto.interpreter.ai;


public interface MethodResultMonad<AVal, AS> {
	public MethodResult join(MethodResult o1, MethodResult o2);
	public MethodResult widen(MethodResult prev, MethodResult next);
	public boolean lessEqual(MethodResult o1, MethodResult o2);
}
