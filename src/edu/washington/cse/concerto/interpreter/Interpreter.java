package edu.washington.cse.concerto.interpreter;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import edu.washington.cse.concerto.interpreter.ai.IntrinsicHandler;
import edu.washington.cse.concerto.interpreter.ai.binop.ObjectIdentityResult;
import edu.washington.cse.concerto.interpreter.ai.instantiation.pta.BasicInterpreter;
import edu.washington.cse.concerto.interpreter.ai.test.AssertionChecker;
import edu.washington.cse.concerto.interpreter.annotations.MutatesFork;
import edu.washington.cse.concerto.interpreter.exception.FailedExecutionException;
import edu.washington.cse.concerto.interpreter.exception.FailedObjectLanguageAssertionException;
import edu.washington.cse.concerto.interpreter.exception.NullPointerException;
import edu.washington.cse.concerto.interpreter.exception.OutOfBoundsArrayAccessException;
import edu.washington.cse.concerto.interpreter.exception.PruneExecutionException;
import edu.washington.cse.concerto.interpreter.exception.ThrowToState;
import edu.washington.cse.concerto.interpreter.exception.UnrecognizedIntrinsicException;
import edu.washington.cse.concerto.interpreter.heap.Heap;
import edu.washington.cse.concerto.interpreter.heap.HeapAccessResult;
import edu.washington.cse.concerto.interpreter.heap.HeapFaultStatus;
import edu.washington.cse.concerto.interpreter.heap.HeapReadResult;
import edu.washington.cse.concerto.interpreter.loop.AugmentedLoop;
import edu.washington.cse.concerto.interpreter.loop.LoopState;
import edu.washington.cse.concerto.interpreter.mock.SimpleForeignHeap;
import edu.washington.cse.concerto.interpreter.state.ConcreteGlobalState;
import edu.washington.cse.concerto.interpreter.state.ExecutionState;
import edu.washington.cse.concerto.interpreter.state.ExecutionState.LocalExecutionFlag;
import edu.washington.cse.concerto.interpreter.state.GlobalState;
import edu.washington.cse.concerto.interpreter.state.PartialConcreteState;
import edu.washington.cse.concerto.interpreter.value.EmbeddedValue;
import edu.washington.cse.concerto.interpreter.value.IValue;
import edu.washington.cse.concerto.interpreter.value.IValue.RuntimeTag;
import edu.washington.cse.concerto.interpreter.value.IValueAction;
import fj.F3;
import fj.P;
import fj.P2;
import fj.P3;
import fj.data.Option;
import fj.data.Seq;
import fj.data.Stream;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import soot.AnySubType;
import soot.Body;
import soot.G;
import soot.Local;
import soot.PatchingChain;
import soot.RefLikeType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.baf.ThrowInst;
import soot.grimp.internal.GNewInvokeExpr;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.ClassConstant;
import soot.jimple.Constant;
import soot.jimple.GotoStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InstanceOfExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.LengthExpr;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.NopStmt;
import soot.jimple.ParameterRef;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.StringConstant;
import soot.jimple.ThisRef;
import soot.jimple.ThrowStmt;
import soot.jimple.internal.AbstractBinopExpr;
import soot.jimple.internal.AbstractFloatBinopExpr;
import soot.jimple.internal.AbstractIntBinopExpr;
import soot.options.Options;
import soot.toolkits.exceptions.ThrowAnalysis;
import soot.toolkits.exceptions.ThrowableSet;
import soot.toolkits.scalar.Pair;
import soot.util.NumberedString;
import soot.util.StringNumberer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;



public class Interpreter<Context, FH> {
	public static boolean LOG_REFLECTION = false;
	public static boolean DEBUG_CALLS = false;
	public static boolean TRACK_CALL_EDGES = false;
	public static Multimap<Object, SootMethod> CALL_GRAPH = HashMultimap.create();

	protected final GlobalState globalState;
	
	private final static InterpreterState<?> nullResult = new InterpreterState<>(null);
	
	private final NumberedString equalIntSig;
	private final NumberedString equalObjectSig;
	private final NumberedString equalBoolSig;
	
	private final NumberedString liftObjectSig;
	private final NumberedString liftIntSig;
	private final NumberedString liftBoolSig;
	
	private final NumberedString printSig;
	private final NumberedString printPolySig;
	private final NumberedString readSig;
	private final NumberedString nondetSig;
	private final NumberedString writeSig;
	
	private final NumberedString dumpStateSig;
	private final NumberedString dumpStateMsgSig;
	private final NumberedString dumpObjectStateSig;
	private final NumberedString dumpIRSig;

	private final NumberedString notEqualIntSig;
	private final NumberedString notEqualObjectSig;
	private final NumberedString notEqualBoolSig;

	private final NumberedString debugSig;
	private final NumberedString abortSig;
	private final NumberedString assertFalseSig;
	private final NumberedString failSig;
	private final NumberedString printStackSig;

	private final NumberedString liftLocationSig;
	private final NumberedString putLocationSig;
	private final NumberedString getLocationSig;

	private final NumberedString assertObjToStringEquals;
	private final NumberedString assertIntToStringEquals;
	private final NumberedString customAssertSig;

	private final NumberedString checkAssertSig;

	protected InterpreterExtension<FH> interpretExtension;
	protected InvokeInterpreterExtension<FH, Context> invokeExtension;

	public static class InterpreterMultiState<FH> {
		public final Map<Unit, InterpreterState<FH>> results = new HashMap<>();
		private boolean incomplete;
		
		public InterpreterMultiState() { }
		
		public InterpreterMultiState<FH> merge(final InterpreterMultiState<FH> other) {
			final Set<Unit> merged = new HashSet<>();
			final InterpreterMultiState<FH> toReturn = new InterpreterMultiState<>();
			for(final Map.Entry<Unit, InterpreterState<FH>> kv : this.results.entrySet()) {
				final Unit stopUnit = kv.getKey();
				merged.add(stopUnit);
				final InterpreterState<FH> stopState = kv.getValue();
				if(other.results.containsKey(stopUnit)) {
					toReturn.results.put(stopUnit, this.mergeResults(stopState, other.results.get(stopUnit)));
				} else {
					toReturn.results.put(stopUnit, stopState);
				}
			}
			for(final Entry<Unit, InterpreterState<FH>> kv : other.results.entrySet()) {
				if(!merged.add(kv.getKey())) {
					continue;
				}
				toReturn.results.put(kv.getKey(), kv.getValue());
			}
			return toReturn;
		}
		
		public static <FH> InterpreterMultiState<FH> lift(final InterpreterState<FH> toLift) {
			final InterpreterMultiState<FH> toRet = new InterpreterMultiState<>();
			toRet.results.put(toLift.stopUnit, toLift);
			return toRet;
		}

		private InterpreterState<FH> mergeResults(final InterpreterState<FH> s1, final InterpreterState<FH> s2) {
			assert s1.stopUnit == s2.stopUnit;
			final Heap finalHeap = mergeToCommonHeap(s1.h, s2.h);
			final MethodState ms = Interpreter.mergeToCommonState(s1.ms, s2.ms);
			final EmbeddedState<FH> fh = joinForeignHeaps(s1.foreignHeap, s2.foreignHeap);
			assert s1.rs == null;
			assert s2.rs == null;
			return new InterpreterState<>(ms, finalHeap, null, s1.stopUnit, fh);
		}

		public InterpreterState<FH> getSingle() {
			assert results.size() == 1;
			return results.values().iterator().next();
		}

		public boolean isEmpty() {
			return results.isEmpty();
		}

		public void markIncomplete() {
			this.incomplete = true;
		}
	}


	public Interpreter(final GlobalState gs, final InterpreterExtension<FH> interpretExtension, final InvokeInterpreterExtension<FH, Context> invokeExtension) {
		this.globalState = gs;
		final StringNumberer numberer = Scene.v().getSubSigNumberer();
		liftObjectSig = numberer.findOrAdd("java.lang.Object lift(java.lang.Object[])");
		liftIntSig = numberer.findOrAdd("int lift(int[])");
		liftBoolSig = numberer.findOrAdd("boolean lift(boolean[])");
		
		equalObjectSig = numberer.findOrAdd("void assertEqual(java.lang.Object,java.lang.Object)");
		equalIntSig = numberer.findOrAdd("void assertEqual(int,int)");
		equalBoolSig = numberer.findOrAdd("void assertEqual(boolean,boolean)");
		
		printSig = numberer.findOrAdd("void println(int)");
		printPolySig = numberer.findOrAdd("void println(java.lang.Object)");
		readSig = numberer.findOrAdd("int read()");
		nondetSig = numberer.findOrAdd("int nondet()");
		writeSig = numberer.findOrAdd("void write(int)");
		
		dumpStateSig = numberer.findOrAdd("void dumpState()");
		dumpStateMsgSig = numberer.findOrAdd("void dumpState(java.lang.String)");
		dumpObjectStateSig = numberer.findOrAdd("void dumpObjectState(java.lang.Object)");
		dumpIRSig = numberer.findOrAdd("void dumpIR()");
		
		notEqualIntSig = numberer.findOrAdd("void assertNotEqual(int,int)");
		notEqualObjectSig = numberer.findOrAdd("void assertNotEqual(java.lang.Object,java.lang.Object)");
		notEqualBoolSig = numberer.findOrAdd("void assertNotEqual(boolean,boolean)");
		
		debugSig = numberer.findOrAdd("void debug(java.lang.String)");
		abortSig = numberer.findOrAdd("void abort()");
		failSig = numberer.findOrAdd("void fail(java.lang.String)");
		assertFalseSig = numberer.findOrAdd("void assertFalse(java.lang.String)");
		printStackSig = numberer.findOrAdd("void printStackTrace()");
		
		liftLocationSig = numberer.findOrAdd("intr.ForeignLocation liftLocation(java.lang.String)");
		getLocationSig = numberer.findOrAdd("int get(intr.ForeignLocation)");
		putLocationSig = numberer.findOrAdd("void put(intr.ForeignLocation,int)");
		
		assertObjToStringEquals = numberer.findOrAdd("void assertToStringEquals(java.lang.Object,java.lang.String)");
		assertIntToStringEquals = numberer.findOrAdd("void assertToStringEquals(int,java.lang.String)");
		customAssertSig = numberer.findOrAdd("void customAssert(java.lang.Object,java.lang.String,java.lang.Class)");
		
		checkAssertSig = numberer.findOrAdd("void checkAsserts()");
		
		this.interpretExtension = interpretExtension;
		this.invokeExtension = invokeExtension;
	}

	public Interpreter(final GlobalState gs) {
		this(gs, null, null);
	}

	@SuppressWarnings("unchecked")
	public static void main(final String[] inArgs) throws IOException {
		final OptionParser parser = new OptionParser("n:");
		parser.accepts("m").withRequiredArg().ofType(String.class).defaultsTo("Main");
		final OptionSet parse = parser.parse(inArgs);
		final String[] args = parse.nonOptionArguments().toArray(new String[0]);
		final String mainClassName = (String) parse.valueOf("m");
		final String classpath = args[0];

		final SootClass mainClass = setupSoot(mainClassName, classpath);
		final GlobalState gs = getState(args[1], (String) parse.valueOf("n"));
		final Interpreter<Void, Void> i = new Interpreter<>(gs);
		i.start(mainClass);
	}
	
	public void start(final SootClass mainClass) {
		this.start(mainClass, "main");		
	}

	public void start(final SootClass mainClass, final String mainMethodName) {
		final SootMethod mainMethod = mainClass.getMethod("void " + mainMethodName + "()");
		final Heap h = new Heap();
		final IValue main = h.allocate(mainClass, ClassConstant.fromType(mainClass.getType()), new Object());
		final ExecutionState<FH, Context> newState = new ExecutionState<>(mainMethod, new MethodState(), h, main, Collections.emptyList(), new LoopState(mainMethod), null, null, null, Option.none());
		this.interpret(newState, mainMethod, main, Collections.emptyList(), Option.none());
	}

	@SuppressWarnings("deprecation")
	public static SootClass setupSoot(final String mainClassName, final String classpath) {
		G.reset();
		Scene.v().setDefaultThrowAnalysis(new ThrowAnalysis() {
			@Override
			public ThrowableSet mightThrowImplicitly(final ThrowStmt t) {
				return ThrowableSet.Manager.v().EMPTY;
			}
			
			@Override
			public ThrowableSet mightThrowImplicitly(final ThrowInst t) {
				return ThrowableSet.Manager.v().EMPTY;
			}
			
			@Override
			public ThrowableSet mightThrowExplicitly(final ThrowStmt t) {
				return ThrowableSet.Manager.v().EMPTY;
			}
			
			@Override
			public ThrowableSet mightThrowExplicitly(final ThrowInst t) {
				return ThrowableSet.Manager.v().EMPTY;
			}
			
			@Override
			public ThrowableSet mightThrow(final Unit u) {
				return ThrowableSet.Manager.v().EMPTY;
			}
		});

		Options.v().set_via_grimp(true);
		Options.v().set_allow_phantom_refs(true);
		Options.v().set_soot_classpath(classpath);
		Options.v().set_keep_line_number(true);
		
		Options.v().set_main_class(mainClassName);
		Scene.v().addBasicClass(mainClassName, SootClass.SIGNATURES);
		G.v().out = new PrintStream(new OutputStream() {
			@Override
			public void write(final int arg0) throws IOException { }
		});
		
		Scene.v().loadBasicClasses();
		final SootClass mainClass = Scene.v().getSootClass(mainClassName);
		return mainClass;
	}
	
	private static GlobalState getState(final String detFile, final String nondetFile) throws FileNotFoundException {
		if(nondetFile != null) {
			return new ConcreteGlobalState(detFile, nondetFile);
		} else {
			return new PartialConcreteState(detFile);
		}
	}
	
	public InterpreterState<FH> interpretUntil(final SootMethod method, final ExecutionState<FH, Context> state, final Unit start, final Object breakPoint) {
		try {
			return this.interpretUntilMayPrune(method, state, start, breakPoint);
		} catch(final PruneExecutionException e) {
			this.incompleteExecutionCallback();
			return null;
		}
	}

	protected void incompleteExecutionCallback() { 
		if(this.interpretExtension != null) {
			this.interpretExtension.markIncompleteExecution();
		}
	}

	protected boolean keepGoing(final Unit u, final Object target) {
		if(target instanceof AugmentedLoop) {
			final AugmentedLoop loop = (AugmentedLoop) target;
			return loop.exitStmts.contains(u);
		} else if(target instanceof LoopToken) {
			final AugmentedLoop loop = ((LoopToken) target).activeLoop;
			return loop.exitStmts.contains(u);
		} else {
			return u != target;
		}
	}
	
	private static final Map<SootMethod, JoinPointFinder> dominators = new HashMap<>();
	
	protected InterpreterState<FH> interpretUntilMayPrune(final SootMethod method, final ExecutionState<FH, Context> state, final Unit start, final Object breakPoint) {
		ReturnState<FH> rs = null;
		final Body body = BodyManager.retrieveBody(method);
		final PatchingChain<Unit> units = body.getUnits();
		Unit u = start == null ? units.getFirst() : start;
		while(keepGoing(u, breakPoint)) {
			state.ls.visitStatement(u);
			if(u instanceof ReturnStmt) {
				checkReturnInLoop(state);
				return returnValue(state, interpretValue(state, ((ReturnStmt) u).getOp()), rs);
			} else if(u instanceof ReturnVoidStmt) {
				checkReturnInLoop(state);
				return returnValue(state, null, rs);
			} else if(u instanceof GotoStmt) {
				u = ((GotoStmt) u).getTarget();
				continue;
			} else if(u instanceof NopStmt) {
				// do nothing
			} else if(u instanceof InvokeStmt) {
				interpretInvoke(state, ((InvokeStmt) u).getInvokeExpr());
			} else if(u instanceof IdentityStmt) {
				final Value rightOp = ((IdentityStmt) u).getRightOp();
				final Local l = (Local) ((IdentityStmt) u).getLeftOp();
				final IValue rhsValue;
				if(rightOp instanceof ParameterRef) {
					rhsValue = state.arguments.get(((ParameterRef) rightOp).getIndex());
				} else if(rightOp instanceof ThisRef) {
					rhsValue = state.receiver;
				} else {
					throw mkValueException(method, state.receiver, state.arguments, rightOp, "identity stmt");
				}
				state.ms.put(l.getName(), rhsValue);
			} else if(u instanceof AssignStmt) {
				final Value rightOp = ((AssignStmt) u).getRightOp();
				final Value leftOp = ((AssignStmt) u).getLeftOp();
				if(leftOp instanceof ArrayRef) {
					final IValue arrayRef = this.arrayBasePointerHook(state, interpretValue(state, ((ArrayRef) leftOp).getBase()), false, (ArrayRef) leftOp);
					final IValue index = interpretValue(state, ((ArrayRef) leftOp).getIndex());
					final IValue rawRhs = interpretValue(state, rightOp);

					doArrayWrite(state, (ArrayRef) leftOp, arrayRef, index, rawRhs, false);
				} else if(leftOp instanceof InstanceFieldRef) {
					final InstanceFieldRef iRef = (InstanceFieldRef) leftOp;
					final IValue base = this.fieldBasePointerHook(state, interpretValue(state, iRef.getBase()), false, iRef);
					
					final IValue rhs = this.fieldOutputHook(state, base, interpretValue(state, rightOp), false, iRef);
					final HeapAccessResult result = state.heap.putField(base, iRef.getFieldRef(), rhs);
					checkHeapFaults(result);
				} else if(leftOp instanceof Local) {
					state.ms.put(((Local) leftOp).getName(), interpretValue(state, rightOp));
				} else {
					throw mkValueException(method, state, rightOp, "right hand form");
				}
			} else if(u instanceof IfStmt) {
				// if this is the first conditional in our loop exit block, try to make it to the end, tracking multiple exists
				if(state.ls.isLoopExitBlock(u) && !(breakPoint instanceof LoopToken)) {
					// we are in a state with a heap H
					// we assume that no loop exit blocks occur in a conditional so the heap here
					// has the same identity as the loop itself
					final ExecutionState<FH, Context> exitState = state.fork();
					final InterpreterMultiState<FH> stopPoints = handleLoopExit(method, exitState, u, units);
					if(stopPoints.results.size() == 1 && !stopPoints.incomplete) {
						// guaranteed to be outside the loop now, the returned state has Heap o H'
						final InterpreterState<FH> nextStep = stopPoints.getSingle();
						u = nextStep.stopUnit;
						// apply all and continue
						assert nextStep.h.descendantOf(state.heap);
						assert nextStep.ms.descendantOf(state.ms);
						
						state.replaceHeap(nextStep.foreignHeap);
						state.heap.mergeAndPopHeap(nextStep.h.popTo(state.heap));
						state.ms.merge(nextStep.ms.popTo(state.ms));
					} else {
						final ExecutionState<FH, Context> loopExit = findLoopFixpoint(method, state, stopPoints);
						assert loopExit.heap.descendantOf(state.heap);
						state.merge(loopExit);
						u = state.ls.getActiveLoop().getLoopSuccessor();
					}
					continue;
				}
				final List<Pair<ExecutionState<FH, Context>, Unit>> successors = getConditionalSuccessors(state, (IfStmt) u);
				// resolve and continue
				if(successors.size() == 1) {
					assert successors.get(0).getO1() == state;
					u = successors.get(0).getO2();
					continue;
				// we're executing looking for a loop exit: don't try joining, just get to either the loop exit or loop head
				} else if(breakPoint instanceof LoopToken) {
					assert successors.size() == 2;
					final InterpreterState<FH> succ1 = executeIfSuccessor(method, breakPoint, successors.get(0));
					final InterpreterState<FH> succ2 = executeIfSuccessor(method, breakPoint, successors.get(1));
					assert succ1 == null && succ2 == null;
					return null;
				// find a join point, fork and then join
				} else {
					final Object joinPoint = this.findJoinPoint(method, u);
					assert successors.size() == 2;
					InterpreterState<FH> succ1 = executeIfSuccessor(method, joinPoint, successors.get(0));
					InterpreterState<FH> succ2 = executeIfSuccessor(method, joinPoint, successors.get(1));
					if(succ1 == null && succ2 == null) {
						return null;
					} else if(succ2 == null) {
						succ2 = nullResult();
					} else if(succ1 == null) {
						succ1 = nullResult();
					}
					rs = mergeReturnState(succ1.rs, succ2.rs, rs);
					final boolean hasResultHeap = mergeInterpreterResults(state, succ1, succ2);
					if(!hasResultHeap) {
						return new InterpreterState<>(rs);
					}
					assert succ1.stopUnit == null || succ2.stopUnit == null || succ1.stopUnit == succ2.stopUnit : succ1.stopUnit + " " + succ2.stopUnit;
					u = succ1.stopUnit != null ? succ1.stopUnit : succ2.stopUnit;
					continue;
				}
			}
			u = units.getSuccOf(u);
		}
		return this.processResult(new InterpreterState<>(state.ms, state.heap, rs, u, state.foreignHeap), breakPoint);
	}

	protected void doArrayWrite(final ExecutionState<FH, Context> state, final ArrayRef leftOp, final IValue arrayRef, final IValue index,
			final IValue rawRhs, final boolean isWeakWrite) {
		final IValue rhs = this.arrayOutputHook(state, arrayRef, rawRhs, false, leftOp);
		final HeapAccessResult stat = state.heap.putArray(arrayRef, index, rhs, isWeakWrite);
		checkHeapFaults(stat);
	}

	public void checkHeapFaults(final HeapAccessResult result) {
		if(result.npe == HeapFaultStatus.MUST) {
			throw new NullPointerException();
		} else if(result.oob == HeapFaultStatus.MUST) {
			throw new OutOfBoundsArrayAccessException();
		}
	}

	private InterpreterState<FH> executeIfSuccessor(final SootMethod method, final Object breakPoint, final Pair<ExecutionState<FH, Context>, Unit> successor) {
		return this.interpretUntil(method, successor.getO1().withFlags(LocalExecutionFlag.RECORD_CYCLE), successor.getO2(), breakPoint);
	}

	protected List<Pair<ExecutionState<FH, Context>, Unit>> getConditionalSuccessors(final ExecutionState<FH, Context> state, final IfStmt u) {
		final IValue res = interpretValue(state, u.getCondition());
		if(res.isDeterministic()) {
			return Collections.singletonList(new Pair<>(state, resolveCondition(state, u, res)));
		} else {
			final List<Pair<ExecutionState<FH, Context>, Unit>> toReturn = new ArrayList<>();
			toReturn.add(new Pair<>(state.fork(), getSuccessor(state, u)));
			toReturn.add(new Pair<>(state.fork(), u.getTarget()));
			return toReturn;
		}
	}

	/* 
	 * We have found a cycle: m1 *> m2 ->* mn) m1, but we have not started executing m1 yet
	 * (where *> indicates a call performed inside a non-deterministic branch)
	 * state is defined as:
	 * H0 o Hm1 * Hj,1 o Hm2 (* Hj,i o Hmi)* o {}
	 * (where * Hj,i indicates j >= 0 or more extra heaps forked during the execution of method i,
	 * and (...)* indicates a repeated pattern of heaps.
	 *
	 * We over-approximate the call m1 and return directly to it's caller. We do this by:
	 * 
	 * 1) Finding H0, and recreating a fresh start state for m1., i.e. H = H0 o { }
	 * 2) Collapsing state to give H' = H0 o H'', where H'' is the sequence Hm1 * Hj,1 o Hm2 (* Hj,i o Hmi)* o {} collapsed
	 * 3) Widening with state, to yield our starting state for the fixpoint computation.
	 * 4) invoking the fixpoint solver, with an _initial_ set of widening points {m1}
	 */
	private IValue findCycleFixpoint(final ExecutionState<FH, Context> state, final SootMethod method, final List<IValue> args, final IValue base) {
		// find our starting state for the head of our cycle
		ExecutionState<FH, Context> callee;
		if(!state.currMethod.equals(method)) {
			callee = state.callerState;
			while(!callee.currMethod.equals(method)) {
				callee = callee.callerState;
			}
		} else {
			callee = state;
		}
		assert callee.localFlags.contains(LocalExecutionFlag.RECORD_CYCLE);
		assert state.heap.descendantOf(callee.heap) || state.heap == callee.heap;
		final ExecutionState<FH, Context> head = callee;
		// callee corresponds to the heap H0 o Hm1 * Hj,1 we must now find it's caller
		while(callee.currMethod.equals(method)) {
			callee = callee.callerState;
		}
		final ExecutionState<FH, Context> prev = new ExecutionState<>(method, new MethodState(), callee.heap.fork(), head.receiver, head.arguments,
			new LoopState(method), callee.foreignHeap, callee.rootContext, null, Option.none());
		final Heap callingHeap = state.heap.popTo(callee.heap);
		final ExecutionState<FH, Context> next = new ExecutionState<>(method, new MethodState(), callingHeap, base, args, new LoopState(method), state.foreignHeap, state.rootContext,
				null, Option.none());
		final ExecutionState<FH, Context> widened = ExecutionState.widen(prev, next);
		final FixpointFinder<FH, Context> fi = new FixpointFinder<>(widened, method, callee, this);
		final InterpreterState<FH> returned = fi.findFixpoint();
		throw new ThrowToState(callee, returned);
	}

	private static <FH, Context> void dumpCallStack(final ExecutionState<FH, Context> state) {
		System.out.println("++ Call stack (last call first) ++");
		ExecutionState<FH, Context> it = state;
		while(it != null) {
			dumpState(it);
			it = it.callerState;
		}
		if(state.rootContext != null) {
			System.out.println("Calling Context: " + state.rootContext);
		}
		System.out.println("+ DONE +");
	}

	private void checkReturnInLoop(final ExecutionState<?, ?> state) {
		if(state.ls.getActiveLoop() != null) {
			throw new RuntimeException("No returns in loops");
		}
	}

	protected Unit resolveCondition(final ExecutionState<FH, Context> state, Unit u, final IValue res) {
		if(res.asBoolean()) {
			u = ((IfStmt) u).getTarget();
		} else {
			u = getSuccessor(state, u);
		}
		return u;
	}

	protected Unit getSuccessor(final ExecutionState<FH, Context> state, final Unit u) {
		return BodyManager.retrieveBody(state.currMethod).getUnits().getSuccOf(u);
	}

	// state.heap is the "active heap" in which we are executing the loop
	// stopPoints maps units to interpreter states, whose heaps/stores are descendants of those in state 
	private ExecutionState<FH, Context> findLoopFixpoint(final SootMethod method, final ExecutionState<FH, Context> state, final InterpreterMultiState<FH> stopPoints) {
		if(stopPoints.incomplete) {
			return computeCoarseLoopFixpoint(method, state, stopPoints);
		}
		final InterpreterMultiState<FH> res = stopPoints;
		final Unit loopBodyStart = state.ls.getActiveLoop().getLoopStart();
		final Unit loopSuccessor = state.ls.getActiveLoop().getLoopSuccessor();
		
		assert res.results.containsKey(loopSuccessor);
		assert res.results.containsKey(loopBodyStart);
		
		final ExecutionState<FH, Context> startState = generateDescendantState(state, res.results.get(loopBodyStart));
		final ExecutionState<FH, Context> exitState = generateDescendantState(state, res.results.get(loopSuccessor));
		while(true) {
			final ExecutionState<FH, Context> initialState = startState.fork();
			final ExecutionState<FH, Context> iterationState = startState.fork().withFlags(LocalExecutionFlag.RECORD_CYCLE);
			final InterpreterState<FH> interpretResult = this.interpretUntil(method, iterationState, loopBodyStart, state.ls.getActiveLoop().getExitDominator());
			// Welp, we can't execute the loop body :(
			// XXX: this may be unsound, let's try to prove this is okay to do
			if(interpretResult == null) {
				return exitState;
			}
			final InterpreterMultiState<FH> loopExits = handleLoopExit(method, iterationState, state.ls.getActiveLoop().getExitDominator(), BodyManager.retrieveBody(method).getUnits());
			if(loopExits.isEmpty()) {
				throw new RuntimeException("Is this impossible?");
//				throw new PruneExecutionException();
			}
			if(!loopExits.results.containsKey(loopBodyStart)) {
				return generateLoopExit(state, loopSuccessor, exitState, loopExits);
			}
			final ExecutionState<FH, Context> afterIter = generateDescendantState(startState, loopExits.results.get(loopBodyStart));
			final ExecutionState<FH, Context> widened = ExecutionState.widen(initialState, afterIter);
			if(widened.lessEqual(initialState)) {
				return generateLoopExit(state, loopSuccessor, exitState, loopExits);
			}
			startState.merge(widened);
		}
	}

	private ExecutionState<FH, Context> computeCoarseLoopFixpoint(final SootMethod method, final ExecutionState<FH, Context> state, final InterpreterMultiState<FH> stopPoints) {
		final Unit loopBodyStart = state.ls.getActiveLoop().getLoopStart();
		final ExecutionState<FH, Context> startState = computeAllPathsStart(state, stopPoints);
		final ExecutionState<FH, Context> exitState = startState.copy();
		while(true) {
			final ExecutionState<FH, Context> initialState = startState.fork();
			final ExecutionState<FH, Context> iterationState = startState.fork().withFlags(LocalExecutionFlag.RECORD_CYCLE);
			final InterpreterState<FH> interpretResult = this.interpretUntil(method, iterationState, loopBodyStart, state.ls.getActiveLoop().getExitDominator());
			// we can't execute the body, so let's pretend it never happened
			if(interpretResult == null) {
				return startState;
			}
			final InterpreterMultiState<FH> loopExits = handleLoopExit(method, iterationState, state.ls.getActiveLoop().getExitDominator(), BodyManager.retrieveBody(method).getUnits());
			final ExecutionState<FH, Context> afterIter = computeAllPathsStart(startState, loopExits);
			final ExecutionState<FH, Context> widened = ExecutionState.widen(initialState, afterIter);
			if(widened.lessEqual(initialState)) {
				return ExecutionState.join(exitState, generateDescendantState(state, initialState));
			}
			startState.merge(widened);
		}
	}

	private ExecutionState<FH, Context> generateDescendantState(final ExecutionState<FH, Context> state, final ExecutionState<FH, Context> initialState) {
		assert initialState.heap.descendantOf(state.heap);
		assert initialState.ms.descendantOf(state.ms);
		final Heap h = initialState.heap.popTo(state.heap);
		final MethodState ms = initialState.ms.popTo(state.ms);
		return new ExecutionState<>(state.currMethod, ms, h, state.receiver, state.arguments, state.ls, state.localFlags, state.globalFlags,
			state.cycleSet, initialState.foreignHeap, state.rootContext, state.callerState, state.callExpr);
	}

	private ExecutionState<FH, Context> computeAllPathsStart(final ExecutionState<FH, Context> state, final InterpreterMultiState<FH> stopPoints) {
		ExecutionState<FH, Context> accum = null;
		for(final InterpreterState<FH> pathState : stopPoints.results.values()) {
			if(accum == null) {
				accum = generateDescendantState(state, pathState);
			} else {
				accum = ExecutionState.join(accum, generateDescendantState(state, pathState));
			}
		}
		return accum;
	}

	private ExecutionState<FH, Context> generateLoopExit(final ExecutionState<FH, Context> state, final Unit loopSuccessor, final ExecutionState<FH, Context> exitState,
			final InterpreterMultiState<FH> loopExits) {
		return ExecutionState.join(generateDescendantState(state, loopExits.results.get(loopSuccessor)), exitState);
	}

	private ExecutionState<FH, Context> generateDescendantState(final ExecutionState<FH, Context> state, final InterpreterState<FH> loopHeadState) {
		assert loopHeadState.h.descendantOf(state.heap);
		assert loopHeadState.ms.descendantOf(state.ms);
		final Heap h = loopHeadState.h.popTo(state.heap);
		final MethodState ms = loopHeadState.ms.popTo(state.ms);
		return new ExecutionState<>(state.currMethod, ms, h, state.receiver, state.arguments, state.ls, state.localFlags, state.globalFlags,
			state.cycleSet, loopHeadState.foreignHeap, state.rootContext, state.callerState, state.callExpr);
	}

	protected static <FH, Context> void dumpState(final ExecutionState<FH, Context> state) {
		System.out.println(">>>");
		System.out.println("METHOD: " + state.currMethod);
		System.out.println("RECEIVER: " + state.receiver);
		System.out.println("ARGS: " + state.arguments);
		System.out.println("STATE: " + state.ms);
		System.out.println("HEAP: " + state.heap);
		System.out.println("FLAGS: " + state.localFlags);
		System.out.println("<<<");
	}
	
	private static class LoopToken {
		private final AugmentedLoop activeLoop;
		public LoopToken(final AugmentedLoop al) {
			this.activeLoop = al;
		}
	}
	
	protected Interpreter<Context, FH> deriveNewInterpreter(final InterpreterExtension<FH> processor) {
		return new Interpreter<>(this.globalState, processor, this.invokeExtension);
	}
	
	protected Interpreter<Context, FH> deriveNewInterpreter(final InvokeInterpreterExtension<FH, Context> processor) {
		return new Interpreter<>(this.globalState, this.interpretExtension, processor);
	}
	
	protected Interpreter<Context, FH> deriveNewInterpreter(final GlobalState gs) {
		return new Interpreter<>(gs, this.interpretExtension, this.invokeExtension);
	}

	private InterpreterMultiState<FH> handleLoopExit(final SootMethod method, final ExecutionState<FH, Context> exitState, final Unit u, final PatchingChain<Unit> units) {
		final LoopToken lt = new LoopToken(exitState.ls.getActiveLoop());
		@SuppressWarnings("unchecked")
		final InterpreterMultiState<FH>[] toReturn = new InterpreterMultiState[1];
		final boolean prunedFlag[] = new boolean[]{false};
		final Interpreter<Context, FH> it = deriveNewInterpreter(new InterpreterExtension<FH>() {
			@Override
			public InterpreterState<FH> processResult(final InterpreterState<FH> inState, final Object breakPoint) {
				if(breakPoint == lt) {
					if(toReturn[0] == null) {
						toReturn[0] = InterpreterMultiState.lift(inState);
					} else {
						toReturn[0] = toReturn[0].merge(InterpreterMultiState.lift(inState));
					}
					return null;
				} else {
					return inState;
				}
			}
			
			@Override
			public void markIncompleteExecution() {
				prunedFlag[0] = true;
			}
		});
		it.interpretUntil(method, exitState, u, lt);
		if(prunedFlag[0]) {
			toReturn[0].markIncomplete();
		}
		return toReturn[0];
	}

	public InterpreterState<FH> processResult(final InterpreterState<FH> inState, final Object target) {
		if(this.interpretExtension != null) {
			return this.interpretExtension.processResult(inState, target);
		} else {
			return inState;
		}
	}
	
	private static <FH, Context> boolean mergeInterpreterResults(final ExecutionState<FH, Context> state, final InterpreterState<FH> trueResult,
			final InterpreterState<FH> falseResult) {
		if(!mergeResultHeaps(state, trueResult, falseResult)) {
			return false;
		}
		mergeResultStates(state.ms, trueResult, falseResult);
		return true;
	}
	
	private static <FH, Context> void mergeResultStates(final MethodState ms, final InterpreterState<FH> trueResult, final InterpreterState<FH> falseResult) {
		assert trueResult.ms != null || falseResult.ms != null;
		if(trueResult.ms == null) {
			ms.merge(falseResult.ms);
		} else if(falseResult.ms == null) {
			ms.merge(trueResult.ms);
		} else {
			ms.merge(trueResult.ms, falseResult.ms);
		}
	}

	private static <FH, Context> boolean mergeResultHeaps(final ExecutionState<FH, Context> parent, final InterpreterState<FH> trueResult, final InterpreterState<FH> falseResult) {
		if(trueResult.h == null && falseResult.h == null) {
			return false;
		}
		if(trueResult.h != null && falseResult.h != null) {
			parent.heap.mergeAndPopHeaps(trueResult.h, falseResult.h);
			parent.replaceHeap(joinForeignHeaps(trueResult.foreignHeap, falseResult.foreignHeap));
		} else if(trueResult.h != null) {
			parent.heap.mergeAndPopHeap(trueResult.h);
			parent.replaceHeap(trueResult.foreignHeap);
		} else {
			parent.heap.mergeAndPopHeap(falseResult.h);
			parent.replaceHeap(falseResult.foreignHeap);
		}
		return true;
	}

	private RuntimeException mkValueException(final SootMethod method, final ExecutionState<?, ?> state, final Value rightOp, final String string) {
		return mkValueException(method, state.receiver, state.arguments, rightOp, string);
	}

	protected static void dumpState(final InterpreterState<?> result) {
		System.out.println(">>>");
		System.out.println("=== Continuation state: ===");
		if(result.h == null) {
			System.out.println("NONE");
		} else {
			System.out.println("STATE: " + result.ms);
			System.out.println("HEAP: " + result.h);
		}
		dumpReturnState(result.rs);
		System.out.println("<<<");
	}

	protected static void dumpReturnState(final ReturnState<?> rs) {
		System.out.println("=== Return state: ===");
		if(rs == null) {
			System.out.println("NONE");
		} else {
			System.out.println("HEAP: " + rs.h);
			System.out.println("RETURN: " + rs.returnValue);
		}
	}

	protected InterpreterState<FH> returnValue(final ExecutionState<FH, Context> state, final IValue returnedValue, final ReturnState<FH> currRs) {
		final ReturnState<FH> rs;
		if(currRs == null) {
			rs = new ReturnState<>(returnedValue, state.heap.copy(), state.foreignHeap);
		} else {
			final IValue newReturn;
			if(returnedValue != null) {
				newReturn = IValue.merge(currRs.returnValue, returnedValue);
			} else {
				newReturn = null;
			}
			currRs.h.mergeHeap(state.heap.copy());
			final EmbeddedState<FH> o = joinForeignHeaps(currRs.foreignHeap, state.foreignHeap);
			rs = new ReturnState<>(newReturn, currRs.h, o);
		}
		return new InterpreterState<>(rs);
	}

	private ReturnState<FH> mergeReturnState(final ReturnState<FH> rs1, final ReturnState<FH> rs2, final ReturnState<FH> currRs) {
		if(rs1 == null && rs2 == null) {
			return currRs;
		}
		final Set<IValue> v = new HashSet<>();
		Heap returnHeap = null;
		EmbeddedState<FH> fh = null;
		if(rs1 != null) {
			returnHeap = rs1.h.popHeap();
			if(rs1.returnValue != null) {
				v.add(rs1.returnValue);
			}
			fh = rs1.foreignHeap;
		}
		if(rs2 != null) {
			if(returnHeap != null) {
				returnHeap.mergeHeap(rs2.h.popHeap());
			} else {
				returnHeap = rs2.h.popHeap();
			}
			if(rs2.returnValue != null) {
				v.add(rs2.returnValue);
			}
			fh = joinForeignHeaps(fh, rs2.foreignHeap);
		}
		if(currRs != null) {
			returnHeap.mergeHeap(currRs.h);
			if(rs2.returnValue != null) {
				v.add(currRs.returnValue);
			}
			fh = joinForeignHeaps(fh, currRs.foreignHeap);
		}
		return new ReturnState<>(v.isEmpty() ? (IValue) null : IValue.lift(v), returnHeap, fh);
	}
	
	protected static <FH> EmbeddedState<FH> joinForeignHeaps(final EmbeddedState<FH> s1, final EmbeddedState<FH> s2) {
		if(s1 == null) {
			return s2;
		} else if(s2 == null) {
			return s1;
		} else {
			final FH joined = s1.stateLattice.join(s1.state, s2.state);
			return new EmbeddedState<>(joined, s1.stateLattice);
		}
	}

	public static Heap mergeToCommonHeap(final Heap h1, final Heap h2) {
		final Set<Heap> presentHeaps = Collections.newSetFromMap(new IdentityHashMap<>());
		Heap it = h1;
		while(it != null) {
			presentHeaps.add(it);
			it = it.parentHeap;
		}
		it = h2;
		while(it != null && !presentHeaps.contains(it)) {
			it = it.parentHeap;
		}
		if(it == null) {
			throw new RuntimeException();
		}
		final Heap h1_ = h1.popTo(it);
		final Heap h2_ = h2.popTo(it);
		return Heap.join(h1_, h2_);
	}

	protected Object findJoinPoint(final SootMethod method, final Unit u) {
		final JoinPointFinder jpf;
		if(dominators.containsKey(method)) {
			jpf = dominators.get(method);
		} else {
			jpf = new JoinPointFinder(BodyManager.retrieveBody(method));
			dominators.put(method, jpf);
		}
		return jpf.getJoinPointFor(u);
	}

	protected IValue interpret(final ExecutionState<FH, Context> es, final SootMethod method, final IValue receiver, final List<IValue> vals, final Option<InvokeExpr> callExpr) {
		if(DEBUG_CALLS) {
			System.out.println("STARTING METHOD => " + method);
		}
		final InterpreterState<FH> is = this.interpretUntil(method, es.forNewMethod(method, receiver, vals, callExpr), null, null);
		if(is == null) {
			throw new PruneExecutionException();
		}
		if(DEBUG_CALLS) {
			System.out.println("DONE EXECUTING METHOD: " + method);
		}
		assert is.rs != null;
		assert is.h == null;
		assert is.rs.h.parentHeap == es.heap.parentHeap;
		es.mergeHeaps(is.rs);
		return is.rs.returnValue;
	}

	@SuppressWarnings("unchecked")
	protected IValue interpretIntrinsic(final ExecutionState<FH, Context> es, final InvokeExpr op) {
		final NumberedString subSignature = op.getMethodRef().getSubSignature();
		if(subSignature == nondetSig) {
			return es.heap.stateReader.readNonDeterministic(globalState);
		} else if(subSignature == readSig) {
			return es.heap.stateReader.readDeterministic(globalState);
		} else if(subSignature == printSig) {
			final IValue printed = interpretValue(es, op.getArg(0));
			if(IntrinsicHandler.ENABLE_PRINTLN) {
				System.out.println(printed);
			}
			return null;
		} else if(subSignature == writeSig) {
			interpretValue(es, op.getArg(0));
			return null;
		} else if(subSignature == liftIntSig || subSignature == liftObjectSig || subSignature == liftBoolSig) {
			final IValue p = interpretValue(es, op.getArg(0));
			assert p.getTag() == RuntimeTag.ARRAY;
			return es.heap.getCollapsedArray(p.getLocation());
		} else if(subSignature == equalIntSig || subSignature == equalBoolSig || subSignature == equalObjectSig) {
			final boolean expected = true;
			doEqualityCheck(es, op, expected);
			return null;
		} else if(subSignature == notEqualIntSig || subSignature == notEqualBoolSig || subSignature == notEqualObjectSig) {
			doEqualityCheck(es, op, false);
			return null;
		} else if(subSignature == dumpIRSig) {
			System.out.println("\n>> DEBUG METHOD <<");
			System.out.println("+ Method: " + es.currMethod);
			System.out.println(BodyManager.retrieveBody(es.currMethod));
			System.out.println(">> DONE <<\n");
			return null;
		} else if(subSignature == dumpStateSig) {
			System.out.println("\n>> DEBUG <<");
			es.dump();
			System.out.println(">> DONE <<\n");
			return null;
		} else if(subSignature == dumpStateMsgSig) {
			System.out.println("\n>> DEBUG : " + op.getArg(0) + " <<");
			es.dump();
			System.out.println(">> DONE <<\n");
			return null;
		} else if(subSignature == dumpObjectStateSig) {
			final IValue p = interpretValue(es, op.getArg(0));
			System.out.println("\n>> OBJECT DEBUG <<");
			System.out.println("= TYPE: " + p.getTag());
			if(p.isHeapValue() || p.isMultiHeap()) {
				p.forEach(new IValueAction() {
					@Override
					public void nondet() { }
					
					@Override
					public void accept(final IValue v, final boolean isMulti) {
						dumpObjectValue(es, v);
					}
				});
			} else if(p.isEmbedded()) {
				System.out.println("FV: " + p.aVal.monad.toString(p.aVal.value));
			}
			System.out.println(">> DONE <<\n");
			return null;
		} else if(subSignature == this.debugSig) {
			assert op.getArg(0) instanceof StringConstant;
			System.out.println(((StringConstant)op.getArg(0)).value);
			return null;
		} else if(subSignature == this.abortSig) {
			System.out.println(">> ABORTED <<");
			System.exit(10);
			return null;
		} else if(subSignature == this.printStackSig) {
			dumpCallStack(es);
			return null;
		} else if(subSignature == this.liftLocationSig) {
			assert op.getArg(0) instanceof StringConstant;
			assert isSimpleFH(es);
			return SimpleForeignHeap.foreignLocation(((StringConstant)op.getArg(0)).value);
		} else if(subSignature == this.getLocationSig) {
			final IValue key = this.interpretValue(es, op.getArg(0));
			return SimpleForeignHeap.get((EmbeddedState<SimpleForeignHeap>) es.foreignHeap, key);
		} else if(subSignature == this.putLocationSig) {
			final IValue key = this.interpretValue(es, op.getArg(0));
			final IValue value = this.interpretValue(es, op.getArg(1));
			es.replaceHeap((EmbeddedState<FH>) SimpleForeignHeap.put((EmbeddedState<SimpleForeignHeap>) es.foreignHeap, key, value));
			return null;
		} else if(subSignature == this.printPolySig) {
			final IValue toPrint = this.interpretValue(es, op.getArg(0));
			if(IntrinsicHandler.ENABLE_PRINTLN) {
				System.out.println(toPrint);
			}
			return null;
		} else if(subSignature == this.assertObjToStringEquals || subSignature == this.assertIntToStringEquals) {
			final IValue v = this.interpretValue(es, op.getArg(0));
			final String repr = v.toString();
			final String expected = ((StringConstant)op.getArg(1)).value;
			if(repr.equals(expected)) {
				return null;
			}
			throw new FailedObjectLanguageAssertionException(op + ": [" + v + "] [" + expected + "]");
		} else if(subSignature == failSig) {
			if(es.localFlags.contains(LocalExecutionFlag.RECORD_CYCLE)) {
				throw new PruneExecutionException();
			}
			throw new FailedExecutionException(((StringConstant)op.getArg(0)).value);
		} else if(subSignature == customAssertSig) {
			final Value checkerClassArg = op.getArg(2);
			if(!(checkerClassArg instanceof ClassConstant)) {
				throw new IllegalArgumentException();
			}
			final ClassConstant checkerClass = (ClassConstant) checkerClassArg;
			final String checkerName = checkerClass.getValue().replace('/', '.').substring(1, checkerClass.getValue().length() - 1);
			final AssertionChecker newInstance;
			try {
				newInstance = (AssertionChecker) Class.forName(checkerName).newInstance();
			} catch (final InstantiationException | IllegalAccessException | ClassNotFoundException e) {
				throw new RuntimeException("Failed to instantiate checker " + checkerName, e);
			}
			final IValue arg = this.interpretValue(es, op.getArg(0));
			final String key = ((StringConstant)op.getArg(1)).value;
			newInstance.assertValue(key, arg, es, (res, msg) -> {
				if(!res) {
					throw new FailedObjectLanguageAssertionException("Checker " + checkerName + " failed for " + arg + " and key " + key + " with message: " + msg);
				}
			});
			return null;
		} else if(subSignature == this.checkAssertSig) {
			return null;
		} else if(subSignature == this.assertFalseSig) {
			throw new FailedObjectLanguageAssertionException("Unrechable statement hit: " + ((StringConstant)op.getArg(0)).value);
		} else {
			throw new UnrecognizedIntrinsicException(op.toString());
		}
	}

	private void doEqualityCheck(final ExecutionState<FH, Context> es, final InvokeExpr op, final boolean expected) {
		IValue v1 = interpretValue(es, op.getArg(0));
		IValue v2 = interpretValue(es, op.getArg(1));
		if(v1.isEmbedded() && !v2.isEmbedded()) {
			v2 = new IValue(new EmbeddedValue(v1.aVal.monad.alpha(v2), v1.aVal.monad));
		} else if(v2.isEmbedded() && !v1.isEmbedded()) {
			v1 = new IValue(new EmbeddedValue(v2.aVal.monad.alpha(v1), v2.aVal.monad));
		}
		final boolean isEqual = v1.equals(v2);
		if(isEqual != expected) {
			final Unit hosting = BodyManager.getHostUnit(op);
			final String lineNumber = getHostLineNumber(hosting);
			throw new FailedObjectLanguageAssertionException(op + ": " + v1 + " " + v2 + " on line " + lineNumber);
		}
	}
	
	private String getHostLineNumber(final Unit hosting) {
		final String lineNumber;
		if(hosting.hasTag("LineNumberTag")) {
			lineNumber = hosting.getTag("LineNumberTag").toString();
		} else {
			lineNumber = "?";
		}
		return lineNumber;
	}

	private boolean isSimpleFH(final ExecutionState<FH, Context> es) {
		return es.foreignHeap == null || es.foreignHeap.state instanceof SimpleForeignHeap;
	}

	private void dumpObjectValue(final ExecutionState<?, ?> es, final IValue p) {
		if(p == IValue.nullConst()) {
			System.out.println("+ NULL");
			return;
		}
		System.out.println("+ Location: " + p.getLocation());
		es.heap.dumpObject(p.getLocation());
	}

	protected IValue interpretInvoke(final ExecutionState<FH, Context> es, final InvokeExpr op) {
		if(op instanceof SpecialInvokeExpr && op.getMethodRef().getSignature().equals("<java.lang.Object: void <init>()>")) {
			return null;
		}
		if(op instanceof StaticInvokeExpr) {
			return interpretIntrinsic(es, op);
		} else {
			final InstanceInvokeExpr iie = (InstanceInvokeExpr) op;
			return interpretCall(es, iie);
		}
	}
	
	// here is where we merge heaps
	protected IValue interpretCall(final ExecutionState<FH, Context> es, final InstanceInvokeExpr iie) {
		final IValue baseValue = interpretValue(es, iie.getBase());
		if(baseValue.isEmbedded()) {
			return this.handleEmbeddedCall(es, baseValue, iie);
		}
		return interpretCall(es, baseValue, iie);
	}

	protected IValue interpretCall(final ExecutionState<FH, Context> es, final IValue baseValue, final InvokeExpr ie) {
		return interpretCall(es, baseValue, ie, ie.getMethodRef());
	}

	protected IValue handleEmbeddedCall(final ExecutionState<FH, Context> es, final IValue baseValue, final InstanceInvokeExpr iie) {
		throw new UnsupportedOperationException();
	}

	protected IValue interpretCall(final ExecutionState<FH, Context> es, final IValue basePreHook, final InvokeExpr op, final SootMethodRef ref) {
		final List<IValue> argsPreHook = new ArrayList<>();
		for(final Value aVal : op.getArgs()) {
			argsPreHook.add(interpretValue(es, aVal));
		}
		return interpretCall(es, basePreHook, op, ref, argsPreHook);
	}

	protected IValue interpretCall(final ExecutionState<FH, Context> es, final IValue basePreHook, final InvokeExpr op, final SootMethodRef ref, final List<IValue> argsPreHook) {
		if(basePreHook.getTag() == RuntimeTag.NULL) {
			throw new NullPointerException();
		}
		final Stream<Pair<IValue, SootMethod>> dispatchees = resolveDispatchees(basePreHook, ref);
		if(dispatchees.isEmpty()) {
			throw new PruneExecutionException();
		}
		final Pair<IValue, StateAccumulator<FH>> ret = handleDispatch(es, dispatchees, argsPreHook, op);
		ret.getO2().mergeToState(es);
		return ret.getO1();
	}

	protected Stream<Pair<IValue, SootMethod>> resolveDispatchees(final IValue base, final SootMethodRef ref) {
		return Stream.iterableStream(base).bind((final IValue recv) -> {
			if(recv.getTag() == RuntimeTag.NULL) {
				return Stream.nil();
			}
			return Stream.iterableStream(this.resolveMethod(recv, ref)).map(m -> new Pair<>(recv, m));
		});
	}

	protected final Pair<IValue, StateAccumulator<FH>> handleDispatch(final @MutatesFork ExecutionState<FH, Context> es, final Stream<Pair<IValue, SootMethod>> callees,
			final List<IValue> argsPreHook, final InvokeExpr op) {
		return handleDispatch(es, callees, argsPreHook, op, false);
	}

	protected final Pair<IValue, StateAccumulator<FH>> handleDispatch(final @MutatesFork ExecutionState<FH, Context> es, final Stream<Pair<IValue, SootMethod>> callees,
			final List<IValue> argsPreHook, final InvokeExpr op, final boolean nullReturnIsNull) {
		final StateAccumulator<FH> accum = new StateAccumulator<>();
		return new Pair<>(handleDispatch(es, callees, argsPreHook, op, accum, nullReturnIsNull), accum);
	}

	protected IValue handleDispatch(final @MutatesFork ExecutionState<FH, Context> es, final Stream<Pair<IValue, SootMethod>> callees, final List<IValue> argsPreHook,
			final InvokeExpr op, final StateAccumulator<FH> accum) {
		return handleDispatch(es, callees, argsPreHook, op, accum, false);
	}

	/*
	 * This method will downcast args to match the declared type. IT IS THE CALLERS RESPONSIBILITY TO ENSURE THIS CAST SUCCEEDS.
	 *
	 * This also does call-graph tracking
	 */
	protected IValue handleDispatch(final @MutatesFork ExecutionState<FH, Context> es, final Stream<Pair<IValue, SootMethod>> callees, final List<IValue> argsPreHook,
			final InvokeExpr op, final StateAccumulator<FH> accum, final boolean nullReturnIsNull) {
		if(callees.isEmpty()) {
			throw new PruneExecutionException();
		}
		final F3<Set<IValue>, IValue, ExecutionState<FH,Context>, Set<IValue>> updateAccum = (acc, ret, postState) -> {
			accum.update(postState.heap, postState.foreignHeap);
			final IValue retVal = ret == null && nullReturnIsNull ? IValue.nullConst() : ret;
			if(retVal == null) {
				return acc;
			} else if(acc == null) {
				final HashSet<IValue> acc_ = new HashSet<>();
				acc_.add(retVal);
				return acc_;
			} else {
				acc.add(retVal);
				return acc;
			}
		};
		final Set<IValue> folded = callees.foldLeft((final Set<IValue> retAccum, final Pair<IValue, SootMethod> dispPair) -> {
			final ExecutionState<FH, Context> forked = es.fork();
			final IValue basePreHook = dispPair.getO1();
			Option<IValue> overrideValue = Option.none();
			final SootMethod method = dispPair.getO2();
			if(TRACK_CALL_EDGES) {
				CALL_GRAPH.put(createCallSiteNode(op, es), method);
			}
			if(this.invokeExtension != null) {
				overrideValue = this.invokeExtension.interpretCall(forked, basePreHook, op, method);
			}
			if(!overrideValue.isNone()) {
				return updateAccum.f(retAccum, overrideValue.some(), forked);
			}
			
			final P3<IValue, List<IValue>, Option<IValue>> postHook = this.preCallHook(forked, basePreHook, argsPreHook, op, method);
			if(postHook._3().isSome()) {
				return updateAccum.f(retAccum, postHook._3().some(), forked);
			}
			final IValue base = postHook._1();
			
			assert method.getParameterCount() == postHook._2().size();
			final List<IValue> args = Stream.iterableStream(postHook._2()).zipWith(Stream.iterableStream(method.getParameterTypes()), (i, t) -> {
					if(t instanceof RefLikeType) {
						return interpretCast(i, t);
					} else {
						return i;
					}
				}).toJavaList();
			try {
				if(method.isPhantom()) {
					throw new RuntimeException("Stub!!!");
				}
				if(inRecursiveCycle(forked, method)) {
					// this actually throws to the caller
					return updateAccum.f(retAccum, findCycleFixpoint(forked, method, args, base), forked);
				}
				final IValue retValPreHook = interpret(forked, method, base, args, Option.some(op));
				final IValue retVal = this.postCallHook(forked, base, args, retValPreHook, op);
				return updateAccum.f(retAccum, retVal, forked);
			} catch(final ThrowToState ts) {
				if(ts.state == forked) {
					assert ts.resultState.rs.h.parentHeap == forked.heap;
					forked.mergeHeaps(ts.resultState.rs);
					return updateAccum.f(retAccum, ts.resultState.rs.returnValue, forked);
				} else {
					throw ts;
				}
			}
		}, null);
		if(folded == null) {
			return null;
		} else {
			return IValue.lift(folded);
		}
	}

	protected Object createCallSiteNode(final InvokeExpr op, final ExecutionState<FH, Context> es) {
		return P.p(this.createAllocationContext(es), op);
	}

	protected final boolean inRecursiveCycle(final ExecutionState<FH, Context> es, final SootMethod method) {
		return es.cycleSet.contains(method) || (es.hasFlag(LocalExecutionFlag.RECORD_CYCLE) && method == es.currMethod);
	}
	
	public final List<SootMethod> resolveMethod(final IValue base, final InvokeExpr op) {
		return resolveMethod(base, op, Option.none());
	}

	public final List<SootMethod> resolveMethod(final IValue base, final InvokeExpr op, final Option<Value> typeHint) {
		return resolveMethod(base, op.getMethodRef());
	}

	protected final List<SootMethod> resolveMethod(final IValue base, final SootMethodRef methodRef) {
		if(base.getTag() == RuntimeTag.BOUNDED_OBJECT) {
			return this.resolveMethod(base.boundedType, methodRef);
		} else {
			return this.resolveMethod(base, methodRef.getSubSignature()).map(Collections::singletonList).orSome(Collections.emptyList());
		}
	}

	private Option<SootMethod> resolveMethod(final IValue base, final NumberedString subSignature) {
		final SootClass sootClass = resolveRuntimeType(base);
		assert sootClass != null : base;
		if(!sootClass.declaresMethod(subSignature)) {
			return Option.none();
		}
		final SootMethod method = sootClass.getMethod(subSignature);
		return Option.some(method);
	}
	
	public List<SootMethod> resolveMethod(final AnySubType boundedType, final SootMethodRef methodRef) {
		final List<SootMethod> toReturn = new ArrayList<>();
		for(final SootClass cls : BodyManager.enumerateFrameworkClasses(boundedType.getBase(), methodRef.declaringClass().getType())) {
			if(cls.declaresMethod(methodRef.getSubSignature()) && cls.getMethod(methodRef.getSubSignature()).isConcrete()) {
				toReturn.add(cls.getMethod(methodRef.getSubSignature()));
			}
		}
		return toReturn;
	}

	private SootClass resolveRuntimeType(final IValue base) {
		return base.getSootClass();
	}

	private RuntimeException mkValueException(
		final SootMethod method,
		final IValue receiver,
		final List<IValue> vals,
		final Value value,
		final String string) {
		return new RuntimeException(	
			"Bad value: " + value + ", " + string + " in method " + method + ", with receiver: " + receiver + " and arguments: " + vals
		);
	}
	
	public IValue interpretValue(final ExecutionState<FH, Context> es, final Value op) {
		if(op instanceof Local) {
			return es.ms.get(((Local) op).getName());
		} else if(op instanceof Constant) {
			return IValue.lift((Constant) op);
		} else if(op instanceof ArrayRef) {
			final ArrayRef aOp = (ArrayRef) op;
			final IValue base = this.arrayBasePointerHook(es, interpretValue(es, aOp.getBase()), true, aOp);
			final IValue index = interpretValue(es, aOp.getIndex());
			return interpretArrayRead(es, aOp, base, index);
		} else if(op instanceof InstanceFieldRef) {
			final InstanceFieldRef iRef = (InstanceFieldRef) op;
			final IValue base = this.fieldBasePointerHook(es, interpretValue(es, iRef.getBase()), true, iRef);
			final HeapReadResult<IValue> readField = es.heap.getField(base, iRef.getFieldRef());
			checkHeapFaults(readField);
			return this.fieldOutputHook(es, base, readField.value, true, iRef);
		} else if(op instanceof LengthExpr) {
			final IValue base = interpretValue(es, ((LengthExpr) op).getOp());
			return interpretArrayLength(es, base);
		} else if((op instanceof AbstractIntBinopExpr) || (op instanceof AbstractFloatBinopExpr)) {
			final ExpressionInterpreter sw = getExpressionInterpreter(es);
			final AbstractBinopExpr binop = (AbstractBinopExpr)op;
			final IValue op1 = this.interpretValue(es, binop.getOp1());
			final IValue op2 = this.interpretValue(es, binop.getOp2());
			final IValue result = sw.interpretWith(binop, op1, op2);
			if(result == null) {
				throw new RuntimeException("Expression " + op + " did not produce a result");
			}
			return result;
		} else if(op instanceof InstanceOfExpr) {
			final Value toCheck = ((InstanceOfExpr) op).getOp();
			final IValue v = this.interpretValue(es, toCheck);
			final ObjectIdentityResult result = this.interpretInstanceCheck(v, ((InstanceOfExpr) op).getCheckType());
			if(result == ObjectIdentityResult.MAY_BE) {
				return IValue.nondet();
			} else if(result == ObjectIdentityResult.MUST_NOT_BE) {
				return IValue.lift(false);
			} else {
				return IValue.lift(true);
			}
		} else if(op instanceof GNewInvokeExpr) {
			final Type t = op.getType();
			assert t instanceof RefType;
			final SootClass sootClass = ((RefType)t).getSootClass();
			final IValue baseObject = allocateObject(es, op, sootClass);
			interpretCall(es, baseObject, (InvokeExpr) op);
			return baseObject;
		} else if(op instanceof InvokeExpr) {
			return interpretInvoke(es, (InvokeExpr) op);
		} else if(op instanceof NewArrayExpr) {
			final IValue sz = interpretValue(es, ((NewArrayExpr) op).getSize());
			final Type type = op.getType();
			return allocateArray(es, op, sz, type);
		} else if(op instanceof NewMultiArrayExpr) {
			final List<Value> sizes = ((NewMultiArrayExpr) op).getSizes();
			final List<IValue> szVals = new ArrayList<>();
			
			for(final Value sz : sizes) {
				szVals.add(interpretValue(es, sz));
			}
			final Type type = ((NewMultiArrayExpr) op).getType();
			return allocateArray(es, op, szVals, type);
		} else if(op instanceof NewExpr) {
			return this.allocateObject(es, op, ((RefType)op.getType()).getSootClass());
		} else if(op instanceof CastExpr) {
			final IValue toCast = interpretValue(es, ((CastExpr) op).getOp());
			return interpretCast(toCast, ((CastExpr) op).getCastType());
		} else {
			throw new RuntimeException("Unrecognized value form " + op);
		}
	}
	
	// start eval hooks

	protected IValue interpretArrayRead(final ExecutionState<FH, Context> es, final ArrayRef aOp, final IValue base, final IValue index) {
		final HeapReadResult<IValue> res = es.heap.getArray(base, index);
		checkHeapFaults(res);
		return this.arrayOutputHook(es, base, res.value, true, aOp);
	}

	protected IValue interpretArrayLength(final ExecutionState<FH, Context> es, final IValue base) {
		return es.heap.getLength(base);
	}

	protected IValue interpretCast(final IValue toCast, final Type castType) {
		final IValue casted = toCast.downCast(castType);
		if(casted == null) {
			throw new ClassCastException();
		}
		return casted;
	}
	
	// end eval hooks

	protected ExpressionInterpreter getExpressionInterpreter(final ExecutionState<FH, Context> es) {
		return new ExpressionInterpreter(es);
	}

	protected IValue allocateObject(final ExecutionState<FH, Context> es, final Value op, final SootClass sootClass) {
		final IValue alloced = es.heap.allocate(sootClass, op, createAllocationContext(es));
		es.heap.assertSaneStructure();
		return alloced;
	}

	protected IValue allocateArray(final ExecutionState<FH, Context> es, final Value op, final List<IValue> szVals, final Type type) {
		return es.heap.allocateArray(type, szVals, op, createAllocationContext(es));
	}

	protected IValue allocateArray(final ExecutionState<FH, Context> es, final Value op, final IValue sz, final Type type) {
		return es.heap.allocateArray(type, sz, op, createAllocationContext(es));
	}
	
	protected Object createAllocationContext(final ExecutionState<FH, Context> es) {
		if(this.invokeExtension != null) {
			return this.invokeExtension.createAllocationContext(es);
		}
		ExecutionState<?, ?> it = es; 
		Seq<P2<SootMethod, Option<InvokeExpr>>> cfa = Seq.empty();
		while(it != null) {
			assert it.currMethod != null : it.toString();
			cfa = cfa.cons(P.p(it.currMethod, it.callExpr));
			it = it.callerState;
		}
		if(es.rootContext != null) {
			return P.p(es.rootContext.rootContext, cfa);
		} else {
			return cfa;
		}
	}

	public static MethodState mergeToCommonState(final MethodState ms1, final MethodState ms2) {
		final Set<MethodState> presentStates = Collections.newSetFromMap(new IdentityHashMap<>());
		MethodState it = ms1;
		while(it != null) {
			presentStates.add(it);
			it = it.parentState;
		}
		it = ms2;
		while(it != null && !presentStates.contains(it)) {
			it = it.parentState;
		}
		if(it == null) {
			throw new RuntimeException();
		}
		final MethodState h1_ = ms1.popTo(it);
		final MethodState h2_ = ms2.popTo(it);
		return MethodState.join(h1_, h2_);
	}

	@SuppressWarnings("unchecked")
	public static <FH> InterpreterState<FH> nullResult() {
		return (InterpreterState<FH>) nullResult;
	}

	public static void resetGlobalState() {
		G.reset();
		Heap.resetCounters();
		BodyManager.reset();
		BasicInterpreter.reset();
	}
	
	// HOOKS
	
	protected IValue arrayOutputHook(final ExecutionState<FH, Context> state, final IValue basePointer, final IValue outputValue, final boolean isRead, final ArrayRef arrayOp) {
		return outputValue;
	}

	protected IValue arrayBasePointerHook(final ExecutionState<FH, Context> state, final IValue basePointer, final boolean isRead, final ArrayRef arrayOp) {
		return basePointer;
	}
	
	protected IValue fieldBasePointerHook(final ExecutionState<FH, Context> state, final IValue basePointer, final boolean isRead, final InstanceFieldRef iRef) {
		return basePointer;
	}
	
	protected IValue fieldOutputHook(final ExecutionState<FH, Context> state, final IValue basePointer, final IValue outputValue, final boolean isRead, final InstanceFieldRef iOp) {
		return outputValue;
	}

	protected IValue postCallHook(final ExecutionState<FH, Context> es, final IValue base, final List<IValue> args, final IValue retVal, final InvokeExpr op) {
		return retVal;
	}

	protected P3<IValue, List<IValue>, Option<IValue>> preCallHook(final ExecutionState<FH, Context> es, final IValue base, final List<IValue> args, final InvokeExpr op,
			final SootMethod method) {
		return P.p(base, args, Option.<IValue>none());
	}

	protected ObjectIdentityResult interpretInstanceCheck(final IValue arg, final Type parameterType) {
		return arg.valueStream().map(v -> v.isInstanceOf(parameterType)).foldLeft1(ObjectIdentityResult::join);
	}
}
