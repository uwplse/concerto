package edu.washington.cse.concerto.interpreter;

import edu.washington.cse.concerto.interpreter.value.IValue;
import edu.washington.cse.concerto.interpreter.value.ValueMerger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class MethodState {
	private final Map<String, IValue> locals = new HashMap<>();
	final MethodState parentState;
	
	public MethodState(final MethodState s) {
		this.parentState = s;
	}
	
	public MethodState() {
		this.parentState = null;
	}
	
	private MethodState(final MethodState parentState, final Map<String, IValue> locals) {
		this.parentState = parentState;
		this.locals.putAll(locals);
	}

	public IValue get(final String s) {
		if(!locals.containsKey(s)) {
			if(parentState != null) {
				return parentState.get(s);
			} else {
				return null;
			}
		}
		return locals.get(s);
	}
	
	public void put(final String s, final IValue v) {
		locals.put(s, v);
	}

	public MethodState fork() {
		return new MethodState(this);
	}
	
	public MethodState copy() {
		return new MethodState(this.parentState, locals);
	}

	public void merge(final MethodState ms) {
		assert ms.parentState == this;
		this.locals.putAll(ms.locals);
	}

	public void merge(final MethodState ms1, final MethodState ms2) {
		assert ms1.parentState == this;
		assert ms2.parentState == this;
		mergeLoop(this, ms1, ms2, ValueMerger.STRICT_MERGE, null);
	}
	
	private static void mergeLoop(final MethodState target, final MethodState ms1, final MethodState ms2, final ValueMerger vm, final Set<String> toSkip) {
		final Set<String> bindings = new HashSet<>();
		bindings.addAll(ms1.locals.keySet());
		bindings.addAll(ms2.locals.keySet());
		for(final String s : bindings) {
			if(toSkip != null && toSkip.contains(s)) {
				continue;
			}
			if(ms1.locals.containsKey(s) && ms2.locals.containsKey(s)) {
				target.locals.put(s, vm.merge(ms1.locals.get(s), ms2.locals.get(s)));
			} else if(ms1.locals.containsKey(s)) {
				final IValue old = ms2.get(s);
				if(old == null) {
					target.locals.put(s, ms1.locals.get(s));
				} else {
					target.locals.put(s, vm.merge(ms1.locals.get(s), old));
				}
			} else {
				assert ms2.locals.containsKey(s);
				final IValue old = ms1.get(s);
				if(old == null) {
					target.locals.put(s, ms2.locals.get(s));
				} else {
					target.locals.put(s, vm.merge(old, ms2.locals.get(s)));
				}
			}
		}
	}
	
	@Override
	public String toString() {
		final Map<String, IValue> collapsed = this.collapse();
		return collapsed.toString();
	}

	private Map<String, IValue> collapse() {
		MethodState it = this;
		final Map<String, IValue> toReturn = new HashMap<>();
		while(it != null) {
			for(final String k : it.locals.keySet()) {
				if(!toReturn.containsKey(k)) {
					toReturn.put(k, it.locals.get(k));
				}
			}
			it = it.parentState;
		}
		return toReturn;
	}

	public static boolean functionalEquivalence(final MethodState ms1, final MethodState ms2) {
		final Set<String> allBindings = new HashSet<>(ms1.locals.keySet()); 
		allBindings.addAll(ms2.locals.keySet());
		for(final String s : allBindings) {
			final IValue v1 = ms1.get(s);
			final IValue v2 = ms2.get(s);
			if(!Objects.equals(v1, v2)) {
				return false;
			}
		}
		return true;
	}

	public static MethodState widen(final MethodState prev, final MethodState next) {
		assert prev.parentState == next.parentState;
		final Set<String> toSkip = new HashSet<>();
		for(final String s : next.locals.keySet()) {
			if(prev.get(s) == null) {
				toSkip.add(s);
			}
		}
		final MethodState toReturn = new MethodState(prev.parentState);
		mergeLoop(toReturn, prev, next, ValueMerger.WIDENING_MERGE, toSkip);
		return toReturn;
	}

	public static MethodState join(final MethodState first, final MethodState second) {
		assert first.parentState == second.parentState;
		final MethodState toReturn = new MethodState(first.parentState);
		mergeLoop(toReturn, first, second, ValueMerger.STRICT_MERGE, null);
		return toReturn;
	}

	public boolean lessEqual(final MethodState second) {
		assert this.parentState == second.parentState;
		for(final String s : locals.keySet()) {
			final IValue v = second.get(s);
			if(v == null) {
				return false;
			}
			if(!locals.get(s).lessEqual(v)) {
				return false;
			}
		}
		return true;
	}

	public MethodState popTo(final MethodState targetState) {
		if(targetState == parentState) {
			return this.copy();
		}
		final MethodState toReturn = new MethodState(targetState);
		toReturn.locals.putAll(this.locals);
		MethodState parent = this.parentState;
		while(parent != targetState) {
			assert parent != null;
			for(final String l : parent.locals.keySet()) {
				if(toReturn.locals.containsKey(l)) {
					continue;
				}
				toReturn.locals.put(l, parent.locals.get(l));
			}
			parent = parent.parentState;
		}
		return toReturn;
	}

	public boolean descendantOf(final MethodState parent) {
		MethodState s = this.parentState;
		while(s != null) {
			if(s == parent) {
				return true;
			}
			s = s.parentState;
		}
		return false;
	}
}
