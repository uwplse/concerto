package edu.washington.cse.concerto.interpreter.ai.instantiation;

import edu.washington.cse.concerto.interpreter.ai.EvalResult;
import edu.washington.cse.concerto.interpreter.ai.MethodResult;
import edu.washington.cse.concerto.interpreter.ai.instantiation.pta.JValue;
import edu.washington.cse.concerto.interpreter.meta.InstrumentedState;
import soot.PatchingChain;
import soot.Unit;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;

public class CFGUtil {
	private final Unit u;
	private final PatchingChain<Unit> chain;
	public CFGUtil(final Unit u, final PatchingChain<Unit> chain) {
		this.u = u;
		this.chain = chain;
	}
	public InterpResult next(final InstrumentedState newState) {
		if(u instanceof GotoStmt) {
			return new InterpResult(((GotoStmt) u).getTarget(), newState);
		} else if(u instanceof IfStmt) {
			final InterpResult toReturn = new InterpResult(((IfStmt) u).getTarget(), newState);
			toReturn.add(getSucc(u), newState);
			return toReturn;
		} else {
			return new InterpResult(getSucc(u), newState);
		}
	}
	
	public Unit fallthrough() {
		return getSucc(u);
	}
	
	public InterpResult succ(final InstrumentedState newState) {
		return new InterpResult(getSucc(u), newState);
	}
	
	public InterpResult target(final InstrumentedState newState) {
		return new InterpResult(((IfStmt)u).getTarget(), newState);
	}
	
	public InterpResult next(final MethodResult res) {
		return new InterpResult(getSucc(u), res.getState());
	}
	
	private Unit getSucc(final Unit u) {
		return chain.getSuccOf(u);
	}
	
	public InterpResult returnValue(final EvalResult toReturn) {
		return new InterpResult(toReturn.state, toReturn.value);
	}
	
	public InterpResult returnVoid(final InstrumentedState state) {
		return new InterpResult(state);
	}

	public InterpResult returnBottom(final InstrumentedState state, final JValue bottom) {
		return new InterpResult(state, bottom);
	}
}