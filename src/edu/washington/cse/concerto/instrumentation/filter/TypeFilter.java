package edu.washington.cse.concerto.instrumentation.filter;

import soot.Type;

public interface TypeFilter {
	public boolean accept(Type t);
}
