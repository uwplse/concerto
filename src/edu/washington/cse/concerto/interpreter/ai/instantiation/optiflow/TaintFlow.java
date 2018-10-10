package edu.washington.cse.concerto.interpreter.ai.instantiation.optiflow;

import edu.washington.cse.concerto.interpreter.ai.PrettyPrintable;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Unit;

import java.util.Objects;

public class TaintFlow implements PrettyPrintable {
	public final Object taintedValue;
	public final SootMethod containingMethod;
	public final Unit site;
	public final SootMethodRef sinkingMethod;

	public TaintFlow(final Object taintedValue, final SootMethod containingMethod, final Unit site, final SootMethodRef sinkingMethod) {
		this.taintedValue = taintedValue;
		this.containingMethod = containingMethod;
		this.site = site;
		this.sinkingMethod = sinkingMethod;
	}

	@Override public String prettyPrint() {
		return "Found flow from source to sink: " + this.sinkingMethod + " in method: " + this.containingMethod;
	}

	@Override public boolean equals(final Object o) {
		if(this == o)
			return true;
		if(o == null || getClass() != o.getClass())
			return false;
		final TaintFlow taintFlow = (TaintFlow) o;
		return Objects.equals(taintedValue, taintFlow.taintedValue) && Objects.equals(containingMethod, taintFlow.containingMethod) && Objects.equals(site, taintFlow.site) && Objects
				.equals(sinkingMethod, taintFlow.sinkingMethod);
	}

	@Override public int hashCode() {
		return Objects.hash(taintedValue, containingMethod, site, sinkingMethod);
	}
}
