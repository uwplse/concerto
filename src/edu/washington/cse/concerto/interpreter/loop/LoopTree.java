package edu.washington.cse.concerto.interpreter.loop;

import java.util.HashMap;
import java.util.Map;

import soot.Body;
import soot.Unit;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.jimple.toolkits.annotation.logic.LoopFinder;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.CollectionFlowUniverse;

public class LoopTree {
	public static void printLoop(final Loop l) {
		System.out.println(">>>");
		System.out.println("HEAD: " + l.getHead());
		System.out.println("BACKJUMPS: " + l.getBackJumpStmt());
		System.out.println("EXITS: " + l.getLoopExits());
		System.out.println("BODY: " + l.getLoopStatements());
		System.out.println("<<<");
	}

	@SuppressWarnings("unused")
	private final Map<Unit, AugmentedLoop> headerToLoop = new HashMap<>();
	public LoopTree(final Body b) {
		final CollectionFlowUniverse<Unit> universe = new CollectionFlowUniverse<>(b.getUnits());
		final DirectedGraph<Unit> unitGraph = new BriefUnitGraph(b);
		
		final LoopFinder lf = new LoopFinder();
		lf.transform(b);

		for(final Loop l : lf.loops()) {
			final AugmentedLoop al = new AugmentedLoop(l, universe, unitGraph);

			headerToLoop.put(l.getHead(), al);
		}
	}

	public boolean isHeader(final Unit u) {
		return headerToLoop.containsKey(u);
	}

	public AugmentedLoop getLoop(final Unit u) {
		return headerToLoop.get(u);
	}

	public void dump() {
		for(final Unit u : headerToLoop.keySet()) {
			final AugmentedLoop l = headerToLoop.get(u);
			printLoop(l.l);
			System.out.println(l.exitStmts);
		}
	}
}
