package edu.washington.cse.concerto.interpreter.loop;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import soot.Body;
import soot.SootMethod;
import soot.Unit;
import edu.washington.cse.concerto.interpreter.BodyManager;

public class LoopState {
	private static final Map<SootMethod, LoopTree> loops = new HashMap<>();

	private final LinkedList<AugmentedLoop> loopStack = new LinkedList<>();
	private final LoopTree tr;
	
	public LoopState(final SootMethod m) { 
		if(loops.containsKey(m)) {
			this.tr = loops.get(m);
		} else {
			final Body body = BodyManager.retrieveBody(m);
			tr = new LoopTree(body);
			loops.put(m, tr);
		}
	}

	public LoopState(final LoopTree tr, final List<AugmentedLoop> arrayList) {
		this.tr = tr;
		this.loopStack.addAll(arrayList);
	}

	public boolean isEmpty() {
		return loopStack.isEmpty();
	}
	
	public LoopState fork() {
		return new LoopState(this.tr, loopStack);
	}
	
	public AugmentedLoop getActiveLoop() {
		if(loopStack.isEmpty()) {
			return null;
		}
		return loopStack.getLast();
	}

	public void visitStatement(final Unit u) {
		if(!loopStack.isEmpty() && !loopStack.getLast().stmts.contains(u)) {
			loopStack.removeLast();
		}
		if(this.tr.isHeader(u)) {
			final AugmentedLoop l = this.tr.getLoop(u);
			if(!loopStack.isEmpty() && loopStack.getLast() == l) {
				return;
			} else {
				loopStack.add(l);
			}
		}
	}

	public boolean isLoopExitBlock(final Unit u) {
		if(this.loopStack.isEmpty()) {
			return false;
		}
		return loopStack.getLast().exitStmts.contains(u);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((loopStack == null) ? 0 : loopStack.hashCode());
		result = prime * result + ((tr == null) ? 0 : tr.hashCode());
		return result;
	}
	
	@Override
	public String toString() {
		return loopStack.toString();
	}

	@Override
	public boolean equals(final Object obj) {
		if(this == obj) {
			return true;
		}
		if(obj == null) {
			return false;
		}
		if(getClass() != obj.getClass()) {
			return false;
		}
		final LoopState other = (LoopState) obj;
		if(!loopStack.equals(other.loopStack)) {
			return false;
		}
		return tr == other.tr;
	}
}
