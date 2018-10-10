package edu.washington.cse.concerto.interpreter;

import edu.washington.cse.concerto.interpreter.exception.PruneExecutionException;
import edu.washington.cse.concerto.interpreter.state.ExecutionState;
import edu.washington.cse.concerto.interpreter.value.IValue;
import fj.P;
import fj.data.Option;
import fj.data.Seq;
import soot.SootMethod;
import soot.Value;
import soot.jimple.InvokeExpr;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FixpointInterpreter<FH, Context> implements InvokeInterpreterExtension<FH, Context> {
	private final FixpointState<FH, Context> fs;
	private final Set<SootMethod> changedReturn = new HashSet<>();
	private final Set<SootMethod> changedCall = new HashSet<>();
	private final Object cycleContext;
	private Interpreter<Context, FH> interpreter;

	public FixpointInterpreter(final FixpointState<FH, Context> fs, final ExecutionState<FH, Context> rootContext, final Interpreter<Context, FH> sourceInterpreter) {
		this.fs = fs;
		cycleContext = sourceInterpreter.createAllocationContext(rootContext);
	}
	
	public void setInterpreter(final Interpreter<Context, FH> interpreter) {
		this.interpreter = interpreter;
	}

	public boolean interpret() {
		this.changedReturn.clear();
		this.changedCall.clear();
		for(final SootMethod m : fs.getWideningPoints()) {
			final InterpreterState<FH> resultState = this.interpreter.interpretUntil(m, fs.getStartState(m), null, null);
			if(resultState != null) {
				if(fs.registerReturn(m, resultState.rs)) {
					changedReturn.add(m);
				}
			}
		}
		return changedReturn.size() != 0 || changedCall.size() != 0;
	}
	
	@Override
	public Object createAllocationContext(final ExecutionState<FH, Context> es) {
		ExecutionState<?, ?> it = es;
		Seq<Option<InvokeExpr>> callStack = Seq.empty();
		while(it != null) {
			callStack = callStack.cons(it.callExpr);
			it = it.callerState;
		}
		return P.p(cycleContext, callStack);
	}
	
	@Override
	public Option<IValue> interpretCall(final ExecutionState<FH, Context> es, final IValue base, final InvokeExpr op, final SootMethod targetMethod) {
		if(this.interpreter.inRecursiveCycle(es, targetMethod)) {
			fs.getWideningPoints().add(targetMethod);
		}
		if(fs.getWideningPoints().contains(targetMethod)) {
			this.interpreter.incompleteExecutionCallback();
			final List<IValue> args = new ArrayList<>();
			for(final Value v : op.getArgs()) {
				try {
					args.add(this.interpreter.interpretValue(es, v));
				} catch(final PruneExecutionException e) {
					throw e;
				}
			}
			if(fs.registerCaller(op, base, args, targetMethod, es)) {
				changedCall.add(targetMethod);
			}
			if(!fs.hasSummary(targetMethod)) {
				throw new PruneExecutionException();
			}
			final ReturnState<FH> summaryState = fs.getReturnState(targetMethod);
			es.heap.applyHeap(summaryState.h);
			es.replaceHeap(summaryState.foreignHeap);
			return Option.some(summaryState.returnValue);
		} else {
			return Option.none();
		}
	}
}
