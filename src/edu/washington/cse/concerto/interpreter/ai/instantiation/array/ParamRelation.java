package edu.washington.cse.concerto.interpreter.ai.instantiation.array;

import fj.Ord;
import fj.Ordering;
import fj.P;
import fj.P2;
import fj.data.*;
import soot.Local;
import soot.SootFieldRef;
import soot.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParamRelation {
	public final static class ParamSpec extends APLocation {
		public final int pNum;

		public ParamSpec(final int pNum, final Seq<SootFieldRef> fields, final boolean lengthAP) {
			super(fields, lengthAP);
			this.pNum = pNum;
		}
		
		public KLimitAP toAP(final Local l) {
			return new KLimitAP(l, this.fields, this.lengthAP);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + pNum;
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
			if(!(obj instanceof ParamSpec)) {
				return false;
			}
			final ParamSpec other = (ParamSpec) obj;
			if(pNum != other.pNum) {
				return false;
			}
			return true;
		}

		@Override public String toString() {
			final StringBuilder sb = new StringBuilder();
			sb.append("[").append(pNum).append("]");
			for(final SootFieldRef sf : fields) {
				sb.append(".").append(sf);
			}
			if(this.lengthAP) {
				sb.append(".length");
			}
			return sb.toString();
		}
	}
	public static Ord<ParamSpec> ord = Ord.ord((a, b) -> {
		if(a.pNum != b.pNum) {
			return Ordering.fromInt(a.pNum - a.pNum);
		}
		return APLocation.partialOrder.compare(a, b);
	});
	private static final TreeMap<ParamSpec, Set<ParamSpec>> EMPTY_MAP = TreeMap.empty(ParamRelation.ord);
	public static final ParamRelation empty = new ParamRelation(ParamRelation.EMPTY_MAP, ParamRelation.EMPTY_MAP, ParamRelation.EMPTY_MAP);
	
	public final TreeMap<ParamSpec, Set<ParamSpec>> equivTree;
	public final TreeMap<ParamSpec, Set<ParamSpec>> ltTree;
	public final TreeMap<ParamSpec, Set<ParamSpec>> gtTree;
	
	public ParamRelation(final TreeMap<ParamSpec, Set<ParamSpec>> equivTree, final TreeMap<ParamSpec, Set<ParamSpec>> ltTree, final TreeMap<ParamSpec, Set<ParamSpec>> gtTree) {
		this.equivTree = equivTree;
		this.ltTree = ltTree;
		this.gtTree = gtTree;
	}

	private static Stream<ParamSpec> toParamSpec(final java.util.Map<KLimitAP, Integer> mapping, final KLimitAP ap) {
		final List<ParamSpec> toReturn = new ArrayList<>();
		for(final Map.Entry<KLimitAP, Integer> kv : mapping.entrySet()) {
			if(kv.getKey().equals(ap)) {
				toReturn.add(new ParamSpec(kv.getValue(), Seq.empty(), false));
			} else if(ap.isSuffixOf(kv.getKey())) {
				toReturn.add(new ParamSpec(kv.getValue(), ap.fields.drop(kv.getKey().fields.length()), ap.lengthAP));
			}
		}
		return Stream.iterableStream(toReturn);
	}
	
	public static ParamRelation fromState(final ARState st, final List<Value> argValues) {
		final java.util.Map<KLimitAP, Integer> paramNumber = new HashMap<>();
		for(int i = 0; i < argValues.size(); i++) {
			final Option<KLimitAP> argAP = KLimitAP.of(argValues.get(i));
			if(argAP.isSome()) {
				paramNumber.put(argAP.some(), i);
			}
		}
		final TreeMap<ParamSpec, Set<ParamSpec>> equivTree = ParamRelation.mapTreeToParams(paramNumber, st.congruenceClosure, false);
		final TreeMap<ParamSpec, Set<ParamSpec>> ltTree = ParamRelation.mapTreeToParams(paramNumber, st.ltClosure, true);
		final TreeMap<ParamSpec, Set<ParamSpec>> gtTree = ParamRelation.mapTreeToParams(paramNumber, st.gtClosure, true);
		assert ARState.isRelationConsistent(ltTree, equivTree, gtTree);
		return new ParamRelation(equivTree, ltTree, gtTree);
	}

	private static TreeMap<ParamSpec, Set<ParamSpec>> mapTreeToParams(final java.util.Map<KLimitAP, Integer> paramNumber, final TreeMap<KLimitAP, Set<KLimitAP>> tmp, final boolean allowSingleton) {
		final Stream<P2<ParamSpec, Set<ParamSpec>>> equivParam = tmp.toStream().bind(equivMapping -> ParamRelation.toParamSpec(paramNumber, equivMapping._1()).bind(param -> {
				final Stream<ParamSpec> equivParams = equivMapping._2().toStream().bind(equivAp -> ParamRelation.toParamSpec(paramNumber, equivAp));
				if(equivParams.isEmpty() || (!allowSingleton && equivParams.tail()._1().isEmpty())) {
					return Stream.nil();
				} else {
					return Stream.stream(P.p(param, Set.iterableSet(ParamRelation.ord, equivParams)));
				}
			})
		);
		final TreeMap<ParamSpec, Set<ParamSpec>> ret = equivParam.foldLeft(
			(accum, kv) -> accum.update(kv._1(), s -> s.union(kv._2()), kv._2()), ParamRelation.EMPTY_MAP);
		return ret;
	}

}
