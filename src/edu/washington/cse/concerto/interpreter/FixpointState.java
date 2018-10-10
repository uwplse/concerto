package edu.washington.cse.concerto.interpreter;

import edu.washington.cse.concerto.interpreter.heap.Heap;
import edu.washington.cse.concerto.interpreter.loop.LoopState;
import edu.washington.cse.concerto.interpreter.state.ExecutionState;
import edu.washington.cse.concerto.interpreter.value.IValue;
import fj.data.Option;
import soot.SootMethod;
import soot.jimple.InvokeExpr;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FixpointState<FH, Context> {
	private final Set<SootMethod> wideningPoints = new HashSet<>();
	private final Map<SootMethod, ReturnState<FH>> returnState = new HashMap<>();
	private final Map<SootMethod, ExecutionState<FH, Context>> startState = new HashMap<>();
	private final Heap parentHeap;

	public FixpointState(final SootMethod m, final ExecutionState<FH, Context> startState) {
		this.parentHeap = startState.heap.parentHeap;
		this.startState.put(m, startState);
	}

	public ReturnState<FH> getReturnState(final SootMethod targetMethod) {
		return returnState.get(targetMethod);
	}

	public boolean registerCaller(final InvokeExpr op, final IValue v, final List<IValue> args, final SootMethod resolvedMethod, final ExecutionState<FH, Context> es) {
		final Heap h = es.heap.popTo(parentHeap);
		final ExecutionState<FH, Context> state = new ExecutionState<FH, Context>(resolvedMethod, new MethodState(), h, v, args, new LoopState(resolvedMethod),
				es.foreignHeap, es.rootContext, null, Option.none());
		if(!startState.containsKey(resolvedMethod)) {
			startState.put(resolvedMethod, state);
			return true;
		}
		final ExecutionState<FH, Context> curr = startState.get(resolvedMethod);
		final ExecutionState<FH, Context> widened = ExecutionState.widen(curr, state);
		assert curr.lessEqual(widened);
		if(widened.lessEqual(curr)) {
			return false;
		}
		startState.put(resolvedMethod, widened);
		return true;
	}

	public boolean hasSummary(final SootMethod m) {
		return returnState.containsKey(m);
	}

	public boolean registerReturn(final SootMethod currMethod, final ReturnState<FH> rs) {
		final Heap returnHeap = computeReturnHeap(rs.h);
		final ReturnState<FH> toWiden = new ReturnState<>(rs.returnValue, returnHeap, rs.foreignHeap);
		if(!returnState.containsKey(currMethod)) {
			returnState.put(currMethod, toWiden);
			return true;
		}
		final ReturnState<FH> curr = returnState.get(currMethod);
		final ReturnState<FH> widened = ReturnState.widen(curr, toWiden);
		if(widened.lessEqual(curr)) {
			return false;
		}
		returnState.put(currMethod, widened);
		return true;
	}

	private Heap computeReturnHeap(final Heap h) {
		return h.popTo(parentHeap);
	}

	public ExecutionState<FH, Context> getStartState(final SootMethod m) {
		assert startState.containsKey(m);
		return startState.get(m).copy();
	}

	public void dump() {
		for(final SootMethod m : this.startState.keySet()) {
			System.out.println("++ Method: " + m);
			System.out.println(">> Start state:");
			startState.get(m).dump();
			if(returnState.containsKey(m)) {
				System.out.println(">> Return state");
				returnState.get(m).dump();
			}
			System.out.println();
		}
		
	}

	public Set<SootMethod> getWideningPoints() {
		return wideningPoints;
	}
}
