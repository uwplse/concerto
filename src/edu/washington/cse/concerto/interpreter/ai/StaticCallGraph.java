package edu.washington.cse.concerto.interpreter.ai;

import heros.solver.Pair;
import soot.toolkits.graph.HashMutableDirectedGraph;
import soot.toolkits.graph.StronglyConnectedComponentsFast;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StaticCallGraph<ContextKey> {
	private final HashMutableDirectedGraph<ContextKey> callGraph;
	private final HashMutableDirectedGraph<ContextKey> sccGraph = new HashMutableDirectedGraph<>();
	private final Set<ContextKey> wideningPoint = new HashSet<>();
	private final Set<Pair<ContextKey, ContextKey>> backEdges = new HashSet<>();

	public StaticCallGraph() {
		this.callGraph = new HashMutableDirectedGraph<>();
	}

	public void registerCall(final ContextKey callerContext, final ContextKey calleeContext) {
		if(!this.callGraph.containsNode(calleeContext)) {
			this.callGraph.addNode(calleeContext);
			final int newVert;
			this.position.put(calleeContext, newVert = bound++);
			this.vertex.put(newVert, calleeContext);
			/*if(trackComponents) {
				this.sccGraph.addNode(calleeContext);
			}*/
		}
		if(!this.callGraph.containsNode(callerContext)) {
			this.callGraph.addNode(callerContext);
			final int newVert;
			this.position.put(callerContext, newVert = bound++);
			this.vertex.put(newVert, callerContext);
			/*if(trackComponents) {
				this.sccGraph.addNode(callerContext);
			}*/
		}
		if(callerContext.equals(calleeContext)) {
			// trivial self edge
			this.wideningPoint.add(calleeContext);
			return;
		}
		if(!this.callGraph.containsEdge(callerContext, calleeContext) && !backEdges.contains(new Pair<>(callerContext, calleeContext))) {
			this.callGraph.addEdge(callerContext, calleeContext);
			/*if(trackComponents) {
				final ContextKey fCallee = find(calleeContext);
				final ContextKey fCaller = find(callerContext);
				assert sccGraph.containsNode(fCallee) && sccGraph.containsNode(fCaller);
				if(!fCallee.equals(fCaller) && !this.sccGraph.containsEdge(fCallee, fCaller)) {
					sccGraph.addEdge(fCaller, fCallee);
				}
			}*/
//			if(this.gt(callerContext, calleeContext)) {
//				this.topologicalSearch(callerContext, calleeContext);
//			}
			if(this.hasCycle(calleeContext, callerContext)) {
//				System.out.println("HAS CYCLE " + calleeContext + " " + callerContext);
				this.backEdges.add(new Pair<>(callerContext, calleeContext));
				this.wideningPoint.add(calleeContext);
				this.callGraph.removeEdge(callerContext, calleeContext);
				assert this.noStronglyConnectedComponents();
			}
			/*if(trackComponents && this.gt(find(callerContext), find(calleeContext))) {
				this.topologicalSearch(find(callerContext), find(calleeContext));
			}*/
		}
		assert this.noStronglyConnectedComponents() : dumpGraph() + " " + callerContext  + " -> " + calleeContext + " & " + this.backEdges;
	}

	int numAdded = 0;

	private boolean hasCycle(final ContextKey calleeContext, final ContextKey callerContext) {
		final boolean debug = false;
		final Set<ContextKey> colored = new HashSet<>();
		final LinkedList<ContextKey> worklist = new LinkedList<>();
		worklist.add(calleeContext);
		while(!worklist.isEmpty()) {
			final ContextKey curr = worklist.removeFirst();
			if(curr.equals(callerContext)) {
				return true;
			}
			if(!colored.add(curr)) {
				continue;
			}
			worklist.addAll(callGraph.getSuccsOfAsSet(curr));
		}
		return false;
	}

	private boolean noStronglyConnectedComponents() {
		final List<List<ContextKey>> trueComponents = new StronglyConnectedComponentsFast<>(this.callGraph).getTrueComponents();
		if(trueComponents.size() != 0) {
			System.out.println("You're going to have a bad time");
			System.out.println(trueComponents);
		}
		return trueComponents.size() == 0;
	}

	private void topologicalSearch(final ContextKey v, final ContextKey w) {
		/*
			F = [ ]; B = [ ]; inject(w, F ); inject(v, B)
			i = position(w); j = position(v); vertex (i) = vertex (j) = null
			while true do
				i = i +1
				while i < j and ∀u ∈ F (A(position(u), i) = 0) do i = i + 1
				if i = j then return else
					inject(vertex (i), F ); vertex (i) = null
				end
				j = j − 1
				while i < j and ∀z ∈ B(A(j, position(z)) = 0) do j = j − 1
				if i = j then return else
					inject(vertex (j), B); vertex (j) = null
				end
			end
		 */
		final LinkedList<ContextKey> F = new LinkedList<>();
		final LinkedList<ContextKey> B = new LinkedList<>();
		F.add(w);
		B.add(v);
		int i = position.get(w);
		int j = position.get(v);
		vertex.put(i, null);
		vertex.put(j, null);
		while(true) {
			i++;
			while(i < j && predF(F, i)) {
				i++;
			}
			if(i == j) {
				break;
			} else {
				F.add(vertex.get(i));
				vertex.put(i, null);
			}
			j--;
			while(i < j && predB(B, j)) {
				j--;
			}
			if(i == j) {
				break;
			} else {
				B.add(vertex.get(j));
				vertex.put(j, null);
			}
		}
		// hmm
		boolean hasCycle = false;
		outer_search: for(final ContextKey u : F) {
			for(final ContextKey z : B) {
				if(callGraph.containsEdge(u, z)) {
					hasCycle = true;
					break outer_search;
				}
			}
		}
		if(hasCycle) {
			System.out.println("FOUND CYCLE");
			wideningPoint.add(w);
			this.backEdges.add(new Pair<>(v, w));
			this.callGraph.removeEdge(v, w);
			assert this.noStronglyConnectedComponents() : this.dumpGraph() + " " + new StronglyConnectedComponentsFast<>(callGraph).getTrueComponents();
			return;
		}
		assert this.noStronglyConnectedComponents() : this.dumpGraph() + " " + new StronglyConnectedComponentsFast<>(callGraph).getTrueComponents();
		/*final StronglyConnectedComponentsFast<ContextKey> scc = new StronglyConnectedComponentsFast<>(new DirectedGraph<ContextKey>() {
			@Override public List<ContextKey> getHeads() {
				throw new UnsupportedOperationException();
			}

			@Override public List<ContextKey> getTails() {
				throw new UnsupportedOperationException();
			}

			@Override public List<ContextKey> getPredsOf(final ContextKey s) {
				return StaticCallGraph.this.callGraph.getPredsOf(s);
			}

			@Override public List<ContextKey> getSuccsOf(final ContextKey s) {
				return StaticCallGraph.this.callGraph.getSuccsOf(s);
			}

			@Override public int size() {
				return F.size() + B.size();
			}

			@Override public @Nonnull Iterator<ContextKey> iterator() {
				return Stream.iterableStream(F).append(Stream.iterableStream(B)).iterator();
			}
		});

		if(scc.getTrueComponents().size() > 0) {
			assert scc.getTrueComponents().size() == 1 : scc.getTrueComponents() + " " + v + " -> " + w;
		}*/
		/*
			while F 6 = [ ] do
				if vertex (i) 6 = null and ∃u ∈ F (A(u, vertex (i)) = 1) then
					inject(vertex (i), F ); vertex (i) = null
				end
				if vertex (i) = null then
					x = pop(F ); vertex (i) = x; position(x) = i
				end
				i = i +1
			end
			while B 6 = [ ] do
				j = j − 1
				if vertex (j) 6 = null and ∃z ∈ B(A(vertex (j), z) = 1) then
				inject(vertex (j), B); vertex (j) = null
				end
				if vertex (j) = null then
				y = pop(B); vertex (j) = y; position(y) = j
				end
			end
		 */
		while(!F.isEmpty()) {
			if(vertex.get(i) != null && existsUF(F, i)) {
				F.add(vertex.get(i)); vertex.put(i, null);
			}
			if(vertex.get(i) == null) {
				final ContextKey x = F.removeFirst();
				vertex.put(i, x);
				position.put(x, i);
			}
			i++;
		}
		while(!B.isEmpty()) {
			j--;
			if(vertex.get(j) != null && existsZB(B, j)) {
				B.add(vertex.get(j)); vertex.put(j, null);
			}
			if(vertex.get(j) == null) {
				final ContextKey y = B.removeFirst();
				vertex.put(j, y);
				position.put(y, j);
			}
		}
		/*if(scc.getTrueComponents().size() > 0) {
			final Set<ContextKey> vertexSet = new HashSet<>(scc.getTrueComponents().get(0));
			final Set<ContextKey> canonCandidates = new HashSet<>();
			final Set<ContextKey> newSccPreds = new HashSet<>();
			final Set<ContextKey> newSccSuccs = new HashSet<>();
			for(final ContextKey c : vertexSet) {
				assert sccGraph.containsNode(c);
				for(final ContextKey pred : sccGraph.getPredsOfAsSet(c)) {
					if(!vertexSet.contains(pred)) {
						newSccPreds.add(pred);
					}
				}
				for(final ContextKey succ : sccGraph.getSuccsOfAsSet(c)) {
					if(!vertexSet.contains(succ)) {
						newSccSuccs.add(succ);
					}
				}
				if(wideningPoint.contains(c)) {
					canonCandidates.add(c);
				}
			}
			final ContextKey newCanon;
			if(canonCandidates.isEmpty()) {
				newCanon = vertexSet.iterator().next();
			} else {
				newCanon = canonCandidates.iterator().next();
			}
			for(final ContextKey c : vertexSet) {
				if(c.equals(newCanon)) {
					continue;
				}
				this.unionFind.put(c, newCanon);
				sccGraph.removeNode(c);
				vertex.remove(position.get(c));
				position.remove(c);
			}
			final int oldBound = bound;
			bound = 0;
			for(int k = 0; k < oldBound; k++) {
				if(vertex.containsKey(k)) {
					final ContextKey toShuffle = vertex.get(k);
					assert position.containsKey(toShuffle);
					final int newPosition = bound++;
					System.out.println("Shuffling " + toShuffle + " to " + newPosition);
					assert bound <= k;
					assert !vertex.containsKey(newPosition);
					vertex.put(newPosition, toShuffle);
					vertex.remove(k);
					position.put(toShuffle, newPosition);
				}
			}
		}*/
	}

	private String dumpGraph() {
		final StringBuilder sb = new StringBuilder();
		for(final ContextKey v : this.callGraph) {
			sb.append(">> NODE: ").append(v).append("\n");
			for(final ContextKey succ : this.callGraph.getSuccsOfAsSet(v)) {
				sb.append("\t -> ").append(succ).append("\n");
			}
		}
		return sb.toString();
	}

	private boolean noCycles() {
		for(final ContextKey k : this.callGraph) {
			final Set<ContextKey> visited = new HashSet<>();
			final LinkedList<ContextKey> worklist = new LinkedList<>();
			worklist.add(k);
			while(!worklist.isEmpty()) {
				final ContextKey item = worklist.removeFirst();
				if(!visited.add(item)) {
					System.out.println(visited);
					return false;
				}
				worklist.addAll(callGraph.getSuccsOfAsSet(item));
			}
		}
		return true;
	}

	private boolean existsZB(final LinkedList<ContextKey> b, final int j) {
		for(final ContextKey z : b) {
			if(callGraph.containsEdge(vertex.get(j), z)) {
				return true;
			}
		}
		return false;
	}

	// 				if vertex (i) 6 = null and ∃u ∈ F (A(u, vertex (i)) = 1) then
	private boolean existsUF(final List<ContextKey> f, final int i) {
		for(final ContextKey u : f) {
			if(callGraph.containsEdge(u, vertex.get(i))) {
				return true;
			}
		}
		return false;
	}

	private boolean predB(final List<ContextKey> b, final int j) {
		for(final ContextKey z : b) {
			if(callGraph.containsEdge(vertex.get(j), z)) {
				return false;
			}
		}
		return true;
	}

	private boolean predF(final List<ContextKey> f, final int i) {
		for(final ContextKey u : f) {
			if(callGraph.containsEdge(u, vertex.get(i))) {
				return false;
			}
		}
		return true;
	}

	private final Map<ContextKey, Integer> position = new HashMap<>();
	private final Map<Integer, ContextKey> vertex = new HashMap<>();

	public boolean isWideningPoint(final ContextKey cc) {
		return this.wideningPoint.contains(cc);
	}


	private int bound = 0;

	private final Map<ContextKey, ContextKey> unionFind = new HashMap<>();

	private ContextKey find(final ContextKey o1) {
		ContextKey it = o1;
		while(true) {
			if(unionFind.containsKey(o1)) {
				it = unionFind.get(o1);
			} else {
				return it;
			}
		}
	}

	private boolean gt(final ContextKey u, final ContextKey s) {
		return this.position.get(u) > position.get(s);
	}
}
