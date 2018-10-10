package edu.washington.cse.concerto.interpreter.ai.instantiation.array;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import edu.washington.cse.concerto.instrumentation.InstrumentationManager;
import edu.washington.cse.concerto.instrumentation.actions.FieldReadAction;
import edu.washington.cse.concerto.instrumentation.actions.FieldWriteAction;
import edu.washington.cse.concerto.instrumentation.actions.ValueReplacement;
import edu.washington.cse.concerto.interpreter.BodyManager;
import edu.washington.cse.concerto.interpreter.ReflectionEnvironment;
import edu.washington.cse.concerto.interpreter.ai.AbstractionFunction;
import edu.washington.cse.concerto.interpreter.ai.CompareResult;
import edu.washington.cse.concerto.interpreter.ai.ConcreteValueMapper;
import edu.washington.cse.concerto.interpreter.ai.ContextManager;
import edu.washington.cse.concerto.interpreter.ai.ContinuationManager;
import edu.washington.cse.concerto.interpreter.ai.EntryPointContextManager;
import edu.washington.cse.concerto.interpreter.ai.EvalResult;
import edu.washington.cse.concerto.interpreter.ai.HeapMutator;
import edu.washington.cse.concerto.interpreter.ai.HeapReader;
import edu.washington.cse.concerto.interpreter.ai.HeapUpdateResult;
import edu.washington.cse.concerto.interpreter.ai.IntrinsicHandler;
import edu.washington.cse.concerto.interpreter.ai.Lattices;
import edu.washington.cse.concerto.interpreter.ai.MethodResult;
import edu.washington.cse.concerto.interpreter.ai.MonadicLattice;
import edu.washington.cse.concerto.interpreter.ai.ObjectOperations;
import edu.washington.cse.concerto.interpreter.ai.PathSensitiveBranchInterpreter;
import edu.washington.cse.concerto.interpreter.ai.PureFunction;
import edu.washington.cse.concerto.interpreter.ai.RecursiveTransformer;
import edu.washington.cse.concerto.interpreter.ai.ReflectiveOperationContext;
import edu.washington.cse.concerto.interpreter.ai.RelationalAbstractInterpretation;
import edu.washington.cse.concerto.interpreter.ai.RelationalCallHandler;
import edu.washington.cse.concerto.interpreter.ai.ResultCollectingAbstractInterpretation;
import edu.washington.cse.concerto.interpreter.ai.ResultStream;
import edu.washington.cse.concerto.interpreter.ai.StandardOutResultStream;
import edu.washington.cse.concerto.interpreter.ai.State;
import edu.washington.cse.concerto.interpreter.ai.StateUpdater;
import edu.washington.cse.concerto.interpreter.ai.StateValueUpdater;
import edu.washington.cse.concerto.interpreter.ai.StaticCallGraph;
import edu.washington.cse.concerto.interpreter.ai.UpdateResult;
import edu.washington.cse.concerto.interpreter.ai.ValueMapper;
import edu.washington.cse.concerto.interpreter.ai.ValueStateTransformer;
import edu.washington.cse.concerto.interpreter.ai.binop.ArithmeticEvaluator;
import edu.washington.cse.concerto.interpreter.ai.binop.DefaultPrimitiveOps;
import edu.washington.cse.concerto.interpreter.ai.binop.ObjectIdentityResult;
import edu.washington.cse.concerto.interpreter.ai.binop.RelationalPrimitiveOperations;
import edu.washington.cse.concerto.interpreter.ai.injection.Injectable;
import edu.washington.cse.concerto.interpreter.ai.instantiation.CFGUtil;
import edu.washington.cse.concerto.interpreter.ai.instantiation.InterpResult;
import edu.washington.cse.concerto.interpreter.exception.FailedObjectLanguageAssertionException;
import edu.washington.cse.concerto.interpreter.exception.NullPointerException;
import edu.washington.cse.concerto.interpreter.heap.Heap;
import edu.washington.cse.concerto.interpreter.heap.HeapAccessResult;
import edu.washington.cse.concerto.interpreter.heap.HeapFaultStatus;
import edu.washington.cse.concerto.interpreter.heap.HeapReadResult;
import edu.washington.cse.concerto.interpreter.meta.BoundaryInformation;
import edu.washington.cse.concerto.interpreter.meta.InstrumentedState;
import edu.washington.cse.concerto.interpreter.meta.Monads;
import edu.washington.cse.concerto.interpreter.state.ExecutionState;
import edu.washington.cse.concerto.interpreter.value.IValue;
import edu.washington.cse.concerto.interpreter.value.IValue.RuntimeTag;
import fj.F;
import fj.F2;
import fj.Ord;
import fj.P;
import fj.P2;
import fj.data.Either;
import fj.data.Option;
import fj.data.Seq;
import fj.data.Set;
import fj.data.Stream;
import soot.AnySubType;
import soot.ArrayType;
import soot.Body;
import soot.Local;
import soot.PatchingChain;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.grimp.internal.GNewInvokeExpr;
import soot.jimple.AddExpr;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.ConditionExpr;
import soot.jimple.Expr;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.LengthExpr;
import soot.jimple.MulExpr;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.NullConstant;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.StringConstant;
import soot.jimple.SubExpr;
import soot.jimple.internal.AbstractBinopExpr;
import soot.jimple.internal.AbstractFloatBinopExpr;
import soot.jimple.internal.AbstractIntBinopExpr;
import soot.toolkits.scalar.Pair;
import soot.util.NumberedString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ArrayBoundsChecker implements RelationalAbstractInterpretation<PValue, ARHeap, ARState, CallSite, ParamRelation>, ResultCollectingAbstractInterpretation {
	public static boolean DEBUG_LEQ = false;
	private RelationalCallHandler<CallSite, ParamRelation> callHandler;
	private final Map<P2<CallSite, Unit>, InstrumentedState> memo = new HashMap<>();
	private Monads<PValue, ARState> monads;
	private final LinkedList<P2<CallSite, Unit>> worklist = new LinkedList<>();
	private final Map<CallSite, MethodResult> returnValues = new HashMap<>();
	private final Multimap<CallSite, P2<CallSite, Unit>> returnSites = HashMultimap.create();
	private ContinuationManager<P2<CallSite, Unit>, CallSite, ARState> continuationManager;
	private final StaticCallGraph<CallSite> callGraph = new StaticCallGraph<>();
	private PathSensitiveBranchInterpreter<PValue, ARState> branchInterpreter;

	private final java.util.Set<Pair<SootMethod, CallSite>> checkAssertContexts = new HashSet<>();
	private final java.util.Set<P2<CallSite, Unit>> proofObligations = new HashSet<>();
	private final java.util.Set<P2<CallSite, ArrayRef>> arrayReads = new HashSet<>();
	private final java.util.Set<P2<CallSite, Value>> nullPointerDereferences = new HashSet<>();
	private ResultStream resultStream = new StandardOutResultStream();


	private boolean timeout = false;

	@Override public void interrupt() {
		this.timeout = true;
	}

	@Override public void inject(final Monads<PValue, ARState> monads) {
		this.monads = monads;
		this.continuationManager = new ContinuationManager<>(ARState.CONTINUATION_ORD, monads.stateMonad);
	}

	@Override
	public State<ARHeap, ARState> stateTransformer() {
		return new State<ARHeap, ARState>() {
			@Override
			public ARHeap project(final ARState state) {
				return state.heap;
			}

			@Override
			public ARState inject(final ARState state, final ARHeap heap) {
				return state.withHeap(heap);
			}
			
			@Override
			public ARState emptyState() {
				return ARState.empty();
			}
		};
	}

	@Override
	public AbstractionFunction<PValue> alpha() {
		return new AbstractionFunction<PValue>() {
			
			@Override
			public PValue lift(final IValue v) {
				if(v.isMulti()) {
					int min = Integer.MAX_VALUE;
					int max = Integer.MIN_VALUE;
					final Iterator<IValue> variants = v.variants();
					while(variants.hasNext()) {
						final IValue val = variants.next();
						if(val.getTag() != RuntimeTag.INT) {
							return PValue.bottom();
						}
						min = Math.min(min, val.asInt());
						max = Math.max(max, val.asInt());
					}
					return PValue.interval(min, max);
				} else if(v.getTag() == RuntimeTag.NULL) {
					return PValue.nullPtr();
				} else if(v.getTag() == RuntimeTag.NONDET) {
					return PValue.fullInterval();
				} else if(v.getTag() == RuntimeTag.INT) {
					return PValue.lift(v.asInt());
				} else {
					return PValue.bottom();
				}
			}
		};
	}

	@Override
	public Lattices<PValue, ARHeap, ARState> lattices() {
		return new Lattices<PValue, ARHeap, ARState>() {
			@Override
			public MonadicLattice<PValue, PValue, ARState> valueLattice() {
				return PValue.lattice;
			}

			@Override
			public MonadicLattice<ARHeap, PValue, ARState> heapLattice() {
				return ARHeap.lattice;  
			}

			@Override
			public MonadicLattice<ARState, PValue, ARState> stateLattice() {
				return ARState.lattice;
			}
		};
	}


	@Override public Option<MethodResult> handleCall(final SootMethod callee, final PValue receiver, final List<Object> arguments, final InstrumentedState callState, final CallSite calleeContext,
			final BoundaryInformation<CallSite> callContext, final ParamRelation rel) {
		final InstrumentedState startState = bindArgsForCallee(receiver, arguments, callState, callee, rel, Option.some(callContext));
		if(startState == null) {
			return null;
		}
		return this.retrieveSummary(callee, startState, calleeContext, callContext);
	}

	public InstrumentedState bindArgsForCallee(final PValue receiver, final List<Object> arguments,
			final InstrumentedState callState, final SootMethod callee, final ParamRelation rel, final Option<BoundaryInformation<CallSite>> callInfo) {
		final Body body = BodyManager.retrieveBody(callee);
		final Map<Local, Object> startLocals = new HashMap<>();
		final PValue filteredThis = receiver.filterType(callee.getDeclaringClass().getType());
		if(filteredThis.isBottom()) {
			return null;
		}
		startLocals.put(body.getThisLocal(), filteredThis);
		for(int i = 0; i < arguments.size(); i++) {
			startLocals.put(body.getParameterLocal(i), arguments.get(i));
		}
		return monads.stateMonad.mapState(callState, in ->
			in.withLocals(startLocals).applyRelation(body.getParameterLocals(), rel).withContinuation(continuationManager.initContinuation(callInfo.map(this::getContinuation)))
		);
	}

	protected P2<CallSite, Unit> getContinuation(final BoundaryInformation<CallSite> callInfo) {
		return P.p(callInfo.rootContext, BodyManager.getHostUnit(callInfo.rootInvoke));
	}

	private Option<MethodResult> retrieveSummary(final SootMethod m, final InstrumentedState state, final CallSite calleeContext, final BoundaryInformation<CallSite> callContext) {
		final P2<CallSite, Unit> continuation = getContinuation(callContext);
		this.registerCaller(calleeContext, continuation);
		if(!this.enqueueMethod(m, calleeContext, state) && hasResultForCall(calleeContext, callContext)) {
			return Option.some(this.returnValues.get(calleeContext));
		}
		return Option.none();
	}

	private boolean hasResultForCall(final CallSite calleeContext, final BoundaryInformation<CallSite> callContext) {
		return this.returnValues.get(calleeContext) != null && this.continuationManager.isCompatibleReturn(calleeContext, this.returnValues.get(calleeContext).getState());
	}

	private void registerCaller(final CallSite calleeContext, final P2<CallSite, Unit> callerContextAndReturn) {
		this.callGraph.registerCall(callerContextAndReturn._1(), calleeContext);
		this.returnSites.put(calleeContext, callerContextAndReturn);
	}
	
	@Override
	public List<SootMethod> getMethodForRef(final SootMethodRef mref, final PValue receiver) {
		final NumberedString calledSig = mref.getSubSignature();
		final List<SootMethod> toReturn = new ArrayList<>();
		for(final AbstractLocation al : receiver.addresses()) {
			final Type type = al.type;
			if(type instanceof AnySubType) {
				for(final SootClass cls : BodyManager.enumerateApplicationClasses(mref.declaringClass().getType(), ((AnySubType) type).getBase())) {
					if(!cls.declaresMethod(calledSig)) {
						continue;
					}
					toReturn.add(cls.getMethod(calledSig));
				}
				continue;
			}
			if(!(type instanceof RefType)) {
				continue;
			}
			final SootClass klass = ((RefType) type).getSootClass();
			if(!klass.declaresMethod(calledSig)) {
				continue;
			}
			toReturn.add(klass.getMethod(calledSig));
		}
		return toReturn;
	}

	@Override
	public List<Injectable> monadUsers() {
		final List<Injectable> toReturn = new ArrayList<>();
		toReturn.add(AbstractObject.injector);
		toReturn.add(ARHeap.injector);
		return toReturn;
	}

	@Override
	public ContextManager<CallSite, PValue, ARState, ARHeap> contextManager() {
		return new EntryPointContextManager<CallSite, PValue, ARState, ARHeap>() {
			
			@Override
			public void inject(final Monads<PValue, ARState> monads) { }

			@Override
			public CallSite contextForCall(final Either<Pair<CallSite, InstrumentedState>, ExecutionState<ARHeap, CallSite>> calleeContext,
					final SootMethod targetMethod, final Object base, final List<Object> values, final InvokeExpr callExpr) {
				return new CallSite(targetMethod, callExpr);
			}

			@Override
			public CallSite contextForAllocation(final Either<Pair<CallSite, InstrumentedState>, ExecutionState<ARHeap, CallSite>> allocContext, final Value allocExpr) {
				return allocContext.either(new F<Pair<CallSite,InstrumentedState>, CallSite>() {
					@Override
					public CallSite f(final Pair<CallSite, InstrumentedState> a) {
						return a.getO1();
					}
				}, new F<ExecutionState<ARHeap, CallSite>, CallSite>() {
					@Override
					public CallSite f(final ExecutionState<ARHeap, CallSite> a) {
						return new CallSite(a.currMethod, a.rootContext == null ? null : a.rootContext.rootInvoke);
					}
				});
			}

			@Override
			public CallSite initialContext(final SootMethod m) {
				return new CallSite(m, null);
			}

			@Override
			public CallSite initialAllocationContext(final SootMethod mainMethod) {
				return CallSite.initContext();
			}
		};
	}

	@Override
	public boolean modelsType(final Type type) {
		return true;
	}

	@Override
	public RelationalPrimitiveOperations<PValue, ARState> primitiveOperations() {
		return new IntervalPrimitiveOps();
	}

	@Override
	public MethodResult interpretToFixpoint(final SootMethod m, final PValue receiver, final List<Object> arguments, final InstrumentedState calleeState, final CallSite context) {
		this.enqueueMethod(m, context, bindArgsForCallee(receiver, arguments, calleeState, m, ParamRelation.empty, Option.none()));
		while(!worklist.isEmpty() && !this.timeout) {
			final P2<CallSite, Unit> currItem = worklist.removeFirst();
			final CallSite currContext = currItem._1();
			final PatchingChain<Unit> unitChain = BodyManager.retrieveBody(currContext.method).getUnits();
			final InstrumentedState currState = this.memo.get(currItem);
			final Unit currUnit = currItem._2();
			final InterpResult res;
			try {
				res = this.interpret(currState, currUnit, new CFGUtil(currUnit, unitChain), currContext);	
			} catch(final NoMethodSummary | PruneExecution e) {
				continue;
			}
			if(res.methodResult != null) {
				this.registerReturn(currContext, res.methodResult);
			} else {
				for(final Map.Entry<Unit, InstrumentedState> states : res.successorStates.entrySet()) {
					final Unit target = states.getKey();
					final InstrumentedState updated;
					if(unitChain.follows(currUnit, target)) {
						updated = widenWith(currContext, target, states.getValue());	
					} else {
						updated = joinWith(currContext, target, states.getValue());
					}
					this.enqueueState(currContext, target, updated);
				}
			}
		}
		if(this.timeout) {
			return null;
		}
		return this.returnValues.get(context);
	}

	private void registerReturn(final CallSite context, final MethodResult methodResult) {
		if(!this.returnValues.containsKey(context)) {
			this.returnValues.put(context, methodResult);
			this.enqueueCallers(context);
		} else if(!monads.methodResultMonad.lessEqual(methodResult, this.returnValues.get(context))) {
			final MethodResult newResult;
			if(this.callGraph.isWideningPoint(context)) {
				newResult = monads.methodResultMonad.widen(this.returnValues.get(context), monads.methodResultMonad.join(methodResult, this.returnValues.get(context)));
			} else {
				newResult = monads.methodResultMonad.join(methodResult, this.returnValues.get(context));
			}
			this.returnValues.put(context, newResult);
			this.enqueueCallers(context);
		}
	}

	private void enqueueCallers(final CallSite context) {
		this.worklist.addAll(this.returnSites.get(context));
	}

	private InstrumentedState widenWith(final CallSite context, final Unit target, final InstrumentedState value) {
		final P2<CallSite, Unit> key = P.p(context, target);
		if(!this.memo.containsKey(key)) {
			return value;
		} else {
			final InstrumentedState prev = this.memo.get(key);
			final InstrumentedState next = monads.stateMonad.join(this.memo.get(key), value);
			return monads.stateMonad.widen(prev, next);
		}
	}

	private InstrumentedState joinWith(final CallSite context, final Unit target, final InstrumentedState value) {
		final P2<CallSite, Unit> key = P.p(context, target);
		if(!this.memo.containsKey(key)) {
			return value;
		} else {
			return monads.stateMonad.join(this.memo.get(key), value);
		}
	}

	private boolean enqueueState(final CallSite context, final Unit target, final InstrumentedState updated) {
		final P2<CallSite, Unit> key = P.p(context, target);
		if(!this.memo.containsKey(key) || !monads.stateMonad.lessEqual(updated, this.memo.get(key))) {
			this.memo.put(key, updated);
			assert BodyManager.retrieveBody(context.method).getUnits().contains(target) : context + " " + target + " " + updated;
			this.worklist.add(key);
			return true;
		}
		return false;
	}

	@Override public void setResultStream(final ResultStream s) {
		this.resultStream = s;
	}

	private final class IntervalPrimitiveOps extends DefaultPrimitiveOps<PValue> implements RelationalPrimitiveOperations<PValue, ARState> {
		@Override
		public CompareResult cmp(final PValue a, final PValue b) {
			if(a.isInterval() && b.isInterval()) {
				return a.compare(b);
			} else if(!a.isInterval() && !b.isInterval()) {
				if(a.address.intersect(b.address).isEmpty()) {
					return CompareResult.NE;
				}  
				return CompareResult.nondet();
			} else {
				return CompareResult.nondet();
			}
		}

		@Override
		public PValue plus(final PValue a, final PValue b) {
			assert a.isInterval() && b.isInterval();
			return a.plus(b);
		}
		
		@Override
		public PValue minus(final PValue a, final PValue b) {
			assert a.isInterval() && b.isInterval();
			return a.minus(b);
		}
		
		@Override
		public PValue mult(final PValue a, final PValue b) {
			assert a.isInterval() && b.isInterval();
			if(a.singleton() && b.singleton()) {
				return PValue.lift(a.asInt() * b.asInt());
			} else if(a.isFinite() && b.isFinite()) {
				assert a.interval.min != null && a.interval.max != null && b.interval.min != null && b.interval.max != null;
				final Stream<Integer> cross = Stream.arrayStream(
						a.interval.min * b.interval.min,
						a.interval.max * b.interval.min,
						a.interval.min * b.interval.max,
						a.interval.max * b.interval.max);
				final int min = cross.foldLeft1(Math::min);
				final int max = cross.foldLeft1(Math::max);
				return PValue.interval(min, max);
			} else if(a.isPositive() && b.isPositive()) {
				return PValue.fullInterval().withMin(a.interval.min * b.interval.min);
			} else if(a.isStrictlyPositive() && b.isStrictlyNegative()) {
				return PValue.fullInterval().withMax(a.interval.min * b.interval.max);
			} else if(a.isStrictlyNegative() && b.isStrictlyPositive()) {
				return PValue.fullInterval().withMax(a.interval.max * b.interval.min);
			} else if((a.isPositive() && b.isNegative()) || (a.isNegative() && b.isPositive())) {
				return PValue.fullInterval().withMax(0);
			} else if(a.isNegative() && b.isNegative()) {
				return PValue.fullInterval().withMin(a.interval.max * b.interval.max);
			}
			return PValue.fullInterval(); 
		}

		@Override
		protected PValue binop(final PValue a, final PValue b) {
			return PValue.fullInterval();
		}

		@Override
		public PValue propagateLT(final PValue left, final PValue right) {
			if(!right.isInterval() || !left.isInterval()) {
				return left;
			}
			if(right.interval.max != null) {
				return left.withMax(right.interval.max - 1);
			} else {
				return left;
			}
		}

		@Override
		public PValue propagateLE(final PValue left, final PValue right) {
			if(!right.isInterval() || !left.isInterval()) {
				return left;
			}
			if(right.interval.max != null) {
				return left.withMax(right.interval.max);
			} else {
				return left;
			}
		}

		@Override
		public PValue propagateEQ(final PValue left, final PValue right) {
			if(left.isInterval() && right.isInterval()) {
				return left.withBounds(right);
			} else {
				return left.intersectAddress(right);
			}
		}

		@Override
		public PValue propagateNE(final PValue left, final PValue right) {
			if(left.isInterval()) {
				return left;
			} else if(right.address.size() == 1 && right.addresses().iterator().next().equals(AbstractLocation.NULL_LOCATION)) {
				return left.filterAddress(right);
			} else {
				return left;
			}
		}

		@Override
		public PValue propagateGT(final PValue left, final PValue right) {
			if(!right.isInterval() || !left.isInterval()) {
				return left;
			}
			if(right.interval.min != null) {
				return left.withMin(right.interval.min + 1);
			} else {
				return left;
			}
		}

		@Override
		public PValue propagateGE(final PValue left, final PValue right) {
			if(!right.isInterval() || !left.isInterval()) {
				return left;
			}
			if(right.interval.min != null) {
				return left.withMin(right.interval.min);
			} else {
				return left;
			}
		}

		@Override
		public Option<CompareResult> cmpRelational(final InstrumentedState state, final Value leftOp, final Value rightOp) {
			return KLimitAP.of(leftOp).bind(lP ->
				KLimitAP.of(rightOp).bind(rP ->
					monads.stateMonad.map(state, as -> {
						if(as.congruenceClosure.get(lP).map(cc -> cc.member(rP)).orSome(false)) {
							return Option.some(CompareResult.EQ);
						} else if(as.ltClosure.get(lP).map(cc -> cc.member(rP)).orSome(false)) {
							return Option.some(CompareResult.LT);
						} else if(as.ltClosure.get(rP).map(cc -> cc.member(lP)).orSome(false)) {
							return Option.some(CompareResult.GT);
						} else {
							return Option.none();
						}
					})
				)
			);
		}
		
		@Override
		public InstrumentedState propagateRelationLT(final InstrumentedState inputState, final Value lop, final Value rop) {
			return monads.stateMonad.mapState(inputState, as -> as.propagateLTAssumption(lop, rop));
		}
		
		@Override
		public InstrumentedState propagateRelationGT(final InstrumentedState inputState, final Value lop, final Value rop) {
			return monads.stateMonad.mapState(inputState, as -> as.propagateLTAssumption(rop, lop));
		}
		
		@Override
		public InstrumentedState propagateRelationEQ(final InstrumentedState inputState, final Value lop, final Value rop) {
			return monads.stateMonad.mapState(inputState, as -> as.propagateEQAssumption(lop, rop));
		}
	}

	private class EvalChain {
		private final Seq<Value> toEval;

		public EvalChain() {
			toEval = Seq.empty();
		}
		
		private EvalChain(final Seq<Value> newEval) {
			toEval = newEval;
		}
		
		public EvalChain chain(final Value v) {
			return new EvalChain(toEval.snoc(v));
		}
		
		public EvalChain chain(final Iterable<Value> v) {
			return new EvalChain(toEval.append(Seq.iterableSeq(v)));
		}
		
		public Pair<List<Object>, InstrumentedState> eval(final InstrumentedState startState, final CallSite context) {
			final List<Object> toReturn = new ArrayList<>();
			InstrumentedState stateAccum = startState;
			for(final Value v : toEval) {
				final EvalResult er = ArrayBoundsChecker.this.eval(stateAccum, v, context);
				if(er == null) {
					throw new NoMethodSummary();
				}
				toReturn.add(er.value);
				stateAccum = er.state;
			}
			return new Pair<>(toReturn, stateAccum);
		}
	}
	
	private InterpResult interpret(final InstrumentedState currState, final Unit toExecute, final CFGUtil cfgUtil, final CallSite context) {
		if(toExecute instanceof IfStmt) {
			final IfStmt ifStmt = (IfStmt) toExecute;
			final ConditionExpr condition = (ConditionExpr) ifStmt.getCondition();
			final Value expr1 = condition.getOp1();
			final Value expr2 = condition.getOp2();
			final Pair<List<Object>, InstrumentedState> evaled = new EvalChain().chain(expr1).chain(expr2).eval(currState, context);
			final Object rawOp1 = evaled.getO1().get(0);
			final Object rawOp2 = evaled.getO1().get(1);
			final InstrumentedState resultState = evaled.getO2();
			final Map<Unit, InstrumentedState> interpreted = this.branchInterpreter.interpretBranch(ifStmt, rawOp1, rawOp2, resultState, new StateValueUpdater<ARState>() {
				@Override
				public ARState updateForValue(final Value v, final ARState state, final Object value) {
					if(v instanceof Local) {
						return state.set((Local) v, value);
					} else {
						return state;
					}
				}
			});
			return InterpResult.of(interpreted);
		} else if(toExecute instanceof ReturnStmt) {
			final Value op = ((ReturnStmt) toExecute).getOp();
			final EvalResult er = eval(currState, op, context);
			return cfgUtil.returnValue(er);
		} else if(toExecute instanceof ReturnVoidStmt) {
			return new InterpResult(currState);
		} else if(toExecute instanceof GotoStmt) {
			return new InterpResult(((GotoStmt) toExecute).getTarget(), currState);
		} else if(toExecute instanceof InvokeStmt) {
			if(((InvokeStmt) toExecute).getInvokeExpr() instanceof StaticInvokeExpr) {
				return cfgUtil.next(this.evalIntrinsic(currState, (StaticInvokeExpr) ((InvokeStmt) toExecute).getInvokeExpr(), context).mapTo(this::havocHeapRelations).state);
			}
			final InstanceInvokeExpr iie = (InstanceInvokeExpr) ((InvokeStmt) toExecute).getInvokeExpr();
			return someOrThrow(interpretCall(currState, context, iie).map(cfgUtil::next));
		} else if(toExecute instanceof AssignStmt) {
			final AssignStmt assignStmt = (AssignStmt) toExecute;
			final Value lhs = assignStmt.getLeftOp();
			final Value rhs = assignStmt.getRightOp();
			if(lhs instanceof Local) {
				final SymbEvalResult toSet = evalSymbolic(currState, rhs, context);
				return cfgUtil.next(monads.stateMonad.mapState(toSet.state, in -> {
					return in.set((Local) lhs, toSet.value, toSet.res);
				}));
			} else if(lhs instanceof ArrayRef) {
				final Value arrayBase = ((ArrayRef) lhs).getBase();
				final Value arrayIndex = ((ArrayRef) lhs).getIndex();
				final EvalResult baseResult = this.eval(currState, arrayBase, context);
				final SymbEvalResult indexResult = this.evalSymbolic(baseResult.state, arrayIndex, context);
				final Option<KLimitAP> baseAP_ = KLimitAP.of(arrayBase).map(KLimitAP::makeLengthAP);
				final boolean provablyWithinUB;
				if(baseAP_.isSome()) {
					provablyWithinUB = indexResult.res.upperBounds().toStream().exists(ap -> monads.stateMonad.map(indexResult.state, as -> as.getLtClosure(ap).member(baseAP_.some())));
				} else {
					provablyWithinUB = false;
				}
				final boolean provablyWithinLB = monads.valueMonad.alpha(indexResult.value).isPositive();
				final EvalResult rhsResult = this.eval(indexResult.state, rhs, context);
				
				final PValue index = monads.valueMonad.alpha(indexResult.value);
				final Object toSet = rhsResult.value;
				final AbstractHeapAccessResult[] faults = new AbstractHeapAccessResult[]{ AbstractHeapAccessResult.INFEASIBLE};
				final InstrumentedState newState = monads.stateMonad.updateValue(rhsResult.state, baseResult.value, new ValueStateTransformer<PValue, ARState>() {
					@Override
					public UpdateResult<ARState, PValue> mapAbstract(final PValue val, final ARState state, final Heap h,
							final RecursiveTransformer<ARState, UpdateResult<ARState, ?>> recursor) {
						final P2<ARHeap, AbstractHeapAccessResult> upH;
						if(provablyWithinUB && provablyWithinLB) {
							upH = state.heap.setArraySafe(val, index, toSet);
						} else {
							upH = state.heap.setArray(val, index, toSet);
						}
						faults[0] = AbstractHeapAccessResult.join(faults[0], upH._2());
						return new UpdateResult<>(state.withHeap(upH._1()), h, val);
					}

					@Override
					public UpdateResult<ARState, IValue> mapConcrete(final IValue v, final ARState state, final Heap h, final HeapMutator mutator,
							final RecursiveTransformer<ARState, UpdateResult<ARState, ?>> recursor) {
						final HeapUpdateResult<ARState> toReturn;
						if(index.isFinite()) {
							toReturn = mutator.updateAtIndex(state, h, v, IValue.liftNative(index.concretize()), toSet);
						} else {
							toReturn = mutator.updateNondetIndex(state, h, v, toSet);
						}
						faults[0] = AbstractHeapAccessResult.join(faults[0], AbstractHeapAccessResult.lift(new HeapAccessResult(toReturn.nullBasePointer, toReturn.oobAccess)));
						return toReturn;
					}
				}, StateUpdater.IdentityUpdater.<ARState>v());
				this.checkArrayAccessResult(faults[0], (ArrayRef) lhs, context);
				return cfgUtil.next(newState);
			} else {
				assert lhs instanceof InstanceFieldRef;
				final InstanceFieldRef ifr = (InstanceFieldRef) lhs;
				final EvalResult baseEval = this.eval(currState, ifr.getBase(), context);
				final SymbEvalResult rhsResult = this.evalSymbolic(baseEval.state, rhs, context);
				final PValue basePointer = monads.valueMonad.alpha(baseEval.value);
				final InstrumentedState postAssign = monads.stateMonad.mapState(rhsResult.state, in -> {
					final P2<ARHeap, AbstractHeapAccessResult> result = in.heap.setField(basePointer, ifr.getField().getSignature(), rhsResult.value);
					return in.killRelationForField(ifr.getFieldRef()).withHeap(filterHeapUpdate(result, ifr, context))
									.propagateSymbolicResult(KLimitAP.of(lhs), rhsResult.res);
						}
				);
				return cfgUtil.next(postAssign);
			}
		} else {
			return cfgUtil.next(currState);
		}
	}

	private ARHeap filterHeapUpdate(final P2<ARHeap, AbstractHeapAccessResult> updateResult, final Value op, final CallSite context) {
		checkHeapAccessResult(updateResult._2(), op, context);
		return updateResult._1();
	}


	private void checkArrayAccessResult(final AbstractHeapAccessResult heapUpdate, final ArrayRef op, final CallSite context) {
		// report and prune
		if(heapUpdate.oob.isPossible()) {
			this.arrayReads.add(P.p(context, op));
		}
		checkHeapAccessResult(heapUpdate, op, context);
	}

	private void checkHeapAccessResult(final AbstractHeapAccessResult heapUpdate, final Value op, final CallSite context) {
		final P2<CallSite, Value> nullDeref = P.p(context, op);
		if(heapUpdate.npe == HeapFaultStatus.MUST) {
			this.nullPointerDereferences.add(nullDeref);
		} else if(nullPointerDereferences.contains(nullDeref)) {
			this.nullPointerDereferences.remove(nullDeref);
		}
		if(heapUpdate.shouldPrune()) {
			throw new NoMethodSummary();
		}
	}

	private Option<MethodResult> interpretCall(final InstrumentedState currState, final CallSite context, final InstanceInvokeExpr iie) {
		if(iie instanceof SpecialInvokeExpr && iie.getMethodRef().getSignature().equals("<java.lang.Object: void <init>()>")) {
			return Option.some(new MethodResult(currState));
		}
		final Pair<List<Object>, InstrumentedState> chainResult = new EvalChain().chain(iie.getBase()).chain(iie.getArgs()).eval(currState, context);
		final ParamRelation relation = monads.stateMonad.map(chainResult.getO2(), (ar -> ParamRelation.fromState(ar, iie.getArgs())));
		final List<Object> baseAndArgs = chainResult.getO1();

		return this.callHandler.handleCall(context, iie, baseAndArgs.get(0), baseAndArgs.subList(1, iie.getArgCount() + 1), chainResult.getO2(), relation);
	}
	
	private static class NoMethodSummary extends RuntimeException { }
	
	private final ArithmeticEvaluator<PValue> arith = new ArithmeticEvaluator<>(this.primitiveOperations());

	
	private static class SymbEvalResult extends EvalResult {
		private final RelationResult res;

		public SymbEvalResult(final InstrumentedState state, final Object result, final RelationResult res) {
			super(state, result);
			this.res = res;
		}
		
		@Override
		public String toString() {
			return this.value + " " + this.res;
		}
	}
	
	private SymbEvalResult liftNone(final InstrumentedState state, final Object val) {
		return new SymbEvalResult(state, val, RelationResult.none);
	}
	
	private F2<InstrumentedState, Object, SymbEvalResult> mapWithCode(final RelationResult.RelCode code, final KLimitAP... ap) {
		return (inst, val) -> { return new SymbEvalResult(inst, val, new RelationResult(code, ap)); };
	}
	
	private F2<InstrumentedState, Object, SymbEvalResult> mapWithCode(final RelationResult.RelCode code, final Option<KLimitAP> ap) {
		if(ap.isNone()) {
			return (inst, val) -> { return new SymbEvalResult(inst, val, RelationResult.none); };
		}
		return mapWithCode(code, ap.some());
	}

	private SymbEvalResult havocHeapRelations(final InstrumentedState currState, final Object ret) {
		return new SymbEvalResult(monads.stateMonad.mapState(currState, ARState::killHeapRelations), ret, RelationResult.none);
	}
	
	private SymbEvalResult evalSymbolic(final InstrumentedState currState, final Value op, final CallSite context) {
		if(op instanceof IntConstant) {
			return new SymbEvalResult(currState, monads.valueMonad.lift(PValue.lift(((IntConstant) op).value)), RelationResult.none);
		} else if(op instanceof Local) {
			return monads.stateMonad.mapToResult(currState, new PureFunction<ARState>() {
				@Override
				protected Object innerApply(final ARState in) {
					return in.get((Local)op);
				}
			}).mapTo(this.mapWithCode(RelationResult.RelCode.EQ, KLimitAP.of((Local)op)));
		} else if(op instanceof InstanceFieldRef) {
			final EvalResult base = eval(currState, ((InstanceFieldRef) op).getBase(), context);
			final PValue v = monads.valueMonad.alpha(base.value);
			return monads.stateMonad.mapToResult(base.state, new PureFunction<ARState>() {
				@Override
				protected Object innerApply(final ARState in) {
					final AbstractHeapUpdateResult<Object> result = in.heap.readField(v, ((InstanceFieldRef) op).getField().getSignature());
					checkHeapAccessResult(result, op, context);
					return result.value;
				}
			}).mapTo(mapWithCode(RelationResult.RelCode.EQ, KLimitAP.of(op)));
		} else if(op instanceof NullConstant) {
			return new EvalResult(currState, PValue.nullPtr()).mapTo(this::liftNone);
		} else if(op instanceof GNewInvokeExpr) {
			final List<Value> constructorArgs = ((GNewInvokeExpr) op).getArgs();
			final Pair<List<Object>, InstrumentedState> constrResult = (new EvalChain()).chain(constructorArgs).eval(currState, context);
			return someOrThrow(this.callHandler.allocType((GNewInvokeExpr) op, constrResult.getO2(), constrResult.getO1(), context)).mapTo(this::havocHeapRelations);
		} else if(op instanceof NewArrayExpr) {
			final SymbEvalResult result = this.evalSymbolic(currState, ((NewArrayExpr) op).getSize(), context);
			return this.callHandler.allocArray(((NewArrayExpr)op), result.state, result.value, context).mapTo((inst, val) -> new SymbEvalResult(inst, val, result.res.toLength()));
		} else if(op instanceof NewMultiArrayExpr) {
			final Pair<List<Object>, InstrumentedState> chainResult = new EvalChain().chain(((NewMultiArrayExpr) op).getSizes()).eval(currState, context);
			return this.callHandler.allocArray((NewMultiArrayExpr)op, chainResult.getO2(), chainResult.getO1(), context).mapTo(this::liftNone);
		} else if(op instanceof StaticInvokeExpr) {
			final EvalResult intrinsicResult = this.evalIntrinsic(currState, (StaticInvokeExpr) op, context);
			if(intrinsicResult == null) {
				throw new NoMethodSummary();
			}
			return intrinsicResult.mapTo(this::havocHeapRelations);
		} else if(op instanceof InvokeExpr) {
			final InstanceInvokeExpr iie = (InstanceInvokeExpr) op;
			return someOrThrow(this.interpretCall(currState, context, iie).map(mr -> this.havocHeapRelations(mr.getState(), mr.getReturnValue())));
		} else if(op instanceof AbstractFloatBinopExpr) {
			final RelationResult r1, r2;
			final InstrumentedState resultState;
			final Value rop = ((AbstractFloatBinopExpr) op).getOp2();
			final Value lop = ((AbstractFloatBinopExpr) op).getOp1();
			final PValue a1, a2;
			final Object added;
			PValue absAdded;
			{
				final SymbEvalResult symb1 = this.evalSymbolic(currState, lop, context);
				r1 = symb1.res;
				final SymbEvalResult symb2 = this.evalSymbolic(currState, rop, context);
				r2 = symb2.res;
				resultState = symb2.state;
				a1 = monads.valueMonad.alpha(symb1.value);
				a2 = monads.valueMonad.alpha(symb2.value);
				absAdded = arith.eval((Expr) op, a1, a2);
				added = monads.valueMonad.lift(absAdded);
			}
			if(this.hasMethodCall(lop) || this.hasMethodCall(rop)) {
				return new SymbEvalResult(resultState, added, RelationResult.none);
			}
			if(op instanceof AddExpr) {
				if(a1.isStrictlyPositive() && a2.isStrictlyPositive()) {
					return new SymbEvalResult(resultState, added, new RelationResult(RelationResult.RelCode.GT, r1.lowerBounds().union(r2.lowerBounds())));
				} else if(a1.isPositive() && a2.isPositive()) {
					return new SymbEvalResult(resultState, added, new RelationResult(RelationResult.RelCode.GE, r1.lowerBounds().union(r2.lowerBounds())));
				} else if(a1.isStrictlyNegative() && a2.isStrictlyNegative()) {
					return new SymbEvalResult(resultState, added, new RelationResult(RelationResult.RelCode.LT, r1.upperBounds().union(r2.upperBounds())));
				} else if(a1.isNegative() && a2.isNegative()) {
					return new SymbEvalResult(resultState, added, new RelationResult(RelationResult.RelCode.LE, r1.upperBounds().union(r2.upperBounds())));
				} else if(a1.isStrictlyPositive()) {
					return new SymbEvalResult(resultState, added, new RelationResult(RelationResult.RelCode.GT, r2.upperBounds()));
				} else if(a2.isStrictlyPositive()) {
					return new SymbEvalResult(resultState, added, new RelationResult(RelationResult.RelCode.GT, r1.upperBounds()));
				} else if(a1.isPositive()) {
					return new SymbEvalResult(resultState, added, new RelationResult(RelationResult.RelCode.GE, r2.upperBounds()));
				} else if(a2.isPositive()) {
					return new SymbEvalResult(resultState, added, new RelationResult(RelationResult.RelCode.GE, r1.upperBounds()));
				} else if(a1.isStrictlyNegative()) {
					return new SymbEvalResult(resultState, added, new RelationResult(RelationResult.RelCode.LT, r2.upperBounds()));
				} else if(a2.isStrictlyNegative()) {
					return new SymbEvalResult(resultState, added, new RelationResult(RelationResult.RelCode.LT, r1.upperBounds()));
				}  else if(a1.isNegative()) {
					return new SymbEvalResult(resultState, added, new RelationResult(RelationResult.RelCode.LE, r2.upperBounds()));
				} else if(a2.isNegative()) {
					return new SymbEvalResult(resultState, added, new RelationResult(RelationResult.RelCode.LE, r1.upperBounds()));
				}
			} else if(op instanceof SubExpr) {
				final boolean provablyLess = KLimitAP.of(lop).bind(lap -> KLimitAP.of(rop).map(rap ->
					monads.stateMonad.map(resultState, as -> as.getLtClosure(lap).member(rap))
				)).orSome(false);
				if(provablyLess) {
					absAdded = absAdded.withMin(1);
				}
				if(a2.isStrictlyPositive()) {
					return new SymbEvalResult(resultState, monads.valueMonad.lift(absAdded), new RelationResult(RelationResult.RelCode.LT, r1.upperBounds()));
				} else if(a2.isPositive()) {
					return new SymbEvalResult(resultState, monads.valueMonad.lift(absAdded), new RelationResult(RelationResult.RelCode.LE, r1.upperBounds()));
				}
			} else if(op instanceof MulExpr) {
				if(a1.isStrictlyPositive() && a2.isStrictlyPositive()) {
					return new SymbEvalResult(resultState, added, new RelationResult(RelationResult.RelCode.GT, r1.lowerBounds().union(r2.lowerBounds())));
				} else if(a1.isPositive() && a2.isPositive() && a1.couldBeZero() && a2.couldBeZero()) {
					return new SymbEvalResult(resultState, added, new RelationResult(RelationResult.RelCode.GE, r1.lowerBounds().union(r2.lowerBounds())));
				} else if(a1.isPositive() && a1.couldBeZero() && a2.isPositive()) {
					return new SymbEvalResult(resultState, added, new RelationResult(RelationResult.RelCode.GE, r1.lowerBounds()));
				} else if(a2.isPositive() && a2.couldBeZero() && a1.isPositive()) {
					return new SymbEvalResult(resultState, added, new RelationResult(RelationResult.RelCode.GE, r2.lowerBounds()));
				} else if(a1.isStrictlyNegative() && a2.isStrictlyNegative()) {
					return new SymbEvalResult(resultState, added, new RelationResult(RelationResult.RelCode.GT, r1.upperBounds().union(r2.upperBounds())));
				} else if(a1.isNegative() && a2.isNegative()) {
					return new SymbEvalResult(resultState, added, new RelationResult(RelationResult.RelCode.GE, r1.upperBounds().union(r2.upperBounds())));
				}
			}
			return new EvalResult(resultState, added).mapTo(this::liftNone);
		} else if((op instanceof AbstractIntBinopExpr) || (op instanceof AbstractFloatBinopExpr)) {
			final InstrumentedState state = new EvalChain().chain(((AbstractBinopExpr) op).getOp1()).chain(((AbstractBinopExpr) op).getOp2()).eval(currState, context).getO2();
			return new EvalResult(state, monads.valueMonad.lift(PValue.fullInterval())).mapTo(this::liftNone);
		} else if(op instanceof CastExpr) {
			final EvalResult toCast = eval(currState, ((CastExpr) op).getOp(), context);
			final Type castType = ((CastExpr) op).getCastType();
			final Object castValue = monads.stateMonad.mapValue(currState, toCast.value, new ValueMapper<PValue, ARState, Object>() {
				@Override
				public Object mapAbstract(final PValue val, final ARState state, final Heap h, final RecursiveTransformer<ARState, Object> recursor) {
					return monads.valueMonad.lift(objectOperations().downcast(val, castType));
				}

				@Override
				public Object mapConcrete(final IValue v, final ARState state, final Heap h, final HeapReader<ARState, PValue> reader,
						final RecursiveTransformer<ARState, Object> recursor) {
					return monads.valueMonad.lift(v.downCast(castType));
				}

				@Override
				public Object merge(final Object v1, final Object v2) {
					return monads.valueMonad.join(v1, v2);
				}
			});
			return new EvalResult(toCast.state, castValue).mapTo(this::liftNone);
		} else if(op instanceof ArrayRef) {
			final ArrayRef arrayRef = (ArrayRef) op;
			final EvalResult baseResult = this.evalSymbolic(currState, arrayRef.getBase(), context);
			final SymbEvalResult indexResult = this.evalSymbolic(baseResult.state, arrayRef.getIndex(), context);
			final boolean checkIndex;
			final Option<KLimitAP> baseLengthAP = KLimitAP.of(arrayRef.getBase()).map(KLimitAP::makeLengthAP);
			final PValue ind = monads.valueMonad.alpha(indexResult.value);
			if(baseLengthAP.isSome()) {
				final boolean provablyWithinBounds = monads.stateMonad.map(indexResult.state, as ->
					indexResult.res.upperBounds().toStream().exists(apUB ->
						as.getLtClosure(apUB).member(baseLengthAP.some())
					)
				);
				final boolean provablyPositive = monads.valueMonad.alpha(ind).isPositive();
				checkIndex = !provablyWithinBounds || !provablyPositive;
			} else {
				checkIndex = true;
			}
			final AbstractHeapUpdateResult<Object> readValue = monads.stateMonad.mapValue(indexResult.state, baseResult.value, new ValueMapper<PValue, ARState, AbstractHeapUpdateResult<Object>>() {
				@Override
				public AbstractHeapUpdateResult<Object> mapAbstract(final PValue val, final ARState state, final Heap h, final RecursiveTransformer<ARState, AbstractHeapUpdateResult<Object>> recursor) {
					if(!checkIndex) {
						return state.heap.readArraySafe(val, ind);
					} else {
						return state.heap.readArray(val, ind);
					}
				}

				@Override
				public AbstractHeapUpdateResult<Object> mapConcrete(final IValue v, final ARState state, final Heap h, final HeapReader<ARState, PValue> heapAccessor,
						final RecursiveTransformer<ARState, AbstractHeapUpdateResult<Object>> recursor) {
					final HeapReadResult<Object> toReturn;
					if(ind.isFinite()) {
						toReturn = heapAccessor.readIndex(h, v, IValue.liftNative(ind.concretize()));
					} else {
						toReturn = heapAccessor.readNondetIndex(h, v);
					}
					return AbstractHeapUpdateResult.lift(toReturn);
				}

				@Override
				public AbstractHeapUpdateResult<Object> merge(final AbstractHeapUpdateResult<Object> v1, final AbstractHeapUpdateResult<Object> v2) {
					return v1.merge(v2, monads.valueMonad::join);
				}
			});
			this.checkArrayAccessResult(readValue, arrayRef, context);
			return new SymbEvalResult(indexResult.state, readValue.value, RelationResult.none);
		} else if(op instanceof LengthExpr) {
			final Value toBase = ((LengthExpr) op).getOp();
			final EvalResult baseResult = eval(currState, toBase, context);
			final PValue len = monads.stateMonad.mapValue(baseResult.state, baseResult.value, new ValueMapper<PValue, ARState, PValue>() {

				@Override
				public PValue mapAbstract(final PValue val, final ARState state, final Heap h, final RecursiveTransformer<ARState, PValue> recursor) {
					return objectOperations().arrayLength(val, state.heap);
				}

				@Override
				public PValue mapConcrete(final IValue v, final ARState state, final Heap h, final HeapReader<ARState, PValue> heapAccessor,
						final RecursiveTransformer<ARState, PValue> recursor) {
					return v.valueStream().foldLeft(new F2<PValue, IValue, PValue>() {
						@Override
						public PValue f(final PValue a, final IValue b) {
							if(b.getTag() == RuntimeTag.ARRAY) {
								final IValue length = h.getLength(b);
								if(length.isNonDet()) {
									return PValue.positiveInterval();
								} else {
									return PValue.lattice.join(alpha().lift(length), a);
								}
							} else {
								return a;
							}
						}
					}, PValue.bottom());
				}

				@Override
				public PValue merge(final PValue v1, final PValue v2) {
					return PValue.lattice.join(v1, v2);
				}
			});
			final Option<KLimitAP> lenAp = KLimitAP.of(op);
			if(lenAp.isSome()) {
				return new SymbEvalResult(baseResult.state, monads.valueMonad.lift(len), new RelationResult(RelationResult.RelCode.EQ, KLimitAP.singleton(lenAp.some()), true));
			} else {
				return new SymbEvalResult(baseResult.state, monads.valueMonad.lift(len), RelationResult.none);
			}
		} else {
			return null;
		}
	}

	private boolean hasMethodCall(final Value op1) {
		for(final ValueBox vb : op1.getUseBoxes()) {
			if(vb.getValue() instanceof InvokeExpr) {
				return true;
			}
		}
		return false;
	}

	private EvalResult eval(final InstrumentedState currState, final Value op, final CallSite context) {
		return this.evalSymbolic(currState, op, context);
	}

	private final IntrinsicHandler<ARState, CallSite> intrinsicHandler = new IntrinsicHandler<ARState, CallSite>(() -> this.monads.stateMonad) {
		@Override protected EvalResult handleLift(final InstrumentedState currState, final StaticInvokeExpr sie, final CallSite context) {
			final EvalResult res = this.eval(currState, sie.getArg(0), context);
			final Object lifted = monads.stateMonad.mapValue(currState, res.value, new ValueMapper<PValue, ARState, Object>() {
				@Override
				public Object mapAbstract(final PValue val, final ARState state, final Heap h, final RecursiveTransformer<ARState, Object> recursor) {
					assert !val.address.isEmpty() : val;
					return val.address.toStream().map(addr ->
							state.heap.map.get(addr).some().collapseToValue()
					).foldLeft1(monads.valueMonad::join);
				}

				@Override
				public Object mapConcrete(final IValue v, final ARState state, final Heap h, final HeapReader<ARState, PValue> heapAccessor,
						final RecursiveTransformer<ARState, Object> recursor) {
					return heapAccessor.readNondetIndex(h, v);
				}
				@Override
				public Object merge(final Object v1, final Object v2) {
					return monads.valueMonad.join(v1, v2);
				}
			});
			return new EvalResult(res.state, lifted);
		}

		@Override protected EvalResult handleConstantAlloc(final InstrumentedState currState, final StaticInvokeExpr sie, final String className, final CallSite callSite) {
			return someOrThrow(callHandler.allocType(className, currState, sie, callSite));
		}

		@Override protected EvalResult handleInvoke(final InstrumentedState currState, final StaticInvokeExpr sie, final CallSite context) {
			final Pair<List<Object>, InstrumentedState> invokeArgs = new EvalChain().chain(sie.getArgs()).eval(currState, context);
			return someOrThrow(callHandler.handleInvoke(context, sie, invokeArgs.getO1(), invokeArgs.getO2()).map(EvalResult::new));
		}

		@Override protected EvalResult handleAllocate(final InstrumentedState currState, final StaticInvokeExpr sie, final CallSite context) {
			if(sie.getArg(0) instanceof IntConstant) {
				return someOrThrow(ReflectionEnvironment.v().resolve(((IntConstant) sie.getArg(0)).value).bind(klass ->
						callHandler.allocType(klass.getName(), currState, sie, context)
				).orElse(() -> callHandler.allocUnknownType(currState, sie, context)));
			} else if(sie.getArg(0) instanceof StringConstant) {
				final StringConstant sc = (StringConstant) sie.getArg(0);
				return someOrThrow(callHandler.allocType(sc.value, currState, sie, context));
			} else {
				final EvalResult argRes = this.eval(currState, sie.getArg(0), context);
				final fj.data.Set<String> allocNames = monads.stateMonad.mapValue(argRes.state, argRes.value, new ValueMapper<PValue, ARState, fj.data.Set<String>>() {
					@Override
					public fj.data.Set<String> mapAbstract(final PValue val, final ARState state, final Heap h, final RecursiveTransformer<ARState, fj.data.Set<String>> recursor) {
						if(val.isInterval() && val.isFinite()) {
							assert val.interval.isFinite();
							return resolveIntStream(Stream.iterableStream(val.concretize()).<Integer>map(o -> ((Integer)o)));
						} else {
							return null;
						}
					}

					private Set<String> resolveIntStream(final Stream<Integer> map) {
						return map.foldLeft((accum, iv) -> {
							if(accum == null) {
								return null;
							}
							final Option<SootClass> resolved = ReflectionEnvironment.v().resolve(iv);
							if(resolved.isNone()) {
								return null;
							} else {
								return accum.insert(resolved.some().getName());
							}
						}, Set.empty(Ord.stringOrd));
					}

					@Override
					public fj.data.Set<String> mapConcrete(final IValue v, final ARState state, final Heap h, final HeapReader<ARState, PValue> heapAccessor,
							final RecursiveTransformer<ARState, fj.data.Set<String>> recursor) {
						if(v.getTag() == RuntimeTag.INT) {
							return ReflectionEnvironment.v().resolve(v.asInt()).map(klass -> fj.data.Set.single(Ord.stringOrd, klass.getName())).toNull();
						} else if(v.getTag() == RuntimeTag.MULTI_VALUE) {
							return resolveIntStream(v.valueStream().map(IValue::asInt));
						} else {
							return null;
						}
					}

					@Override
					public fj.data.Set<String> merge(final fj.data.Set<String> v1, final fj.data.Set<String> v2) {
						if(v1 == null ) {
							return null;
						} else if(v2 == null) {
							return null;
						} else {
							return v1.union(v2);
						}
					}
				});
				if(allocNames == null) {
					return someOrThrow(callHandler.allocUnknownType(argRes.state, sie, context));
				}
				if(allocNames.isEmpty()) {
					return new EvalResult(argRes.state, monads.valueMonad.lift(PValue.bottom()));
				}
				return allocNames.toStream().map(n -> callHandler.allocType(n, argRes.state, sie, context)).map(ArrayBoundsChecker.this::someOrThrow).foldLeft1((a, b) ->
						new EvalResult(monads.stateMonad.join(a.state, b.state), monads.valueMonad.join(a.value, b.value)));
			}

		}

		@Override protected EvalResult handleIO(final InstrumentedState currState, final CallSite context) {
			return new EvalResult(currState, monads.valueMonad.lift(PValue.fullInterval()));
		}

		@Override protected EvalResult eval(final InstrumentedState currState, final Value arg, final CallSite context) {
			return ArrayBoundsChecker.this.eval(currState, arg, context);
		}

		@Override protected EvalResult handleAssert(final InstrumentedState currState, final StaticInvokeExpr sie, final CallSite context) {
			final InstrumentedState outState = new EvalChain().chain(sie.getArgs()).eval(currState, context).getO2();
			if(checkAssertContexts.contains(new Pair<>(BodyManager.getHostMethod(sie), context))) {
				final Unit stmt = BodyManager.getHostUnit(sie);
				assert stmt instanceof InvokeStmt && ((InvokeStmt)stmt).getInvokeExpr() == sie;
				proofObligations.add(P.p(context, stmt));
			}
			return new EvalResult(outState, null);
		}

		@Override protected EvalResult handleFailure(final InstrumentedState currState, final StaticInvokeExpr sie) {
			throw new NoMethodSummary();
		}

		@Override protected EvalResult handleGetClass(final InstrumentedState state, final Object value, final CallSite callSite) {
			return new EvalResult(state, monads.valueMonad.lift(PValue.positiveInterval()));
		}

		@Override protected EvalResult handleCheckAsserts(final InstrumentedState currState, final StaticInvokeExpr sie, final CallSite context) {
			checkAssertContexts.add(new Pair<>(BodyManager.getHostMethod(sie), context));
			return new EvalResult(currState, null);
		}
	};

	private EvalResult evalIntrinsic(final InstrumentedState currState, final StaticInvokeExpr sie, final CallSite context) {
		return intrinsicHandler.handleIntrinsic(currState, sie, context);
	}
	
	@Override
	public void dischargeProofObligations() {
		for(final P2<CallSite, Unit> proofObligation : this.proofObligations) {
			final InstrumentedState stateAtAssert = this.memo.get(proofObligation);
			final InvokeStmt s = (InvokeStmt) proofObligation._2();
			final String assertName = s.getInvokeExpr().getMethodRef().name();
			final Pair<List<Object>, InstrumentedState> ch = new EvalChain().chain(s.getInvokeExpr().getArgs()).eval(
					stateAtAssert, proofObligation._1());
			final Object op1 = ch.getO1().get(0);
			final Object op2 = ch.getO1().get(1);
			final boolean isEqual = monads.valueMonad.lessEqual(op1, op2) && 
				monads.valueMonad.lessEqual(op2, op1);
			if(assertName.equals("assertEqual")) {
				 if(!isEqual) {
					 throw new FailedObjectLanguageAssertionException("Failed equals assertion: " + op1 + " == " + op2);
				 }
			} else if(assertName.equals("assertNotEqual")) {
				if(!isEqual) {
					 throw new FailedObjectLanguageAssertionException("Failed not equals assertion: " + op1 + " != " + op2);
				 }
			}
		}
		for(final P2<CallSite, ArrayRef> oob : this.arrayReads) {
			this.resultStream.outputAnalysisResult("Potentially out of bounds reference: " + oob);
		}
		for(final P2<CallSite, Value> oob : this.nullPointerDereferences) {
			this.resultStream.outputAnalysisResult("Definite null pointer dereference: " + oob);
		}
	}
	
	private static class PruneExecution extends RuntimeException { }
	
	private <R> R someOrThrow(final Option<R> o) {
		return o.orSome(() -> { throw new NoMethodSummary(); });
	}

	private boolean enqueueMethod(final SootMethod m, final CallSite context, final InstrumentedState state) {
		final Unit startUnit = BodyManager.retrieveBody(m).getUnits().getFirst();
		final InstrumentedState incoming = this.callGraph.isWideningPoint(context) ? widenWith(context, startUnit, state) : joinWith(context, startUnit, state);
		final P2<InstrumentedState, Boolean> initAndNew = this.continuationManager.getInitialState(incoming, context);
		if(initAndNew._2()) {
		final P2<CallSite, Unit> key = P.p(context, startUnit);
			this.memo.put(key, initAndNew._1());
			this.worklist.add(key);
			return true;
		}
		return false;
	}

	@Override
	public Pair<ARHeap, PValue> abstractObjectAlloc(final RefType t, final ARHeap inputHeap, final Value allocationExpr, final CallSite allocContext) {
		return inputHeap.allocate(t, allocContext, allocationExpr);
	}

	@Override
	public Pair<ARHeap, PValue> abstractArrayAlloc(final ArrayType t, final ARHeap inputHeap, final Value allocationExpr, final List<PValue> sizes, final CallSite allocContext) {
		return inputHeap.allocate(t, allocContext, sizes, allocationExpr);
	}

	@Override
	public void instrument(final InstrumentationManager<PValue, ARHeap, ARState> instManager) {
		final ValueMapper<PValue, ARState, Boolean> nullPointerChecker = new ConcreteValueMapper<PValue, ARState, Boolean>() {
			@Override
			public Boolean mapConcrete(final IValue v, final ARState state, final Heap h, final HeapReader<ARState, PValue> heapAccessor,
					final RecursiveTransformer<ARState, Boolean> recursor) {
				return v.getTag() == RuntimeTag.NULL;
				/*final boolean[] isNull = new boolean[1];
				v.forEach(new IValueAction() {
					
					@Override
					public void nondet() { }
					
					@Override
					public void accept(final IValue v, final boolean isMulti) {
						if(v.getTag() == RuntimeTag.NULL) {
							isNull[0] = true;
						}
					}
				});
				return isNull[0];*/
			}
		};
		instManager
			.selector()
				.fieldRead().build().withAction(new FieldReadAction<PValue, ARHeap, ARState>() {
					
					@Override
					public void postBase(final ValueReplacement<PValue, ARHeap, ARState> baseReplacement) {
						if(baseReplacement.getReader().read(nullPointerChecker)) {
							throw new NullPointerException();
						}
					}
					
					@Override
					public void postRead(final ValueReplacement<PValue, ARHeap, ARState> valueReplacement) { }
				});
		instManager.selector().fieldWrite().build().withAction(new FieldWriteAction<PValue, ARHeap, ARState>() {
			
			@Override
			public void postBase(final ValueReplacement<PValue, ARHeap, ARState> baseReplacement) {
				if(baseReplacement.getReader().read(nullPointerChecker)) {
					throw new NullPointerException();
				}
			}
			
			@Override
			public void preWrite(final ValueReplacement<PValue, ARHeap, ARState> valueReplacement) { }
		});
	}

	@Override
	public void setBranchInterpreter(final PathSensitiveBranchInterpreter<PValue, ARState> bInterp) {
		this.branchInterpreter = bInterp;
	}

	@Override
	public ObjectOperations<PValue, ARHeap> objectOperations() {
		return new ObjectOperations<PValue, ARHeap>() {

			@Override
			public Option<Object> readArray(final ARHeap h, final PValue basePointer, final PValue index, final ArrayRef context) {
				final AbstractHeapUpdateResult<Object> readResult = h.readArraySafe(basePointer, index);
				if(readResult.shouldPrune()) {
					return Option.none();
				} else {
					return Option.some(readResult.value);
				}
			}

			@Override
			public ARHeap writeArray(final ARHeap h, final PValue basePointer, final PValue index, final Object value, final ArrayRef context) {
				return h.setArraySafe(basePointer, index, value)._1();
			}

			@Override
			public PValue arrayLength(final PValue val, final ARHeap h) {
				return Stream.iterableStream(val.addresses()).foldLeft(new F2<PValue, AbstractLocation, PValue>() {
					@Override
					public PValue f(final PValue a, final AbstractLocation b) {
						return PValue.lattice.join(h.getLength(b), a);
					}
				}, PValue.bottom());
			}

			@Override
			public PValue downcast(final PValue v, final Type castType) {
				return v.filterType(castType);
			}

			@Override
			public ObjectIdentityResult isNull(final PValue a) {
				if(a.address.member(AbstractLocation.NULL_LOCATION)) {
					if(a.address.size() == 1) {
						return ObjectIdentityResult.MUST_BE;
					} else {
						return ObjectIdentityResult.MAY_BE;
					}
				} else {
					return ObjectIdentityResult.MUST_NOT_BE;
				}
			}

			@Override
			public Stream<Type> possibleTypes(final PValue a) {
				return a.address.toStream().map(addr -> addr.type);
			}
		};
	}

	@Override
	public Option<Pair<ARHeap, PValue>> allocateUnknownObject(final ARHeap inputHeap, final Value allocationExpr, final CallSite allocationContext,
			final ReflectiveOperationContext ctxt) {
		final RefType upperBound;
		if(ctxt.castHint().isSome()) {
			upperBound = (RefType) ctxt.castHint().some();
			if(!BodyManager.enumerateApplicationClasses(upperBound).iterator().hasNext()) {
				return Option.none();
			}
		} else {
			upperBound = Scene.v().getObjectType();
		}
		return Option.some(inputHeap.allocateUnknownType(upperBound, allocationContext, allocationExpr));
	}

	@Override
	public ParamRelation defaultRelation() {
		return ParamRelation.empty;
	}

	@Override
	public void setCallHandler(final RelationalCallHandler<CallSite, ParamRelation> ch) {
		this.callHandler = ch;
	}
}
