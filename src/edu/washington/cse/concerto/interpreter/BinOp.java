package edu.washington.cse.concerto.interpreter;

import soot.Value;
import edu.washington.cse.concerto.interpreter.ai.ValueMonad;
import edu.washington.cse.concerto.interpreter.ai.binop.PrimitiveOperations;
import edu.washington.cse.concerto.interpreter.value.IValue;

public interface BinOp {
	public IValue apply(IValue v1, IValue v2, HeapProvider heap);
	public <AVal> IValue apply(AVal a1, AVal a2, Value expr, ValueMonad<?> monad, PrimitiveOperations<AVal> op);

}
