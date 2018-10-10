package edu.washington.cse.concerto.interpreter.ai;

import edu.washington.cse.concerto.interpreter.ai.binop.PathSensitivePrimitiveOperations;
import fj.Ordering;
import soot.Unit;
import soot.jimple.ConditionExpr;
import soot.jimple.EqExpr;
import soot.jimple.GeExpr;
import soot.jimple.GtExpr;
import soot.jimple.IfStmt;
import soot.jimple.LeExpr;
import soot.jimple.LtExpr;
import soot.jimple.NeExpr;
import soot.toolkits.scalar.Pair;

public class AbstractComparison {
	public static <Ret> Ret abstractComparison(final ConditionExpr condition, final CompareResult compare, final Ret unknown, final Ret takeBranch, final Ret fallThrough) {
		if(compare == CompareResult.NO_RESULT) {
			return null;
		} else if(compare == CompareResult.NONDET) {
			return unknown;
		}
		if(condition instanceof LtExpr) {
			if(compare == CompareResult.LT) {
				return takeBranch;
			} else if(!compare.hasFlag(Ordering.LT)) {
				return fallThrough;
			}
		} else if(condition instanceof LeExpr) {
			// we are definitely NOT greater (take branch)
			if(!compare.hasFlag(Ordering.GT)) {
				return takeBranch;
			} else if(compare == CompareResult.GT) {
				// we are definitely greater (fall through)
				return fallThrough;
			}
		} else if(condition instanceof EqExpr && compare == CompareResult.EQ) {
			return takeBranch;
		} else if(condition instanceof EqExpr && !compare.hasFlag(Ordering.EQ)) {
			return fallThrough;
		} else if(condition instanceof NeExpr) {
			if(!compare.hasFlag(Ordering.EQ)) {
				return takeBranch;
			} else if(compare == CompareResult.EQ) {
				return fallThrough;
			}
		} else if(condition instanceof GtExpr) {
			if(compare == CompareResult.GT) {
				return takeBranch;
			} else if(!compare.hasFlag(Ordering.GT)) {
				return fallThrough;
			}
		} else if(condition instanceof GeExpr) {
			// we are greater, equal, or both, take branch
			if(!compare.hasFlag(Ordering.LT)) {
				return takeBranch;
			// we are definitely less than (aka not greater or equal) fall through
			} else if(compare == CompareResult.LT) {
				return fallThrough;
			}
		}
		return unknown;
	}
	
	public static <AVal> AVal propagateLeft(final AVal leftOp, final AVal rightOp, final IfStmt ifStmt, final Unit succ,
			final PathSensitivePrimitiveOperations<AVal> primOperations) {
		final Pair<AVal, AVal> propagated = propagateBranch(leftOp, rightOp, ifStmt, succ, primOperations);
		return propagated.getO1();
	}
	
	public static <AVal> AVal propagateRight(final AVal leftOp, final AVal rightOp, final IfStmt ifStmt, final Unit succ,
			final PathSensitivePrimitiveOperations<AVal> primOperations) {
		final Pair<AVal, AVal> propagated = propagateBranch(leftOp, rightOp, ifStmt, succ, primOperations);
		return propagated.getO2();
	}

	public static <AVal> Pair<AVal, AVal> propagateBranch(final AVal leftOp, final AVal rightOp, final IfStmt ifStmt, final Unit succ,
			final PathSensitivePrimitiveOperations<AVal> primOperations) {
		final boolean isTrueBranch = ifStmt.getTarget() == succ;
		final ConditionExpr expr = (ConditionExpr) ifStmt.getCondition();
		final Pair<AVal, AVal> propagated;
		if(expr instanceof LtExpr) {
			if(isTrueBranch) {
				propagated = new Pair<>(primOperations.propagateLT(leftOp, rightOp), primOperations.propagateGT(rightOp, leftOp));
			} else {
				propagated = new Pair<>(primOperations.propagateGE(leftOp, rightOp), primOperations.propagateLE(rightOp, leftOp));
			}

		} else if(expr instanceof LeExpr) {
			if(isTrueBranch) {
				propagated = new Pair<>(primOperations.propagateLE(leftOp, rightOp), primOperations.propagateGE(rightOp, leftOp));
			} else {
				propagated = new Pair<>(primOperations.propagateGT(leftOp, rightOp), primOperations.propagateLT(rightOp, leftOp));
			}

		} else if(expr instanceof EqExpr) {
			if(isTrueBranch) {
				propagated = new Pair<>(primOperations.propagateEQ(leftOp, rightOp), primOperations.propagateEQ(rightOp, leftOp));
			} else {
				propagated = new Pair<>(primOperations.propagateNE(leftOp, rightOp), primOperations.propagateNE(rightOp, leftOp));
			}

		} else if(expr instanceof NeExpr) {
			if(isTrueBranch) {
				propagated = new Pair<>(primOperations.propagateNE(leftOp, rightOp), primOperations.propagateNE(rightOp, leftOp));
			} else {
				propagated = new Pair<>(primOperations.propagateEQ(leftOp, rightOp), primOperations.propagateEQ(rightOp, leftOp));
			}

		} else if(expr instanceof GtExpr) {
			if(isTrueBranch) {
				propagated = new Pair<>(primOperations.propagateGT(leftOp, rightOp), primOperations.propagateLT(rightOp, leftOp));
			} else {
				propagated = new Pair<>(primOperations.propagateLE(leftOp, rightOp), primOperations.propagateGE(rightOp, leftOp));
			}

		} else if(expr instanceof GeExpr) {
			if(isTrueBranch) {
				propagated = new Pair<>(primOperations.propagateGE(leftOp, rightOp), primOperations.propagateLE(rightOp, leftOp));
			} else {
				propagated = new Pair<>(primOperations.propagateLT(leftOp, rightOp), primOperations.propagateGT(rightOp, leftOp));
			}
		} else {
			throw new RuntimeException();
		}
		return propagated;
	}

}
