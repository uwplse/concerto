package edu.washington.cse.concerto.interpreter.ai;

import edu.washington.cse.concerto.interpreter.value.IValue;

public interface ValueStateTransformer<AVal, AState> extends ValueTransfomer<AVal, AState, UpdateResult<AState, AVal>, UpdateResult<AState, IValue>, UpdateResult<AState, ?>, HeapMutator>{
}
