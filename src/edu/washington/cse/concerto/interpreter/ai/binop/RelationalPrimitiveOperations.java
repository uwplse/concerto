package edu.washington.cse.concerto.interpreter.ai.binop;

import edu.washington.cse.concerto.interpreter.ai.CompareResult;
import edu.washington.cse.concerto.interpreter.meta.InstrumentedState;
import fj.data.Option;
import soot.Value;

/*
 * This operations propagate relational information in the state after any value propagation has occurred.
 */
public interface RelationalPrimitiveOperations<AVal, AS> extends PathSensitivePrimitiveOperations<AVal> {
	public default Option<CompareResult> cmpRelational(final InstrumentedState state, final Value leftOp, final Value rightOp) {
		return Option.none();
	}
	
	/*
	 * Propagate that lop < rop
	 */
	default public InstrumentedState propagateRelationLT(final InstrumentedState inputState, final Value lop, final Value rop) {
		return inputState;
	}
	/*
	 * Propagate that lop <= rop
	 */
	public default InstrumentedState propagateRelationLE(final InstrumentedState inputState, final Value lop, final Value rop) {
		return inputState;
	}

	/*
	 * Propagate that lop == rop
	 */
	public default InstrumentedState propagateRelationEQ(final InstrumentedState inputState, final Value lop, final Value rop) {
		return inputState;
	}
	/*
	 * Propagate that lop != rop
	 */
	public default InstrumentedState propagateRelationNE(final InstrumentedState inputState, final Value lop, final Value rop) {
		return inputState;
	}

	/*
	 * Propagate that lop > rop
	 */
	public default InstrumentedState propagateRelationGT(final InstrumentedState inputState, final Value lop, final Value rop) {
		return inputState;
	}
	/*
	 * Propagate that lop < rop
	 */
	public default InstrumentedState propagateRelationGE(final InstrumentedState inputState, final Value lop, final Value rop) {
		return inputState;
	}
}
