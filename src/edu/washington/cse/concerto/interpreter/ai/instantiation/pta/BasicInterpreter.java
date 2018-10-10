package edu.washington.cse.concerto.interpreter.ai.instantiation.pta;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import edu.washington.cse.concerto.Function;
import edu.washington.cse.concerto.instrumentation.InstrumentationManager;
import edu.washington.cse.concerto.interpreter.BodyManager;
import edu.washington.cse.concerto.interpreter.ai.AbstractInterpretation;
import edu.washington.cse.concerto.interpreter.ai.AbstractionFunction;
import edu.washington.cse.concerto.interpreter.ai.CallHandler;
import edu.washington.cse.concerto.interpreter.ai.ContextManager;
import edu.washington.cse.concerto.interpreter.ai.ContinuationManager;
import edu.washington.cse.concerto.interpreter.ai.EntryPointContextManager;
import edu.washington.cse.concerto.interpreter.ai.EvalResult;
import edu.washington.cse.concerto.interpreter.ai.Lattices;
import edu.washington.cse.concerto.interpreter.ai.MethodResult;
import edu.washington.cse.concerto.interpreter.ai.MethodResultMonad;
import edu.washington.cse.concerto.interpreter.ai.MonadicLattice;
import edu.washington.cse.concerto.interpreter.ai.ObjectOperations;
import edu.washington.cse.concerto.interpreter.ai.PureFunction;
import edu.washington.cse.concerto.interpreter.ai.ReflectiveOperationContext;
import edu.washington.cse.concerto.interpreter.ai.State;
import edu.washington.cse.concerto.interpreter.ai.StateMonad;
import edu.washington.cse.concerto.interpreter.ai.StaticCallGraph;
import edu.washington.cse.concerto.interpreter.ai.ValueMonad;
import edu.washington.cse.concerto.interpreter.ai.ValueMonadLattice;
import edu.washington.cse.concerto.interpreter.ai.binop.DefaultPrimitiveOps;
import edu.washington.cse.concerto.interpreter.ai.binop.ObjectIdentityResult;
import edu.washington.cse.concerto.interpreter.ai.binop.PrimitiveOperations;
import edu.washington.cse.concerto.interpreter.ai.injection.Injectable;
import edu.washington.cse.concerto.interpreter.ai.instantiation.CFGUtil;
import edu.washington.cse.concerto.interpreter.ai.instantiation.InterpResult;
import edu.washington.cse.concerto.interpreter.lattice.Lattice;
import edu.washington.cse.concerto.interpreter.meta.BoundaryInformation;
import edu.washington.cse.concerto.interpreter.meta.InstrumentedState;
import edu.washington.cse.concerto.interpreter.meta.Monads;
import edu.washington.cse.concerto.interpreter.state.ExecutionState;
import edu.washington.cse.concerto.interpreter.value.IValue;
import fj.F;
import fj.F2;
import fj.Monoid;
import fj.Ord;
import fj.Ordering;
import fj.P2;
import fj.data.Either;
import fj.data.Option;
import fj.data.Set;
import fj.data.Stream;
import soot.AnySubType;
import soot.ArrayType;
import soot.Body;
import soot.Local;
import soot.PatchingChain;
import soot.PrimType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.grimp.internal.GNewInvokeExpr;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.NullConstant;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.internal.AbstractBinopExpr;
import soot.jimple.internal.AbstractFloatBinopExpr;
import soot.jimple.internal.AbstractIntBinopExpr;
import soot.toolkits.scalar.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class BasicInterpreter implements AbstractInterpretation<JValue, StupidHeap, StupidState, SootMethod> {
	private StateMonad<StupidState, JValue> stateMonad;
	private ValueMonad<JValue> valueMonad;
	private CallHandler<SootMethod> callHandler;
	
	private final Multimap<SootMethod, ReturnSite> callers = HashMultimap.create();
	private final Table<SootMethod, Unit, InstrumentedState> memo = HashBasedTable.create();
	private final Map<SootMethod, MethodResult> resultStates = new HashMap<>();
	private final LinkedList<Pair<SootMethod, Unit>> worklist = new LinkedList<>();
	private MethodResultMonad<JValue, StupidState> methodResult;
	private ContinuationManager<ReturnSite, SootMethod, StupidState> continuationManager;
	private final StaticCallGraph<SootMethod> callGraph = new StaticCallGraph<>();
	private boolean timeout = false;

	@Override public void interrupt() {
		this.timeout = true;
	}

	@Override public State<StupidHeap, StupidState> stateTransformer() {
		return new State<StupidHeap, StupidState>() {
			@Override
			public StupidHeap project(final StupidState state) {
				return state.heap;
			}

			@Override
			public StupidState inject(final StupidState state, final StupidHeap heap) {
				return state.setHeap(heap);
			}
			
			@Override
			public StupidState emptyState() {
				return StupidState.empty;
			}
		};
	}

	@Override
	public AbstractionFunction<JValue> alpha() {
		return new AbstractionFunction<JValue>() {
			@Override
			public JValue lift(final IValue v) {
				if(v.getTag() == IValue.RuntimeTag.NULL) {
					return JValue.lift(AbstractAddress.NULL_ADDRESS.f());
				}
				return JValue.bottom;
			}
		};
	}
	
	@Override
	public void inject(final Monads<JValue, StupidState> monads) {
		this.valueMonad = monads.valueMonad;
		this.stateMonad = monads.stateMonad;
		this.methodResult = monads.methodResultMonad;
		this.continuationManager = new ContinuationManager<>(ReturnSite.contiuationOrd, stateMonad);
	}

	@Override
	public void setCallHandler(final CallHandler<SootMethod> ch) {
		this.callHandler = ch;
	}

	@Override
	public Lattices<JValue, StupidHeap, StupidState> lattices() {
		return new Lattices<JValue, StupidHeap, StupidState>() {
			@Override
			public Lattice<JValue> valueLattice() {
				return JValue.lattice;
			}
			
			@Override
			public MonadicLattice<StupidState, JValue, StupidState> stateLattice() {
				return StupidState.lattice;
			}
			
			@Override
			public ValueMonadLattice<StupidHeap> heapLattice() {
				return StupidHeap.lattice;
			}
		};
	}
	
	private static final Ord<SootMethod> methodOrd = Ord.ord(new F2<SootMethod, SootMethod, Ordering>() {
		@Override
		public Ordering f(final SootMethod a, final SootMethod b) {
			return Ordering.fromInt(a.getSignature().compareTo(b.getSignature()));
		}
	});

	@Override
	public List<SootMethod> getMethodForRef(final SootMethodRef mref, final JValue receiver) {
		final Set<AbstractAddress> addrSet = receiver.addressSet;
		return addrSet.foldMap(new F<AbstractAddress, Set<SootMethod>>() {
			@Override
			public Set<SootMethod> f(final AbstractAddress a) {
				final Type t = a.t;
				if(t instanceof AnySubType) {
					return Stream.iterableStream(BodyManager.enumerateApplicationClasses(mref.declaringClass().getType(), ((AnySubType) t).getBase())).foldLeft((accum, cls) -> {
						if(cls.declaresMethod(mref.getSubSignature())) {
							return accum.insert(cls.getMethod(mref.getSubSignature()));
						} else {
							return accum;
						}
					}, Set.empty(BasicInterpreter.methodOrd));
				}
				if(!(t instanceof RefType)) {
					return Set.empty(BasicInterpreter.methodOrd);
				}
				final SootClass cls = ((RefType) t).getSootClass();
				if(!cls.declaresMethod(mref.getSubSignature())) {
					return Set.empty(BasicInterpreter.methodOrd);
				}
				return Set.single(BasicInterpreter.methodOrd, cls.getMethod(mref.getSubSignature()));
			}
		}, Monoid.setMonoid(BasicInterpreter.methodOrd)).toJavaList();
	}

	private Option<MethodResult> retrieveSummary(final SootMethod m, final InstrumentedState state, final ReturnSite returnSite) {
		this.callers.put(m, returnSite);
		this.callGraph.registerCall(returnSite.method, m);
		final PatchingChain<Unit> units = getMethodUnits(m);
		final InstrumentedState preContinuation = callGraph.isWideningPoint(m) ? this.widenWith(m, units.getFirst(), state) : this.joinWith(m, units.getFirst(), state);
		final P2<InstrumentedState, Boolean> postContinuation = continuationManager.getInitialState(preContinuation, m);
		if(postContinuation._2()) {
			final boolean enqueued = this.enqueueState(m, units.getFirst(), postContinuation._1());
			assert enqueued;
			return Option.none();
		} else if(resultStates.containsKey(m) && continuationManager.isCompatibleReturn(m, resultStates.get(m).getState())) {
			return Option.some(this.resultStates.get(m));
		} else {
			return Option.none();
		}
	}

	private void enqueueMethod(final SootMethod m, final InstrumentedState newState) {
		this.joinAndEnqueueState(m, getMethodUnits(m).getFirst(), newState);
	}
	
	private void joinAndEnqueueState(final SootMethod m, final Unit target, final InstrumentedState state) {
		enqueueState(m, target, joinWith(m, target, state));
	}

	private boolean enqueueState(final SootMethod m, final Unit target, final InstrumentedState state) {
		if(this.memo.contains(m, target)) {
			if(!this.stateMonad.lessEqual(state, this.memo.get(m, target))) {
				this.memo.put(m, target, state);
				this.worklist.add(new Pair<>(m, target));
				return true;
			}
		} else {
			this.memo.put(m, target, state);
			this.worklist.add(new Pair<>(m, target));
			return true;
		}
		return false;
	}
	
	@Override
	public MethodResult interpretToFixpoint(final SootMethod m, final JValue receiver, final List<Object> arguments, final InstrumentedState state, final SootMethod callContext) {
		assert m == callContext;
		this.enqueueMethod(callContext, bindParameters(m, receiver, arguments, state, Option.none()));
		while(!this.worklist.isEmpty() && !this.timeout) {
			final Pair<SootMethod, Unit> curr = worklist.removeFirst();
			final Unit currUnit = curr.getO2();
			final InstrumentedState currState = this.memo.get(curr.getO1(), currUnit);
			final PatchingChain<Unit> unitChain = getMethodUnits(curr.getO1());
			final InterpResult res;
			try {
				res = interpret(currState, currUnit, new CFGUtil(currUnit, unitChain), curr.getO1());
			} catch(final NoMethodSummary e) {
				continue;
			}
			if(res.methodResult != null) {
				this.registerReturn(curr.getO1(), res.methodResult);
			} else {
				for(final Map.Entry<Unit, InstrumentedState> states : res.successorStates.entrySet()) {
					final Unit target = states.getKey();
					final InstrumentedState updated;
					if(unitChain.follows(currUnit, target)) {
						updated = widenWith(curr.getO1(), target, states.getValue());
					} else {
						updated = joinWith(curr.getO1(), target, states.getValue());
					}
					this.enqueueState(curr.getO1(), target, updated);
				}
			}
		}
		if(timeout) {
			return null;
		}
		return this.resultStates.get(m);
	}
	
	private InstrumentedState joinWith(final SootMethod o1, final Unit target, final InstrumentedState value) {
		if(!this.memo.contains(o1, target)) {
			return value;
		} else {
			return this.stateMonad.join(this.memo.get(o1, target), value);
		}
	}

	private InstrumentedState widenWith(final SootMethod o1, final Unit target, final InstrumentedState value) {
		if(!this.memo.contains(o1, target)) {
			return value;
		} else {
			final InstrumentedState curr = this.memo.get(o1, target);
			return this.stateMonad.widen(curr, stateMonad.join(curr, value));
		}
	}

	private void registerReturn(final SootMethod toRegister, final MethodResult methodResult) {
		if(!this.resultStates.containsKey(toRegister)) {
			this.resultStates.put(toRegister, methodResult);
			this.enqueueCallers(toRegister);
		} else {
			final MethodResult currResult = this.resultStates.get(toRegister);
			final MethodResult joined;
			if(this.callGraph.isWideningPoint(toRegister)) {
				joined = this.methodResult.widen(currResult, this.methodResult.join(currResult, methodResult));
			} else {
				joined = this.methodResult.join(currResult, methodResult);
			}
			if(!this.methodResult.lessEqual(joined, currResult)) {
				this.resultStates.put(toRegister, joined);
				this.enqueueCallers(toRegister);
			}
		}
	}

	private void enqueueCallers(final SootMethod toRegister) {
		for(final ReturnSite rs : this.callers.get(toRegister)) {
			this.worklist.add(new Pair<>(rs.method, rs.unit));
		}
	}

	private InterpResult interpret(final InstrumentedState currState, final Unit currUnit, final CFGUtil chain, final SootMethod context) {
		final Stmt s = (Stmt) currUnit;
		if(s instanceof AssignStmt) {
			final AssignStmt assignStmt = (AssignStmt) s;
			final Value lhs = assignStmt.getLeftOp();
			final Value rhs = assignStmt.getRightOp();
			if(lhs instanceof InstanceFieldRef) {
				final Value baseValue = ((InstanceFieldRef) lhs).getBase();
				final String fieldName = ((InstanceFieldRef) lhs).getField().getName();
				final EvalResult base = eval(currState,  baseValue, context);
				final EvalResult toSet = eval(base.state, rhs, context);
				if(((InstanceFieldRef) lhs).getField().getType() instanceof PrimType) {
					return chain.next(toSet.state);
				}
				return updateHeap(currUnit, chain, fieldName, base.value, toSet);
			} else if(lhs instanceof ArrayRef) {
				final ArrayType type = (ArrayType) ((ArrayRef) lhs).getBase().getType();
				final Value baseValue = ((ArrayRef) lhs).getBase();
				final EvalResult base = eval(currState,  baseValue, context);
				final EvalResult toSet = eval(eval(base.state, ((ArrayRef) lhs).getIndex(), context).state, rhs, context);
				if(type.getElementType() instanceof PrimType) {
					return chain.next(toSet.state);
				}
				return this.updateHeap(currUnit, chain, "*", base.value, toSet);
			} else if(lhs instanceof Local) {
				final EvalResult toSet = eval(currState, rhs, context);
				if(rhs.getType() instanceof PrimType) {
					return chain.next(toSet.state);
				}
				final InstrumentedState newState = stateMonad.mapState(toSet.state, new Function<StupidState, StupidState>() {
					@Override
					public StupidState apply(final StupidState in) {
						return in.put(((Local) lhs).getName(), toSet.value);
					}
				});
				return chain.next(newState);
			} else {
				throw new RuntimeException();
			}
		} else if(s instanceof IfStmt) {
			final Value condition = ((IfStmt) s).getCondition();
			return chain.next(eval(currState, condition, context).state);
		} else if(s instanceof GotoStmt) {
			return chain.next(currState);
		} else if(s instanceof ReturnStmt) {
			final Value op = ((ReturnStmt) s).getOp();
			final EvalResult toReturn = eval(currState, op, context);
			if(op.getType() instanceof PrimType) {
				return chain.returnBottom(toReturn.state, JValue.bottom);
			} else {
				return chain.returnValue(toReturn);
			}
		} else if(s instanceof ReturnVoidStmt) {
			return chain.returnVoid(currState);
		} else if(s instanceof InvokeStmt) {
			if(s.getInvokeExpr() instanceof StaticInvokeExpr) {
				return chain.next(this.evalIntrinsic(currState, (StaticInvokeExpr) s.getInvokeExpr(), context).state);
			}
			final InstanceInvokeExpr iie = (InstanceInvokeExpr) s.getInvokeExpr();
			final MethodResult mr = interpretCall(currState, iie, context);
			return chain.next(mr.getState());
		} else {
			return chain.next(currState);
		}
	}


	private static class NoMethodSummary extends RuntimeException { }

	private MethodResult interpretCall(final InstrumentedState currState, final InstanceInvokeExpr iie, final SootMethod context) {
		if(iie instanceof SpecialInvokeExpr && iie.getMethodRef().getSignature().equals("<java.lang.Object: void <init>()>")) {
			return new MethodResult(currState);
		}
		final EvalResult baseResult = eval(currState, iie.getBase(), context);
		final Object base = baseResult.value;
		final List<Value> argList = iie.getArgs();
		InstrumentedState stateAccum = baseResult.state;
		final List<Object> args = new ArrayList<>();
		for(final Value v : argList) {
			final EvalResult er = this.eval(stateAccum, v, context);
			stateAccum = er.state;
			args.add(er.value);
		}
		return this.callHandler.handleCall(context, iie, base, args, stateAccum).orSome(() -> { throw new NoMethodSummary(); });
	}

	private InterpResult updateHeap(final Unit currUnit, final CFGUtil chain, final String fieldName, final Object value, final EvalResult toSet) {
		final InstrumentedState newState = this.stateMonad.mapState(toSet.state, new Function<StupidState, StupidState>() {
			@Override
			public StupidState apply(final StupidState in) {
				return in.withHeap(new Function<StupidHeap, StupidHeap>() {
					@Override
					public StupidHeap apply(final StupidHeap inHeap) {
						final JValue baseValue = valueMonad.alpha(value);
						return inHeap.set(baseValue, fieldName, toSet.value);
					}
				});
			}
		});
		return chain.next(newState);
	}
	
	private EvalResult eval(final InstrumentedState currState, final Value val, final SootMethod context) {
		if(val instanceof Local) {
			return stateMonad.mapToResult(currState, new PureFunction<StupidState>() {
				@Override
				public Object innerApply(final StupidState in) {
					return in.get(((Local) val).getName());
				}
			});
		} else if(val instanceof InstanceFieldRef && !(val.getType() instanceof PrimType)) {
			final EvalResult baseResult = eval(currState, ((InstanceFieldRef) val).getBase(), context);
			final String fieldName = ((InstanceFieldRef) val).getField().getName();
			final JValue rawBase = valueMonad.alpha(baseResult.value);
			return readHeap(baseResult.state, rawBase, fieldName);
		} else if(val instanceof ArrayRef && !(val.getType() instanceof PrimType)) {
			final EvalResult baseResult = eval(currState, ((ArrayRef)val).getBase(), context);
			final InstrumentedState toReadState = eval(baseResult.state, ((ArrayRef)val).getIndex(), context).state;
			final JValue rawBase = valueMonad.alpha(baseResult.value);
			return readHeap(toReadState, rawBase, "*");
		} else if(val instanceof GNewInvokeExpr) {
			final GNewInvokeExpr newExpr = (GNewInvokeExpr)val;
			final List<Object> args = new ArrayList<>();
			final InstrumentedState argEvalState = fj.data.Stream.iterableStream(newExpr.getArgs()).foldLeft(new F2<InstrumentedState, Value, InstrumentedState>() {
				@Override
				public InstrumentedState f(final InstrumentedState a, final Value b) {
					final EvalResult eval = eval(a, b, context);
					if(modelsType(b.getType())) {
						args.add(eval.value);
					} else {
						args.add(null);
					}
					return eval.state;
				}
			}, currState);
			final Option<EvalResult> alloced = callHandler.allocType(newExpr, argEvalState, args, context);
			return alloced.orSome(() -> { throw new NoMethodSummary(); });
		} else if(val instanceof StaticInvokeExpr) {
			return this.evalIntrinsic(currState, (StaticInvokeExpr) val, context);
		} else if(val instanceof InvokeExpr) {
			assert val instanceof InstanceInvokeExpr : val;
			final MethodResult mr = this.interpretCall(currState, (InstanceInvokeExpr) val, context);
			return new EvalResult(mr);
		} else if(val instanceof NewArrayExpr) {
			return eval(currState, ((NewArrayExpr) val).getSize(), context).map(new F2<InstrumentedState, Object, EvalResult>() {
				@Override
				public EvalResult f(final InstrumentedState state, final Object value) {
					return callHandler.allocArray((NewArrayExpr)val, state, value, null);
				}
			});
		} else if(val instanceof NewMultiArrayExpr) {
			final List<Value> szs = ((NewMultiArrayExpr) val).getSizes();
			final List<Object> values = new ArrayList<>();
			final InstrumentedState state = stateMonad.iterateState(currState, szs, new F2<InstrumentedState, Value, InstrumentedState>() {
				@Override
				public InstrumentedState f(final InstrumentedState a, final Value b) {
					final EvalResult szResult = eval(a, b, context);
					values.add(szResult.value);
					return szResult.state;
				}
			});
			return callHandler.allocArray((NewMultiArrayExpr)val, state, values, null);
		} else if((val instanceof AbstractIntBinopExpr) || (val instanceof AbstractFloatBinopExpr)) {
			final AbstractBinopExpr bop = (AbstractBinopExpr) val;
			final EvalResult result = eval(currState, bop.getOp1(), context);
			return eval(result.state, bop.getOp2(), context);
		} else if(val instanceof CastExpr) {
			return this.eval(currState, ((CastExpr) val).getOp(), context);
		} else if(val instanceof NullConstant) {
			return new EvalResult(currState, valueMonad.lift(JValue.lift(AbstractAddress.NULL_ADDRESS.f())));
		} else {
			return new EvalResult(currState, valueMonad.lift(JValue.bottom));
		}
	}

	private EvalResult readHeap(final InstrumentedState toRead, final JValue rawBase, final String fieldName) {
		return stateMonad.mapToResult(toRead, new PureFunction<StupidState>() {
			@Override
			public Object innerApply(final StupidState in) {
				return in.heap.get(rawBase, fieldName);
			}
		});
	}

	private PatchingChain<Unit> getMethodUnits(final SootMethod m) {
		final Body body = BodyManager.retrieveBody(m);
		final PatchingChain<Unit> units = body.getUnits();
		return units;
	}

	@Override
	public List<Injectable> monadUsers() {
		final ArrayList<Injectable> toReturn = new ArrayList<>();
		toReturn.add(AbstractObject.monadHolder);
		toReturn.add(StupidState.vmHolder);
		toReturn.add(StupidHeap.injector);
		return toReturn;
	}

	@Override
	public boolean modelsType(final Type type) {
		return !(type instanceof PrimType);
	}

	@Override
	public ContextManager<SootMethod, JValue, StupidState, StupidHeap> contextManager() {
		return new EntryPointContextManager<SootMethod, JValue, StupidState, StupidHeap>() {
			@Override public SootMethod initialAllocationContext(final SootMethod mainMethod) {
				return null;
			}

			@Override public SootMethod initialContext(final SootMethod m) {
				return m;
			}

			@Override
			public void inject(final Monads<JValue, StupidState> monads) { }

			@Override
			public SootMethod contextForAllocation(final Either<Pair<SootMethod, InstrumentedState>, ExecutionState<StupidHeap, SootMethod>> allocContext, final Value allocExpr) {
				return null;
			}
			@Override
			public SootMethod contextForCall(final Either<Pair<SootMethod, InstrumentedState>, ExecutionState<StupidHeap, SootMethod>> callingContext,
					final SootMethod targetMethod, final Object base, final List<Object> values, final InvokeExpr callExpr) {
				return targetMethod;
			}
			
		};
	}

	/*
	 * Couldn't we do this automatically? The mapping of receiver is a little weird I guess. We could finesse that in the heap implementation,
	 * but it's not terribly general
	 */
	@Override
	public Option<MethodResult> handleCall(final SootMethod callee, final JValue receiver, final List<Object> arguments,
			final InstrumentedState callerState, final SootMethod calleeContext, final BoundaryInformation<SootMethod> rootContext) {
		final ReturnSite continuation = getContinuation(rootContext);
		final InstrumentedState calleeState = bindParameters(callee, receiver, arguments, callerState, Option.some(continuation));
		if(calleeState == null) {
			return null;
		}
		return this.retrieveSummary(callee, calleeState, continuation);
	}

	protected ReturnSite getContinuation(final BoundaryInformation<SootMethod> rootContext) {
		return new ReturnSite(rootContext.rootContext, BodyManager.getHostUnit(rootContext.rootInvoke));
	}

	public InstrumentedState bindParameters(final SootMethod callee, final JValue receiver, final List<Object> arguments, final InstrumentedState callerState, final Option<ReturnSite> continuation) {
		final Body body = BodyManager.retrieveBody(callee);
		final RefType receiverType = callee.getDeclaringClass().getType();
		final InstrumentedState calleeState = stateMonad.mapState(callerState, new Function<StupidState, StupidState>() {
			@Override
			public StupidState apply(final StupidState _in) {
				final Map<String, Object> params = new HashMap<>();
				final AbstractAddress receiverAddr = AbstractAddress.getAddress(receiverType);
				if(!receiver.compatibleWith(receiverAddr)) {
					return null;
				}
				params.put(body.getThisLocal().getName(), valueMonad.lift(receiver.narrowToAddress(receiverAddr)));
				final List<Local> locals = body.getParameterLocals();
				for(int i = 0; i < locals.size(); i++) {
					if(arguments.get(i) == null) {
						continue;
					}
					params.put(locals.get(i).getName(), arguments.get(i));
				}
				return StupidState.lift(params, _in.heap, continuationManager.initContinuation(continuation));
			}
		});
		return calleeState;
	}

	@Override
	public PrimitiveOperations<JValue> primitiveOperations() {
		return new DefaultPrimitiveOps<JValue>() {
			@Override protected JValue binop(final JValue a, final JValue b) {
				return JValue.bottom;
			}
		};
	}

	@Override
	public Pair<StupidHeap, JValue> abstractObjectAlloc(final RefType t, final StupidHeap inputHeap, final Value allocationExpr, final SootMethod allocationContext) {
		final SootClass loadClass = BodyManager.loadClass(t);
		return inputHeap.alloc(loadClass.getType());
	}

	@Override
	public Pair<StupidHeap, JValue> abstractArrayAlloc(final ArrayType t, final StupidHeap inputHeap,
			final Value allocationExpr, final List<JValue> sizes, final SootMethod context) {
		return inputHeap.alloc(t, sizes.size());
	}
	
	public static void reset() {
		AbstractAddress.reset();
	}

	@Override
	public void instrument(final InstrumentationManager<JValue, StupidHeap, StupidState> instManager) { }

	@Override
	public ObjectOperations<JValue, StupidHeap> objectOperations() {
		return new ObjectOperations<JValue, StupidHeap>() {
			
			@Override
			public StupidHeap writeArray(final StupidHeap h, final JValue basePointer, final JValue index, final Object value, final ArrayRef context) {
				if(isPrimArray(context)) {
					return h;
				}
				return h.set(basePointer, "*", value);
			}
			
			@Override
			public Option<Object> readArray(final StupidHeap h, final JValue basePointer, final JValue index, final ArrayRef context) {
				if(isPrimArray(context)) {
					return Option.some(valueMonad.lift(JValue.bottom));
				} else {
					return Option.some(h.get(basePointer, "*"));
				}
			}

			private boolean isPrimArray(final ArrayRef context) {
				return ((ArrayType)context.getBase().getType()).getElementType() instanceof PrimType;
			}

			@Override public ObjectIdentityResult isNull(final JValue a) {
				return a.isNull();
			}

			@Override
			public JValue downcast(final JValue v, final Type t) {
				return v;
			}
			
			@Override
			public JValue arrayLength(final JValue basePointer, final StupidHeap h) {
				return JValue.bottom;
			}

			@Override
			public Stream<Type> possibleTypes(final JValue a) {
				return a.addressSet.toStream().map(t -> t.t);
			}
		};
	}

	private final SimpleIntrinsicInterpreter<StupidState, SootMethod, JValue> intrInterp = new SimpleIntrinsicInterpreter<StupidState, SootMethod, JValue>(() -> this.stateMonad, () -> this.valueMonad, () -> this.callHandler) {
		@Override protected JValue nondetInt() {
			return JValue.bottom;
		}

		@Override protected EvalResult handleFailure(final InstrumentedState currState, final StaticInvokeExpr sie) {
			return this.handleMissingSummary();
		}

		@Override protected Object readAllFields(final StupidState state, final JValue arrayRef) {
			return state.heap.get(arrayRef, "*");
		}

		@Override protected EvalResult handleMissingSummary() {
			throw new NoMethodSummary();
		}

		@Override protected EvalResult eval(final InstrumentedState currState, final Value arg, final SootMethod sootMethod) {
			return BasicInterpreter.this.eval(currState, arg, sootMethod);
		}
	};
	
	private EvalResult evalIntrinsic(final InstrumentedState currState, final StaticInvokeExpr sie, final SootMethod context) {
		return intrInterp.handleIntrinsic(currState, sie, context);
	}

	@Override
	public Option<Pair<StupidHeap, JValue>> allocateUnknownObject(final StupidHeap inputHeap, final Value allocationExpr, final SootMethod allocationContext,
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
		return Option.some(inputHeap.allocUnknownType(upperBound));
	}
}
