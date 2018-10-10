package edu.washington.cse.concerto.interpreter.meta;

import edu.washington.cse.concerto.interpreter.BodyManager;
import edu.washington.cse.concerto.interpreter.ExpressionInterpreter;
import edu.washington.cse.concerto.interpreter.HeapProvider;
import edu.washington.cse.concerto.interpreter.ai.AbstractComparison;
import edu.washington.cse.concerto.interpreter.ai.AbstractInterpretation;
import edu.washington.cse.concerto.interpreter.ai.CompareResult;
import edu.washington.cse.concerto.interpreter.ai.ValueMonad;
import edu.washington.cse.concerto.interpreter.ai.binop.ObjectIdentityResult;
import edu.washington.cse.concerto.interpreter.ai.binop.PrimitiveOperations;
import edu.washington.cse.concerto.interpreter.value.IValue;
import edu.washington.cse.concerto.interpreter.value.IValue.RuntimeTag;
import fj.Ordering;
import soot.PatchingChain;
import soot.RefLikeType;
import soot.Type;
import soot.Unit;
import soot.jimple.ConditionExpr;
import soot.jimple.EqExpr;
import soot.jimple.IfStmt;
import soot.jimple.InstanceOfExpr;
import soot.jimple.NeExpr;
import soot.jimple.NullConstant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * This class interprets branches, and using the hooks in the abstract interpretation,
 * provides a convenience method for deciding what branches are taken.
 * 
 * The following checks are supported:
 * a) Equality checks
 * b) instanceof checks
 * c) primitive operations
 * d) explicit null pointer checks
 * 
 * These are done using the abstract value definitions AVal. AI's are more than welcome
 * to ignore this class (but must still implement the hooks for the benefit of the semi-concrete
 * interpreter)
 */
public class BranchInterpreterImpl<AVal> {
	private final ValueMonad<AVal> valueMonad;
	private final AbstractInterpretation<AVal, ?, ?, ?> ai;

	protected BranchInterpreterImpl(final ValueMonad<AVal> valueMonad, final AbstractInterpretation<AVal, ?, ?, ?> ai) {
		this.ai = ai;
		this.valueMonad = valueMonad;
	}

	private boolean isDefinitelyNotEqual(final IValue a, final IValue b) {
		return a.isDefinitelyNotEqual(b);
	}
	
	private boolean isDefinitelyNotEqual(final AVal a1, final AVal a2) {
		final PrimitiveOperations<AVal> primOps = ai.primitiveOperations();
		final CompareResult cmp = primOps.cmp(a1, a2);
		return !cmp.hasFlag(Ordering.EQ);
	}
	
	protected boolean isDefinitelyNotEqual(final IValue op1, final AVal op2) {
		// op1 is not null, and op2 may be null, but either way it doesn't matter
		if(isDefinitelyNotEqual(op1, IValue.nullConst())) {
			return true;
		}
		// op1 may be null. Can op2? If so, the two may be equal
		if(ai.objectOperations().isNull(op2) != ObjectIdentityResult.MUST_NOT_BE) {
			// op2 must or may be null, and op1 may be null, so they may be equal, and are therefore NOT definitely not equal
			return false;
		} else {
			// op2 must not be null, so even tho op1 is null, the two are not equal
			return true;
		}
	}
	
	@SuppressWarnings("unchecked")
	public List<Unit> interpretBranch(final IfStmt stmt, final Object op1, final HeapProvider state) {
		assert stmt.getCondition() instanceof InstanceOfExpr;
		final InstanceOfExpr ioexpr = (InstanceOfExpr) stmt.getCondition();
		final Type t = ioexpr.getCheckType();
		final ObjectIdentityResult res;
		if(op1 instanceof IValue) {
			final IValue iVal = (IValue) op1;
			res = iVal.isInstanceOf(t);
		} else if(op1 instanceof CombinedValue) {
			final CombinedValue combinedValue = (CombinedValue) op1;
			res = combinedValue.concreteComponent.isInstanceOf(t).join(ai.objectOperations().isInstanceOf((AVal) combinedValue.abstractComponent, t));
		} else {
			res = ai.objectOperations().isInstanceOf((AVal) op1, t);
		}
		if(res == ObjectIdentityResult.MUST_NOT_BE) {
			return fallthrough(stmt);
		} else if(res == ObjectIdentityResult.MUST_BE) {
			return taken(stmt);
		} else {
			return unknown(stmt); 
		}
	}
	
	@SuppressWarnings("unchecked")
	public List<Unit> interpretBranch(final IfStmt stmt, final Object op1, final Object op2, final HeapProvider state) {
		final ConditionExpr condition = (ConditionExpr) stmt.getCondition();
		if(op1 instanceof IValue && op2 instanceof IValue) {
			final ExpressionInterpreter eInterp = new ExpressionInterpreter(state);
			final IValue res = eInterp.interpretWith(condition, (IValue) op1, (IValue)op2);
			if(res.isNonDet()) {
				return unknown(stmt);
			} else if(res.asBoolean()) {
				return taken(stmt);
			} else {
				return fallthrough(stmt);
			}
		}
		if(condition.getOp1() instanceof NullConstant || condition.getOp2() instanceof NullConstant) {
			return this.resolveNullCheck(stmt, op1, op2, state);
		}
		if(op1 instanceof IValue && !Monads.isCombinable((IValue) op1)) {
			return interpretBranch(stmt, valueMonad.alpha(op1), op2, state);
		} else if(op2 instanceof IValue && !Monads.isCombinable((IValue)op2)) {
			return interpretBranch(stmt, op1, valueMonad.alpha(op2), state);
		}
		if(op2 instanceof IValue || op1 instanceof IValue || op1 instanceof CombinedValue || op2 instanceof CombinedValue) {
			assert condition.getOp1().getType() instanceof RefLikeType;
			assert condition.getOp2().getType() instanceof RefLikeType;
		}
		
		if(op1 instanceof IValue && !(op2 instanceof CombinedValue)) {
			return resolve(stmt, isDefinitelyNotEqual((IValue)op1, (AVal) op2));
		}
		if(op2 instanceof IValue && !(op1 instanceof CombinedValue)) {
			return resolve(stmt, isDefinitelyNotEqual((IValue)op2, (AVal) op1));
		}
		
		// we have ruled out IValue(h) <=> AVal
		// we also cannot have IValue(h) <=> IValue(h) (above case)
		// so at this point, we have CV <=> IValue or AVal <=> AVal or AVal <=> CV or CV <=> CV
		// so if neither op is CV, it is safe to case to AVal
		if(op1 instanceof CombinedValue) {
			final CombinedValue cv1 = (CombinedValue) op1;
			assert condition instanceof EqExpr || condition instanceof NeExpr;
			if(op2 instanceof CombinedValue) {
				final CombinedValue cv2 = (CombinedValue) op2;
				final boolean cNotEq = isDefinitelyNotEqual(cv1.concreteComponent, cv2.concreteComponent);
				final boolean aNotEq = isDefinitelyNotEqual((AVal)cv1.abstractComponent, (AVal)cv2.abstractComponent);
				final boolean caNotEq1 = isDefinitelyNotEqual(cv1.concreteComponent, (AVal) cv2.abstractComponent);
				final boolean caNotEq2 = isDefinitelyNotEqual(cv2.concreteComponent, (AVal) cv1.abstractComponent);
				return resolve(stmt, cNotEq && aNotEq && caNotEq1 && caNotEq2);
			} else if(op2 instanceof IValue) {
				return resolve(stmt, isDefinitelyNotEqual(cv1.concreteComponent, (IValue) op2) && isDefinitelyNotEqual((IValue) op2, (AVal)cv1.abstractComponent));
			} else {
				return resolve(stmt, isDefinitelyNotEqual((AVal) cv1.abstractComponent, (AVal)op2) && isDefinitelyNotEqual(cv1.concreteComponent, (AVal)op2));
			}
		} else if(op2 instanceof CombinedValue) {
			final CombinedValue cv2 = (CombinedValue) op2;
			assert condition instanceof EqExpr || condition instanceof NeExpr;
			if(op1 instanceof IValue) {
				return resolve(stmt, isDefinitelyNotEqual((IValue) op1, cv2.concreteComponent) && isDefinitelyNotEqual((IValue)op1, (AVal)cv2.abstractComponent));
			} else {
				return resolve(stmt, isDefinitelyNotEqual((AVal)op1, (AVal)cv2.abstractComponent) && isDefinitelyNotEqual(cv2.concreteComponent, (AVal)op1));
			}
		} else {
			// okay, we have AVal <=> AVal
			final PrimitiveOperations<AVal> primitiveOperations = ai.primitiveOperations();
			final CompareResult cmp = primitiveOperations.cmp((AVal)op1, (AVal)op2);
			return AbstractComparison.abstractComparison(condition, cmp, unknown(stmt), taken(stmt), fallthrough(stmt));
		}
	}
	
	private List<Unit> resolveNullCheck(final IfStmt stmt, final Object op1, final Object op2, final HeapProvider state) {
		final ConditionExpr condition = (ConditionExpr) stmt.getCondition();
		if(condition.getOp1() instanceof NullConstant) {
			return resolveNullCmpOp(stmt, op2);
		} else {
			assert condition.getOp2() instanceof NullConstant;
			return resolveNullCmpOp(stmt, op1);
		}
	}

	private List<Unit> resolveNullCmpOp(final IfStmt stmt, final Object toCmp) {
		final ConditionExpr condition = (ConditionExpr) stmt.getCondition();
		if(toCmp instanceof IValue) {
			final IValue iValue = (IValue) toCmp;
			if(iValue.getTag() == RuntimeTag.NULL) {
				if(condition instanceof EqExpr) {
					return taken(stmt);
				} else {
					assert condition instanceof NeExpr;
					return fallthrough(stmt);
				}
			}
			return resolve(stmt, isDefinitelyNotEqual(iValue, IValue.nullConst()));
		} else if(toCmp instanceof CombinedValue) {
			final CombinedValue cv = (CombinedValue) toCmp;
			final IValue concreteComponent = cv.concreteComponent;
			@SuppressWarnings("unchecked")
			final AVal abstractComponent = (AVal) cv.abstractComponent;
			// we can only prove non-nullness
			final boolean isDefinitelyNotNull = ai.objectOperations().isNull(abstractComponent) == ObjectIdentityResult.MUST_NOT_BE &&
				isDefinitelyNotEqual(concreteComponent, IValue.nullConst());
			if(isDefinitelyNotNull && condition instanceof EqExpr) {
				return fallthrough(stmt);
			} else if(isDefinitelyNotNull && condition instanceof NeExpr) {
				return taken(stmt);
			} else {
				return unknown(stmt);
			}
		} else {
			@SuppressWarnings("unchecked")
			final AVal aop = (AVal) toCmp;
			final ObjectIdentityResult nullness = ai.objectOperations().isNull(aop);
			if(nullness == ObjectIdentityResult.MAY_BE) {
				return unknown(stmt);
			} else if(nullness == ObjectIdentityResult.MUST_BE) {
				if(condition instanceof EqExpr) {
					return taken(stmt);
				} else {
					return fallthrough(stmt);
				}
			} else {
				assert nullness == ObjectIdentityResult.MUST_NOT_BE;
				if(condition instanceof EqExpr) {
					return fallthrough(stmt);
				} else {
					return taken(stmt);
				}
			}
		}
	}

	private List<Unit> resolve(final IfStmt stmt, final boolean isDefinitelyNotEqual) {
		final ConditionExpr cond = (ConditionExpr) stmt.getCondition();
		assert cond instanceof EqExpr || cond instanceof NeExpr;
		if(isDefinitelyNotEqual && cond instanceof EqExpr) {
			return fallthrough(stmt);
		} else if(isDefinitelyNotEqual && cond instanceof NeExpr) {
			return taken(stmt);
		} else {
			final List<Unit> toReturn = unknown(stmt);
			return toReturn;
		}
	}

	protected List<Unit> unknown(final IfStmt stmt) {
		final PatchingChain<Unit> chain = BodyManager.retrieveBody(BodyManager.getHostMethod(stmt)).getUnits();
		final List<Unit> toReturn = new ArrayList<>();
		toReturn.add(stmt.getTarget());
		toReturn.add(chain.getSuccOf(stmt));
		return toReturn;
	}

	protected List<Unit> taken(final IfStmt stmt) {
		return Collections.<Unit>singletonList(stmt.getTarget());
	}

	protected List<Unit> fallthrough(final IfStmt stmt) {
		final Unit succ = MetaInterpreter.getIfFallthrough(stmt);
		return Collections.singletonList(succ);
	}
}
