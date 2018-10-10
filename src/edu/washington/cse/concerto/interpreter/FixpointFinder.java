package edu.washington.cse.concerto.interpreter;

import soot.SootMethod;
import edu.washington.cse.concerto.interpreter.state.ExecutionState;
import edu.washington.cse.concerto.interpreter.state.NondetGlobalState;

public class FixpointFinder<FH, Context> {
	private FixpointState<FH, Context> fs;
	private final ExecutionState<FH, Context> startState;
	private final SootMethod method;
	private final ExecutionState<FH, Context> rootContext;
	private final Interpreter<Context, FH> rootInterpreter;

	public FixpointFinder(final ExecutionState<FH, Context> widened, final SootMethod method, final ExecutionState<FH, Context> rootContext, final Interpreter<Context, FH> rootInterpreter) {
		this.startState = widened;
		this.method = method;
		this.rootContext = rootContext;
		this.rootInterpreter = rootInterpreter;
	}

	public InterpreterState<FH> findFixpoint() {
		fs = new FixpointState<>(method, this.startState);
		final FixpointInterpreter<FH, Context> fi = new FixpointInterpreter<FH, Context>(fs, rootContext, rootInterpreter);
		final Interpreter<Context, FH> extendedInterp = rootInterpreter.deriveNewInterpreter(fi).deriveNewInterpreter(new NondetGlobalState());
		fi.setInterpreter(extendedInterp);
		fs.getWideningPoints().add(method);
		boolean changed;
		do {
			changed = fi.interpret();
		} while(changed);
		return new InterpreterState<>(fs.getReturnState(method));
	}

	public void dump() {
		System.out.println(">>> Start heap:");
		System.out.println(startState.heap);
		System.out.println("<<<\n");
		System.out.println(">> Method Summaries <<");
		fs.dump();
	}
}
