package edu.washington.cse.concerto.interpreter.ai;

import java.util.List;

import soot.Unit;
import soot.jimple.IfStmt;
import edu.washington.cse.concerto.interpreter.meta.InstrumentedState;

public interface BranchInterpreter<AVal, AS> {
	public List<Unit> interpretBranch(IfStmt stmt, Object op1, Object op2, InstrumentedState branchState);
	public List<Unit> interpretBranch(IfStmt stmt, Object op1, InstrumentedState branchState);
}
