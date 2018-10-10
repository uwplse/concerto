package edu.washington.cse.concerto.interpreter.ai.binop;

import soot.jimple.AbstractExprSwitch;
import soot.jimple.AddExpr;
import soot.jimple.DivExpr;
import soot.jimple.Expr;
import soot.jimple.MulExpr;
import soot.jimple.SubExpr;

public class ArithmeticEvaluator<AVal> extends AbstractExprSwitch {
	private AVal op2;
	private AVal op1;
	private final PrimitiveOperations<AVal> ops;
	public ArithmeticEvaluator(final PrimitiveOperations<AVal> ops) {
		this.ops = ops;
	}
	@Override
	public void caseAddExpr(final AddExpr v) {
		setResult(ops.plus(op1, op2));
	}
	
	@Override
	public void caseSubExpr(final SubExpr v) {
		setResult(ops.minus(op1, op2));
	}
	
	@Override
	public void caseMulExpr(final MulExpr v) {
		setResult(ops.mult(op1, op2));
	}
	
	@Override
	public void caseDivExpr(final DivExpr v) {
		setResult(ops.div(op1, op2));
	}
	
	@SuppressWarnings("unchecked")
	public AVal eval(final Expr e, final AVal op1, final AVal op2) {
		this.op1 = op1;
		this.op2 = op2;
		e.apply(this);
		return (AVal) getResult();
	}
}
