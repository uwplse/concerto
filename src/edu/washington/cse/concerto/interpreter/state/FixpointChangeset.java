package edu.washington.cse.concerto.interpreter.state;

import java.util.Set;

import soot.SootMethod;
import soot.toolkits.scalar.Pair;

public class FixpointChangeset extends Pair<Set<SootMethod>, Set<SootMethod>> {
	public FixpointChangeset(final Set<SootMethod> o1, final Set<SootMethod> o2) {
		super(o1, o2);
	}
	
	public boolean isEmpty() {
		return this.o1.isEmpty() && this.o2.isEmpty();
	}
	
	public Set<SootMethod> getChangedCallees() {
		return this.getO1();
	}
	
	public Set<SootMethod> getChangedReturn() {
		return this.getO2();
	}
}
