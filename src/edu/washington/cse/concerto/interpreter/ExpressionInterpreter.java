package edu.washington.cse.concerto.interpreter;

import soot.jimple.AbstractExprSwitch;
import soot.jimple.AddExpr;
import soot.jimple.BinopExpr;
import soot.jimple.DivExpr;
import soot.jimple.EqExpr;
import soot.jimple.Expr;
import soot.jimple.GeExpr;
import soot.jimple.GtExpr;
import soot.jimple.LeExpr;
import soot.jimple.LtExpr;
import soot.jimple.MulExpr;
import soot.jimple.NeExpr;
import soot.jimple.SubExpr;
import edu.washington.cse.concerto.interpreter.value.IValue;
import edu.washington.cse.concerto.interpreter.value.IValueTransformer;

public class ExpressionInterpreter extends AbstractExprSwitch {
	protected final HeapProvider es;
	protected IValue op2;
	protected IValue op1;

	public ExpressionInterpreter(final HeapProvider es) {
		this.es = es;
	}

	@Override
	public void caseAddExpr(final AddExpr v) {
		apply(ConcreteBinOps.ADD, v);
	}

	@Override
	public void caseDivExpr(final DivExpr v) {
		apply(ConcreteBinOps.DIV, v);
	}

	@Override
	public void caseEqExpr(final EqExpr v) {
		apply(ConcreteBinOps.EQ, v);
	}

	@Override
	public void caseGeExpr(final GeExpr v) {
		apply(ConcreteBinOps.GE, v);
	}

	@Override
	public void caseGtExpr(final GtExpr v) {
		apply(ConcreteBinOps.GT, v);
	}

	@Override
	public void caseLeExpr(final LeExpr v) {
		apply(ConcreteBinOps.LE, v);
	}

	@Override
	public void caseLtExpr(final LtExpr v) {
		apply(ConcreteBinOps.LT, v);
	}

	@Override
	public void caseMulExpr(final MulExpr v) {
		apply(ConcreteBinOps.MUL, v);
	}

	@Override
	public void caseNeExpr(final NeExpr v) {
		apply(ConcreteBinOps.NE, v);
	}

	@Override
	public void caseSubExpr(final SubExpr v) {
		apply(ConcreteBinOps.SUB, v);
	}
	
	protected void evalOp(final BinOp op, final IValue v1, final IValue v2) {
		if(v1.isMulti() && v2.isDeterministic()) {
			setResult(v1.mapValue(new IValueTransformer() {
				@Override
				public IValue transform(final IValue innerValue, final boolean isMulti) {
					return op.apply(innerValue, v2, es);
				}
			}));
		} else if(v2.isMulti() && v1.isDeterministic()) {
			setResult(v2.mapValue(new IValueTransformer() {
				@Override
				public IValue transform(final IValue innerValue, final boolean isMulti) {
					return op.apply(v1, innerValue, es);
				}
			}));
		} else if(v1.isDeterministic() && v2.isDeterministic()) {
			setResult(op.apply(v1, v2, es));
		} else {
			setResult(IValue.nondet());
		}
	}

	protected void apply(final BinOp op, final BinopExpr expr) {
		evalOp(op, this.op1, this.op2);
	}

	public IValue interpretWith(final Expr exp, final IValue op1, final IValue op2) {
		this.op1 = op1;
		this.op2 = op2;
		exp.apply(this);
		return (IValue) this.getResult();
	}
}