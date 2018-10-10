package edu.washington.cse.concerto.interpreter.meta;

import edu.washington.cse.concerto.interpreter.HeapProvider;
import edu.washington.cse.concerto.interpreter.ai.AbstractComparison;
import edu.washington.cse.concerto.interpreter.ai.PathSensitiveAbstractInterpretation;
import edu.washington.cse.concerto.interpreter.ai.StateValueUpdater;
import edu.washington.cse.concerto.interpreter.ai.ValueMonad;
import edu.washington.cse.concerto.interpreter.ai.binop.PathSensitivePrimitiveOperations;
import edu.washington.cse.concerto.interpreter.heap.Heap;
import edu.washington.cse.concerto.interpreter.value.IValue;
import soot.Unit;
import soot.Value;
import soot.jimple.ConditionExpr;
import soot.jimple.EqExpr;
import soot.jimple.IfStmt;
import soot.jimple.NeExpr;
import soot.jimple.NullConstant;
import soot.jimple.Stmt;
import soot.toolkits.scalar.Pair;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * An extensions of the propagating branch interpreter which propagates information
 * implied by the branch condition along each branch.
 */
public abstract class PropagatingBranchInterpreter<AVal, State> extends BranchInterpreterImpl<AVal> {
	private static enum PropagateTarget {
		LEFT,
		RIGHT
	}

	private final PathSensitiveAbstractInterpretation<AVal, ?, ?, ?> psAi;
	private final ValueMonad<AVal> valueMonad;
	
	public PropagatingBranchInterpreter(final PathSensitiveAbstractInterpretation<AVal, ?, ?, ?> psAi, final ValueMonad<AVal> valueMonad) {
		super(valueMonad, psAi);
		this.psAi = psAi;
		this.valueMonad = valueMonad;
	}
	
	protected abstract Heap extractHeap(State s);
	protected abstract State copyForBranch(State s);
	
	public Map<Unit, State> interpretBranch(final IfStmt stmt, final Object op1, final State branchState) {
		final List<Unit> targets = this.interpretBranch(stmt, op1, getBranchProvider(branchState));
		final HashMap<Unit, State> toReturn = new HashMap<>();
		if(targets.size() == 1) {
			toReturn.put(targets.get(0), branchState);
			return toReturn;
		}
		return propagateSame(branchState, targets, toReturn);
	}

	private HeapProvider getBranchProvider(final State branchState) {
		return new HeapProvider() {
			@Override
			public Heap getHeap() {
				return extractHeap(branchState);
			}
			
			@Override
			public Object getState() {
				return branchState;
			}
		};
	}
	
	@SuppressWarnings("unchecked")
	public Map<Unit, State> interpretBranch(final IfStmt stmt, final Object op1, final Object op2, final State branchState, final StateValueUpdater<State> updater) {
		final State instBranchState = branchState;
		final List<Unit> branch = this.interpretBranch(stmt, op1, op2, getBranchProvider(branchState));
		final Map<Unit, State> toReturn = new HashMap<>();
		if(branch.size() == 1) {
			toReturn.put(branch.get(0), branchState);
			return toReturn;
		}
		assert branch.size() == 2;
		if(op1 instanceof IValue && op2 instanceof IValue) {
			if(!Monads.isCombinable((IValue) op1) && !Monads.isCombinable((IValue) op2)) {
				return interpretBranch(stmt, valueMonad.alpha(op1), valueMonad.alpha(op2), instBranchState, updater);
			} else {
				return propagateSame(branchState, branch, toReturn);
			}
		}
		final ConditionExpr expr = (ConditionExpr) stmt.getCondition();
		if(expr.getOp1() instanceof NullConstant) {
			return propagateNullCheck(stmt, op2, expr.getOp2(), branchState, updater, toReturn);
		} else if(expr.getOp2() instanceof NullConstant) {
			return propagateNullCheck(stmt, op1, expr.getOp1(), branchState, updater, toReturn);
		}
		if(op1 instanceof IValue) {
			if(op2 instanceof CombinedValue) {
				assert expr instanceof EqExpr || expr instanceof NeExpr;
				if(expr instanceof EqExpr) {
					propagateForIVandCV((IValue) op1, (CombinedValue) op2, toReturn, instBranchState, expr, stmt.getTarget(), MetaInterpreter.getIfFallthrough(stmt), updater);
				} else {
					propagateForIVandCV((IValue) op1, (CombinedValue) op2, toReturn, instBranchState, expr, MetaInterpreter.getIfFallthrough(stmt), stmt.getTarget(), updater);
				}
			} else if(Monads.isCombinable((IValue) op1)) {
				// we probably shouldn't be here?
				return propagateSame(branchState, branch, toReturn);
			} else {
				if(((IValue) op1).isNonDet()) {
					final AVal aOp1 = valueMonad.alpha(op1);
					final AVal aOp2 = (AVal) op2;
					propagateAlongBranch(aOp1, aOp2, toReturn, instBranchState, stmt, stmt.getTarget(), updater);
					propagateAlongBranch(aOp1, aOp2, toReturn, instBranchState, stmt, MetaInterpreter.getIfFallthrough(stmt), updater);
				} else {
					// AVal
					propagateAVal(stmt, valueMonad.alpha(op1), (AVal) op2, toReturn, instBranchState, PropagateTarget.RIGHT, updater);
				}
			}
		} else if(op1 instanceof CombinedValue) {
			assert expr instanceof EqExpr || expr instanceof NeExpr;
			// sweet god in heaven
			if(op2 instanceof IValue) {
				if(expr instanceof EqExpr) {
					propagateForCVandIV((CombinedValue) op1, (IValue) op2, toReturn, instBranchState, expr, stmt.getTarget(), MetaInterpreter.getIfFallthrough(stmt), updater);
				} else {
					propagateForCVandIV((CombinedValue) op1, (IValue) op2, toReturn, instBranchState, expr, MetaInterpreter.getIfFallthrough(stmt), stmt.getTarget(), updater);
				}
			} else if(op2 instanceof CombinedValue) {
				if(expr instanceof EqExpr) {
					propagateEqCV((CombinedValue)op1, (CombinedValue) op2, expr, instBranchState, toReturn, stmt.getTarget(), updater);
					toReturn.put(MetaInterpreter.getIfFallthrough(stmt), copyForBranch(instBranchState));
				} else {
					propagateEqCV((CombinedValue)op1, (CombinedValue) op2, expr, instBranchState, toReturn, MetaInterpreter.getIfFallthrough(stmt), updater);
					toReturn.put(stmt.getTarget(), copyForBranch(instBranchState));
				}
			} else {
				// CV <=> AVal
				if(expr instanceof EqExpr) {
					propagateForCVandAVal((CombinedValue)op1, (AVal)op2, toReturn, instBranchState, expr, stmt.getTarget(), MetaInterpreter.getIfFallthrough(stmt), updater);
				} else {
					propagateForCVandAVal((CombinedValue)op1, (AVal)op2, toReturn, instBranchState, expr, MetaInterpreter.getIfFallthrough(stmt), stmt.getTarget(), updater);
				}
			}
		} else {
			final AVal aOp1 = (AVal) op1;
			if(op2 instanceof CombinedValue) {
				assert expr instanceof EqExpr || expr instanceof NeExpr;
				if(expr instanceof EqExpr) {
					propagateForAValandCV(aOp1, (CombinedValue)op2, toReturn, instBranchState, expr, (Unit)stmt.getTarget(), MetaInterpreter.getIfFallthrough(stmt), updater);
				} else {
					propagateForAValandCV(aOp1, (CombinedValue)op2, toReturn, instBranchState, expr, MetaInterpreter.getIfFallthrough(stmt), (Unit)stmt.getTarget(), updater);
				}
			} else if(op2 instanceof IValue && Monads.isCombinable((IValue) op2)) {
				propagateSame(instBranchState, branch, toReturn);
			} else if(op2 instanceof IValue) {
				if(((IValue) op2).isNonDet()) {
					final AVal aOp2 = valueMonad.alpha(op2);
					propagateAlongBranch(aOp1, aOp2, toReturn, instBranchState, stmt, stmt.getTarget(), updater);
					propagateAlongBranch(aOp1, aOp2, toReturn, instBranchState, stmt, MetaInterpreter.getIfFallthrough(stmt), updater);
				} else {
					propagateAVal(stmt, aOp1, valueMonad.alpha(op2), toReturn, instBranchState, PropagateTarget.LEFT, updater);
				}
			} else {
				// AVal <=> AVal
				final AVal aOp2 = (AVal) op2;
				propagateAlongBranch(aOp1, aOp2, toReturn, instBranchState, stmt, stmt.getTarget(), updater);
				propagateAlongBranch(aOp1, aOp2, toReturn, instBranchState, stmt, MetaInterpreter.getIfFallthrough(stmt), updater);
			}
		}
		return toReturn;
	}

	private Map<Unit, State> propagateNullCheck(final IfStmt stmt, final Object op1, final Value toPropagateValue, final State branchState,
			final StateValueUpdater<State> updater, final Map<Unit, State> toReturn) {
		final ConditionExpr expr = (ConditionExpr) stmt.getCondition();
		if(expr instanceof NeExpr) {
			return propagateNullnessForTargets(op1, branchState, updater, toReturn, toPropagateValue, MetaInterpreter.getIfFallthrough(stmt), stmt.getTarget());
		} else {
			assert expr instanceof EqExpr;
			return propagateNullnessForTargets(op1, branchState, updater, toReturn, toPropagateValue, stmt.getTarget(), MetaInterpreter.getIfFallthrough(stmt));
		}
	}

	private Map<Unit, State> propagateNullnessForTargets(final Object op1, final State branchState, final StateValueUpdater<State> updater,
			final Map<Unit, State> toReturn, final Value expr,
			final Unit isNullTarget, final Unit nonNullTarget) {
		final PathSensitivePrimitiveOperations<AVal> primOps = psAi.primitiveOperations();
		if(op1 instanceof IValue) {
			final State nullState = copyForBranch(branchState);
			toReturn.put(isNullTarget, updater.updateForValue(expr, nullState, valueMonad.lift(IValue.nullConst())));
			
			final State nonNullState = copyForBranch(branchState);
			toReturn.put(nonNullTarget, updater.updateForValue(expr, nonNullState, valueMonad.lift(IValue.propagator.propagateNE((IValue) op1, IValue.nullConst()))));
		} else if(op1 instanceof CombinedValue) {
			final CombinedValue cv = (CombinedValue) op1;
			final AVal nullAbstraction = valueMonad.alpha(valueMonad.lift(IValue.nullConst()));
			
			final State nullState = copyForBranch(branchState);
			final CombinedValue isNull = new CombinedValue(IValue.nullConst(), nullAbstraction);
			toReturn.put(isNullTarget, updater.updateForValue(expr, nullState, isNull));
			
			final State nonNullState = copyForBranch(branchState);
			@SuppressWarnings("unchecked")
			final CombinedValue isNonNull = new CombinedValue(
					IValue.propagator.propagateNE(cv.concreteComponent, IValue.nullConst()),
					primOps.propagateNE((AVal) cv.abstractComponent, nullAbstraction)
			);
			toReturn.put(nonNullTarget, updater.updateForValue(expr, nonNullState, isNonNull));
		} else {
			@SuppressWarnings("unchecked")
			final AVal aop = (AVal) op1;
			final AVal nullAbstraction = valueMonad.alpha(valueMonad.lift(IValue.nullConst()));
			
			final State nullState = copyForBranch(branchState);
			toReturn.put(isNullTarget, updater.updateForValue(expr, nullState, nullAbstraction));
			
			final State nonNullState = copyForBranch(branchState);
			toReturn.put(nonNullTarget, updater.updateForValue(expr, nonNullState, valueMonad.lift(primOps.propagateNE(aop, nullAbstraction))));
		}
		return toReturn;
	}

	private void propagateAlongBranch(final AVal aOp1, final AVal aOp2, final Map<Unit, State> toReturn,
			final State instBranchState, final IfStmt ifStmt, final Unit target, final StateValueUpdater<State> updater) {
		final ConditionExpr expr = (ConditionExpr) ifStmt.getCondition();
		final Pair<AVal, AVal> propagatedTaken = AbstractComparison.propagateBranch(aOp1, aOp2, ifStmt, target, psAi.primitiveOperations());
		State accum = this.copyForBranch(instBranchState);
		accum = updater.updateForValue(expr.getOp1(), accum, valueMonad.lift(propagatedTaken.getO1()));
		accum = updater.updateForValue(expr.getOp2(), accum, valueMonad.lift(propagatedTaken.getO2()));
		toReturn.put(target, accum);
	}

	public void propagateAVal(final IfStmt stmt, final AVal a1, final AVal a2,
			final Map<Unit, State> toReturn, final State instBranchState, final PropagateTarget propTarget, final StateValueUpdater<State> updater) {
		final ConditionExpr expr = (ConditionExpr) stmt.getCondition();
		// taken branch
		{
			final Stmt targetUnit = stmt.getTarget();
			final Pair<AVal, AVal> propagatedTaken = AbstractComparison.propagateBranch(a1, a2, stmt, stmt.getTarget(), psAi.primitiveOperations());
			propagateAlongBranch(toReturn, instBranchState, propTarget, expr, targetUnit, propagatedTaken, updater);
		}
		{
			
			final Pair<AVal, AVal> propagatedFallthrough = AbstractComparison.propagateBranch(a1, a2, stmt, MetaInterpreter.getIfFallthrough(stmt), psAi.primitiveOperations());
			propagateAlongBranch(toReturn, instBranchState, propTarget, expr, MetaInterpreter.getIfFallthrough(stmt), propagatedFallthrough, updater);
		}
	}

	private void propagateAlongBranch(final Map<Unit, State> toReturn, final State instBranchState,
			final PropagateTarget target, final ConditionExpr expr, final Unit targetUnit, final Pair<AVal, AVal> propagatedTaken, final StateValueUpdater<State> updater) {
		final AVal propTaken;
		final Value targetValue;
		if(target == PropagateTarget.RIGHT) {
			propTaken = propagatedTaken.getO2();
			targetValue = expr.getOp2();
		} else {
			propTaken = propagatedTaken.getO1();
			targetValue = expr.getOp1();
		}
		toReturn.put(targetUnit, updater.updateForValue(targetValue, this.copyForBranch(instBranchState), valueMonad.lift(propTaken)));
	}

	private void propagateForAValandCV(final AVal aOp1, final CombinedValue op2, final Map<Unit, State> toReturn,
			final State instBranchState, final ConditionExpr expr, final Unit eqTarget, final Unit neTarget, final StateValueUpdater<State> updater) {
		if(mayBeEqual(op2.concreteComponent, aOp1)) {
			propagateSame(instBranchState, Arrays.asList(eqTarget, neTarget), toReturn);
			return;
		}
		toReturn.put(eqTarget, propagateEq(aOp1, expr.getOp1(), op2, expr.getOp2(), instBranchState, updater));
		toReturn.put(neTarget, propagateNe(op2, expr.getOp2(), aOp1, instBranchState, updater));
	}

	private void propagateForCVandAVal(final CombinedValue op1, final AVal op2, final Map<Unit, State> toReturn,
			final State instBranchState, final ConditionExpr expr, final Unit equalTarget, final Unit neTarget, final StateValueUpdater<State> updater) {
		if(mayBeEqual(op1.concreteComponent, op2)) {
			propagateSame(instBranchState, Arrays.asList(equalTarget, neTarget), toReturn);
			return;
		}
		toReturn.put(equalTarget, propagateEq(op2, expr.getOp2(), op1, expr.getOp1(), instBranchState, updater));
		toReturn.put(neTarget, propagateNe(op1, expr.getOp1(), op2, instBranchState, updater));
	}

	@SuppressWarnings("unchecked")
	private void propagateEqCV(final CombinedValue op1, final CombinedValue op2, final ConditionExpr expr,
			final State instBranchState, final Map<Unit, State> toReturn, final Unit target, final StateValueUpdater<State> updater) {
		State accum = this.copyForBranch(instBranchState);
		final PathSensitivePrimitiveOperations<AVal> pOps = psAi.primitiveOperations();
		final CombinedValue prop1 = 
			new CombinedValue(IValue.propagator.propagateEQ(op1.concreteComponent, op2.concreteComponent),
					pOps.propagateEQ((AVal)op1.abstractComponent, (AVal) op2.abstractComponent));
		
		final CombinedValue prop2 = 
				new CombinedValue(IValue.propagator.propagateEQ(op2.concreteComponent, op1.concreteComponent),
						pOps.propagateEQ((AVal)op2.abstractComponent, (AVal) op1.abstractComponent));
		
		accum = updater.updateForValue(expr.getOp1(), accum, prop1);
		accum = updater.updateForValue(expr.getOp2(), accum, prop2);
		toReturn.put(target, accum);
	}

	@SuppressWarnings("unchecked")
	private State propagateNe(final CombinedValue cvOp, final Value cvExpr, final AVal aOp, final State state, final StateValueUpdater<State> updater) {
		final CombinedValue c = new CombinedValue(cvOp.concreteComponent, psAi.primitiveOperations().propagateNE((AVal) cvOp.abstractComponent, aOp));
		return updater.updateForValue(cvExpr, copyForBranch(state), c);
	}

	@SuppressWarnings("unchecked")
	private State propagateEq(final AVal aOp, final Value aExpr, final CombinedValue cOp, final Value cvExpr,
			final State instBranchState, final StateValueUpdater<State> updater) {
		State accum = copyForBranch(instBranchState);
		final PathSensitivePrimitiveOperations<AVal> pOps = psAi.primitiveOperations();
		final AVal propA = pOps.propagateEQ(aOp, (AVal) cOp.abstractComponent);
		final AVal propCV = pOps.propagateEQ((AVal) cOp.abstractComponent, aOp);
		accum = updater.updateForValue(cvExpr, accum, propCV);
		accum = updater.updateForValue(aExpr, accum, propA);
		return accum;
	}
	
	@SuppressWarnings("unchecked")
	private void propagateForCVandIV(final CombinedValue op1, final IValue op2, final Map<Unit, State> toReturn,
			final State instBranchState, final ConditionExpr expr, final Unit equalTarget, final Unit neTarget, final StateValueUpdater<State> updater) {
		if(mayBeEqual(op2, (AVal) op1.abstractComponent)) {
			propagateSame(instBranchState, Arrays.asList(equalTarget, neTarget), toReturn);
			return;
		}
		toReturn.put(equalTarget, propagateEq(op2, expr.getOp2(), op1, expr.getOp1(), instBranchState, updater));
		toReturn.put(neTarget, propagateNe(op1, expr.getOp1(), op2, instBranchState, updater));
	}
	
	protected boolean mayBeEqual(final IValue op1, final AVal a2) {
		return !isDefinitelyNotEqual(op1, a2);
	}
	
	@SuppressWarnings("unchecked")
	private void propagateForIVandCV(final IValue op1, final CombinedValue op2, final Map<Unit, State> toReturn,
			final State instBranchState, final ConditionExpr expr, final Unit equalTarget, final Unit neTarget, final StateValueUpdater<State> updater) {
		if(mayBeEqual(op1, (AVal) op2.abstractComponent)) {
			propagateSame(instBranchState, Arrays.asList(equalTarget, neTarget), toReturn);
			return;
		}
		toReturn.put(equalTarget, propagateEq(op1, expr.getOp1(), op2, expr.getOp2(), instBranchState, updater));
		toReturn.put(neTarget, propagateNe(op2, expr.getOp2(), op1, instBranchState, updater));
	}
	
	private State propagateEq(final IValue iOp, final Value iExpr, final CombinedValue cvOp, final Value cvExpr,
			final State instBranchState, final StateValueUpdater<State> updater) {
		State accum = copyForBranch(instBranchState);
		final IValue propI = IValue.propagator.propagateEQ(iOp, cvOp.concreteComponent);
		final IValue propCV = IValue.propagator.propagateEQ(cvOp.concreteComponent, iOp);
		accum = updater.updateForValue(iExpr, accum, valueMonad.lift(propI));
		accum = updater.updateForValue(cvExpr, accum, valueMonad.lift(propCV));
		return accum;
	}
	
	private State propagateNe(final CombinedValue cvOp, final Value cvExpr, final IValue iOp, final State state, final StateValueUpdater<State> updater) {
		final CombinedValue c = new CombinedValue(IValue.propagator.propagateNE(cvOp.concreteComponent, iOp), cvOp.abstractComponent);
		return updater.updateForValue(cvExpr, copyForBranch(state), c);
	}
	
	private Map<Unit, State> propagateSame(final State branchState, final List<Unit> branch, final Map<Unit, State> toReturn) {
		toReturn.put(branch.get(0), copyForBranch(branchState));
		toReturn.put(branch.get(1), copyForBranch(branchState));
		return toReturn;
	}
}
