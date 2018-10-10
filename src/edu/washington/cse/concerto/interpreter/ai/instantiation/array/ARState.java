package edu.washington.cse.concerto.interpreter.ai.instantiation.array;

import edu.washington.cse.concerto.interpreter.ai.Continuation;
import edu.washington.cse.concerto.interpreter.ai.ContinuationBasedState;
import edu.washington.cse.concerto.interpreter.ai.MappedValueLatticeHelper;
import edu.washington.cse.concerto.interpreter.ai.MonadicLattice;
import edu.washington.cse.concerto.interpreter.ai.PowersetLattice;
import edu.washington.cse.concerto.interpreter.ai.StrictMappedValueLatticeHelper;
import edu.washington.cse.concerto.interpreter.ai.UnitOrd;
import edu.washington.cse.concerto.interpreter.ai.instantiation.array.RelationResult.RelCode;
import edu.washington.cse.concerto.interpreter.lattice.Lattice;
import edu.washington.cse.concerto.interpreter.meta.Monads;
import fj.F;
import fj.F2;
import fj.Ord;
import fj.Ordering;
import fj.P;
import fj.P2;
import fj.P3;
import fj.data.Option;
import fj.data.Set;
import fj.data.Stream;
import fj.data.TreeMap;
import soot.Local;
import soot.SootFieldRef;
import soot.Unit;
import soot.Value;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ARState implements ContinuationBasedState<P2<CallSite,Unit>,ARState> {

	protected static MonadicLattice<ARState, PValue, ARState> lattice = new MonadicLattice<ARState, PValue, ARState>() {
		private MappedValueLatticeHelper<Local, Object> mapLattice;
		@Override
		public void inject(final Monads<PValue, ARState> monads) {
			 this.mapLattice = new MappedValueLatticeHelper<>(monads.valueMonad);
		}
		
		private final StrictMappedValueLatticeHelper<KLimitAP, Set<KLimitAP>> relationalLattice = new StrictMappedValueLatticeHelper<>(new Lattice<Set<KLimitAP>>() {
			@Override
			public Set<KLimitAP> widen(final Set<KLimitAP> prev, final Set<KLimitAP> next) {
				return this.join(prev, next);
			}

			@Override
			public Set<KLimitAP> join(final Set<KLimitAP> first, final Set<KLimitAP> second) {
				return first.intersect(second);
			}

			@Override
			public boolean lessEqual(final Set<KLimitAP> first, final Set<KLimitAP> second) {
				return second.subsetOf(first);
			}
		}, KLimitAP.emptySet);
		
		@Override
		public ARState widen(final ARState prev, final ARState next) {
			return new ARState(mapLattice.widen(prev.localState, next.localState), ARHeap.lattice.widen(prev.heap, next.heap),
					relationalLattice.widen(prev.congruenceClosure, next.congruenceClosure),
					relationalLattice.widen(prev.ltClosure, next.ltClosure),
					relationalLattice.widen(prev.gtClosure, next.gtClosure),
					Continuation.widen(prev.continuation, next.continuation));
		}
		
		@Override
		public boolean lessEqual(final ARState first, final ARState second) {
			return mapLattice.lessEqual(first.localState, second.localState) &&
					ARHeap.lattice.lessEqual(first.heap, second.heap) &&
					relationalLattice.lessEqual(first.congruenceClosure, second.congruenceClosure) &&
					relationalLattice.lessEqual(first.ltClosure, second.ltClosure) &&
					relationalLattice.lessEqual(first.gtClosure, second.gtClosure) &&
					Continuation.lessEqual(first.continuation, second.continuation);
		}
		
		@Override
		public ARState join(final ARState first, final ARState second) {
			return new ARState(mapLattice.join(first.localState, second.localState), ARHeap.lattice.join(first.heap, second.heap),
				relationalLattice.join(first.congruenceClosure, second.congruenceClosure),
				relationalLattice.join(first.ltClosure, second.ltClosure),
				relationalLattice.join(first.gtClosure, second.gtClosure),
				Continuation.join(first.continuation, second.continuation));
		}
	};
	private static final Ord<Local> localOrd = Ord.ord(new F2<Local, Local, Ordering>() {
		@Override
		public Ordering f(final Local a, final Local b) {
			return Ordering.fromInt(a.getName().compareTo(b.getName()));
		}
	});
	public static final Ord<P2<CallSite, Unit>> CONTINUATION_ORD = Ord.p2Ord(CallSite.SITE_ORDER, UnitOrd.unitOrdering);
	public final TreeMap<Local, Object> localState;
	public final TreeMap<KLimitAP, Set<KLimitAP>> congruenceClosure;
	public final TreeMap<KLimitAP, Set<KLimitAP>> ltClosure;
	public final TreeMap<KLimitAP, Set<KLimitAP>> gtClosure;
	public final Continuation<P2<CallSite, Unit>> continuation;
	public final ARHeap heap;
	private static final ARState emptyState = new ARState(TreeMap.empty(ARState.localOrd), ARHeap.empty,
			TreeMap.empty(KLimitAP.apOrd), TreeMap.empty(KLimitAP.apOrd), TreeMap.empty(KLimitAP.apOrd), Continuation.bottom(CONTINUATION_ORD));
	private static final Lattice<Set<CallSite>> contLattice = new PowersetLattice<>(CallSite.SITE_ORDER);

	public ARState(final TreeMap<Local, Object> localState, final ARHeap heap, final TreeMap<KLimitAP, Set<KLimitAP>> congruenceClosure,
			final TreeMap<KLimitAP, Set<KLimitAP>> ltClosure, final TreeMap<KLimitAP, Set<KLimitAP>> gtClosure, final Continuation<P2<CallSite, Unit>> cont) {
		this.localState = localState;
		this.heap = heap;
		this.congruenceClosure = ARState.filterTree(congruenceClosure);
		this.ltClosure = ARState.filterTree(ltClosure);
		this.gtClosure = ARState.filterTree(gtClosure);
		this.continuation = cont;
		assert ARState.isRelationConsistent(this.ltClosure, this.congruenceClosure, this.gtClosure) : this.ltClosure + " == " + this.congruenceClosure + " == " + this.gtClosure;
	}

	private static TreeMap<KLimitAP, Set<KLimitAP>> filterTree(final TreeMap<KLimitAP, Set<KLimitAP>> tr) {
		return TreeMap.iterableTreeMap(KLimitAP.apOrd, tr.toStream().filter(kv -> !kv._2().isEmpty()));
	}

	public ARState withHeap(final ARHeap heap) {
		return new ARState(localState, heap, this.congruenceClosure, this.ltClosure, this.gtClosure, this.continuation);
	}

	public static ARState empty() {
		return ARState.emptyState;
	}

	public ARState set(final Local l, final Object value) {
		return new ARState(localState.set(l, value), heap, this.congruenceClosure, this.ltClosure, this.gtClosure, this.continuation);
	}

	public Object get(final Local op) {
		// FIXME: unacceptable punning
		return localState.get(op).orSome(PValue.bottom());
	}

	public ARState withLocals(final Map<Local, Object> startLocals) {
		return new ARState(TreeMap.fromMutableMap(ARState.localOrd, startLocals), heap, TreeMap.empty(KLimitAP.apOrd), TreeMap.empty(KLimitAP.apOrd), TreeMap.empty(KLimitAP.apOrd), this.continuation);
	}

	@Override
	public String toString() {
		return this.toStringFull();
	}
	
	public String toStringFull() {
		return "{{ " + localState.toString() + " X " + this.heap.toString() + " X CONGR: " + this.congruenceClosure + " X LT: " + this.ltClosure + " X GT: " + this.gtClosure + " X CT: " + this.continuation + "}}";
	}

	private ARState killState(final Set<KLimitAP> toPurge) {
		final P3<TreeMap<KLimitAP, Set<KLimitAP>>, TreeMap<KLimitAP, Set<KLimitAP>>, TreeMap<KLimitAP, Set<KLimitAP>>> ret = killForStream(toPurge);
		return new ARState(localState, heap, ret._1(), ret._2(), ret._3(), this.continuation);
	}

	private P3<TreeMap<KLimitAP, Set<KLimitAP>>, TreeMap<KLimitAP, Set<KLimitAP>>, TreeMap<KLimitAP, Set<KLimitAP>>> killForStream(final Set<KLimitAP> toPurge_) {
		if(toPurge_.isEmpty()) {
			return P.p(congruenceClosure, ltClosure, gtClosure);
		}
		final Set<KLimitAP> toPurge = toPurge_.bind(KLimitAP.apOrd, ap ->
			 findSuffixLocations(ap, ltClosure, congruenceClosure, gtClosure)
		);
		final Set<KLimitAP> lt = toPurge.toStream().map(this::getLtClosure).foldLeft1(Set::union);
		final Set<KLimitAP> gt = toPurge.toStream().map(this::getGtClosure).foldLeft1(Set::union);
		final Set<KLimitAP> equivs = toPurge.toStream().map(this::materializeEquivalenceClass).foldLeft1(Set::union);
		final TreeMap<KLimitAP, Set<KLimitAP>> updatedGt = this.purgeTree(lt, toPurge, gtClosure);
		final TreeMap<KLimitAP, Set<KLimitAP>> updatedLt = this.purgeTree(gt, toPurge, ltClosure);
		final TreeMap<KLimitAP, Set<KLimitAP>> congruence = this.purgeTree(equivs, toPurge, congruenceClosure);
		final P3<TreeMap<KLimitAP, Set<KLimitAP>>, TreeMap<KLimitAP, Set<KLimitAP>>, TreeMap<KLimitAP, Set<KLimitAP>>> ret = P.p(congruence, updatedLt, updatedGt);
		return ret;
	}

	@SafeVarargs
	private final Set<KLimitAP> findSuffixLocations(final KLimitAP ap, final TreeMap<KLimitAP, Set<KLimitAP>>... src) {
		if(!ap.isRefLocation()) {
			return KLimitAP.singleton(ap);
		}
		return Stream.arrayStream(src).bind(TreeMap::toStream).foldLeft((accum, kv) -> {
			if(kv._1().isSuffixOf(ap)) {
				return accum.insert(kv._1());
			} else {
				return accum;
			}
		}, KLimitAP.singleton(ap));
	}

	private TreeMap<KLimitAP, Set<KLimitAP>> purgeTree(final Set<KLimitAP> withRef, final Set<KLimitAP> toPurge, final TreeMap<KLimitAP, Set<KLimitAP>> closureTree) {
		final TreeMap<KLimitAP, Set<KLimitAP>> purgedTrans = withRef.toStream().foldLeft((accum, gelem) -> {
			return accum.update(gelem, s -> s.minus(toPurge))._2();
		}, closureTree);
		return toPurge.toStream().foldLeft((accum, purgeElem) -> accum.delete(purgeElem), purgedTrans);
	}

	public Set<KLimitAP> getLtClosure(final KLimitAP ap) { 
		return this.ltClosure.get(ap).orSome(KLimitAP.emptySet);
	}
	
	public Set<KLimitAP> getGtClosure(final KLimitAP ap) { 
		return this.gtClosure.get(ap).orSome(KLimitAP.emptySet);
	}
	
	public ARState killRelationForField(final SootFieldRef v) {
		return killState(findFieldAPs(this.congruenceClosure, v).append(this.findFieldAPs(ltClosure, v)).foldLeft(Set::insert, KLimitAP.emptySet));
	}

	private Stream<KLimitAP> findFieldAPs(final TreeMap<KLimitAP, Set<KLimitAP>> toFilter, final SootFieldRef v) {
		return filterAPs(toFilter, ap -> ap. hasField(v));
	}

	private Stream<KLimitAP> filterAPs(final TreeMap<KLimitAP, Set<KLimitAP>> toFilter, final F<KLimitAP, Boolean> filterFn) {
		return toFilter.toStream().map(P2::_1).filter(filterFn);
	}
	
	public ARState killHeapRelations() {
		final F<KLimitAP, Boolean> fn = ap -> ap.fields.isNotEmpty();
		return killState(filterAPs(congruenceClosure, fn).append(filterAPs(ltClosure, fn)).foldLeft(Set::insert, KLimitAP.emptySet));
	}
	
	private ARState propagateAssignEq(final KLimitAP ap, final KLimitAP lhs) {
		final P3<TreeMap<KLimitAP, Set<KLimitAP>>, TreeMap<KLimitAP, Set<KLimitAP>>, TreeMap<KLimitAP, Set<KLimitAP>>> killed = this.killForStream(KLimitAP.singleton(ap));
		final P3<TreeMap<KLimitAP, Set<KLimitAP>>, TreeMap<KLimitAP, Set<KLimitAP>>, TreeMap<KLimitAP, Set<KLimitAP>>> propagated = this.findSuffixLocations(lhs, congruenceClosure, ltClosure, gtClosure).toStream().foldLeft((treeAccum, lhsSuff) -> {
				final KLimitAP targetAp;
			if(ap.lengthAP) {
				assert lhsSuff.equals(lhs);
				targetAp = ap;
			} else {
				targetAp = lhsSuff.mapTo(lhs, ap);
			}
			final Set<KLimitAP> newEquiv = this.materializeEquivalenceClass(lhsSuff).insert(targetAp);
			final TreeMap<KLimitAP, Set<KLimitAP>> congruenceClosure = newEquiv.toStream().foldLeft((accum, eqElem) -> accum.set(eqElem, newEquiv), treeAccum._1());
			final Set<KLimitAP> ltClos = this.getLtClosure(lhsSuff);
			final Set<KLimitAP> gtClos = this.getGtClosure(lhsSuff);

			final TreeMap<KLimitAP, Set<KLimitAP>> lt = gtClos.toStream().foldLeft((accum, ltElem) -> accum.update(ltElem, s -> s.insert(targetAp))._2(), treeAccum._2())
					.set(targetAp, ltClos);
			final TreeMap<KLimitAP, Set<KLimitAP>> gt = ltClos.toStream().foldLeft((accum, gtElem) -> accum.update(gtElem, s -> s.insert(targetAp))._2(), treeAccum._3())
					.set(targetAp, gtClos);
			return P.p(congruenceClosure, lt, gt);
		}, killed);
		return new ARState(localState, heap, propagated._1(), propagated._2(), propagated._3(), this.continuation);
	}

	/*
	 * gt are the elements that rhs is GREATER THAN, lt are those that are LESS THAN
	 */
	private ARState propagateAssign(final KLimitAP rhs, final Iterable<KLimitAP> gt, final Iterable<KLimitAP> lt) {
		final P3<TreeMap<KLimitAP, Set<KLimitAP>>, TreeMap<KLimitAP, Set<KLimitAP>>, TreeMap<KLimitAP, Set<KLimitAP>>> killed = this.killForStream(KLimitAP.singleton(rhs));
		final Set<KLimitAP> gtSet = Set.iterableSet(KLimitAP.apOrd, gt);
		final TreeMap<KLimitAP, Set<KLimitAP>> ltTree = Stream.iterableStream(gt).foldLeft((accum, gtElem) ->
			accum.update(gtElem, s -> { return s.insert(rhs); }, KLimitAP.singleton(rhs)), killed._2()
		).set(rhs, Set.iterableSet(KLimitAP.apOrd, lt));
		final TreeMap<KLimitAP, Set<KLimitAP>> gtTree = Stream.iterableStream(lt).foldLeft((accum, ltElem) -> 
			accum.update(ltElem, s -> s.insert(rhs), KLimitAP.singleton(rhs)), killed._3()
		).set(rhs, gtSet);
		return new ARState(localState, heap, congruenceClosure, ltTree, gtTree, this.continuation);
	}
	
	/*
	 * Propagate that the elements of toSaturate are now strictly less than toPropagate. toSaturate is assumed to form an equivalence class, as is toPropagate
	 * so computing the transitive closures for each may be performed at most once.
	 * 
	 * The complexity of this function: O(bad)
	 */
	private P2<TreeMap<KLimitAP, Set<KLimitAP>>, TreeMap<KLimitAP, Set<KLimitAP>>> saturateLTClosure(final TreeMap<KLimitAP, Set<KLimitAP>> leftLTClosure, final TreeMap<KLimitAP, Set<KLimitAP>> leftGtClosure,
			final Set<KLimitAP> toSaturate, final Set<KLimitAP> toPropagate) {
		
		// propagate that these elements are now LT some elements (stored in the gt closure) 
		final KLimitAP ltRepr = toSaturate.iterator().next();
		
		/* 
		 * find the elements for which an elem of toSaturate is greater, and then union it with the toSaturate set,
		 * this gives the set of elements that will be LESS thatn the elements of toPropagate (and it's gt closure)
		 */
		final Set<KLimitAP> ltPropagate = leftGtClosure.get(ltRepr).orSome(KLimitAP.emptySet).union(toSaturate);
		
		// propagate that these elements are now GT than some elements (store in the lt closure)
		final KLimitAP gtRepr = toPropagate.iterator().next();
		/*
		 * Do the opposite trick, find the elements greater than those in toPropagate and union it with toPropagate.
		 * This will yield the elements that will be GREATER than the elements of toSaturate (and it's transitive lt closure)
		 */
		final Set<KLimitAP> gtPropagate = leftLTClosure.get(gtRepr).orSome(KLimitAP.emptySet).union(toPropagate);
		
		final TreeMap<KLimitAP, Set<KLimitAP>> newLtClosure = ltPropagate.toStream().foldLeft(
				(accum, root) -> accum.update(root, s -> s.union(gtPropagate), gtPropagate), leftLTClosure);
		
		final TreeMap<KLimitAP, Set<KLimitAP>> newGtClosure = gtPropagate.toStream().foldLeft(
				(accum, root) -> accum.update(root, s -> s.union(ltPropagate), ltPropagate), leftGtClosure);
		
		return P.p(newLtClosure, newGtClosure);
	}
	
	private P2<TreeMap<KLimitAP, Set<KLimitAP>>, TreeMap<KLimitAP, Set<KLimitAP>>> saturateLTClosureWithEq(final TreeMap<KLimitAP, Set<KLimitAP>> inputLtClosure, final TreeMap<KLimitAP, Set<KLimitAP>> inputGtClosure,
			final Set<KLimitAP> eq1, final Set<KLimitAP> eq2) {
		final KLimitAP eq1Repr = eq1.iterator().next();
		final KLimitAP eq2Repr = eq2.iterator().next();
		
		final Set<KLimitAP> newEquiv = eq1.union(eq2);
		
		// Find the union of the lt-closure of eq1 and eq2, i.e. the less than eq1 and eq2
		final Set<KLimitAP> ltClosure = inputGtClosure.get(eq1Repr).orSome(KLimitAP.emptySet).union(inputGtClosure.get(eq2Repr).orSome(KLimitAP.emptySet));
		final Set<KLimitAP> leClosure = ltClosure.union(newEquiv);
		
		// the gt-closure of eq1 and eq2, i.e. the elements GREATER than eq1 and eq2
		final Set<KLimitAP> gtClosure = inputLtClosure.get(eq1Repr).orSome(KLimitAP.emptySet).union(inputLtClosure.get(eq2Repr).orSome(KLimitAP.emptySet));
		final Set<KLimitAP> geClosure = gtClosure.union(newEquiv);
		
		final TreeMap<KLimitAP, Set<KLimitAP>> newGtTree = gtClosure.toStream().foldLeft((accum, gtElem) ->
			accum.update(gtElem, s -> s.union(leClosure), leClosure),
			newEquiv.toStream().foldLeft((accum, eqElem) -> accum.update(eqElem, s -> s.union(ltClosure), ltClosure), inputGtClosure)
		);
		
		final TreeMap<KLimitAP, Set<KLimitAP>> ltClosureProp = ltClosure.toStream().foldLeft((accum, ltElem) ->
			accum.update(ltElem, s -> s.union(geClosure), geClosure), inputLtClosure
		);
		
		final TreeMap<KLimitAP, Set<KLimitAP>> newLtTree = newEquiv.toStream().foldLeft((accum, equivElem) ->
			accum.update(equivElem, s -> s.union(gtClosure), gtClosure), ltClosureProp
		);
		return P.p(newLtTree, newGtTree);
	}
	
	private Set<KLimitAP> materializeEquivalenceClass(final KLimitAP ap) {
		return this.congruenceClosure.get(ap).map(s -> s.insert(ap)).orSome(KLimitAP.singleton(ap));
	}

	public ARState propagateLTAssumption(final Value lop, final Value rop) {
		final F2<KLimitAP, KLimitAP, ARState> propagateAssumption = (lp, rp) -> {
			final P2<TreeMap<KLimitAP, Set<KLimitAP>>, TreeMap<KLimitAP, Set<KLimitAP>>> sat = this.saturateLTClosure(ltClosure, gtClosure, materializeEquivalenceClass(lp), materializeEquivalenceClass(rp));
			return new ARState(localState, heap, congruenceClosure, sat._1(), sat._2(), this.continuation);
		};
		return propagateAssumptionOnValue(lop, rop, propagateAssumption);
	}

	private ARState propagateAssumptionOnValue(final Value lop, final Value rop, final F2<KLimitAP, KLimitAP, ARState> propagateAssumption) {
		return KLimitAP.of(lop).bind(lp -> 
			KLimitAP.of(rop).map(rp -> propagateAssumption.f(lp, rp))
		).orSome(this);
	}

	public ARState propagateEQAssumption(final Value lop, final Value rop) {
		return propagateAssumptionOnValue(lop, rop, (lp, rp) -> {
			final Set<KLimitAP> rCongr = this.congruenceClosure.get(rp).orSome(Set.single(KLimitAP.apOrd, rp));
			final Set<KLimitAP> lCongr = this.congruenceClosure.get(lp).orSome(Set.single(KLimitAP.apOrd, lp));
			if(rCongr.member(lp)) {
				assert lCongr.member(rp);
				return this;
			}
			final Set<KLimitAP> newCongr = rCongr.union(lCongr);
			final P2<TreeMap<KLimitAP, Set<KLimitAP>>, TreeMap<KLimitAP, Set<KLimitAP>>> saturated = this.saturateLTClosureWithEq(ltClosure, gtClosure, 
					this.materializeEquivalenceClass(lp), this.materializeEquivalenceClass(rp));
			return new ARState(localState, heap, newCongr.toStream().foldLeft((t, elem) -> t.set(elem, newCongr), congruenceClosure), saturated._1(), saturated._2(), this.continuation);
		});
	}

	public ARState set(final Local lhs, final Object value, final RelationResult toSet) {
		final KLimitAP targetAP = toSet.isLength ? KLimitAP.of(lhs).makeLengthAP() : KLimitAP.of(lhs);
		final ARState preSet;
		preSet = propagateSymbolicResult(targetAP, toSet);
		return preSet.set(lhs, value);
	}

	public ARState propagateSymbolicResult(final KLimitAP targetAP, final RelationResult toSet) {
		final Set<KLimitAP> filtered = toSet.rel.filter(ap -> !targetAP.equals(ap));
		if(filtered.isEmpty()) {
			return this;
		}
		final ARState preSet;
		if(toSet.code == RelCode.EQ) {
			assert toSet.rel.size() == 1;
			preSet = this.propagateAssignEq(targetAP, toSet.rel.iterator().next());
		} else if(toSet.code == RelCode.GT) {
			preSet = this.propagateAssign(targetAP, toSet.rel, Collections.emptyList());
		} else if(toSet.code == RelCode.LT) {
			preSet = this.propagateAssign(targetAP, Collections.emptyList(), toSet.rel);
		} else {
			preSet = this;
		}
		return preSet;
	}

	public ARState propagateSymbolicResult(final Option<KLimitAP> of, final RelationResult res) {
		if(of.isNone()) {
			return this;
		} else {
			return this.propagateSymbolicResult(of.some(), res);
		}
	}

	public static <V> boolean isRelationConsistent(final TreeMap<V, Set<V>> ltTree, final TreeMap<V, Set<V>> equivTree,
			final TreeMap<V, Set<V>> gtTree) {
		return ARState.isReflexiveTree(equivTree) && ltTree.toStream().forall(kv -> {
			final V ltElem = kv._1();
			final Set<V> gtVals = kv._2();
			final boolean eqClosureConsistent = equivTree.get(ltElem).map(eqClosure -> eqClosure.toStream().forall(eqElem ->
				ltTree.get(eqElem).map(gtVals::equals).orSome(false)
			)).orSome(true);
			if(!eqClosureConsistent) {
				return false;
			}
			final boolean ltIsClosure = gtVals.toStream().forall(gtElem ->
				ltTree.get(ltElem).map(v -> v.subsetOf(gtVals)).orSome(false)
			);
			if(!ltIsClosure) {
				return false;
			}
			final boolean gtConsistent = gtVals.toStream().forall(gtElem ->
				gtTree.get(gtElem).map(s -> s.member(ltElem)).orSome(false)
			);
			if(!gtConsistent) {
				return false;
			}
			return true;
		}) && gtTree.toStream().forall(kv -> 
			kv._2().toStream().forall(ltElem -> 
				ltTree.get(ltElem).map(s -> s.member(kv._1())).orSome(false)
			)
		);
	}
	
	private static <V> boolean isReflexiveTree(final TreeMap<V, Set<V>> equivTree) {
		return equivTree.toStream().forall(kv ->
			kv._2().toStream().forall(eq -> 
				equivTree.get(eq).map(v -> v.member(kv._1())).orSome(false)
			)
		);
	}

	private F<ParamRelation.ParamSpec, KLimitAP> mapToLocal(final List<Local> paramLocals) {
		return p -> p.toAP(paramLocals.get(p.pNum));
	}

	public ARState applyRelation(final List<Local> paramLocals, final ParamRelation rel) {
		final F<ParamRelation.ParamSpec, KLimitAP> mapper = this.mapToLocal(paramLocals);
		final F<TreeMap<ParamRelation.ParamSpec, Set<ParamRelation.ParamSpec>>, TreeMap<KLimitAP, Set<KLimitAP>>> mapTree = t ->
				TreeMap.iterableTreeMap(KLimitAP.apOrd, t.toStream().map(kv -> P.p(mapper.f(kv._1()), kv._2().map(KLimitAP.apOrd, mapper))));
		return new ARState(localState, heap, mapTree.f(rel.equivTree), mapTree.f(rel.ltTree), mapTree.f(rel.gtTree), this.continuation);
	}

	@Override public ARState withContinuation(final Continuation<P2<CallSite, Unit>> cont) {
		return new ARState(localState, heap, congruenceClosure, ltClosure, gtClosure, cont);
	}

	@Override public Continuation<P2<CallSite, Unit>> getContinuation() {
		return this.continuation;
	}
}
