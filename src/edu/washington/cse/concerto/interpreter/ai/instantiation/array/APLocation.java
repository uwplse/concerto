package edu.washington.cse.concerto.interpreter.ai.instantiation.array;

import fj.Ord;
import fj.Ordering;
import fj.data.Seq;
import fj.data.Stream;
import soot.SootFieldRef;

import java.util.Iterator;

public abstract class APLocation {

	public final Seq<SootFieldRef> fields;
	public final boolean lengthAP;
	
	protected static final Ord<Stream<SootFieldRef>> streamFieldOrd = Ord.streamOrd(Ord.ord((a, b) -> Ordering.fromInt(a.getSignature().compareTo(b.getSignature()))));
	private static final Ord<Seq<SootFieldRef>> fieldOrd = Ord.ord((s1, s2) -> APLocation.streamFieldOrd.compare(s1.toStream(), s2.toStream())
	);
	protected static final Ord<APLocation> partialOrder = Ord.ord((ap1, ap2) -> {
		final Ordering fldCmp = APLocation.fieldOrd.compare(ap1.fields, ap2.fields);
		if(fldCmp != Ordering.EQ) {
			return fldCmp;
		}
		if(ap1.lengthAP == ap2.lengthAP) {
			assert ap1.equals(ap2);
			return Ordering.EQ;
		}
		if(ap1.lengthAP) {
			return Ordering.GT;
		} else if(ap2.lengthAP) {
			return Ordering.LT;
		} else {
			throw new RuntimeException();
		}

	});

	public APLocation(final Seq<SootFieldRef> fields, final boolean lengthAP) {
		this.fields = fields;
		this.lengthAP = lengthAP;
	}

	public boolean hasField(final SootFieldRef v) {
		return this.fields.toStream().exists(v::equals);
	}

	public boolean isSuffixOf(final KLimitAP ap) {
		if(ap.lengthAP) {
			return this.equals(ap);
		}
		if(this.fields.length() < ap.fields.length()) {
			return false;
		}
		final Iterator<SootFieldRef> thisF = this.fields.iterator();
		final Iterator<SootFieldRef> apF = ap.fields.iterator();
		while(apF.hasNext()) {
			assert thisF.hasNext();
			if(!apF.next().equals(thisF.next())) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fields == null) ? 0 : fields.hashCode());
		result = prime * result + (lengthAP ? 1231 : 1237);
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
		if(!(obj instanceof APLocation)) {
			return false;
		}
		final APLocation other = (APLocation) obj;
		if(fields == null) {
			if(other.fields != null) {
				return false;
			}
		} else if(!fields.equals(other.fields)) {
			return false;
		}
		if(lengthAP != other.lengthAP) {
			return false;
		}
		return true;
	}
}