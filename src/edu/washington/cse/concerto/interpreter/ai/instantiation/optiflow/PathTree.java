package edu.washington.cse.concerto.interpreter.ai.instantiation.optiflow;

import edu.washington.cse.concerto.interpreter.ai.instantiation.pta.AbstractAddress;
import edu.washington.cse.concerto.interpreter.lattice.Lattice;
import fj.F;
import fj.F2;
import fj.Ord;
import fj.P2;
import fj.data.Seq;
import fj.data.Set;
import fj.data.TreeMap;
import soot.RefLikeType;
import soot.Scene;
import soot.SootFieldRef;
import soot.Type;

public class PathTree implements PV {
	public final static int K_LIMIT = 5;

	public static Lattice<PathTree> lattice = new Lattice<PathTree>() {

		int widenId = 0;
		@Override
		public PathTree widen(final PathTree prev, final PathTree next) {
			return this.join(prev, next);
		}

		@Override
		public boolean lessEqual(final PathTree first, final PathTree second) {
			return (second.root == TaintFlag.Taint || first.root == TaintFlag.NoTaint) && lessEqual(first, second, Seq.empty());
		}

		private boolean lessEqual(final PathTree first, final PathTree second, final Seq<Set<String>> trFieldAccum) {
			final boolean basicImplication = first.summaryFields.toStream().forall(sf ->
					second.summaryFields.member(sf) || taintImpliedByPath(sf, trFieldAccum)
			);
			if(!basicImplication) {
				return false;
			}
			return first.fields.toStream().forall(new F<P2<String,PathTree>, Boolean>() {
				@Override
				public Boolean f(final P2<String, PathTree> firstBinding) {
					final String k = firstBinding._1();
					final PathTree firstTarget = firstBinding._2();
					if(firstTarget.root != TaintFlag.NoTaint && (!second.fields.contains(k) || second.fields.get(k).some().root == TaintFlag.NoTaint) && !taintImpliedByPath(k, trFieldAccum)) {
						return false;
					}
					if(second.fields.contains(k)) {
						return lessEqual(firstTarget, second.fields.get(k).some(), trFieldAccum.cons(second.summaryFields));
					} else {
						return checkImpliedTaints(firstTarget, trFieldAccum.cons(second.summaryFields));
					}
				}
			});
		}

		private boolean checkImpliedTaints(final PathTree firstTarget, final Seq<Set<String>> cons) {
			if(cons.isEmpty()) {
				return false;
			}
			final Set<String> impliedTaints = cons.toStream().foldLeft1(Set::union);
			return firstTarget.checkImpliedTaints(impliedTaints);
		}

		private boolean taintImpliedByPath(final String k, final Seq<Set<String>> trFieldAccum) {
			return trFieldAccum.toStream().exists(ss -> ss.member(k));
		}

		@Override
		public PathTree join(final PathTree first, final PathTree second) {
			final TreeMap<String, PathTree> joinedFields = joinFields(first.fields, second.fields);
			final Set<String> joinedTrFields = first.summaryFields.union(second.summaryFields);
			return new PathTree(joinedFields, joinedTrFields, TaintFlag.lattice.join(first.root, second.root), Math.max(first.height, second.height));
		}

		private TreeMap<String, PathTree> joinFields(final TreeMap<String, PathTree> f1, final TreeMap<String, PathTree> f2) {
			return f1.toStream().foldLeft(new F2<TreeMap<String, PathTree>, P2<String, PathTree>, TreeMap<String, PathTree>>() {
				@Override
				public TreeMap<String, PathTree> f(final TreeMap<String, PathTree> a, final P2<String, PathTree> b) {
					return a.update(b._1(), new F<PathTree, PathTree>() {

						@Override
						public PathTree f(final PathTree a) {
							return join(a, b._2());
						}
					}, b._2());
				}
			}, f2);
		}
	};

	private boolean checkImpliedTaints(final Set<String> impliedTaints) {
		return this.summaryFields.subsetOf(impliedTaints) && this.fields.toStream().forall(kv -> {
			if(kv._2().root != TaintFlag.NoTaint && !impliedTaints.member(kv._1())) {
				return false;
			}
			return kv._2().checkImpliedTaints(impliedTaints);
		});
	}

	private static final TreeMap<String, PathTree> emptyFields = TreeMap.empty(Ord.stringOrd);
	public static final PathTree bottom = new PathTree(emptyFields, Set.empty(Ord.stringOrd), TaintFlag.NoTaint, 0);

	private final TreeMap<String, PathTree> fields;
	private final Set<String> summaryFields;
	private final int height;
	private final TaintFlag root;

	public PathTree(final TreeMap<String, PathTree> fields, final Set<String> summaryFields, final TaintFlag root, final int height) {
		this.fields = fields;
		this.summaryFields = summaryFields;
		this.root = root;
		this.height = height;
	}
	
	public PathTree(final TreeMap<String, PathTree> fields, final Set<String> summaryFields, final int height) {
		this(fields, summaryFields, TaintFlag.NoTaint, height);
	}

	public PathTree(final TreeMap<String, PathTree> tr, final int height) {
		this(tr, Set.empty(Ord.stringOrd), height);
	}

	public PathTree(final TaintFlag root) {
		this(emptyFields, Set.empty(Ord.stringOrd), root, 0);
	}
	
	private PathTree internalRead(final String key, final Type t) {
		if(fields.contains(key)) {
			return copySummaryFields(t, fields.get(key).some());
		} else if(summaryFields.member(key)) {
			final PathTree tr = new PathTree(TaintFlag.Taint);
			return copySummaryFields(t, tr);
		} else {
			return copySummaryFields(t, PathTree.bottom);
		}
	}

	public PathTree readField(final SootFieldRef fieldRef) {
		final String key = fieldRef.getSignature();
		final Type fieldType = Scene.v().getField(key).getType();
		return internalRead(key, fieldType);
	}

	private PathTree copySummaryFields(final Type t, final PathTree tr) {
		final boolean refType = t instanceof RefLikeType;
		if(refType && !summaryFields.isEmpty()) {
			return new PathTree(
					summaryFields.toStream().foldLeft((acc, sField) ->
						acc.update(sField, pt -> pt.withRoot(TaintFlag.Taint), new PathTree(TaintFlag.Taint)),
					tr.fields), tr.summaryFields.union(summaryFields), tr.root, Math.min(1, tr.height));
		} else {
			return tr;
		}
	}

	private PathTree withRoot(final TaintFlag taint) {
		return new PathTree(this.fields, this.summaryFields, this.root.join(taint), height);
	}

	public PathTree readArray(final Type typeHint) {
		return internalRead("*", typeHint);
	}

	public static PathTree lift(final Seq<String> fields, final PathTree value) {
		assert fields.isNotEmpty();
		final int numFields = fields.length();
		if(numFields + value.height > K_LIMIT) {
			if(numFields >= K_LIMIT) {
				final PathTree collapsed = value.collapseToSummary();
				final Seq<String> pref = fields.take(K_LIMIT - collapsed.height);
				assert pref.length() + collapsed.height <= K_LIMIT;
				return lift(pref, collapsed);
			} else {
				final int maxLength = K_LIMIT - numFields;
				assert maxLength > 0;
				final PathTree collapsed = value.compressToHeight(maxLength);
				assert numFields + collapsed.height <= K_LIMIT;
				return lift(fields, collapsed);
			}
		}
		return fields.toStream().reverse().foldLeft((accum, fieldName) ->
			new PathTree(emptyFields.set(fieldName, accum), Set.empty(Ord.stringOrd), TaintFlag.NoTaint, accum.height + 1),
			value
		);
	}

	private PathTree compressToHeight(final int maxLength) {
		if(this.height <= maxLength) {
			return this;
		}
		if(maxLength == 1) {
			return this.collapseToSummary();
		} else {
			return new PathTree(this.fields.map(pt -> pt.compressToHeight(maxLength - 1)), this.summaryFields, this.root, maxLength);
		}
	}

	private PathTree collapseToSummary() {
		final TreeMap<String, PathTree> directTaints = this.fields.toStream().foldLeft((accum, kv) -> {
			if(kv._2().root == TaintFlag.Taint) {
				return accum.update(kv._1(), kv._2().root::join, kv._2().root);
			} else {
				return accum;
			}
		}, TreeMap.<String, TaintFlag>empty(Ord.stringOrd)).map(PathTree::new);
		final Set<String> summaryFields = this.fields.toStream().foldLeft((accum, kv) -> {
			return accum.union(kv._2().getFieldTaints());
		}, this.summaryFields);
		return new PathTree(directTaints, summaryFields, this.root, directTaints.isEmpty() && summaryFields.isEmpty() ? 0 : 1);
	}

	/*
	 * Return a set of fields. For each field f in this set,
	 * there exists some path through the heap ending in f such that the value accessed
	 * along that path is tainted.
	 */
	private Set<String> getFieldTaints() {
		return this.fields.toStream().foldLeft((accum, kv) -> {
			final Set<String> tr = accum.union(kv._2().getFieldTaints());
			if(kv._2().root == TaintFlag.NoTaint) {
				return tr;
			} else {
				return tr.insert(kv._1());
			}
		}, this.summaryFields);
	}

	public PathTree updatePath(final Seq<String> fields, final PathTree value) {
		return this.updatePath(fields, value, 0);
	}

	private PathTree updatePath(final Seq<String> fields, final PathTree value, final int currDepth) {
		assert fields.isNotEmpty();
		final TreeMap<String, PathTree> updated;
		final int newHeight = 0;
		final int numFields = fields.length();
		if(currDepth == K_LIMIT - 1 && value.height > 0) {
			assert this.height <= 1;
			if(value.root != TaintFlag.NoTaint && numFields == 1) {
				// we can be precise in storing the root taint
				// everything else gets summarized
				return new PathTree(this.fields.set(fields.last(), new PathTree(value.root)), this.summaryFields.union(value.getFieldTaints()), this.root, 1);
			} else {
				final Set<String> summFields = value.root == TaintFlag.Taint ? this.summaryFields.insert(fields.last()) : this.summaryFields;
				return new PathTree(this.fields, summFields.union(value.getFieldTaints()), this.root, 1);
			}
		// just the taint flag
		} else if(currDepth == K_LIMIT - 1 && value.height == 0) {
			// how many fields? if it's just one, we can fit within our height bound
			assert value.fields.isEmpty() && value.summaryFields.isEmpty();
			if(numFields == 1) {
				return new PathTree(this.fields.set(fields.head(), value), this.summaryFields, this.root, Math.max(1, this.height));
			// we'll need at least two field accesses, so add the taint to the summary
			} else {
				return new PathTree(this.fields, this.summaryFields.insert(fields.last()), this.root, Math.max(1, this.height));
			}
		}
		// okay, we have some wiggle room still
		Set<String> newSummary;
		if(!this.summaryFields.isEmpty()) {
			newSummary = this.summaryFields.union(value.getFieldTaints());
			if(numFields > 1 && value.root != TaintFlag.Taint) {
				newSummary = newSummary.insert(fields.last());
			}
		} else {
			newSummary = this.summaryFields;
		}
		if(numFields == 1) {
			if(currDepth + 1 + value.height > K_LIMIT) {
				assert K_LIMIT - (currDepth + 1) >= 1;
				return this.mergeAtField(fields.head(), value.compressToHeight(K_LIMIT - (currDepth + 1)), newSummary);
			} else {
				return new PathTree(this.fields.set(fields.head(), value), newSummary, this.root, Math.max(height, 1 + value.height));
			}
		} else {
			if(this.fields.contains(fields.head())) {
				final PathTree subTree = this.fields.get(fields.head()).some().updatePath(fields.tail(), value, currDepth + 1);
				return new PathTree(this.fields.set(fields.head(), subTree), newSummary, this.root, Math.max(this.height, subTree.height + 1));
			} else {
				final PathTree rest = lift(fields.tail(), value).compressToHeight(K_LIMIT - (currDepth + 1));
				return new PathTree(this.fields.set(fields.head(), rest), newSummary, this.root, Math.max(this.height, rest.height + 1));
			}
		}
	}

	private PathTree mergeAtField(final String head, final PathTree pathTree, final Set<String> summary) {
		if(this.fields.contains(head)) {
			final PathTree merged = PathTree.lattice.join(this.fields.get(head).some(), pathTree);
			return new PathTree(this.fields.set(head, merged), summary, this.root, Math.max(height, 1 + merged.height));
		} else {
			return pathTree;
		}
	}

	@Override
	public boolean isBottom() {
		return this.root == TaintFlag.NoTaint && fields.isEmpty() && summaryFields.isEmpty();
	}
	
	@Override
	public String toString() {
		if(this.fields.isEmpty() && this.summaryFields.isEmpty()) {
			assert this.root != null;
			return this.root.toString();
		}
		final StringBuilder sb = new StringBuilder();
		sb.append("{").append(root).append(": ");
		if(this.fields.isEmpty()) {
			// tr fields is not empty
			appendSummaryFields(sb);
		} else if(this.summaryFields.isEmpty()) {
			// fields is not empty
			appendDirectFields(sb);
		} else {
			appendDirectFields(sb);
			appendSummaryFields(sb);
		}
		sb.setLength(sb.length() - 1);
		sb.append("}");
		return sb.toString();
	}

	private void appendDirectFields(final StringBuilder sb) {
		this.fields.forEach(it -> sb.append("(").append(it._1()).append(" => ").append(it._2()).append(");"));
	}

	protected void appendSummaryFields(final StringBuilder sb) {
		this.summaryFields.forEach(st -> {
			sb.append("[+ => ").append(st).append(" => ").append("Taint").append("]; ");
		});
	}

	public PathTree killPath(final Seq<String> toKillPath) {
		if(!this.fields.contains(toKillPath.head())) {
			return this;
		} else if(toKillPath.tail().isEmpty()) {
			final TreeMap<String, PathTree> deleted = fields.delete(toKillPath.head());
			return postKill(deleted);
		} else {
			final PathTree sub = this.fields.get(toKillPath.head()).some();
			final PathTree deleted = sub.killPath(toKillPath.tail());
			final TreeMap<String, PathTree> withDeletedPath;
			if(deleted.isBottom()) {
				withDeletedPath = this.fields.delete(toKillPath.head());
			} else {
				withDeletedPath = this.fields.set(toKillPath.head(),  deleted);
			}
			return postKill(withDeletedPath);
		}
	}

	protected PathTree postKill(final TreeMap<String, PathTree> deleted) {
		if(deleted.isEmpty() && this.summaryFields.isEmpty() && this.root == TaintFlag.NoTaint) {
			return PathTree.bottom;
		} else {
			return new PathTree(deleted, this.summaryFields, this.root, calcHeight(deleted, this.summaryFields));
		}
	}

	private int calcHeight(final TreeMap<String, PathTree> deleted, final Set<String> summaryFields) {
		final int seed = summaryFields.isEmpty() ? 0 : 1;
		return deleted.toStream().map(kv -> kv._2().height + 1).cons(seed).foldLeft1(Math::max);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fields == null) ? 0 : fields.hashCode());
		result = prime * result + ((root == null) ? 0 : root.hashCode());
		result = prime * result + ((summaryFields == null) ? 0 : summaryFields.hashCode());
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
		final PathTree other = (PathTree) obj;
		if(fields == null) {
			if(other.fields != null) {
				return false;
			}
		} else if(!fields.equals(other.fields)) {
			return false;
		}
		if(root != other.root) {
			return false;
		}
		if(summaryFields == null) {
			if(other.summaryFields != null) {
				return false;
			}
		} else if(!summaryFields.equals(other.summaryFields)) {
			return false;
		}
		return true;
	}

	@Override public PV mapAddress(final F<Set<AbstractAddress>, Set<AbstractAddress>> mapper) {
		return this;
	}
}
