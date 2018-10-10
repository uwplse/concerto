package edu.washington.cse.concerto.instrumentation.filter;

import edu.washington.cse.concerto.interpreter.BodyManager;
import fj.F;
import soot.FastHierarchy;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.Type;

public class TypeFilterBuilder<Ret> {
	private final F<TypeFilter, Ret> finish;

	public TypeFilterBuilder(final F<TypeFilter, Ret> finish) {
		this.finish = finish;
	}
	
	public Ret isSubType(final RefType toCheck) {
		return this.finish.f(new TypeFilter() {
			@Override
			public boolean accept(final Type t) {
				final FastHierarchy fh = Scene.v().getOrMakeFastHierarchy();
				return fh.canStoreType(t, toCheck);
			}
		});
	}
	
	public Ret isSubType(final String s) {
		final SootClass cls = BodyManager.loadClass(s);
		return this.finish.f(new TypeFilter() {
			@Override
			public boolean accept(final Type t) {
				final FastHierarchy fh = Scene.v().getOrMakeFastHierarchy();
				return fh.canStoreType(t, cls.getType());
			}
		});
	}
	
	public Ret is(final Type toCheck) {
		return this.finish.f(new TypeFilter() {
			@Override
			public boolean accept(final Type t) {
				return toCheck.equals(t);
			}
		});
	}
	
	public Ret is(final String s) {
		return this.is(BodyManager.loadClass(s).getType());
	}
}

