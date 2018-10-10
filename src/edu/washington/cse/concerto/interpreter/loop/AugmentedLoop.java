package edu.washington.cse.concerto.interpreter.loop;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.MHGDominatorsFinder;
import soot.toolkits.scalar.ArrayPackedSet;
import soot.toolkits.scalar.CollectionFlowUniverse;

public class AugmentedLoop {
	public final Loop l;
	public final ArrayPackedSet<Unit> stmts;
	public final ArrayPackedSet<Unit> exitStmts;
	
	private final Unit loopStart;
	private final Unit loopDominator;
	private final Unit loopSuccessor;

	public AugmentedLoop(final Loop l, final CollectionFlowUniverse<Unit> universe, final DirectedGraph<Unit> unitGraph) {
		this.l = l;
		this.stmts = new ArrayPackedSet<>(universe);
		for(final Stmt s : l.getLoopStatements()) {
			stmts.add(s);
		}
		
		this.exitStmts = new ArrayPackedSet<>(universe);
		final LinkedList<Unit> worklist = new LinkedList<>();
		worklist.addAll(l.getLoopExits());
		while(!worklist.isEmpty()) {
			final Unit u = worklist.removeFirst();
			if(exitStmts.contains(u)) {
				continue;
			}
			exitStmts.add(u);
			if(u == l.getHead()) {
				continue;
			}
			worklist.addAll(unitGraph.getPredsOf(u));
		}
		final Set<Unit> loopStart = new HashSet<>();
		final Set<Unit> loopSuccessor = new HashSet<>();
		
		final MHGDominatorsFinder<Unit> d = new MHGDominatorsFinder<>(unitGraph);
		final LinkedList<Unit> dominatorCandidates = new LinkedList<>();
		outer_search: for(final Unit u : exitStmts) {
			for(final Unit succ : unitGraph.getSuccsOf(u)) {
				if(!stmts.contains(succ)) {
					loopSuccessor.add(succ);
				} else if(!exitStmts.contains(succ)) {
					loopStart.add(succ);
				}
			}
			
			final Iterator<Unit> candidateIt = dominatorCandidates.iterator();
			while(candidateIt.hasNext()) {
				final Unit candidate = candidateIt.next();
				if(d.isDominatedBy(candidate, u)) {
					candidateIt.remove();
				} else if(d.isDominatedBy(u, candidate)) {
					continue outer_search;
				}
			}
			dominatorCandidates.add(u);
		}
		assert dominatorCandidates.size() == 1;
		this.loopDominator = dominatorCandidates.getFirst();
		assert loopStart.size() == 1;
		assert loopSuccessor.size() == 1;
		this.loopStart = loopStart.iterator().next();
		this.loopSuccessor = loopSuccessor.iterator().next();
	}

	public Unit getLoopStart() {
		return loopStart;
	}

	public Unit getExitDominator() {
		return loopDominator;
	}

	public Unit getLoopSuccessor() {
		return loopSuccessor;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((exitStmts == null) ? 0 : exitStmts.hashCode());
		result = prime * result + ((loopDominator == null) ? 0 : loopDominator.hashCode());
		result = prime * result + ((loopStart == null) ? 0 : loopStart.hashCode());
		result = prime * result + ((loopSuccessor == null) ? 0 : loopSuccessor.hashCode());
		result = prime * result + ((stmts == null) ? 0 : stmts.hashCode());
		return result;
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
		final AugmentedLoop other = (AugmentedLoop) obj;
		if(!exitStmts.equals(other.exitStmts)) {
			return false;
		}
		if(!loopDominator.equals(other.loopDominator)) {
			return false;
		}
		if(!loopStart.equals(other.loopStart)) {
			return false;
		}
		if(!loopSuccessor.equals(other.loopSuccessor)) {
			return false;
		}
		if(!stmts.equals(other.stmts)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "AugmentedLoop[" + loopStart + "]";
	}
}