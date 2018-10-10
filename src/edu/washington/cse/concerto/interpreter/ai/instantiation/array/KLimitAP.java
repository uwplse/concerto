package edu.washington.cse.concerto.interpreter.ai.instantiation.array;

import fj.Ord;
import fj.Ordering;
import fj.data.Option;
import fj.data.Seq;
import fj.data.Set;
import soot.*;
import soot.jimple.InstanceFieldRef;
import soot.jimple.LengthExpr;
import soot.util.ArrayNumberer;

public class KLimitAP extends APLocation {
	public final Local l;
	
	public static Ord<KLimitAP> apOrd = Ord.ord((ap1, ap2) -> {
		final ArrayNumberer<Local> loc = Scene.v().getLocalNumberer();
		final long locCmp = loc.get(ap1.l) - loc.get(ap2.l);
		if(locCmp != 0) {
			return Ordering.fromInt((int) locCmp);
		}
		return APLocation.partialOrder.compare(ap1, ap2);
	});
	
	private KLimitAP(final Local l) {
		super(Seq.empty(), false);
		this.l = l;
	}
	
	public KLimitAP(final Local l, final Seq<SootFieldRef> f, final boolean isLength) {
		super(f, isLength);
		this.l = l;
	}
	
	public static KLimitAP lengthOf(final Local l, final Seq<SootFieldRef> f) {
		return new KLimitAP(l, f, true);
	}
	
	public static KLimitAP lengthOf(final Local l) {
		return KLimitAP.lengthOf(l, Seq.empty());
	}
	
	public static KLimitAP of(final Local l, final Seq<SootFieldRef> f) {
		return new KLimitAP(l, f, false);
	}
	
	public static KLimitAP of(final Local l) {
		return new KLimitAP(l, Seq.empty(), false);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((l == null) ? 0 : l.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if(this == obj) {
			return true;
		}
		if(!super.equals(obj)) {
			return false;
		}
		if(!(obj instanceof KLimitAP)) {
			return false;
		}
		final KLimitAP other = (KLimitAP) obj;
		if(l == null) {
			if(other.l != null) {
				return false;
			}
		} else if(!l.equals(other.l)) {
			return false;
		}
		return true;
	}
	
	public static Option<KLimitAP> of(final Value v) {
		return KLimitAP.of(v, Seq.empty(), false);
	}
	
	public static final Set<KLimitAP> emptySet = Set.empty(KLimitAP.apOrd);
	public static final Set<KLimitAP> singleton(final KLimitAP ap) {
		return Set.single(KLimitAP.apOrd, ap);
	}

	private static Option<KLimitAP> of(final Value v, final Seq<SootFieldRef> fields, final boolean isLength) {
		if(v instanceof Local) {
			return Option.some(new KLimitAP((Local) v, fields, isLength));
		} else if(v instanceof LengthExpr) {
			assert !isLength;
			return KLimitAP.of(((LengthExpr) v).getOp(), fields, true);
		} else if(v instanceof InstanceFieldRef) {
			final InstanceFieldRef iref = (InstanceFieldRef) v; 
			return KLimitAP.of(((InstanceFieldRef) v).getBase(), fields.cons(iref.getFieldRef()), isLength);
		} else {
			return Option.none();
		}
	}

	public KLimitAP makeLengthAP() {
		assert !lengthAP;
		return new KLimitAP(l, fields, true);
	}
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(this.l);
		this.fields.forEach(f -> sb.append(".").append(f.name()));
		if(this.lengthAP) {
			sb.append(".length");
		}
		return sb.toString(); 
	}
	
	public KLimitAP append(final Seq<SootFieldRef> drop, final boolean nLengthAP) {
		assert !lengthAP;
		return new KLimitAP(this.l, fields.append(drop), nLengthAP);
	}

	@Override public boolean isSuffixOf(final KLimitAP ap) {
		if(this.l != ap.l) {
			return false;
		}
		return super.isSuffixOf(ap);
	}

	public boolean isRefLocation() {
		if(this.lengthAP) {
			return false;
		}
		if(fields.isEmpty()) {
			return this.l.getType() instanceof RefLikeType;
		} else {
			return this.fields.last().type() instanceof RefLikeType;
		}
	}

	public KLimitAP mapTo(final KLimitAP pref, final KLimitAP target) {
		assert this.isSuffixOf(pref);
		assert !target.lengthAP && !pref.lengthAP;
		return new KLimitAP(target.l, target.fields.append(this.fields.drop(pref.fields.length())), this.lengthAP);
	}
}
