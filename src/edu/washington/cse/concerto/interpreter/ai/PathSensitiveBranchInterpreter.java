package edu.washington.cse.concerto.interpreter.ai;

import java.util.Map;

import soot.Unit;
import soot.jimple.IfStmt;
import edu.washington.cse.concerto.interpreter.meta.InstrumentedState;

public interface PathSensitiveBranchInterpreter<AVal, AS> {
	public Map<Unit, InstrumentedState> interpretBranch(IfStmt stmt, Object op1, Object op2, InstrumentedState branchState, StateValueUpdater<AS> updater);
	public Map<Unit, InstrumentedState> interpretBranch(IfStmt stmt, Object op1, InstrumentedState branchState);
}
