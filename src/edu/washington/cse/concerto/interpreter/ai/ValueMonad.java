package edu.washington.cse.concerto.interpreter.ai;

import edu.washington.cse.concerto.interpreter.lattice.Lattice;
import edu.washington.cse.concerto.interpreter.value.IValue;


public interface ValueMonad<AVal> extends Lattice<Object> {
	public AVal alpha(Object o);
	public Object lift(AVal toLift);
	public Object lift(IValue downCast);
	public String toString(Object a);
}
