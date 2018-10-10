package edu.washington.cse.concerto.interpreter.ai.instantiation.array;

import fj.data.Set;

class RelationResult {
	public static enum RelCode {
		LT, LE, EQ, GE, GT, NONE
	}

	public final boolean isLength;
	public final Set<KLimitAP> rel;
	public final RelationResult.RelCode code;
	public RelationResult(final RelationResult.RelCode code, final Set<KLimitAP> rel) {
		this(code, rel, false);
	}
	
	public RelationResult(final RelationResult.RelCode code, final KLimitAP... rel) {
		this(code, Set.arraySet(KLimitAP.apOrd, rel), false);
	}
	
	public RelationResult(final RelationResult.RelCode code, final Set<KLimitAP> rel, final boolean isLength) {
		this.code = code;
		this.rel = rel;
		this.isLength = isLength;
	}
	
	public RelationResult toLength() {
		if(this.code == RelationResult.RelCode.NONE || this.isLength) {
			return this;
		} else {
			return new RelationResult(code, rel, true);
		}
	}
	
	public static final RelationResult none = new RelationResult(RelationResult.RelCode.NONE, Set.empty(KLimitAP.apOrd));
	
	public Set<KLimitAP> lowerBounds() {
		if(code == RelationResult.RelCode.EQ || code == RelationResult.RelCode.GE || code == RelationResult.RelCode.GT) {
			return this.rel;
		} else {
			return KLimitAP.emptySet;
		}
	}
	
	public Set<KLimitAP> upperBounds() {
		if(code == RelationResult.RelCode.EQ || code == RelationResult.RelCode.LE || code == RelationResult.RelCode.LT) {
			return this.rel;
		} else {
			return KLimitAP.emptySet;
		}
	}
	
	@Override
	public String toString() {
		return "(" + this.code + " " + this.rel + ")";
	}
}