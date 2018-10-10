package edu.washington.cse.concerto.interpreter.state;

import edu.washington.cse.concerto.interpreter.EmbeddedState;
import edu.washington.cse.concerto.interpreter.HeapProvider;
import edu.washington.cse.concerto.interpreter.MethodState;
import edu.washington.cse.concerto.interpreter.ReturnState;
import edu.washington.cse.concerto.interpreter.heap.Heap;
import edu.washington.cse.concerto.interpreter.loop.LoopState;
import edu.washington.cse.concerto.interpreter.meta.BoundaryInformation;
import edu.washington.cse.concerto.interpreter.value.IValue;
import edu.washington.cse.concerto.interpreter.value.ValueMerger;
import fj.data.Option;
import soot.SootMethod;
import soot.jimple.InvokeExpr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ExecutionState<FH, Context> implements HeapProvider {
	private static final EnumSet<GlobalExecutionFlag> NO_GLOBAL_FLAGS = EnumSet.noneOf(GlobalExecutionFlag.class);
	private static final EnumSet<LocalExecutionFlag> NO_LOCAL_FLAGS = EnumSet.noneOf(LocalExecutionFlag.class);
	public final MethodState ms;
	public final Heap heap;

	public final List<IValue> arguments;
	public final IValue receiver;
	public final LoopState ls;
	
	public final EnumSet<LocalExecutionFlag> localFlags;
	public final EnumSet<GlobalExecutionFlag> globalFlags;
	
	public final Set<SootMethod> cycleSet;
	public final SootMethod currMethod;
	public EmbeddedState<FH> foreignHeap;
	public final BoundaryInformation<Context> rootContext;
	public ExecutionState<FH, Context> callerState;
	public Option<InvokeExpr> callExpr;

	public static enum LocalExecutionFlag {
		RECORD_CYCLE
	}
	
	public static enum GlobalExecutionFlag {
		THROW_ON_CYCLES
	}
	
	public ExecutionState(final SootMethod currMethod, final MethodState ms, final Heap heap, final IValue receiver, final List<IValue> arguments, final LoopState ls,
			final EnumSet<LocalExecutionFlag> flags, final EnumSet<GlobalExecutionFlag> globalFlags, final Set<SootMethod> cycleSet, final EmbeddedState<FH> foreignHeap,
			final BoundaryInformation<Context> rootContext, final ExecutionState<FH, Context> callerState, final Option<InvokeExpr> callExpr) {
		this.currMethod = currMethod;
		this.ms = ms;
		this.heap = heap;
		this.receiver = receiver;
		this.arguments = arguments;
		this.ls = ls;
		this.localFlags = flags;
		this.globalFlags = globalFlags;
		this.cycleSet = cycleSet;
		this.foreignHeap = foreignHeap;
		this.rootContext = rootContext;
		this.callerState = callerState;
		this.callExpr = callExpr;
	}
	
	public ExecutionState(final SootMethod currMethod, final MethodState ms, final Heap heap, final IValue receiver,
			final List<IValue> arguments, final LoopState ls, final EnumSet<LocalExecutionFlag> flags, final EnumSet<GlobalExecutionFlag> globalFlags,
			final EmbeddedState<FH> fh, final BoundaryInformation<Context> rootContext, final ExecutionState<FH, Context> callerState, final Option<InvokeExpr> callExpr) {
		this(currMethod, ms, heap, receiver, arguments, ls, flags, globalFlags, Collections.<SootMethod>emptySet(), fh, rootContext, callerState, callExpr);
	}
	
	public ExecutionState(final SootMethod currMethod, final MethodState ms, final Heap heap, final IValue receiver, final List<IValue> arguments, final LoopState ls,
			final EmbeddedState<FH> foreignHeap, final BoundaryInformation<Context> rootContext, final ExecutionState<FH, Context> callerState, final Option<InvokeExpr> callExpr) {
		this(currMethod, ms, heap, receiver, arguments, ls, NO_LOCAL_FLAGS, NO_GLOBAL_FLAGS, Collections.<SootMethod>emptySet(), foreignHeap, rootContext, callerState, callExpr);
	}

	public ExecutionState<FH, Context> forNewMethod(final SootMethod m, final IValue receiver, final List<IValue> arguments, final Option<InvokeExpr> callExpr) {
		Set<SootMethod> cycleSet = this.cycleSet;
		if(localFlags.contains(LocalExecutionFlag.RECORD_CYCLE)) {
			cycleSet = new HashSet<>(cycleSet);
			cycleSet.add(this.currMethod);
		}
		return new ExecutionState<>(m, new MethodState(), heap, receiver, arguments, new LoopState(m), NO_LOCAL_FLAGS, globalFlags, cycleSet, foreignHeap, rootContext, this, callExpr);
	}

	public ExecutionState<FH, Context> withFlags(final LocalExecutionFlag... newFlags) {
		final EnumSet<LocalExecutionFlag> copy = EnumSet.copyOf(localFlags);
		copy.addAll(Arrays.asList(newFlags));
		return new ExecutionState<>(currMethod, ms, heap, receiver, arguments, ls, copy, globalFlags, cycleSet, foreignHeap, rootContext, this.callerState, this.callExpr);
	}
	
	public ExecutionState<FH, Context> withFlags(final GlobalExecutionFlag... newFlags) {
		final EnumSet<GlobalExecutionFlag> copy = EnumSet.copyOf(globalFlags);
		copy.addAll(Arrays.asList(newFlags));
		return new ExecutionState<>(currMethod, ms, heap, receiver, arguments, ls, localFlags, copy, cycleSet, foreignHeap, rootContext, this.callerState, this.callExpr);
	}
	
	public boolean hasFlag(final GlobalExecutionFlag flag) {
		return this.globalFlags.contains(flag);
	}
	
	public boolean hasFlag(final LocalExecutionFlag flag) {
		return this.localFlags.contains(flag);
	}

	public ExecutionState<FH, Context> fork() {
		return new ExecutionState<>(currMethod, ms.fork(), heap.fork(), receiver, arguments, ls.fork(), localFlags, globalFlags, cycleSet, foreignHeap, rootContext, this.callerState, this.callExpr);
	}

	public boolean lessEqual(final ExecutionState<FH, Context> other) {
		if(!this.receiver.lessEqual(other.receiver) || !this.heap.lessEqual(other.heap) || !this.ms.lessEqual(other.ms)) {
			return false;
		}
		if(this.foreignHeap != null) {
			if(other.foreignHeap == null) {
				return false;
			}
			this.foreignHeap.lessThan(other.foreignHeap);
		}
		assert this.arguments.size() == other.arguments.size();
		for(int i = 0; i < this.arguments.size(); i++) {
			if(!this.arguments.get(i).lessEqual(other.arguments.get(i))) {
				return false;
			}
		}
		return true;
	}

	public static <FH, Context> ExecutionState<FH, Context> widen(final ExecutionState<FH, Context> e1, final ExecutionState<FH, Context> e2) {
		assert e1.arguments.size() == e2.arguments.size();
		final List<IValue> widenedArgs = new ArrayList<>(e2.arguments.size());
		for(int i = 0; i < e2.arguments.size(); i++) {
			widenedArgs.add(ValueMerger.WIDENING_MERGE.merge(e1.arguments.get(i), e2.arguments.get(i)));
		}
		final IValue receiver = ValueMerger.WIDENING_MERGE.merge(e1.receiver, e2.receiver);
		final Heap h = Heap.widen(e1.heap, e2.heap);
		final MethodState ms = MethodState.widen(e1.ms, e2.ms);
		final EmbeddedState<FH> embed;
		if(e1.foreignHeap == null) {
			embed = e2.foreignHeap;
		} else {
			assert e2.foreignHeap != null;
			assert e1.foreignHeap.stateLattice == e2.foreignHeap.stateLattice;
			embed = new EmbeddedState<>(e1.foreignHeap.stateLattice.widen(e1.foreignHeap.state, e2.foreignHeap.state), e2.foreignHeap.stateLattice);
		}
		assert e1.currMethod == e2.currMethod;
		assert e1.ls.equals(e2.ls) : e1.ls + " " + e2.ls;
		assert e1.rootContext == e2.rootContext;
		assert Objects.equals(e1.cycleSet, e2.cycleSet);
		assert e1.callerState == e2.callerState;
		assert e1.callExpr.equals(e2.callExpr);
		return new ExecutionState<>(e1.currMethod, ms, h, receiver, widenedArgs, e1.ls.fork(), NO_LOCAL_FLAGS, NO_GLOBAL_FLAGS, e1.cycleSet, embed, e1.rootContext, e1.callerState, e1.callExpr);
	}

	public ExecutionState<FH, Context> copy() {
		return new ExecutionState<>(currMethod, ms.copy(), heap.copy(), receiver, arguments, ls.fork(), localFlags, globalFlags, cycleSet, foreignHeap, rootContext, this.callerState, this.callExpr);
	}

	public void dump() {
		heap.assertSaneStructure();
		System.out.println("Receiver: " + receiver);
		System.out.println("Args: " + arguments);
		System.out.println("Heap: " + heap);
		System.out.println("State: " + ms);
		System.out.println("Foreign heap: " + (foreignHeap != null ? foreignHeap.state : null));
	}

	public void replaceHeap(final EmbeddedState<FH> fh) {
		this.foreignHeap = fh;
	}
	
	public static <FH, Context> ExecutionState<FH, Context> join(final ExecutionState<FH, Context> e1, final ExecutionState<FH, Context> e2) {
		assert e1.arguments.size() == e2.arguments.size();
		final List<IValue> widenedArgs = new ArrayList<>(e2.arguments.size());
		for(int i = 0; i < e2.arguments.size(); i++) {
			widenedArgs.add(ValueMerger.STRICT_MERGE.merge(e1.arguments.get(i), e2.arguments.get(i)));
		}
		final IValue receiver = ValueMerger.STRICT_MERGE.merge(e1.receiver, e2.receiver);
		final Heap h = Heap.join(e1.heap, e2.heap);
		final MethodState ms = MethodState.join(e1.ms, e2.ms);
		final EmbeddedState<FH> embed;
		if(e1.foreignHeap == null) {
			embed = e2.foreignHeap;
		} else if(e2.foreignHeap == null) {
			embed = e1.foreignHeap;
		} else {
			assert e1.foreignHeap.stateLattice == e2.foreignHeap.stateLattice;
			embed = new EmbeddedState<>(e1.foreignHeap.stateLattice.join(e1.foreignHeap.state, e2.foreignHeap.state), e2.foreignHeap.stateLattice);
		}
		assert e1.currMethod == e2.currMethod;
		assert e1.ls.equals(e2.ls);
		assert e1.rootContext == e2.rootContext;
		assert Objects.equals(e1.cycleSet, e2.cycleSet);
		assert e1.callerState == e2.callerState;
		assert e1.callExpr.equals(e2.callExpr);
		return new ExecutionState<>(e1.currMethod, ms, h, receiver, widenedArgs, e1.ls.fork(), NO_LOCAL_FLAGS, NO_GLOBAL_FLAGS, e1.cycleSet, embed, e1.rootContext, e1.callerState, e1.callExpr);
	}
	
	public void merge(final ExecutionState<FH, ?> other) {
		this.ms.merge(other.ms);
		this.mergeHeaps(other);
	}
	
	public void mergeHeaps(final ExecutionState<FH, ?> other) {
		this.heap.mergeAndPopHeap(other.heap);
		this.foreignHeap = other.foreignHeap;
	}

	@SuppressWarnings("unchecked")
	public void mergeHeaps(final ReturnState<?> rs) {
		this.heap.applyHeap(rs.h);
		this.foreignHeap = (EmbeddedState<FH>) rs.foreignHeap;
	}

	public ExecutionState<FH, Context> withHeap(final Heap collapsed) {
		return new ExecutionState<>(currMethod, ms, collapsed, receiver, arguments, ls, localFlags, globalFlags, cycleSet, foreignHeap, rootContext, callerState, callExpr);
	}

	@Override
	public Heap getHeap() {
		return heap;
	}

	@Override
	public Object getState() {
		return this;
	}
}
