package edu.washington.cse.concerto.interpreter.ai.instantiation.optiflow;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import edu.washington.cse.concerto.Function;
import edu.washington.cse.concerto.instrumentation.InstrumentationManager;
import edu.washington.cse.concerto.instrumentation.actions.MethodCallAction;
import edu.washington.cse.concerto.instrumentation.actions.MultiValueReplacement;
import edu.washington.cse.concerto.interpreter.BodyManager;
import edu.washington.cse.concerto.interpreter.ai.AbstractInterpretation;
import edu.washington.cse.concerto.interpreter.ai.AbstractionFunction;
import edu.washington.cse.concerto.interpreter.ai.CallHandler;
import edu.washington.cse.concerto.interpreter.ai.CompareResult;
import edu.washington.cse.concerto.interpreter.ai.ContextManager;
import edu.washington.cse.concerto.interpreter.ai.ContinuationManager;
import edu.washington.cse.concerto.interpreter.ai.EntryPointContextManager;
import edu.washington.cse.concerto.interpreter.ai.EvalResult;
import edu.washington.cse.concerto.interpreter.ai.HeapMutator;
import edu.washington.cse.concerto.interpreter.ai.HeapReader;
import edu.washington.cse.concerto.interpreter.ai.IntrinsicHandler;
import edu.washington.cse.concerto.interpreter.ai.Lattices;
import edu.washington.cse.concerto.interpreter.ai.MethodResult;
import edu.washington.cse.concerto.interpreter.ai.MethodResultMonad;
import edu.washington.cse.concerto.interpreter.ai.MonadicLattice;
import edu.washington.cse.concerto.interpreter.ai.MonoidalValueMapper;
import edu.washington.cse.concerto.interpreter.ai.ObjectOperations;
import edu.washington.cse.concerto.interpreter.ai.PureFunction;
import edu.washington.cse.concerto.interpreter.ai.RecursiveTransformer;
import edu.washington.cse.concerto.interpreter.ai.ReflectiveOperationContext;
import edu.washington.cse.concerto.interpreter.ai.ResultCollectingAbstractInterpretation;
import edu.washington.cse.concerto.interpreter.ai.ResultStream;
import edu.washington.cse.concerto.interpreter.ai.StandardOutResultStream;
import edu.washington.cse.concerto.interpreter.ai.State;
import edu.washington.cse.concerto.interpreter.ai.StateMonad;
import edu.washington.cse.concerto.interpreter.ai.StateUpdater;
import edu.washington.cse.concerto.interpreter.ai.StaticCallGraph;
import edu.washington.cse.concerto.interpreter.ai.UnitOrd;
import edu.washington.cse.concerto.interpreter.ai.UpdateResult;
import edu.washington.cse.concerto.interpreter.ai.ValueMapper;
import edu.washington.cse.concerto.interpreter.ai.ValueMonad;
import edu.washington.cse.concerto.interpreter.ai.ValueMonadLattice;
import edu.washington.cse.concerto.interpreter.ai.ValueStateTransformer;
import edu.washington.cse.concerto.interpreter.ai.binop.DefaultPrimitiveOps;
import edu.washington.cse.concerto.interpreter.ai.binop.ObjectIdentityResult;
import edu.washington.cse.concerto.interpreter.ai.binop.PrimitiveOperations;
import edu.washington.cse.concerto.interpreter.ai.injection.Injectable;
import edu.washington.cse.concerto.interpreter.ai.instantiation.CFGUtil;
import edu.washington.cse.concerto.interpreter.ai.instantiation.InterpResult;
import edu.washington.cse.concerto.interpreter.ai.instantiation.OrderingUtil;
import edu.washington.cse.concerto.interpreter.ai.instantiation.pta.AbstractAddress;
import edu.washington.cse.concerto.interpreter.ai.instantiation.pta.AbstractObject;
import edu.washington.cse.concerto.interpreter.ai.instantiation.pta.JValue;
import edu.washington.cse.concerto.interpreter.ai.instantiation.pta.SimpleIntrinsicInterpreter;
import edu.washington.cse.concerto.interpreter.ai.instantiation.pta.StupidHeap;
import edu.washington.cse.concerto.interpreter.heap.Heap;
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
import fj.P;
import fj.P2;
import fj.P3;
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
import soot.grimp.internal.GNewInvokeExpr;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.Constant;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.LengthExpr;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.NullConstant;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.internal.AbstractBinopExpr;
import soot.toolkits.scalar.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class OptimisticInformationFlow implements AbstractInterpretation<PV, StupidHeapWithReturnSlot, LocalMap, SootMethod>, ResultCollectingAbstractInterpretation {
	private static final Monoid<Boolean> BOOLEAN_MONOID = Monoid.monoid(new F2<Boolean, Boolean, Boolean>() {
		@Override
		public Boolean f(final Boolean a, final Boolean b) {
			return a || b;
		}
	}, false);
	private StateMonad<LocalMap, PV> stateMonad;
	private ValueMonad<PV> valueMonad;
	private CallHandler<SootMethod> callHandler;
	private final Table<SootMethod, Unit, InstrumentedState> memo = HashBasedTable.create();
	private final LinkedList<Pair<SootMethod, Unit>> worklist = new LinkedList<>();
	private final java.util.Set<Pair<SootMethod, Unit>> workSet = new java.util.HashSet<>();
	private final Table<SootMethod, SootMethod, MethodResult> methodSummaries = HashBasedTable.create();
	private final StaticCallGraph<Pair<SootMethod, SootMethod>> callGraph = new StaticCallGraph<>();

	// K.1 is context, K.2 is the callee
	private final Multimap<Pair<SootMethod, SootMethod>, Pair<SootMethod, Unit>> callees = HashMultimap.create();
	private MethodResultMonad<PV, LocalMap> methodResultMonad;
	private ContinuationManager<Pair<SootMethod, Unit>, Pair<SootMethod, SootMethod>, LocalMap> continuationManager;
	private ResultStream resultStream = new StandardOutResultStream();
	private final java.util.Set<TaintFlow> foundFlows = new HashSet<>();
	public boolean timeout = false;

	@Override
	public void inject(final Monads<PV, LocalMap> monads) {
		this.valueMonad = monads.valueMonad;
		this.stateMonad = monads.stateMonad;
		this.methodResultMonad = monads.methodResultMonad;
		this.continuationManager = new ContinuationManager<>(continuationOrd, stateMonad);
	}

	@Override public void interrupt() {
		this.timeout = true;
	}

	private static final Ord<SootMethod> methodOrd = OrderingUtil.stringBasedOrdering(new F<SootMethod, String>() {
		@Override
		public String f(final SootMethod a) {
			return a.getSignature();
		}
	});

	public static final Ord<Pair<SootMethod, Unit>> continuationOrd = Ord.ord((a, b) -> {
		Ordering firstOrd = methodOrd.compare(a.getO1(), b.getO1());
		if(firstOrd != Ordering.EQ) {
			return firstOrd;
		}
		return UnitOrd.unitOrdering.compare(a.getO2(), b.getO2());
	});

	@Override public State<StupidHeapWithReturnSlot, LocalMap> stateTransformer() {
		return new State<StupidHeapWithReturnSlot, LocalMap>() {
			@Override
			public StupidHeapWithReturnSlot project(final LocalMap state) {
				return state.heap;
			}

			@Override
			public LocalMap inject(final LocalMap state, final StupidHeapWithReturnSlot heap) {
				return state.withHeap(heap);
			}
			
			@Override
			public LocalMap emptyState() {
				return new LocalMap();
			}
		};
	}

	@Override
	public AbstractionFunction<PV> alpha() {
		return new AbstractionFunction<PV>() {
			@Override
			public PV lift(final IValue v) {
				if(v.getTag() == IValue.RuntimeTag.NULL) {
					return JValue.lift(AbstractAddress.NULL_ADDRESS.f());
				}
				return PV.bottom;
			}
		};
	}

	@Override
	public void setCallHandler(final CallHandler<SootMethod> ch) {
		this.callHandler = ch;
	}

	@Override
	public Lattices<PV, StupidHeapWithReturnSlot, LocalMap> lattices() {
		final ValueMonadLattice<StupidHeapWithReturnSlot> heapLattice = StupidHeapWithReturnSlot.lattice;
		return new Lattices<PV, StupidHeapWithReturnSlot, LocalMap>() {
			@Override
			public MonadicLattice<PV, PV, LocalMap> valueLattice() {
				return PV.lattice;
			}
			
			@Override
			public MonadicLattice<LocalMap, PV, LocalMap> stateLattice() {
				return LocalMap.lattice;
			}
			
			@Override
			public ValueMonadLattice<StupidHeapWithReturnSlot> heapLattice() {
				return heapLattice;
			}
		};
	}

	@Override
	public MethodResult interpretToFixpoint(final SootMethod m, final PV receiver, final List<Object> arguments,
			final InstrumentedState calleeState, final SootMethod entryContext) {
		this.registerStart(m, calleeState, receiver, arguments, entryContext);
		computeFixpoint();
		if(timeout) {
			return null;
		}
		return this.getReturnSummary(m, entryContext);
	}

	private void computeFixpoint() {
		while(!worklist.isEmpty() && !this.timeout) {
			final Pair<SootMethod, Unit> currItem = worklist.removeFirst();
			workSet.remove(currItem);
			final SootMethod context = currItem.getO1();
			final Unit currUnit = currItem.getO2();
			final InstrumentedState currState = this.memo.get(context, currUnit);
			final PatchingChain<Unit> methodUnits = getBodyForUnit(currUnit).getUnits();
			final CFGUtil cfg = new CFGUtil(currUnit, methodUnits);
			final InterpResult res;
			try {
				res = this.interpret(currUnit, currState, context, cfg);
			} catch(final NoSuchSummary _e) {
				continue;
			}
			if(res.methodResult != null) {
				final SootMethod currMethod = BodyManager.getHostMethod(currUnit);
					final MethodResult mRes;
				if(currMethod.isConstructor()) {
					assert res.methodResult.getReturnValue() == null;
					mRes = new MethodResult(
						stateMonad.mapState(res.methodResult.getState(), st -> {
							final PV p = valueMonad.alpha(st.localTable.get(currMethod.getActiveBody().getThisLocal()).some());
							final PathTree t;
							if(p instanceof PathTree) {
								t = (PathTree) p;
							} else if(p instanceof JValue) {
								t = PathTree.bottom;
							} else if(p instanceof Product) {
								t = ((Product) p).p2;
							} else {
								throw new RuntimeException();
							}
							if(t.isBottom()) {
								return st;
							} else {
								return st.withHeap(st.heap.withReturnSlot(t));
							}
						}),
						res.methodResult.getReturnValue()
					);
				} else {
					mRes = res.methodResult;
				}
				this.registerReturn(mRes, currUnit, context);
			} else {
				for(final Map.Entry<Unit, InstrumentedState> kv : res.successorStates.entrySet()) {
					final Unit target = kv.getKey();
					if(methodUnits.follows(currUnit, target)) {
						this.widenAndEnqueue(kv.getValue(), kv.getKey(), context);
					} else {
						this.joinAndEnqueue(kv.getValue(), kv.getKey(), context);
					}
				}
			}
		}
	}
	
	public static StateUpdater<LocalMap> pure = new StateUpdater<LocalMap>() {
		@Override
		public LocalMap updateState(final LocalMap state, final Object value) {
			return state;
		}
	};

	
	private InterpResult interpret(final Unit currUnit, final InstrumentedState currState, final SootMethod context, final CFGUtil cfg) {
		if(currUnit instanceof AssignStmt) {
			final Value lhs = ((AssignStmt) currUnit).getLeftOp();
			final Value rhsExpr = ((AssignStmt) currUnit).getRightOp();
			if(lhs instanceof Local) {
				final EvalResult rhsResult = this.eval(rhsExpr, currState, context);
				return cfg.next(stateMonad.mapState(rhsResult.state, new Function<LocalMap, LocalMap>() {
					@Override
					public LocalMap apply(final LocalMap in) {
						return in.set((Local)lhs, rhsResult.value);
					}
				}));
			} else if(lhs instanceof ArrayRef) {
				final P3<Object, InstrumentedState, AP> lhsInfo = this.evalAP(((ArrayRef) lhs).getBase(), currState, context);
				final InstrumentedState indexEvalState = this.eval(((ArrayRef) lhs).getIndex(), lhsInfo._2(), context).state;
				final EvalResult rhs = this.eval(rhsExpr, indexEvalState, context);
				final Pair<Object, PathTree> rhsComp = splitValue(rhs.state, rhs.value);
				final InstrumentedState heapUpdate = stateMonad.updateValue(rhs.state, lhsInfo._1(), new ValueStateTransformer<PV, LocalMap>() {
					@Override
					public UpdateResult<LocalMap, IValue> mapConcrete(final IValue v, final LocalMap state, final Heap h, final HeapMutator heapAccessor,
							final RecursiveTransformer<LocalMap, UpdateResult<LocalMap, ?>> recursor) {
						return heapAccessor.updateNondetIndex(state, h, v, rhs.value);
					}
					
					@Override
					public UpdateResult<LocalMap, PV> mapAbstract(final PV val, final LocalMap state, final Heap h,
							final RecursiveTransformer<LocalMap, UpdateResult<LocalMap, ?>> recursor) {
						if(rhsComp.getO1() == null) {
							return new UpdateResult<>(state, h, val);
						}
						final StupidHeapWithReturnSlot newHeap = objectOperations().writeArray(state.heap, val, PV.bottom, rhsComp.getO1(), (ArrayRef) lhs);
						return new UpdateResult<>(state.withHeap(newHeap), h, val);
					}
				}, OptimisticInformationFlow.pure);
				final AP toSetPath = lhsInfo._3().appendArray();
				final PathTree taintState = rhsComp.getO2();
				final InstrumentedState toReturn;
				try {
					toReturn = updateTaintState(heapUpdate, toSetPath, taintState);
				} catch(final Throwable e) {
					throw e;
				}
				return cfg.next(toReturn);
			} else {
				assert lhs instanceof InstanceFieldRef;
				final InstanceFieldRef iRef = (InstanceFieldRef) lhs;
				final P3<Object, InstrumentedState, AP> lhsInfo = this.evalAP(iRef.getBase(), currState, context);
				
				final EvalResult rhs = this.eval(rhsExpr, lhsInfo._2(), context);
				final Pair<Object, PathTree> rhsComp = this.splitValue(rhs.state, rhs.value);
				final InstrumentedState s1 = stateMonad.updateValue(rhs.state, lhsInfo._1(), new ValueStateTransformer<PV, LocalMap>() {
					
					@Override
					public UpdateResult<LocalMap, IValue> mapConcrete(final IValue v, final LocalMap state, final Heap h, final HeapMutator heapAccessor,
							final RecursiveTransformer<LocalMap, UpdateResult<LocalMap, ?>> recursor) {
						return new UpdateResult<>(state, h, v);
					}
					
					@Override
					public UpdateResult<LocalMap, PV> mapAbstract(final PV val, final LocalMap state, final Heap h, 
							final RecursiveTransformer<LocalMap, UpdateResult<LocalMap, ?>> recursor) {
						if(rhsComp.getO1() == null) {
							return new UpdateResult<>(state, h, val);
						}
						final StupidHeapWithReturnSlot update = state.heap.set(projectJVal(val), iRef.getFieldRef().getSignature(), rhsComp.getO1());
						return new UpdateResult<>(state.withHeap(update), h, val);
					}
				}, OptimisticInformationFlow.pure);
				final InstrumentedState toReturn = updateTaintState(s1, lhsInfo._3().appendField(iRef.getFieldRef()), rhsComp.getO2());
				return cfg.next(toReturn);
			}
		} else if(currUnit instanceof InvokeStmt) {
			final InvokeExpr invokeExpr = ((InvokeStmt) currUnit).getInvokeExpr();
			if(invokeExpr instanceof StaticInvokeExpr) {
				return cfg.next(this.handleStaticCall((StaticInvokeExpr) invokeExpr, currState, context));
			}
			final InstanceInvokeExpr iie = (InstanceInvokeExpr) invokeExpr;
			return someOrThrow(this.interpretCall(currState, iie, context).map(cfg::next));
		} else if(currUnit instanceof IfStmt || currUnit instanceof GotoStmt) {
			return cfg.next(currState);
		} else if(currUnit instanceof ReturnStmt) {
			final EvalResult res = this.eval(((ReturnStmt) currUnit).getOp(), currState, context);
			return cfg.returnValue(res);
		} else if(currUnit instanceof ReturnVoidStmt) {
			return cfg.returnVoid(currState);
		} else {
			return cfg.next(currState);
		}
	}
	
	private <R> R someOrThrow(final Option<R> o) {
		return o.orSome(() -> { throw new NoSuchSummary(); });
	}
	
	private JValue projectJVal(final PV val) {
		final JValue toSet;
		if(val instanceof JValue) {
			toSet = (JValue) val;
		} else if(val instanceof Product) {
			toSet = ((Product) val).p1;
		} else {
			throw new RuntimeException();
		}
		return toSet;
	}

	private InstrumentedState updateTaintState(final InstrumentedState s1, final AP toSet, final PathTree taintState) {
		final Object root = stateMonad.map(s1, in -> in.get(toSet.getBase()));
		final InstrumentedState toReturn = stateMonad.updateValue(s1, root, new ValueStateTransformer<PV, LocalMap>() {
			
			@Override
			public UpdateResult<LocalMap, IValue> mapConcrete(final IValue v, final LocalMap state, final Heap h, final HeapMutator heapAccessor,
					final RecursiveTransformer<LocalMap, UpdateResult<LocalMap, ?>> recursor) {
				return new UpdateResult<>(state, h, v);
			}
			
			@Override
			public UpdateResult<LocalMap, PV> mapAbstract(final PV val, final LocalMap state, final Heap h,
					final RecursiveTransformer<LocalMap, UpdateResult<LocalMap, ?>> recursor) {
				final PV toReturn;
				final PathTree newRoot;
				final JValue otherComp;
				if(val instanceof JValue) {
					newRoot = taintState != null ? PathTree.lift(toSet.fields, taintState) : PathTree.bottom;
					otherComp = (JValue) val;
				} else if(val instanceof PathTree) {
					newRoot = doUpdate((PathTree) val, toSet.fields, taintState);
					otherComp = JValue.bottom;
				} else if(val instanceof Product) {
					newRoot = doUpdate(((Product) val).p2, toSet.fields, taintState);
					otherComp = ((Product) val).p1;
				} else {
					throw new RuntimeException();
				}
				if(otherComp == null) {
					toReturn = newRoot;
				} else if(newRoot.isBottom()) {
					toReturn = otherComp;
				} else {
					toReturn = new Product(otherComp, newRoot);
				}
				return new UpdateResult<>(state, h, toReturn);
			}
			
			private PathTree doUpdate(final PathTree toUpdate, final Seq<String> fields, final PathTree taint) {
				if(taint != null) {
					return toUpdate.updatePath(fields, taint);
				} else {
					return toUpdate.killPath(fields);
				}
			}
		}, (state, value) -> state.set(toSet.getBase(), value));
		return toReturn;
	}

	private Pair<Object, PathTree> splitValue(final InstrumentedState toSplitState, final Object toSplit) {
		return stateMonad.mapValue(toSplitState, toSplit, new ValueMapper<PV, LocalMap, Pair<Object, PathTree>>() {
			@Override
			public Pair<Object, PathTree> mapAbstract(final PV val, final LocalMap state, final Heap h,
					final RecursiveTransformer<LocalMap, Pair<Object, PathTree>> recursor) {
				if(val instanceof PathTree) {
					return new Pair<>(null, (PathTree) val);
				} else if(val instanceof JValue) {
					return new Pair<>(valueMonad.lift(val), null);
				} else {
					final Product p = (Product) val;
					return new Pair<>(valueMonad.lift(p.p1), p.p2);
				}
			}

			@Override
			public Pair<Object, PathTree> mapConcrete(final IValue v, final LocalMap state, final Heap h, final HeapReader<LocalMap, PV> heapAccessor,
					final RecursiveTransformer<LocalMap, Pair<Object, PathTree>> recursor) {
				return new Pair<>(valueMonad.lift(v), null);
			}

			@Override
			public Pair<Object, PathTree> merge(final Pair<Object, PathTree> v1, final Pair<Object, PathTree> v2) {
				final PathTree t;
				final Object i;
				if(v1.getO2() == null) {
					t = v2.getO2();
				} else if(v2.getO2() == null) {
					t = v1.getO2();
				} else {
					t = PathTree.lattice.join(v1.getO2(), v2.getO2());
				}
				if(v1.getO1() == null) {
					i = v2.getO1();
				} else if(v2.getO1() == null) {
					i = v1.getO1();
				} else {
					i = valueMonad.join(v1.getO1(), v2.getO1());
				}
				return new Pair<>(i, t);
			}
		});
	}

	public Option<MethodResult> interpretCall(final InstrumentedState currState, final InstanceInvokeExpr iie, final SootMethod context) {
		final EvalResult r = this.eval(iie.getBase(), currState, context);
		final Pair<InstrumentedState, List<Object>> argResult = this.interpretList(r.state, iie.getArgs(), context);
		return this.callHandler.handleCall(context, iie, r.value, argResult.getO2(), argResult.getO1());
	}
		
	private P3<Object, InstrumentedState, AP> evalAP(final Value leftOp, final InstrumentedState currState, final SootMethod ctxt) {
		if(leftOp instanceof InstanceFieldRef) {
			final P3<Object, InstrumentedState, AP> ret = this.evalAP(((InstanceFieldRef) leftOp).getBase(), currState, ctxt);
			return ret.map3(new F<AP, AP>() {
				@Override
				public AP f(final AP a) {
					return a.appendField(((InstanceFieldRef) leftOp).getFieldRef());
				}
			});
		} else if(leftOp instanceof Local) {
			final EvalResult res = this.eval(leftOp, currState, ctxt);
			return P.p(res.value, res.state, AP.ofLocal((Local)leftOp));
		} else if(leftOp instanceof ArrayRef) {
			final P3<Object, InstrumentedState, AP> ret = this.evalAP(((ArrayRef) leftOp).getBase(), currState, ctxt);
			return ret.map2(new F<InstrumentedState, InstrumentedState>() {
				@Override
				public InstrumentedState f(final InstrumentedState a) {
					return eval(((ArrayRef) leftOp).getIndex(), a, ctxt).state;
				}
			}).map3(new F<AP, AP>() {
				@Override
				public AP f(final AP a) {
					return a.appendArray();
				}
			});
		} else if(leftOp instanceof InvokeExpr) {
			return someOrThrow(this.interpretCall(currState, (InstanceInvokeExpr) leftOp, ctxt).map(res -> 
				P.p(res.getReturnValue(), res.getState(), AP.ofCall((InvokeExpr)leftOp))
			));
		} else {
			throw new RuntimeException("Unsupported lhs form: " + leftOp);
		}
	}

	private EvalResult eval(final Value op, final InstrumentedState currState, final SootMethod context) {
		if(op instanceof GNewInvokeExpr) {
			final Pair<InstrumentedState, List<Object>> argResults = interpretList(currState, ((GNewInvokeExpr) op).getArgs(), context);
			return applyConstructorSlot(someOrThrow(this.callHandler.allocType((GNewInvokeExpr) op, argResults.getO1(), argResults.getO2(), context)));
		} else if(op instanceof InvokeExpr) {
			if(op instanceof StaticInvokeExpr) {
				return new EvalResult(handleStaticCall((StaticInvokeExpr) op, currState, context));
			}
			return someOrThrow(this.interpretCall(currState, (InstanceInvokeExpr) op, context).map(EvalResult::new));
		} else if(op instanceof ArrayRef) {
			final EvalResult baseResult = this.eval(((ArrayRef) op).getBase(), currState, context);
			final InstrumentedState refState = this.eval(((ArrayRef) op).getIndex(), baseResult.state, context).state;
			final Object readValue = stateMonad.mapValue(refState, baseResult.value, new ValueMapper<PV, LocalMap, Object>() {
				@Override
				public Object mapAbstract(final PV val, final LocalMap state, final Heap h, final RecursiveTransformer<LocalMap, Object> recursor) {
					return objectOperations().readArray(state.heap, val, PV.bottom, (ArrayRef) op).some();
				}

				@Override
				public Object mapConcrete(final IValue v, final LocalMap state, final Heap h, final HeapReader<LocalMap, PV> heapAccessor,
						final RecursiveTransformer<LocalMap, Object> recursor) {
					return heapAccessor.readNondetIndex(h, v).value;
				}

				@Override
				public Object merge(final Object v1, final Object v2) {
					return valueMonad.join(v1, v2);
				}
			});
			return new EvalResult(refState, readValue);
		} else if(op instanceof Local) {
			return stateMonad.mapToResult(currState, new PureFunction<LocalMap>() {
				@Override
				protected Object innerApply(final LocalMap in) {
					return in.lookup((Local)op);
				}
			});
		} else if(op instanceof InstanceFieldRef) {
			final EvalResult baseResult = eval(((InstanceFieldRef) op).getBase(), currState, context);
			final Object readValue = stateMonad.mapValue(baseResult.state, baseResult.value, new ValueMapper<PV, LocalMap, Object>() {
				@Override
				public Object mapAbstract(final PV val, final LocalMap state, final Heap h, final RecursiveTransformer<LocalMap, Object> recursor) {
					if(val instanceof PathTree) {
						final PathTree pt = (PathTree) val;
						return valueMonad.lift(pt.readField(((InstanceFieldRef) op).getFieldRef()));
					} else if(val instanceof JValue) {
						return state.heap.wrapped.get((JValue) val, ((InstanceFieldRef) op).getFieldRef().getSignature());
					} else {
						assert val instanceof Product;
						final Object r1 = state.heap.wrapped.get(((Product) val).p1, ((InstanceFieldRef) op).getFieldRef().getSignature());
						final Object r2 = valueMonad.lift(((Product)val).p2.readField(((InstanceFieldRef) op).getFieldRef()));
						return valueMonad.join(r1, r2);
					}
				}

				@Override
				public Object mapConcrete(final IValue v, final LocalMap state, final Heap h, final HeapReader<LocalMap, PV> heapAccessor,
						final RecursiveTransformer<LocalMap, Object> recursor) {
					return valueMonad.lift(PV.bottom);
				}

				@Override
				public Object merge(final Object v1, final Object v2) {
					return valueMonad.join(v1, v2);
				}
			});
			return new EvalResult(baseResult.state, readValue);
		} else if(op instanceof AbstractBinopExpr) {
			final EvalResult op1 = this.eval(((AbstractBinopExpr) op).getOp1(), currState, context);
			final EvalResult op2 = this.eval(((AbstractBinopExpr) op).getOp2(), op1.state, context);
			final InstrumentedState resultState = op2.state;
			final Object resultValue = valueMonad.join(valueMonad.alpha(op1.value), valueMonad.alpha(op2.value));
			return new EvalResult(resultState, resultValue);
		} else if(op instanceof NullConstant) {
			return new EvalResult(currState, valueMonad.lift(JValue.lift(AbstractAddress.NULL_ADDRESS.f())));
		} else if(op instanceof Constant) {
			return new EvalResult(currState, PV.bottom);
		} else if(op instanceof NewArrayExpr) {
			final EvalResult szResult = this.eval(((NewArrayExpr) op).getSize(), currState, context);
			return this.callHandler.allocArray((NewArrayExpr) op, szResult.state, szResult.value, context);
		} else if(op instanceof NewMultiArrayExpr) {
			final Pair<InstrumentedState, List<Object>> szsResult = interpretList(currState, ((NewMultiArrayExpr) op).getSizes(), context);
			return this.callHandler.allocArray((NewMultiArrayExpr) op, szsResult.getO1(), szsResult.getO2(), context);
		} else if(op instanceof CastExpr) {
			return this.eval(((CastExpr) op).getOp(), currState, context);
		} else if(op instanceof LengthExpr) {
			final EvalResult eval = this.eval(((LengthExpr) op).getOp(), currState, context);
			return new EvalResult(eval.state, valueMonad.lift(PV.bottom));
		} else {
			throw new RuntimeException("Can't handle expr: " + op);
		}
	}

	private EvalResult applyConstructorSlot(final EvalResult allocResult) {
		return allocResult.map((inst, retVal) ->
				stateMonad.mapToResult(inst, st -> {
					final PathTree returned = st.heap.returnSlot;
					if(returned.isBottom()) {
						return new Pair<>(st, retVal);
					} else {
						return new Pair<>(st.withHeap(st.heap.clearSlot()), valueMonad.join(retVal, valueMonad.lift(returned)));
					}
				})
		);
	}

	private final IntrinsicHandler<LocalMap, SootMethod> interp = new SimpleIntrinsicInterpreter<LocalMap, SootMethod, PV>(() -> stateMonad, () -> valueMonad, () -> callHandler) {
		@Override protected EvalResult eval(final InstrumentedState currState, final Value arg, final SootMethod sootMethod) {
			return OptimisticInformationFlow.this.eval(arg, currState, sootMethod);
		}

		@Override protected PV nondetInt() {
			return JValue.bottom;
		}

		@Override protected Object readAllFields(final LocalMap state, final PV arrayRef) {
			return objectOperations().readArray(state.heap, arrayRef, PV.bottom, null);
		}

		@Override protected EvalResult handleMissingSummary() {
			throw new NoSuchSummary();
		}

		@Override protected EvalResult handleFailure(final InstrumentedState currState, final StaticInvokeExpr sie) {
			throw new NoSuchSummary();
		}
	};

	private MethodResult handleStaticCall(final StaticInvokeExpr op, final InstrumentedState currState, final SootMethod context) {
		final String mName = op.getMethodRef().name();
		if(mName.equals("source")) {
			return new MethodResult(currState, valueMonad.lift(new PathTree(TaintFlag.Taint)));
		} else if(mName.equals("sink")) {
			final EvalResult arg = this.eval(op.getArg(0), currState, context);
			final boolean hasTainted = stateMonad.mapValue(currState, arg.value, taintChecker);
			if(hasTainted) {
				this.foundFlows.add(new TaintFlow(arg.value, BodyManager.getHostMethod(op), BodyManager.getHostUnit(op), op.getMethodRef()));
			}
			return new MethodResult(arg.state, null);
		} else {
			return this.interp.handleIntrinsic(currState, op, context).mapTo(MethodResult::new);
		}
	}
	
	private Pair<InstrumentedState, List<Object>> interpretList(final InstrumentedState start,  final List<Value> expr, final SootMethod context) {
		InstrumentedState accum = start;
		final List<Object> argValues = new ArrayList<>();
		for(final Value arg : expr) {
			final EvalResult t = this.eval(arg, accum, context);
			if(t == null) {
				throw new RuntimeException("Got null trying to interpret: " + arg);
			}
			argValues.add(t.value);
			accum = t.state;
		}
		return new Pair<>(accum, argValues);
	}

	private Body getBodyForUnit(final Unit currUnit) {
		final SootMethod host = BodyManager.getHostMethod(currUnit);
		return BodyManager.retrieveBody(host);
	}

	private void widenAndEnqueue(final InstrumentedState s, final Unit u, final SootMethod context) {
		assert s != null : u + " " + context;
		assert u != null : s + " " + context;
		assert context != null: s + " " + u;
		if(!this.memo.contains(context, u)) {
			this.memo.put(context, u, s);
			enqueueWork(u, context);
		} else {
			final InstrumentedState curr = this.memo.get(context, u);
			final InstrumentedState widened = stateMonad.widen(curr, stateMonad.join(curr, s));
			if(!stateMonad.lessEqual(widened, curr)) {
				this.memo.put(context, u, widened);
				enqueueWork(u, context);
			}
		}
	}

	private void joinAndEnqueue(final InstrumentedState s, final Unit u, final SootMethod context) {
		if(!this.memo.contains(context, u)) {
			this.memo.put(context, u, s);
			enqueueWork(u, context);
		} else {
			final InstrumentedState curr = this.memo.get(context, u);
			final InstrumentedState joined = stateMonad.join(curr, s);
			if(!stateMonad.lessEqual(joined, curr)) {
				this.memo.put(context, u, joined);
				enqueueWork(u, context);
			}
		}
	}

	private void enqueueWork(final Unit u, final SootMethod context) {
		final Pair<SootMethod, Unit> workKey = new Pair<>(context, u);
		if(this.workSet.add(workKey)) {
			this.worklist.add(workKey);
		}
	}

	private boolean registerStart(final SootMethod target, final InstrumentedState calleeState,
			final PV receiver, final List<Object> arguments, final SootMethod context) {
		final Map<Local, Object> args = new HashMap<>();
		final Body b = BodyManager.retrieveBody(target);
		assert receiver instanceof Product || receiver instanceof JValue;
		args.put(b.getThisLocal(), valueMonad.lift(receiver.mapAddress(addrSet -> addrSet.filter(addr -> Scene.v().getOrMakeFastHierarchy().canStoreType(addr.t, target.getDeclaringClass().getType())))));
		for(int i = 0; i < arguments.size(); i++) {
			final boolean populated = stateMonad.mapValue(calleeState, arguments.get(i), new ValueMapper<PV, LocalMap, Boolean>() {
				@Override
				public Boolean mapAbstract(final PV val, final LocalMap state, final Heap h, final RecursiveTransformer<LocalMap, Boolean> recursor) {
					return !val.isBottom();
				}

				@Override
				public Boolean mapConcrete(final IValue v, final LocalMap state, final Heap h, final HeapReader<LocalMap, PV> heapAccessor,
						final RecursiveTransformer<LocalMap, Boolean> recursor) {
					return true;
				}

				@Override
				public Boolean merge(final Boolean v1, final Boolean v2) {
					return v1 || v2;
				}
			});
			if(!populated) {
				continue;
			}
			args.put(b.getParameterLocal(i), arguments.get(i));
		}
		final InstrumentedState bound = stateMonad.mapState(calleeState, new Function<LocalMap, LocalMap>() {
			@Override
			public LocalMap apply(final LocalMap in) {
				return in.withLocals(args);
			}
		});
		final Unit firstUnit = b.getUnits().getFirst();
		final boolean widen = this.callGraph.isWideningPoint(new Pair<>(context, target));
		final InstrumentedState joined;
		if(!this.memo.contains(context, firstUnit)) {
			joined = bound;
		} else if(widen) {
			joined = stateMonad.widen(this.memo.get(context, firstUnit), stateMonad.join(this.memo.get(context, firstUnit), bound));
		} else {
			joined = stateMonad.join(this.memo.get(context, firstUnit), bound);
		}
		final P2<InstrumentedState, Boolean> init = this.continuationManager.getInitialState(joined, new Pair<>(context, target));
		if(init._2()) {
			this.memo.put(context, firstUnit, init._1());
			assert this.continuationManager.hasEqualState(init._1(), new Pair<>(context, target));
			enqueueWork(firstUnit, context);
			return true;
		}
		return false;
	}

	@Override
	public Option<MethodResult> handleCall(final SootMethod m, final PV receiver, final List<Object> arguments, final InstrumentedState callState, final SootMethod calleeContext,
			final BoundaryInformation<SootMethod> callerContext) {
		this.registerCaller(m, calleeContext, callerContext);
		final boolean reanalyze = this.registerStart(m, stateMonad.mapState(callState, new Function<LocalMap, LocalMap>() {
			@Override
			public LocalMap apply(final LocalMap in) {
				return new LocalMap().withHeap(in.heap).withContinuation(continuationManager.initContinuation(Option.some(getContinuation(callerContext))));
			}
		}), receiver, arguments, calleeContext);
		if(!reanalyze) {
			return Option.fromNull(this.getReturnSummary(m, calleeContext)).filter(mr -> this.continuationManager.isCompatibleReturn(new Pair<>(calleeContext, m), mr.getState()));
		} else {
			return Option.none();
		}
	}

	private void registerCaller(final SootMethod callee, final SootMethod calleeContext, final BoundaryInformation<SootMethod> callerContext) {
		callGraph.registerCall(new Pair<>(callerContext.rootContext, BodyManager.getHostMethod(callerContext.rootInvoke)), new Pair<>(calleeContext, callee));
		this.callees.put(new Pair<>(calleeContext, callee), getContinuation(callerContext));
	}

	private Pair<SootMethod, Unit> getContinuation(final BoundaryInformation<SootMethod> callerContext) {
		return new Pair<>(callerContext.rootContext, BodyManager.getHostUnit(callerContext.rootInvoke));
	}

	private void registerReturn(final MethodResult newReturnState, final Unit currUnit, final SootMethod context) {
		final SootMethod method = BodyManager.getHostMethod(currUnit);
		if(!this.methodSummaries.contains(method, context)) {
			this.methodSummaries.put(method, context, newReturnState);
			this.enqueueCallers(new Pair<>(context, method));
			return;
		}
		final MethodResult currResult = this.methodSummaries.get(method, context);
		final MethodResult joined;
		if(callGraph.isWideningPoint(new Pair<>(context, method))) {
			joined = this.methodResultMonad.widen(currResult, this.methodResultMonad.join(currResult, newReturnState));
		} else {
			joined = this.methodResultMonad.join(currResult, newReturnState);
		}
		if(!methodResultMonad.lessEqual(joined, currResult)) {
			this.methodSummaries.put(method, context, joined);
			this.enqueueCallers(new Pair<>(context, method));
		}
	}

	private void enqueueCallers(final Pair<SootMethod, SootMethod> pair) {
		this.worklist.addAll(this.callees.get(pair));
	}

	private MethodResult getReturnSummary(final SootMethod m, final SootMethod calleeContext) {
		if(this.methodSummaries.contains(m, calleeContext)) {
			return this.methodSummaries.get(m, calleeContext);
		} else {
			return null;
		}
	}

	@Override
	public List<SootMethod> getMethodForRef(final SootMethodRef mref, final PV receiver) {
		final Set<AbstractAddress> addrSet;
		if(receiver instanceof Product) {
			addrSet = ((Product) receiver).p1.addressSet;
		} else if(receiver instanceof JValue){
			addrSet = ((JValue) receiver).addressSet;
		} else {
			return Collections.emptyList();
		}
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
					}, Set.empty(OptimisticInformationFlow.methodOrd));
				}
				if(!(t instanceof RefType)) {
					return Set.empty(OptimisticInformationFlow.methodOrd);
				}
				final SootClass cls = ((RefType) t).getSootClass();
				if(!cls.declaresMethod(mref.getSubSignature())) {
					return Set.empty(OptimisticInformationFlow.methodOrd);
				}
				return Set.single(OptimisticInformationFlow.methodOrd, cls.getMethod(mref.getSubSignature()));
			}
		}, Monoid.setMonoid(OptimisticInformationFlow.methodOrd)).toJavaList();
	}
	
	@Override
	public List<Injectable> monadUsers() {
		final List<Injectable> toReturn = new ArrayList<>();
		toReturn.add(LocalMap.injector);
		toReturn.add(StupidHeap.injector);
		toReturn.add(AbstractObject.monadHolder);
		return toReturn;
	}

	@Override
	public ContextManager<SootMethod, PV, LocalMap, StupidHeapWithReturnSlot> contextManager() {
		return new EntryPointContextManager<SootMethod, PV, LocalMap, StupidHeapWithReturnSlot>() {

			@Override public SootMethod initialContext(final SootMethod m) {
				return m;
			}

			@Override public SootMethod initialAllocationContext(final SootMethod mainMethod) {
				return mainMethod;
			}

			@Override
			public void inject(final Monads<PV, LocalMap> monads) { }
			
			@Override
			public SootMethod contextForCall(final Either<Pair<SootMethod, InstrumentedState>, ExecutionState<StupidHeapWithReturnSlot, SootMethod>> callingContext,
					final SootMethod targetMethod, final Object base, final List<Object> values, final InvokeExpr callExpr) {
				return callingContext.either(a -> BodyManager.getHostMethod(callExpr), a -> a.currMethod);
			}
			
			@Override
			public SootMethod contextForAllocation(final Either<Pair<SootMethod, InstrumentedState>, ExecutionState<StupidHeapWithReturnSlot, SootMethod>> allocContext, final Value allocExpr) {
				return allocContext.either(a -> a.getO1(), a -> a.currMethod);
			}
		};
	}

	@Override
	public boolean modelsType(final Type type) {
		return true;
	}

	@Override
	public Pair<StupidHeapWithReturnSlot, PV> abstractObjectAlloc(final RefType t, final StupidHeapWithReturnSlot inputHeap, final Value allocationExpr, final SootMethod allocationContext) {
		return upcastAlloc(inputHeap.alloc(t));
	}

	private Pair<StupidHeapWithReturnSlot, PV> upcastAlloc(final Pair<StupidHeapWithReturnSlot, JValue> alloc) {
		return new Pair<>(alloc.getO1(), alloc.getO2());
	}

	@Override
	public Pair<StupidHeapWithReturnSlot, PV> abstractArrayAlloc(final ArrayType t, final StupidHeapWithReturnSlot inputHeap, final Value allocationExpr,
			final List<PV> sizes, final SootMethod allocationContext) {
		return upcastAlloc(inputHeap.alloc(t, sizes.size()));
	}

	@Override
	public PrimitiveOperations<PV> primitiveOperations() {
		return new DefaultPrimitiveOps<PV>() {
			@Override
			public CompareResult cmp(final PV a, final PV b) {
				return CompareResult.NONDET;
			}

			@Override
			protected PV binop(final PV a, final PV b) {
				return PV.lattice.join(a, b);
			}
		};
	}
	
	private final ValueMapper<PV, LocalMap, Boolean> taintChecker = new MonoidalValueMapper<PV, LocalMap, Boolean>() {
		@Override
		public Boolean mapAbstract(final PV val, final LocalMap state, final Heap h, final RecursiveTransformer<LocalMap, Boolean> recursor) {
			if(val.isBottom()) {
				return false;
			}
			if(val instanceof JValue) {
				return recurseHeap((JValue) val, state, h, recursor);
			} else if(val instanceof PathTree) {
				return !val.isBottom();
			} else {
				final Product p = (Product) val;
				return !p.p2.isBottom() || recurseHeap(p.p1, state, h, recursor);
			}
		}

		private boolean recurseHeap(final JValue val, final LocalMap state, final Heap h, final RecursiveTransformer<LocalMap, Boolean> recursor) {
			return val.addressSet.foldMap(new F<AbstractAddress, Boolean>() {
				@Override
				public Boolean f(final AbstractAddress a) {
					return state.heap.wrapped.mapping.get(a).map(new F<AbstractObject, Boolean>() {
						@Override
						public Boolean f(final AbstractObject a) {
							return a.fields.toStream().map(P2.__2()).exists(new F<Object, Boolean>() {
								@Override
								public Boolean f(final Object a) {
									return recursor.mapValue(state, h, a);
								}
							});
						}
					}).orSome(false);
				}
			}, OptimisticInformationFlow.BOOLEAN_MONOID);
		}

		@Override
		public Boolean mapConcrete(final IValue v, final LocalMap state, final Heap h, final HeapReader<LocalMap, PV> heapAccessor,
				final RecursiveTransformer<LocalMap, Boolean> recursor) {
			return heapAccessor.forEachField(v, state, h, recursor, this);
		}

		@Override
		public Boolean merge(final Boolean v1, final Boolean v2) {
			return v1 || v2;
		}

		@Override
		public Boolean zero() {
			return false;
		}
	};

	@Override
	public void instrument(final InstrumentationManager<PV, StupidHeapWithReturnSlot, LocalMap> instManager) {
		instManager.selector()
			.methodCases().cases()
				.methodFilter().name("writeDatabase").build()
				.basePointerFilter().typeFilter().isSubType(RefType.v("meta.framework.Dispatcher")).build().build()
			.cases()
				.methodFilter().name("sink").build().build()
			.cases()
				.methodFilter().name("test_sensitiveData").build().build()
			.withAction(new MethodCallAction<PV, StupidHeapWithReturnSlot, LocalMap>() {
				
				@Override
				public Option<PV> preCall(final MultiValueReplacement<PV, StupidHeapWithReturnSlot, LocalMap> argReplacer, final InvokeExpr op, final SootMethodRef target) {
					final Boolean isTainted = argReplacer.getReader().read(0, taintChecker);
					if(isTainted) {
						foundFlows.add(new TaintFlow(op.getArg(0), BodyManager.getHostMethod(op), BodyManager.getHostUnit(op), target));
					}
					return Option.none();
				}
				
			});
		instManager.selector()
			.methodCall()
				.methodFilter().subSigIs("int sanitize(int)").build()
			.build().withAction(new MethodCallAction<PV, StupidHeapWithReturnSlot, LocalMap>() {
			@Override public Option<PV> preCall(final MultiValueReplacement<PV, StupidHeapWithReturnSlot, LocalMap> argReplacer, final InvokeExpr op, final SootMethodRef target) {
				return Option.some(PV.bottom);
			}
		});
		instManager.selector()
			.methodCall()
				.methodFilter().declaringType().isSubType("meta.framework.db.DirectProvider").subSigIs("void setData(int,int)").build()
			.build().withAction(new MethodCallAction<PV, StupidHeapWithReturnSlot, LocalMap>() {
			@Override public Option<PV> preCall(final MultiValueReplacement<PV, StupidHeapWithReturnSlot, LocalMap> argReplacer, final InvokeExpr op, final SootMethodRef target) {
				for(int i = 0; i < 2; i++) {
					if(argReplacer.getReader().read(i, taintChecker)) {
						foundFlows.add(new TaintFlow(op.getArg(i), BodyManager.getHostMethod(op), BodyManager.getHostUnit(op), target));
					}
				}
				return Option.none();
			}
		});
		instManager.selector()
				.methodCall()
				.methodFilter().declaringType().isSubType("meta.framework.db.DirectProvider").subSigIs("int readData(int)").build()
				.build().withAction(new MethodCallAction<PV, StupidHeapWithReturnSlot, LocalMap>() {
			@Override public Option<PV> preCall(final MultiValueReplacement<PV, StupidHeapWithReturnSlot, LocalMap> argReplacer, final InvokeExpr op, final SootMethodRef target) {
				if(argReplacer.getReader().read(0, taintChecker)) {
					foundFlows.add(new TaintFlow(op.getArg(0), BodyManager.getHostMethod(op), BodyManager.getHostUnit(op), target));
				}
				return Option.none();
			}
		});

		instManager.selector()
			.methodCall()
				.methodFilter().subSigIs("int getRequestData(int)").build()
				.basePointerFilter().typeFilter().isSubType("meta.framework.Request").build()
			.build().withAction(new MethodCallAction<PV, StupidHeapWithReturnSlot, LocalMap>() {
				@Override
				public Option<PV> preCall(final MultiValueReplacement<PV, StupidHeapWithReturnSlot, LocalMap> argReplacer, final InvokeExpr op, final SootMethodRef target) {
					return Option.some(new PathTree(TaintFlag.Taint));
				}
			});
	}

	@Override
	public ObjectOperations<PV, StupidHeapWithReturnSlot> objectOperations() {
		return new ObjectOperations<PV, StupidHeapWithReturnSlot>() {
			
			@Override
			public StupidHeapWithReturnSlot writeArray(final StupidHeapWithReturnSlot h, final PV basePointer, final PV index, final Object value, final ArrayRef context) {
				final JValue toSet = projectJVal(basePointer);
				return h.set(toSet, "*", value);
			}
			
			@Override
			public Option<Object> readArray(final StupidHeapWithReturnSlot heap, final PV val, final PV index, final ArrayRef context) {
				return Option.some(readArray(heap, val, context));
			}

			protected Object readArray(final StupidHeapWithReturnSlot heap, final PV val, final ArrayRef context) {
				if(val instanceof Product) {
					final Object r1 = heap.wrapped.get(((Product)val).p1, "*");
					final Object r2 = valueMonad.lift(((Product)val).p2.readArray(context == null ? Scene.v().getObjectType() : context.getType()));
					return valueMonad.join(r1, r2);
				} else if(val instanceof PathTree) {
					return valueMonad.lift(((PathTree) val).readArray(context.getType()));
				} else if(val instanceof JValue) {
					return heap.wrapped.get((JValue) val, "*");
				} else {
					throw new RuntimeException();
				}
			}

			@Override public ObjectIdentityResult isNull(final PV a) {
				if(a instanceof PathTree) {
					return ObjectIdentityResult.MUST_NOT_BE;
				} else if(a instanceof Product) {
					return ((Product) a).p1.isNull();
				} else {
					return ((JValue)a).isNull();
				}
			}

			@Override
			public PV downcast(final PV v, final Type t) {
				return v;
			}
			
			@Override
			public PV arrayLength(final PV basePointer, final StupidHeapWithReturnSlot h) {
				return PV.bottom;
			}

			@Override
			public Stream<Type> possibleTypes(final PV a) {
				if(a instanceof PathTree) {
					return Stream.nil();
				}
				final JValue jv;
				if(a instanceof Product) {
					jv = ((Product) a).p1;
				} else {
					jv = (JValue) a;
				} 
				return jv.addressSet.toStream().map(addr -> addr.t);
			}
		};
	}

	@Override
	public Option<Pair<StupidHeapWithReturnSlot, PV>> allocateUnknownObject(final StupidHeapWithReturnSlot inputHeap, final Value allocationExpr, final SootMethod allocationContext,
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
		return Option.some(upcastAlloc(inputHeap.allocUnknownType(upperBound)));
	}

	@Override public void setResultStream(final ResultStream s) {
		this.resultStream = s;
	}

	@Override public void dischargeProofObligations() {
		for(final TaintFlow tf : this.foundFlows) {
			this.resultStream.outputAnalysisResult(tf);
		}
	}
}
