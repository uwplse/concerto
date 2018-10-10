package edu.washington.cse.concerto.interpreter;

import soot.Body;
import soot.Unit;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.MHGPostDominatorsFinder;

public class JoinPointFinder {
	private MHGPostDominatorsFinder<Unit> postDominators;

	public JoinPointFinder(final Body b) {
		this.findJoinPoints(b);
	}
	
	private void findJoinPoints(final Body b) {
		final BriefUnitGraph ug = new BriefUnitGraph(b);
		postDominators = new MHGPostDominatorsFinder<>(ug);
	}

	public Unit getJoinPointFor(final Unit u) {
		return postDominators.getImmediateDominator(u);
	}
}
