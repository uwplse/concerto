package edu.washington.cse.concerto.interpreter.ai.instantiation.optiflow;

import soot.SootFieldRef;
import fj.F2;
import fj.Ord;
import fj.Ordering;

public final class FieldSpec {
	private final String fieldName;
	private final boolean transitive;

	public static final Ord<FieldSpec> ordering = Ord.ord(new F2<FieldSpec, FieldSpec, Ordering>() {
		private final Ord<Boolean> flagOrd = Ord.booleanOrd;
		
		@Override
		public Ordering f(final FieldSpec a, final FieldSpec b) {
			final int cmp = a.fieldName.compareTo(b.fieldName);
			if(cmp != 0) {
				return Ordering.fromInt(cmp);
			}
			return flagOrd.compare(a.transitive, b.transitive);
		}
	});

	public FieldSpec(final String fieldName, final boolean transitive) {
		this.fieldName = fieldName;
		this.transitive = transitive;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fieldName == null) ? 0 : fieldName.hashCode());
		result = prime * result + (transitive ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if(this == obj) {
			return true;
		}
		if(obj == null) {
			return false;
		}
		if(getClass() != obj.getClass()) {
			return false;
		}
		final FieldSpec other = (FieldSpec) obj;
		if(fieldName == null) {
			if(other.fieldName != null) {
				return false;
			}
		} else if(!fieldName.equals(other.fieldName)) {
			return false;
		}
		if(transitive != other.transitive) {
			return false;
		}
		return true;
	}

	public static FieldSpec transitive(final SootFieldRef fieldRef) {
		// TODO Auto-generated method stub
		return null;
	}

	public static FieldSpec direct(final SootFieldRef fieldRef) {
		// TODO Auto-generated method stub
		return null;
	}
}
