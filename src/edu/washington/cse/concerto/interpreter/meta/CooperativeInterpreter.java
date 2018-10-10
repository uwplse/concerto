package edu.washington.cse.concerto.interpreter.meta;

import edu.washington.cse.concerto.instrumentation.InstrumentationManager;
import edu.washington.cse.concerto.instrumentation.InstrumentationManager.PreCallInstrumentation;
import edu.washington.cse.concerto.instrumentation.forms.HeapLocation;
import edu.washington.cse.concerto.interpreter.BinOp;
import edu.washington.cse.concerto.interpreter.BodyManager;
import edu.washington.cse.concerto.interpreter.EmbeddedState;
import edu.washington.cse.concerto.interpreter.ExpressionInterpreter;
import edu.washington.cse.concerto.interpreter.Interpreter;
import edu.washington.cse.concerto.interpreter.InterpreterExtension;
import edu.washington.cse.concerto.interpreter.InterpreterState;
import edu.washington.cse.concerto.interpreter.InvokeInterpreterExtension;
import edu.washington.cse.concerto.interpreter.MethodState;
import edu.washington.cse.concerto.interpreter.ReflectionEnvironment;
import edu.washington.cse.concerto.interpreter.StateAccumulator;
import edu.washington.cse.concerto.interpreter.ai.AbstractInterpretation;
import edu.washington.cse.concerto.interpreter.ai.BottomAware;
import edu.washington.cse.concerto.interpreter.ai.Concretizable;
import edu.washington.cse.concerto.interpreter.ai.MethodResult;
import edu.washington.cse.concerto.interpreter.ai.ReflectiveOperationContext;
import edu.washington.cse.concerto.interpreter.ai.State;
import edu.washington.cse.concerto.interpreter.ai.StateValueUpdater;
import edu.washington.cse.concerto.interpreter.ai.binop.ObjectIdentityResult;
import edu.washington.cse.concerto.interpreter.ai.binop.PrimitiveOperations;
import edu.washington.cse.concerto.interpreter.annotations.MutatesFork;
import edu.washington.cse.concerto.interpreter.annotations.RequiresForked;
import edu.washington.cse.concerto.interpreter.exception.NullPointerException;
import edu.washington.cse.concerto.interpreter.exception.OutOfBoundsArrayAccessException;
import edu.washington.cse.concerto.interpreter.exception.PruneExecutionException;
import edu.washington.cse.concerto.interpreter.exception.ReflectiveOperationException;
import edu.washington.cse.concerto.interpreter.heap.Heap;
import edu.washington.cse.concerto.interpreter.lattice.Lattice;
import edu.washington.cse.concerto.interpreter.loop.LoopState;
import edu.washington.cse.concerto.interpreter.meta.Monads.InstrumentedStateImpl;
import edu.washington.cse.concerto.interpreter.meta.ReflectionModel.InvokeMoke;
import edu.washington.cse.concerto.interpreter.meta.TypeOracle.TypeOwner;
import edu.washington.cse.concerto.interpreter.state.ExecutionState;
import edu.washington.cse.concerto.interpreter.state.GlobalState;
import edu.washington.cse.concerto.interpreter.value.EmbeddedValue;
import edu.washington.cse.concerto.interpreter.value.IValue;
import edu.washington.cse.concerto.interpreter.value.IValue.RuntimeTag;
import edu.washington.cse.concerto.interpreter.value.IValueTransformer;
import edu.washington.cse.concerto.interpreter.value.ValueMerger;
import fj.F;
import fj.F2;
import fj.Ord;
import fj.Ordering;
import fj.P;
import fj.P2;
import fj.P3;
import fj.data.Either;
import fj.data.Option;
import fj.data.Set;
import fj.data.Stream;
import soot.AnySubType;
import soot.IntType;
import soot.Local;
import soot.PrimType;
import soot.RefLikeType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.SootMethodRefImpl;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.VoidType;
import soot.jimple.ArrayRef;
import soot.jimple.BinopExpr;
import soot.jimple.ClassConstant;
import soot.jimple.ConditionExpr;
import soot.jimple.Constant;
import soot.jimple.IfStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InstanceOfExpr;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.LengthExpr;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.StringConstant;
import soot.toolkits.scalar.Pair;
import soot.util.NumberedString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CooperativeInterpreter<AVal, AHeap, AS, Context> extends Interpreter<Context, AHeap> {
	private static final String INVOKE_OBJ_OBJ = "java.lang.Object invokeObj(java.lang.Object,int,int,java.lang.Object)";
	private static final String INVOKE_INT_INT = "int invokeInt(java.lang.Object,int,int,int)";
	private static final String INVOKE_INT_OBJ = "int invokeInt(java.lang.Object,int,int,java.lang.Object)";
	private static final String INVOKE_OBJ_INT = "java.lang.Object invokeObj(java.lang.Object,int,int,int)";
	private static final String INVOKE_OBJ_NULL = "java.lang.Object invokeObj(java.lang.Object,int,int)";
	private static final String INVOKE_INT_NULL = "int invokeInt(java.lang.Object,int,int)";
	private static final InvokeResolution<Stream<IValue>, Pair<IValue, SootMethod>> CONCRETE_RESOLVER = new InvokeResolution<Stream<IValue>, Pair<IValue, SootMethod>>() {
		@Override public Stream<SootClass> getTargetClassStream(final Stream<IValue> r, final Value receiverArg, final InvokeMoke mode) {
			return CooperativeInterpreter.getTargetClassStream(r, receiverArg, mode);
		}

		@Override public Stream<Pair<IValue, SootMethod>> generateForMethod(final Stream<IValue> r, final SootMethod m) {
			return generateReceiverPairs(r, m);
		}
	};

	private final AbstractInterpretation<AVal, AHeap, AS, Context> ai;
	private final State<AHeap, AS> stateManipulator;
	private final Monads<AVal, AS> monads;
	private final Lattice<AHeap> heapLattice;
	
	private final NumberedString allocAppTypeSigStr;
	private final NumberedString allocAppTypeSigInt;
	private final NumberedString allocAppTypeSigClass;
	private final NumberedString invokeSigObjObj;
	private final NumberedString invokeSigObjInt;
	private final NumberedString invokeSigIntObj;
	private final NumberedString invokeSigObjNull;
	private final NumberedString invokeSigIntNull;
	private final NumberedString invokeSigIntInt;
	private final NumberedString getClassSig;

	private final InstrumentationManager<AVal, AHeap, AS> instManager;
	protected BranchInterpreterImpl<AVal> branchInterpreter;
	private final TypeOracle oracle;

	public CooperativeInterpreter(final GlobalState gs, final AbstractInterpretation<AVal, AHeap, AS, Context> ai,
		final State<AHeap, AS> stateManipulator, final Monads<AVal, AS> monads, final Lattice<AHeap> heapLattice,
		final InstrumentationManager<AVal, AHeap, AS> instManager, final TypeOracle oracle) {
		this(gs, null, null, ai, stateManipulator, monads, heapLattice, instManager, null, oracle);
	}
	
	private CooperativeInterpreter(final GlobalState gs, final InterpreterExtension<AHeap> ie, 
			final InvokeInterpreterExtension<AHeap, Context> iie, final AbstractInterpretation<AVal, AHeap, AS, Context> ai,
			final State<AHeap, AS> stateManipulator, final Monads<AVal, AS> monads, final Lattice<AHeap> heapLattice,
			final InstrumentationManager<AVal, AHeap, AS> instManager, final BranchInterpreterImpl<AVal> branchInterp,
			final TypeOracle oracle) {
		super(gs, ie, iie);
		this.ai = ai;
		this.stateManipulator = stateManipulator;
		this.monads = monads;
		this.heapLattice = heapLattice;
		this.instManager = instManager;
		
		this.allocAppTypeSigStr = Scene.v().getSubSigNumberer().findOrAdd("java.lang.Object allocateType(java.lang.String)");
		this.allocAppTypeSigInt = Scene.v().getSubSigNumberer().findOrAdd("java.lang.Object allocateType(int)");
		this.allocAppTypeSigClass = Scene.v().getSubSigNumberer().findOrAdd("java.lang.Object allocateType(java.lang.Class)");
		this.getClassSig = Scene.v().getSubSigNumberer().findOrAdd("int getClass(java.lang.Object)");
		this.invokeSigObjObj = Scene.v().getSubSigNumberer().findOrAdd(INVOKE_OBJ_OBJ);
		this.invokeSigObjInt = Scene.v().getSubSigNumberer().findOrAdd(INVOKE_OBJ_INT);
		this.invokeSigIntObj = Scene.v().getSubSigNumberer().findOrAdd(INVOKE_INT_OBJ);
		this.invokeSigIntInt = Scene.v().getSubSigNumberer().findOrAdd(INVOKE_INT_INT);
		this.invokeSigIntNull = Scene.v().getSubSigNumberer().findOrAdd(INVOKE_INT_NULL);
		this.invokeSigObjNull = Scene.v().getSubSigNumberer().findOrAdd(INVOKE_OBJ_NULL);
		this.branchInterpreter = branchInterp;
		this.oracle = oracle;
	}
	
	@Override
	protected IValue handleEmbeddedCall(final ExecutionState<AHeap, Context> es, final IValue baseValue, final InstanceInvokeExpr iie) {
		final EmbeddedValue aVal = baseValue.aVal;
		if(aVal.value instanceof IValue) {
			throw new RuntimeException();
		}
		
		final List<IValue> argVals = new ArrayList<>();
		for(final Value arg : iie.getArgs()) {
			argVals.add(this.interpretValue(es, arg));
		}
		return handleEmbeddedCall(es, iie, iie.getMethodRef(), aVal, argVals);
	}

	@SuppressWarnings("unchecked")
	private IValue handleEmbeddedCall(final ExecutionState<AHeap, Context> es, final InvokeExpr iie, final SootMethodRef ref, final EmbeddedValue aVal, final List<IValue> argVals) {
		final AVal abstractReceiver;
		final StateAccumulator<AHeap> resultAccum;
		final List<IValue> returnVals = new ArrayList<>();
		final Heap collapsed = es.heap.collapseToSingle();
		final ExecutionState<AHeap, Context> esForCall = es.withHeap(collapsed);
		if(aVal.value instanceof CombinedValue) {
			final CombinedValue cVal = (CombinedValue) aVal.value;
			/* first let's compute the result state for the concrete callees
			 * this may prune, in which case, we need to catch it
			 * We do so, but it turns out this will, at worst, bubble up to the AI -> FR call
			 * that led to this embedded invocation. (Note that we won't prune for
			 * "top-level" ai invocations because we iterate to fixpoint). In that case, the outer
			 * fixpoint will fill in whatever missing summaries caused us to prune, and reanalyze
			 * the AI code that led to the original AI -> FR call that led here,
			 * except this time, we won't prune. 
			 */
			Option<Pair<IValue, StateAccumulator<AHeap>>> partialResult_;
			try {
				partialResult_ = Option.some(this.handleDispatch(esForCall, this.resolveDispatchees(cVal.concreteComponent, iie.getMethodRef()), argVals, iie));
			} catch(final PruneExecutionException e) {
				partialResult_ = Option.none();
			}
			if(partialResult_.isNone()) {
				resultAccum = new StateAccumulator<>();
			} else {
				resultAccum = partialResult_.some().getO2();
				if(partialResult_.some().getO1() != null) {
					returnVals.add(partialResult_.some().getO1());
				}
			}
			abstractReceiver = (AVal) cVal.abstractComponent;
		} else {
			resultAccum = new StateAccumulator<>();
			abstractReceiver = (AVal) aVal.value;
		}
		final boolean hasResult = this.interpretAbstractCall(esForCall, abstractReceiver, argVals, iie, ref, resultAccum, returnVals);
		if(!hasResult) {
			throw new PruneExecutionException();
		}
		resultAccum.applyToState(es);
		if(returnVals.size() == 0) {
			return null;
		} else {
			return IValue.lift(returnVals);
		}
	}
	
	private boolean interpretAbstractCall(final @MutatesFork ExecutionState<AHeap, Context> es, final AVal abstractReceiver, final List<IValue> concreteArgVals, final InvokeExpr iie,
			final SootMethodRef ref, final StateAccumulator<AHeap> resultAccum, final List<IValue> returnVals) {
		return interpretAbstractCall(es, abstractReceiver, concreteArgVals, iie, Stream.iterableStream(ai.getMethodForRef(ref, abstractReceiver)), resultAccum, returnVals);
	}
	
	private boolean interpretAbstractCall(final @MutatesFork ExecutionState<AHeap, Context> es, final AVal abstractReceiver, final List<IValue> concreteArgVals, final InvokeExpr iie,
			final Stream<SootMethod> ref, final StateAccumulator<AHeap> resultAccum, final List<IValue> returnVals) {
		final List<Value> argExpressions = iie.getArgs();
		return interpretAbstractCall(es, abstractReceiver, concreteArgVals, iie, ref, resultAccum, returnVals, argExpressions);
	}

	private boolean interpretAbstractCall(final @MutatesFork ExecutionState<AHeap, Context> es, final AVal abstractReceiver, final List<IValue> concreteArgVals, final InvokeExpr iie,
			final Stream<SootMethod> ref, final StateAccumulator<AHeap> resultAccum, final List<IValue> returnVals, final List<Value> argExpressions) {
		final ExecutionState<AHeap, Context> argEvalContext = es.fork();
		final List<Object> argValues = this.convertArgs(concreteArgVals, argExpressions);
		argEvalContext.heap.assertSaneStructure();
		final Heap collapsed = argEvalContext.heap.collapseToSingle();
		collapsed.assertSaneStructure();
		boolean hasResult = false;
		for(final SootMethod m : ref) {
			collapsed.assertSaneStructure();
			final Heap ghostCopy = collapsed.fork();
			final AS startAbsState = getCallState(argEvalContext);
			if(Interpreter.DEBUG_CALLS) {
				System.out.println("Calling AI: " + m);
			}
			ghostCopy.assertSaneStructure();
			final Option<MethodResult> summary_ = doAbstractCall(es, iie, startAbsState, m, abstractReceiver, argValues, ghostCopy);
			if(Interpreter.DEBUG_CALLS) {
				System.out.println("DONE with AI: " + m);
			}
			if(summary_.isNone()) {
				continue;
			}
			hasResult = true;
			assert summary_.some() != null : abstractReceiver + " " + m + " "  + iie + " " + summary_;
			final MethodResult summary = summary_.some();
			final Object returnValue = summary.getReturnValue();
			final InstrumentedState returnState = summary.getState();
			if(returnValue != null) {
				returnVals.add(convertToConcrete(returnValue));
			}
			@SuppressWarnings("unchecked")
			final InstrumentedStateImpl<AS> state = (InstrumentedStateImpl<AS>) returnState;
			final EmbeddedState<AHeap> embeddedFH = getEmbeddedReturnHeap(startAbsState, state.state, new MergeContext(abstractReceiver, argValues, m, true));
			state.concreteHeap.assertSaneStructure();
			resultAccum.update(state.concreteHeap, embeddedFH);
		}
		return hasResult;
	}

	private AS getCallState(final ExecutionState<AHeap, Context> es) {
		final AS startAbsState;
		if(es.foreignHeap.state == null) {
			startAbsState = stateManipulator.emptyState();
		} else {
			startAbsState = stateManipulator.inject(stateManipulator.emptyState(), es.foreignHeap.state);
		}
		return startAbsState;
	}
	
	@Override
	protected Interpreter<Context, AHeap> deriveNewInterpreter(final InterpreterExtension<AHeap> processor) {
		return new CooperativeInterpreter<>(this.globalState, processor, this.invokeExtension, ai, stateManipulator, monads, heapLattice,
				instManager, branchInterpreter, oracle);
	}
	
	@Override
	protected Interpreter<Context, AHeap> deriveNewInterpreter(final GlobalState gs) {
		return new CooperativeInterpreter<>(gs, interpretExtension, invokeExtension, ai, stateManipulator, monads, heapLattice, instManager, branchInterpreter, oracle);
	}
	
	@Override
	protected Interpreter<Context, AHeap> deriveNewInterpreter(final InvokeInterpreterExtension<AHeap, Context> processor) {
		return new CooperativeInterpreter<>(globalState, interpretExtension, processor, ai, stateManipulator, monads, heapLattice,
				instManager, branchInterpreter, oracle);
	}

	private boolean canPropagate(final IValue op1, final IValue op2) {
		return !Monads.isCombinable(op2) && !Monads.isCombinable(op1);
	}
	
	@Override
	protected List<Pair<ExecutionState<AHeap, Context>, Unit>> getConditionalSuccessors(final ExecutionState<AHeap, Context> state, final IfStmt u) {
		if(u.getCondition() instanceof InstanceOfExpr) {
			return getTypeCheckSuccessors(state, u);
		}
		final ConditionExpr condition = (ConditionExpr) u.getCondition();
		final IValue op1 = this.interpretValue(state, condition.getOp1());
		final IValue op2 = this.interpretValue(state, condition.getOp2());
		if(!op1.isEmbedded() && !op2.isEmbedded()) {
			final ExpressionInterpreter eInterpret = this.getExpressionInterpreter(state);
			final IValue result = eInterpret.interpretWith(condition, op1, op2);
			if(result.isDeterministic()) {
				return Collections.singletonList(new Pair<>(state, resolveCondition(state, u, result)));
			} else if(!this.canPropagate(op1, op2)) {
				return bothBranches(state, u);
			}
		}
		// we have at least one abstract argument
		final Object aop1 = this.convertToAbstract(op1);
		final Object aop2 = this.convertToAbstract(op2);
		
		if(!(branchInterpreter instanceof PropagatingBranchInterpreter) || !isStaticValueLocation(condition.getOp1()) || !isStaticValueLocation(condition.getOp2())) {
			final List<Unit> interpretedBranch = branchInterpreter.interpretBranch(u, aop1, aop2, state);
			return resolveBranchInterpretation(state, u, interpretedBranch);
		}
		@SuppressWarnings("unchecked")
		final PropagatingBranchInterpreter<AVal, ExecutionState<AHeap, Context>> pbi = (PropagatingBranchInterpreter<AVal, ExecutionState<AHeap, Context>>) branchInterpreter;
		final Map<Unit, ExecutionState<AHeap, Context>> interpretBranch = pbi.interpretBranch(u, aop1, aop2, state, new StateValueUpdater<ExecutionState<AHeap, Context>>() {
			@Override public ExecutionState<AHeap, Context> updateForValue(final Value v, final ExecutionState<AHeap, Context> state, final Object value) {
				updateLocation(state, v, value);
				return state;
			}
		});

		final List<Pair<ExecutionState<AHeap, Context>, Unit>> toReturn = new ArrayList<>();
		for(final Map.Entry<Unit, ExecutionState<AHeap, Context>> kv : interpretBranch.entrySet()) {
			toReturn.add(new Pair<>(kv.getValue(), kv.getKey()));
		}
		return toReturn;
	}

	private List<Pair<ExecutionState<AHeap, Context>, Unit>> getTypeCheckSuccessors(final ExecutionState<AHeap, Context> state, final IfStmt u) {
		final InstanceOfExpr condition = (InstanceOfExpr) u.getCondition();
		final Object op = convertToAbstract(this.interpretValue(state, condition.getOp()));
		final List<Unit> targets = this.branchInterpreter.interpretBranch(u, op, state);
		return resolveBranchInterpretation(state, u, targets);
	}

	private List<Pair<ExecutionState<AHeap, Context>, Unit>> resolveBranchInterpretation(final ExecutionState<AHeap, Context> state, final IfStmt u, final List<Unit> targets) {
		if(targets.size() == 2) {
			return bothBranches(state, u);
		} else {
			return Collections.singletonList(new Pair<>(state, targets.get(0)));
		}
	}

	public List<Pair<ExecutionState<AHeap, Context>, Unit>> bothBranches(final ExecutionState<AHeap, Context> state, final IfStmt u) {
		final List<Pair<ExecutionState<AHeap, Context>, Unit>> toReturn = new ArrayList<>();
		toReturn.add(new Pair<>(state.fork(), getSuccessor(state, u)));
		toReturn.add(new Pair<>(state.fork(), u.getTarget()));
		return toReturn;
	}

	private void updateLocation(final ExecutionState<AHeap, Context> toReturn, final Value toUpdate, final Object leftOp) {
		final IValue embedded = this.convertToConcrete(leftOp);
		if(toUpdate instanceof Local) {
			toReturn.ms.put(((Local) toUpdate).getName(), embedded);
		} else if(toUpdate instanceof InstanceFieldRef) {
			final IValue base = this.interpretValue(toReturn, ((InstanceFieldRef) toUpdate).getBase());
			toReturn.heap.putField(base, ((InstanceFieldRef) toUpdate).getFieldRef(), embedded);
		} else if(toUpdate instanceof ArrayRef) {
			final IValue base = this.interpretValue(toReturn, ((ArrayRef) toUpdate).getBase());
			final IValue index = this.interpretValue(toReturn, ((ArrayRef) toUpdate).getIndex());
			toReturn.heap.putArray(base, index, embedded);
		}
	}

	private boolean isStaticValueLocation(final Value op1) {
		if(op1 instanceof Local) {
			return true;
		} else if(op1 instanceof InstanceFieldRef) {
			return isStaticValueLocation(((InstanceFieldRef) op1).getBase());
		} else if(op1 instanceof ArrayRef) {
			return isStaticValueLocation(((ArrayRef) op1).getBase()) && isStaticValueLocation(((ArrayRef) op1).getIndex());
		} else if(op1 instanceof Constant) {
			return true;
		} else if(op1 instanceof LengthExpr) {
			return isStaticValueLocation(((LengthExpr) op1).getOp());
		} else {
			return false;
		}
	}

	private EmbeddedState<AHeap> getEmbeddedForeignHeap(final InstrumentedStateImpl<AS> state) {
		return new EmbeddedState<>(stateManipulator.project(state.state), this.heapLattice);
	}

	protected static IValue convertToConcrete(final Object returnValue, final Monads<?, ?> monads) {
		if(returnValue instanceof IValue) {
			return (IValue) returnValue;
		} else {
			return new IValue(new EmbeddedValue(returnValue, monads.valueMonad));
		}
	}
	
	protected IValue convertToConcrete(final Object returnValue) {
		return convertToConcrete(returnValue, monads);
	}
	
	List<IValue> convertToConcrete(final List<Object> argValues, final F<Integer, Value> ie) {
		return convertToConcrete(monads, ai, argValues, ie);
	}
	
	static List<IValue> convertToConcrete(final Monads<?, ?> monads, final AbstractInterpretation<?, ?, ?, ?> ai, 
			final List<Object> argValues, final F<Integer, Value> ie) {
		final List<IValue> toReturn = new ArrayList<>();
		for(int i = 0; i < argValues.size(); i++) {
			final Object av = argValues.get(i);
			if(useLiteralArgument(ai, ie, i, av) && ie.f(i) instanceof IntConstant) {
				toReturn.add(IValue.lift(((IntConstant)ie.f(i)).value));
			} else if(av == null) {
				toReturn.add(IValue.nondet());
			} else {
				toReturn.add(convertToConcrete(av, monads));
			}
		}
		return toReturn;
	}


	private static boolean useLiteralArgument(final AbstractInterpretation<?, ?, ?, ?> ai, final F<Integer, Value> ie, final int i, final Object av) {
		return !ai.modelsType(ie.f(i).getType()) || (av != null && av instanceof BottomAware && ((BottomAware) av).isBottom());
	}

	private List<Object> convertArgs(final List<IValue> values, final List<Value> argExprs) {
		final List<Object> toPass = new ArrayList<>();
		for(int i = 0; i < argExprs.size(); i++) {
			final IValue ev = values.get(i);
			final Value v = argExprs.get(i);
			if(ai.modelsType(v.getType())) {
				toPass.add(convertToAbstract(ev));
			} else {
				toPass.add(null);
			}
		}
		return toPass;
	}

	private Object convertToAbstract(final IValue ev) {
		if(ev.isEmbedded()) {
			final EmbeddedValue eVal = ev.aVal;
			return eVal.value;
		} else {
			return monads.valueMonad.lift(ev);
		}
	}

	/*
	 * call-graph tracking: callee, handleMultiDispatch handles
	 */
	public Option<P3<Object, Heap, AHeap>> handleConcreteCall(final Context context, final ConcreteMethodCallMirror iie, final IValue receiverPreInst,
			final List<Object> arguments, final Object callState) {
		@SuppressWarnings("unchecked")
		final InstrumentedStateImpl<AS> state = (InstrumentedStateImpl<AS>) callState;
		final Heap baseHeap = state.concreteHeap.copy();
		final List<Pair<IValue, SootMethod>> dispatchees = new ArrayList<>();
		final List<IValue> convertedArgsPreInst = convertToConcrete(arguments, iie::argExpr);

		final InstrumentedStateImpl<AS> instrumentedCallState = new InstrumentedStateImpl<>(state.state, baseHeap);
		if(receiverPreInst.isMultiHeap()) {
			final Iterator<IValue> variants = receiverPreInst.variants();
			while(variants.hasNext()) {
				final IValue v = variants.next();
				if(v.isHeapValue()) {
					for(final SootMethod callee : iie.resolveMethod(v)) {
						dispatchees.add(new Pair<>(v, callee));
					}
				}
			}
		} else if(receiverPreInst.getTag() == RuntimeTag.NULL) {
			// lol I unno
			throw new RuntimeException();
		} else if(receiverPreInst.getTag() == RuntimeTag.OBJECT || receiverPreInst.getTag() == RuntimeTag.BOUNDED_OBJECT) {
			for(final SootMethod callee : iie.resolveMethod(receiverPreInst)) {
				dispatchees.add(new Pair<>(receiverPreInst, callee));
			}
		}
		return handleMultiDispatch(context, dispatchees, convertedArgsPreInst, instrumentedCallState, iie.invokeExpr());
	}
	/*
	 * call-graph tracking: this call handles tracking
	 */
	public Option<P3<Object, Heap, AHeap>> handleInvokeConcrete(final Context context, final InvokeExpr op, final Stream<Pair<IValue,SootMethod>> stream,
		final List<Object> arguments, final Object callState) {
		@SuppressWarnings("unchecked")
		final InstrumentedStateImpl<AS> state = (InstrumentedStateImpl<AS>) callState;
		final List<IValue> preInstArgs = convertToConcrete(arguments, i -> i == 0 ? op.getArg(3) : null);
		
		final StateAccumulator<AHeap> accum = new StateAccumulator<>();
		final Option<Object> retOption = stream.map(tr -> { 
			final IValue receiver = tr.getO1();
			final SootMethod m = tr.getO2();
			final Heap baseHeap = state.concreteHeap.copy();
			if(TRACK_CALL_EDGES) {
				CALL_GRAPH.put(P.p(context, op), m);
			}
			final PreCallInstrumentation<AHeap> instResult = instManager.preCallCoop(baseHeap, stateManipulator.project(state.state), receiver, preInstArgs, m.makeRef(), op);
			if(instResult.summary.isSome()) {
				accum.update(baseHeap.fork(), instResult.fh);
				return instResult.summary.map(this::convertToAbstract);
			}
			final AS postInst = stateManipulator.inject(state.state, instResult.fh.state);
			final InstrumentedStateImpl<AS> as = new InstrumentedStateImpl<>(postInst, baseHeap);
			final BoundaryInformation<Context> boundary = new BoundaryInformation<>(as, context, op);
			final Heap forked = baseHeap.fork();
			final IValue receiverInst = instResult.receiver;
			final List<IValue> argsInst;
			if(m.getParameterCount() == 1) {
				assert instResult.arguments.size() == 1;
				if(m.getParameterType(0) instanceof RefLikeType) {
					argsInst = Collections.singletonList(this.interpretCast(instResult.arguments.get(0), m.getParameterType(0)));
				} else {
					argsInst = instResult.arguments;
				}
			} else {
				assert instResult.arguments.size() == 0;
				argsInst = Collections.emptyList();
			}
			Option<Object> returnValue = this.doConcreteCall(m, forked, receiverInst, argsInst, instResult.fh, boundary, accum);
			return returnValue.map(iv -> iv == null ? IValue.nullConst() : iv);
		}).foldLeft1((a, b) -> {
			if(a.isNone()) {
				return b;
			} else if(b.isNone()) {
				return a;
			} else if(a.some() == null) {
				assert b.some() == null;
				return Option.some(null);
			} else {
				return Option.some(monads.valueMonad.join(a.some(), b.some()));
			}
		});
		if(retOption.isNone()) {
			return Option.none();
		} else {
			return Option.some(P.p(retOption.some(), accum.heap, accum.foreignHeap.state));
		}
	}

	/*
	 * Call-graph tracking: this method handles
	 */
	private Option<P3<Object, Heap, AHeap>> handleMultiDispatch(final Context context, final List<Pair<IValue, SootMethod>> receiverTargets, final List<IValue> arguments,
			final InstrumentedStateImpl<AS> state, final InvokeExpr iExpr) {
		// we have a beginning heap H o Hs, where H is the merge over all collapsed
		// calling heaps at point where we have FR -> AI, and Hs is the
		// accumulated heap effects from executing the AI up until this point
		final Heap baseHeap = state.concreteHeap;
		final StateAccumulator<AHeap> accum = new StateAccumulator<>();
		Object returnAccum = null;
		boolean hasResult = false;
		final BoundaryInformation<Context> boundary = new BoundaryInformation<>(state, context, iExpr);
		for(final Pair<IValue, SootMethod> receiverAndTarget : receiverTargets) {
			final SootMethod targetMethod = receiverAndTarget.getO2();
			final IValue receiverPreInst = receiverAndTarget.getO1();
			final Heap forkedHeap = baseHeap.fork(); // We now have H o Hs o Hc where Hc = {}
			final IValue receiver;
			final List<IValue> convertedArgs;
			final EmbeddedState<AHeap> fh;
			if(TRACK_CALL_EDGES) {
				CALL_GRAPH.put(P.p(boundary.rootContext, iExpr), targetMethod);
			}
			{
				// forkedHeap is a fork of a copy of the calling state's heap, we are free to mutate it
				final PreCallInstrumentation<AHeap> preCall = instManager.preCallCoop(forkedHeap, stateManipulator.project(state.state),
						receiverPreInst, arguments, targetMethod.makeRef(), iExpr);
				if(preCall.summary.isSome()) {
					accum.update(forkedHeap, preCall.fh);
					hasResult = true;
					returnAccum = updateReturnAccum(returnAccum, Option.some(convertToAbstract(preCall.summary.some())));
					continue;
				}
				receiver = preCall.receiver;
				convertedArgs = preCall.arguments;
				fh = preCall.fh;
			}
			final Option<Object> ret = this.doConcreteCall(targetMethod, forkedHeap, receiver, convertedArgs, fh, boundary, accum);
			if(ret.isNone()) {
				continue;
			}
			hasResult = true;
			returnAccum = updateReturnAccum(returnAccum, ret);
		}
		if(hasResult) {
			return Option.some(P.p(returnAccum, accum.heap, accum.foreignHeap.state));
		} else {
			return Option.none();
		}
	}

	private Object updateReturnAccum(Object returnAccum, final Option<Object> ret) {
		if(returnAccum == null) {
			returnAccum = ret.some();
		} else if(ret.some() != null) {
			returnAccum = monads.valueMonad.join(returnAccum, ret.some());
		}
		return returnAccum;
	}

	private Option<Object> doConcreteCall(final SootMethod targetMethod, final Heap forkedHeap, final IValue base, final List<IValue> arguments, 
			final EmbeddedState<AHeap> fh, final BoundaryInformation<Context> boundary, final StateAccumulator<AHeap> accum) {
		final ExecutionState<AHeap, Context> forked = new ExecutionState<>(targetMethod, new MethodState(), forkedHeap, base, arguments, new LoopState(targetMethod), fh, boundary,
				null, Option.some(boundary.rootInvoke));
		if(Interpreter.DEBUG_CALLS) {
			System.out.println("AI -> FR: " + targetMethod + " at " + boundary.rootInvoke + " in " + boundary.rootContext);
		}
		final InterpreterState<AHeap> returnState = this.interpretUntil(targetMethod, forked, null, null);
		if(Interpreter.DEBUG_CALLS) {
			System.out.println("DONE AI -> FR: " + targetMethod + " at " + boundary.rootInvoke + " in " + boundary.rootContext);
		}
		if(returnState == null) {
			return Option.none();
		}
		// assuming we didn't prune we now have H o Hs o Hc o Hr, where Hc != {}, and Hr != {}
		forkedHeap.applyHeap(returnState.rs.h);
		// now we have forkedHeap: H o Hs o Hr' where we have collapsed the effect of the call into Hr'
		final P2<IValue, EmbeddedState<AHeap>> instPostCall = this.instManager.postCallCoop(forkedHeap, returnState.rs.foreignHeap.state, base, arguments, returnState.rs.returnValue,
				targetMethod.makeRef(), boundary.rootInvoke);

		accum.update(forkedHeap, instPostCall._2());
		final IValue retVal = instPostCall._1();
		if(retVal == null) {
			return Option.some(null);
		} else {
			return Option.some(convertToAbstract(retVal));
		}
	}

	@Override
	protected ExpressionInterpreter getExpressionInterpreter(final ExecutionState<AHeap, Context> es) {
		return new ExpressionInterpreter(es) {
			@Override
			protected void apply(final BinOp op, final BinopExpr expr) {
				final IValue o1 = this.op1;
				final IValue o2 = this.op2;
				if(!o1.isEmbedded() && !o2.isEmbedded()) {
					super.evalOp(op, o1, o2);
				} else if(o1.isEmbedded()) {
					evalEmbedded(o1.aVal, o2, op, expr, true);
				} else {
					evalEmbedded(o2.aVal, o1, op, expr, false);
				}
			}
			
			@SuppressWarnings("unchecked")
			private void evalEmbedded(final EmbeddedValue aVal, final IValue o1, final BinOp op, final BinopExpr expr, final boolean embedLeft) {
				if(o1.isMultiHeap() || o1.isNonDet() || o1.isHeapValue()) {
					throw new RuntimeException();
				}
				assert o1.getTag() == RuntimeTag.EMBEDDED || o1.isPrimitive();
				final AVal a2;
				if(o1.isPrimitive()) {
					a2 = monads.valueMonad.alpha(o1);
				} else {
					a2 = (AVal) o1.aVal.value;
				}
				final AVal a1 = (AVal) aVal.value;
				final PrimitiveOperations<AVal> primOps = ai.primitiveOperations();
				final IValue computed = op.apply(embedLeft ? a1 : a2, embedLeft ? a2 : a1, expr, monads.valueMonad, primOps);
				setResult(computed);
			}
		};
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected IValue interpretIntrinsic(final ExecutionState<AHeap, Context> es, final InvokeExpr op) {
		final NumberedString subSig = op.getMethodRef().getSubSignature();
		if(op.getMethodRef().name().startsWith("assertEqual") || op.getMethodRef().name().startsWith("assertNotEqual") && !ai.modelsType(op.getArg(0).getType())) {
			// eval the two args, but ignore
			this.interpretValue(es, op.getArg(0));
			this.interpretValue(es, op.getArg(1));
			return null;
		}
		if(subSig == this.allocAppTypeSigStr) {
			final String className = ((StringConstant) op.getArg(0)).value;
			return allocateType(es, op, className);
		} else if(subSig == this.allocAppTypeSigClass) {
			final String jvmRepr = ((ClassConstant)op.getArg(0)).value;
			final String languageRepr = jvmRepr.substring(1, jvmRepr.length() - 1).replace('/', '.');
			return allocateType(es, op, languageRepr);
		} else if(subSig == this.allocAppTypeSigInt) {
			final IValue arg = this.interpretValue(es, op.getArg(0));
			final IValue lifted;
			
			if(arg.getTag() == RuntimeTag.EMBEDDED) {
				assert !(arg.aVal.value instanceof CombinedValue);
				@SuppressWarnings("unchecked")
				final AVal av = (AVal) arg.aVal.value;
				if(av instanceof Concretizable && ((Concretizable) av).concretizable()) {
					lifted = IValue.liftNative(((Concretizable)av).concretize());
				} else {
					lifted = IValue.nondet();
				}
			} else {
				lifted = arg;
			}

			if(lifted.getTag() == RuntimeTag.NONDET || lifted.valueStream().exists(iv -> ReflectionEnvironment.v().resolve(iv.asInt()).isNone())) {
				return nondetAllocation(es, op);
			}

			final StateAccumulator<AHeap> accum = new StateAccumulator<>();
			final IValue v = lifted.mapValue(new IValueTransformer() {
				@Override
				public IValue transform(final IValue v, final boolean isMulti) {
					if(v.getTag() == RuntimeTag.NONDET) {
						throw new UnsupportedOperationException();
					}
					assert v.getTag() == RuntimeTag.INT;
					final ExecutionState<AHeap, Context> forked = es.fork();
					final IValue alloced = allocateType(forked, op, ReflectionEnvironment.v().resolve(v.asInt()).some().getName());
					accum.update(forked.heap, forked.foreignHeap);
					return alloced;
				}
			});
			accum.applyToState(es);
			return v;
		} else if(subSig == invokeSigObjObj || subSig == invokeSigObjInt || subSig == invokeSigIntObj || subSig == invokeSigIntInt
				|| subSig == invokeSigIntNull || subSig == invokeSigObjNull) {
			final IValue receiver = this.interpretValue(es, op.getArg(0));
			if(receiver.getTag() == RuntimeTag.NULL) {
				throw new NullPointerException();
			}
			final IValue klassKey = this.interpretValue(es, op.getArg(1));
			final IValue methodKey = this.interpretValue(es, op.getArg(2));
			final IValue arg;
			final List<IValue> argList;
			final List<Value> argumentExprList;

			if(op.getArgCount() > 3) {
				arg = this.interpretValue(es, op.getArg(3));
				argList = Collections.singletonList(arg);
				argumentExprList = Collections.singletonList(op.getArg(3));
			} else {
				arg = null;
				argList = Collections.emptyList();
				argumentExprList = Collections.emptyList();
			}
			final ExecutionState<AHeap, Context> esForCall = es.withHeap(es.heap.collapseToSingle());

			final AVal abstractComponent;
			final IValue concreteComponent;
			if(receiver.getTag() == RuntimeTag.EMBEDDED) {
				if(receiver.aVal.value instanceof CombinedValue) {
					final CombinedValue comb = (CombinedValue) receiver.aVal.value;
					abstractComponent = (AVal) comb.abstractComponent;
					concreteComponent = comb.concreteComponent;
				} else {
					abstractComponent = (AVal) receiver.aVal.value;
					concreteComponent = null;
				}
			} else {
				abstractComponent = null;
				concreteComponent = receiver;
			}
			final List<IValue> returnVals = new ArrayList<>();
			final StateAccumulator<AHeap> resultAccum;
			final Pair<Stream<SootMethod>, Stream<Pair<IValue, SootMethod>>> resolvedCallees = this
					.resolveInvokees((StaticInvokeExpr) op, abstractComponent, concreteComponent, klassKey, methodKey, arg, MetaInterpreter.makeReflectiveAllocationContext((StaticInvokeExpr) op));
			if(resolvedCallees.getO1().isEmpty() && resolvedCallees.getO2().isEmpty()) {
				throw new PruneExecutionException();
			}
			if(Interpreter.LOG_REFLECTION) {
				MetaInterpreter.logInvoke(resolvedCallees, true);
			}
			boolean isIncomplete = false;
			if(resolvedCallees.getO2().isNotEmpty()) {
				final Pair<IValue, StateAccumulator<AHeap>> handled = this.handleDispatch(esForCall, resolvedCallees.getO2(), argList, op, true);
				if(handled.getO1() != null) {
					returnVals.add(handled.getO1());
				}
				resultAccum = handled.getO2();
			} else {
				resultAccum = new StateAccumulator<>();
			}
			if(resolvedCallees.getO1().isNotEmpty()) {
				isIncomplete = !this.interpretAbstractCall(esForCall, abstractComponent, argList, op, resolvedCallees.getO1(), resultAccum, returnVals, argumentExprList);
			}
			if(isIncomplete) {
				throw new PruneExecutionException();
			}
			resultAccum.applyToState(es);
			if(returnVals.isEmpty()) {
				return IValue.nullConst();
			} else {
				return IValue.lift(returnVals);
			}
		} else if(subSig == this.getClassSig) {
			final IValue v = this.interpretValue(es, op.getArg(0));
			if(v.getTag() == RuntimeTag.EMBEDDED) {
				return IValue.nondet();
			} else {
				return resolveRuntimeType(v);
			}
		} else {
			return super.interpretIntrinsic(es, op);
		}
	}

	private IValue resolveRuntimeType(final IValue v) {
		final Stream<IValue> bound = v.valueStream().bind(iv -> {
			if(iv.getTag() == RuntimeTag.NULL) {
				return Stream.nil();
			} else if(iv.getTag() == RuntimeTag.BOUNDED_OBJECT) {
				return Stream.single(IValue.nondet());
			} else if(iv.getTag() == RuntimeTag.OBJECT) {
				return Stream.single(IValue.lift(ReflectionEnvironment.v().getKeyForClass(iv.getSootClass().getType())));
			} else {
				return Stream.nil();
			}
		});
		if(bound.isEmpty()) {
			throw new ReflectiveOperationException();
		}
		return bound.foldLeft1(ValueMerger.STRICT_MERGE::merge);
	}

	/*
	 * Public for the meta interpreter. No way we're duplicating this logic, jfc
	 */
	public Pair<Stream<SootMethod>, Stream<Pair<IValue, SootMethod>>> resolveInvokees(final StaticInvokeExpr op,
			final AVal abstractComponent, final IValue concreteComponent, final IValue klassKey, final IValue methodKey, final IValue arg, final ReflectiveOperationContext ctxt) {
		return resolveInvokees(op, abstractComponent, concreteComponent, klassKey, methodKey, arg,
			toCheck -> toCheck.valueStream().forall(wr -> wr.isNonDet() || wr.getTag() == RuntimeTag.INT || wr.getTag() == RuntimeTag.EMBEDDED),
				(i, t) -> interpretInstanceCheck(i, t) != ObjectIdentityResult.MUST_NOT_BE, ai, ctxt, this.oracle);
	}

	private interface InvokeResolution<Recv, V> {
		Stream<SootClass> getTargetClassStream(Recv r, Value receiverArg, InvokeMoke mode);
		Stream<V> generateForMethod(Recv r, SootMethod m);
	}

	private interface CompatibilityChecker {
		boolean argCompatible(SootMethod m);
		boolean returnCompatible(Type t);

		static CompatibilityChecker ofSingle(final F<Type, Boolean> checkTypeCompat, final F<Type, Boolean> checkReturnCompat) {
			return new CompatibilityChecker() {
				@Override public boolean argCompatible(final SootMethod m) {
					return m.getParameterCount() == 1 && checkTypeCompat.f(m.getParameterType(0));
				}

				@Override public boolean returnCompatible(final Type t) {
					return checkReturnCompat.f(t);
				}
			};
		}

		static CompatibilityChecker ofNull(final F<Type, Boolean> checkReturnCompat) {
			return new CompatibilityChecker() {
				@Override public boolean argCompatible(final SootMethod m) {
					return m.getParameterCount() == 0;
				}

				@Override public boolean returnCompatible(final Type t) {
					return checkReturnCompat.f(t);
				}
			};
		}
	}



	/*
	 * Public for the meta interpreter. No way we're duplicating this logic, jfc
	 */
	public static <V, AVal> Pair<Stream<SootMethod>, Stream<Pair<IValue, SootMethod>>> resolveInvokees(final StaticInvokeExpr op,
			final AVal abstractComponent, final IValue concreteComponent, final IValue klassKey, final IValue methodKey, final V arg,
			final F<V, Boolean> checkIntType, final F2<V, Type, Boolean> checkRefType, final AbstractInterpretation<AVal, ?, ?, ?> ai,
			final ReflectiveOperationContext ctxt, final TypeOracle oracle) {
		final NumberedString subSig = op.getMethodRef().getSubSignature();
		final CompatibilityChecker cc;
		final F<Type, Boolean> checkReturnCompat;
		if(subSig.toString().equals(INVOKE_OBJ_OBJ) || subSig.toString().equals(INVOKE_OBJ_INT) || subSig.toString().equals(INVOKE_OBJ_NULL)) {
			final RefLikeType returnBound = ctxt.castHint().orSome(Scene.v().getObjectType());
			checkReturnCompat = t -> (t == VoidType.v() || (t instanceof RefLikeType && Scene.v().getOrMakeFastHierarchy().canStoreType(t, returnBound)));
		} else {
			checkReturnCompat = t -> t == IntType.v();
		}
		if(op.getArgCount() == 3) {
			assert subSig.toString().equals(INVOKE_INT_NULL) || subSig.toString().equals(INVOKE_OBJ_NULL);
			cc = CompatibilityChecker.ofNull(checkReturnCompat);
		} else {
			final F<Type, Boolean> checkTypeCompat;
			if(subSig.toString().equals(INVOKE_OBJ_INT) || subSig.toString().equals(INVOKE_INT_INT)) {
				checkTypeCompat = t -> t == IntType.v() && checkIntType.f(arg);
			} else {
				assert subSig.toString().equals(INVOKE_OBJ_OBJ) || subSig.toString().equals(INVOKE_INT_OBJ);
				checkTypeCompat = t -> t instanceof RefLikeType && checkRefType.f(arg, t);
			}
			cc = CompatibilityChecker.ofSingle(checkTypeCompat, checkReturnCompat);
		}
		final Stream<SootMethod> resolvedAbstractStream;
		if(abstractComponent != null) {
			resolvedAbstractStream = resolveStream(abstractComponent, klassKey, methodKey, new InvokeResolution<AVal, SootMethod>() {
				@Override public Stream<SootClass> getTargetClassStream(final AVal r, final Value receiverArg, final InvokeMoke mode) {
					return CooperativeInterpreter.getTargetClassStream(r, receiverArg, mode, ai);
				}

				@Override public Stream<SootMethod> generateForMethod(final AVal r, final SootMethod m) {
					if(filterAbstractReceivers(r, m, ai)) {
						return Stream.single(m);
					} else {
						return Stream.nil();
					}
				}
			}, cc, op, oracle, TypeOwner.FRAMEWORK);
		} else {
			resolvedAbstractStream = Stream.nil();
		}

		final Stream<Pair<IValue, SootMethod>> resolvedConcreteStream;
		if(concreteComponent != null) {
			final Stream<IValue> receiverStream = concreteComponent.valueStream().filter(v -> v.getTag() != RuntimeTag.NULL && v.getTag() != RuntimeTag.ARRAY);
			resolvedConcreteStream = resolveStream(receiverStream, klassKey, methodKey, CONCRETE_RESOLVER, cc, op, oracle, TypeOwner.APPLICATION);
		} else {
			resolvedConcreteStream = Stream.nil();
		}
		return new Pair<>(resolvedAbstractStream, resolvedConcreteStream);
	}

	public static <V, Recv> Stream<V> resolveStream(final Recv receiverStream, final IValue klassKey, final IValue methodKey, final InvokeResolution<Recv, V> resolver,
			final CompatibilityChecker cc, final StaticInvokeExpr op, final TypeOracle oracle, final TypeOwner excludedTypeOwner) {
		final Stream<V> resolvedConcreteStream;
		if(isClassNonDeterministic(klassKey) && methodKey.isNonDet()) {
			/*
			 * _completely_ non-deterministic. In this case, we use the invoke mode to find the host classes,
			 * and search for all compatible methods within each of those classes
			 */
			resolvedConcreteStream = resolver.getTargetClassStream(receiverStream, op.getArg(0), ReflectionEnvironment.v().invokeResolutionMode()).bind(cls ->
					findCompatibleMethodsInClass(cls, cc).bind(method -> resolver.generateForMethod(receiverStream, method))
			);
		} else if(isClassNonDeterministic(klassKey) && !methodKey.isNonDet()) {
			/* non-deterministic class, but deterministic method. to resolve we:
			 * 1) use the invoke mode to figure out where look for these methods
			 * 2) resolve those methods
			 */
			final Stream<Integer> methodStream = Stream.iterableStream(methodKey).map(IValue::asInt);
			resolvedConcreteStream = resolver.getTargetClassStream(receiverStream, op.getArg(0), ReflectionEnvironment.v().invokeResolutionMode()).bind(cls ->
					resolveReferences(cls, methodStream, cc).bind(method -> resolver.generateForMethod(receiverStream, method))
			);
		} else if(!isClassNonDeterministic(klassKey) && methodKey.isNonDet()) {
			/*
			 * deterministic(-ish) class but non-deterministic method. Enumerate the classes, and then
			 * search for compatible methods (using whatever run-time information may be present
			 */
			resolvedConcreteStream = klassKey.valueStream().map(IValue::asInt).bind(k -> {
				final SootClass cls = ReflectionEnvironment.v().resolve(k).some();
				if(isTypeExcluded(oracle, excludedTypeOwner, cls)) {
					return Stream.nil();
				}
				return findCompatibleMethodsInClass(cls, cc).bind(method -> resolver.generateForMethod(receiverStream, method));
			});
		} else {
			final Stream<Integer> methodStream = Stream.iterableStream(methodKey).map(IValue::asInt);
			final Stream<Integer> classStream = klassKey.valueStream().map(IValue::asInt);
			resolvedConcreteStream = classStream.bind(k -> {
				final SootClass cls = ReflectionEnvironment.v().resolve(k).some();
				if(isTypeExcluded(oracle, excludedTypeOwner, cls)) {
					return Stream.nil();
				}
				return resolveReferences(cls, methodStream, cc).bind(meth -> {
					return resolver.generateForMethod(receiverStream, meth);
				});
			});
			// deterministic-ish
		}
		return resolvedConcreteStream;
	}

	protected static boolean isTypeExcluded(final TypeOracle oracle, final TypeOwner excludedTypeOwner, final SootClass cls) {
		final TypeOwner typeOwner = oracle.classifyType(cls.getName());
		return typeOwner == excludedTypeOwner || typeOwner == TypeOwner.IGNORE;
	}

	protected static boolean isClassNonDeterministic(final IValue klassKey) {
		return klassKey.isNonDet() || klassKey.valueStream().exists(iv -> ReflectionEnvironment.v().resolve(iv.asInt()).isNone());
	}

	private static <AVal> boolean filterAbstractReceivers(final AVal v, final SootMethod meth, final AbstractInterpretation<AVal, ?, ?, ?> ai) {
		return ai.objectOperations().isInstanceOf(v, meth.getDeclaringClass().getType()) != ObjectIdentityResult.MUST_NOT_BE;
	}


	private static Stream<SootClass> getTargetClassStream(final Stream<IValue> receiverStream, final Value receiverArg, final InvokeMoke invokeResolutionMode) {
		// use the inferred type of the argument
		if(invokeResolutionMode == InvokeMoke.UseDeclaredType || invokeResolutionMode == InvokeMoke.UseTransitiveDeclaredType) {
			if(!(receiverArg.getType() instanceof RefType)) {
				return Stream.nil();
			}
			final SootClass cls = ((RefType) receiverArg.getType()).getSootClass();
			if(invokeResolutionMode == InvokeMoke.UseTransitiveDeclaredType) {
				return toTransitiveTypes(cls);
			} else {
				return Stream.single(cls);
			}
		} else if(invokeResolutionMode == InvokeMoke.UseRuntimeType) {
			return receiverStream.map(CooperativeInterpreter::toRuntimeClass);
		} else {
			return receiverStream.map(CooperativeInterpreter::toRuntimeClass).bind(CooperativeInterpreter::toTransitiveTypes);
		}
	}
	
	private static <AVal> Stream<SootClass> getTargetClassStream(final AVal receiver, final Value receiverArg,
			final InvokeMoke invokeResolutionMode, final AbstractInterpretation<AVal, ?, ?, ?> ai) {
		// use the inferred type of the argument
		if(invokeResolutionMode == InvokeMoke.UseDeclaredType || invokeResolutionMode == InvokeMoke.UseTransitiveDeclaredType) {
			if(!(receiverArg.getType() instanceof RefType)) {
				return Stream.nil();
			}
			final SootClass cls = ((RefType) receiverArg.getType()).getSootClass();
			if(invokeResolutionMode == InvokeMoke.UseTransitiveDeclaredType) {
				return toTransitiveTypes(cls);
			} else {
				return Stream.single(cls);
			}
		} else if(invokeResolutionMode == InvokeMoke.UseRuntimeType) {
			return Stream.iterableStream(ai.objectOperations().possibleTypes(receiver)).bind(t -> {
				if(t instanceof RefType) {
					return Stream.single(((RefType) t).getSootClass());
				} else if(t instanceof AnySubType) {
					return Stream.single(((AnySubType) t).getBase().getSootClass());
				} else {
					return Stream.nil();
				}
			});
		} else {
			return Stream.iterableStream(ai.objectOperations().possibleTypes(receiver)).bind(t -> {
				if(t instanceof RefType) {
					return Stream.iterableStream(BodyManager.enumerateApplicationClasses((RefType)t));
				} else if(t instanceof AnySubType) {
					return Stream.iterableStream(BodyManager.enumerateApplicationClasses(((AnySubType)t).getBase()));
				} else {
					return Stream.nil();
				}
			});
		}
	}

		
	private static SootClass toRuntimeClass(final IValue i) {
		if(i.getTag() == RuntimeTag.BOUNDED_OBJECT) {
			return i.boundedType.getBase().getSootClass();
		} else {
			return i.getSootClass();
		}
	}

	private static Stream<SootClass> toTransitiveTypes(final SootClass cls) {
		if(cls.isInterface()) {
			return Stream.iterableStream(BodyManager.enumerateFrameworkClasses(cls.getType()));
		} else if(cls.getName().equals("java.lang.Object")) {
			return Stream.iterableStream(BodyManager.enumerateFrameworkClasses());
		} else {
			assert Scene.v().getOrMakeFastHierarchy().getSubclassesOf(cls).isEmpty();
			return Stream.single(cls);
		}
	}

	private static Stream<Pair<IValue, SootMethod>> generateReceiverPairs(final Stream<IValue> receivers, final SootMethod callee) {
		return receivers.bind(recv -> {
			assert recv.getTag() == RuntimeTag.OBJECT || recv.getTag() == RuntimeTag.BOUNDED_OBJECT : recv;
			if((recv.getTag() == RuntimeTag.BOUNDED_OBJECT && !Scene.v().getOrMakeFastHierarchy().canStoreType(recv.boundedType, callee.getDeclaringClass().getType())) ||
					(recv.getTag() == RuntimeTag.OBJECT && !Scene.v().getOrMakeFastHierarchy().canStoreType(recv.getSootClass().getType(), callee.getDeclaringClass().getType()))) {
				return Stream.nil();
			}
			return Stream.single(new Pair<>(recv, callee));
		});
	}
	
	private static Stream<SootMethod> resolveReferences(final SootClass klass, final Stream<Integer> methodKeys, 
			final CompatibilityChecker cc) {
		return methodKeys.bind(key -> {
			return ReflectionEnvironment.v().resolve(klass, key).filter(m -> isCompatibleMethod(m, cc)).map(Stream::single).orSome(Stream.nil());
		});
	}
	
	private static boolean isCompatibleMethod(final SootMethod m, final CompatibilityChecker cc) {
		return !m.isPrivate() && !m.isStatic() && !m.isConstructor() && m.isConcrete() && cc.argCompatible(m) && cc.returnCompatible(m.getReturnType());
	}

	private static Stream<SootMethod> findCompatibleMethodsInClass(final SootClass cls, final CompatibilityChecker cc) {
		return Stream.iterableStream(cls.getMethods()).filter(m -> {
			return isCompatibleMethod(m, cc);
		});
	}

	static class AllocResult<AVal> {
		private static final AllocResult<?> INCOMPL_ = new AllocResult<>(true);
		private static final AllocResult<?> EMPTY_ = new AllocResult<>(false);
		private final Object ret;
		private final boolean incompleteFlag;

		private AllocResult(final Object ret) {
			this.ret = ret;
			this.incompleteFlag = false;
		}
		private AllocResult(final boolean incompleteFlag) {
			this.ret = null;
			this.incompleteFlag = incompleteFlag;
		}
		@SuppressWarnings("unchecked")
		public AVal result() {
			assert this.ret != null;
			return (AVal) this.ret;
		}
		public boolean isIncomplete() {
			return ret == null && this.incompleteFlag;
		}
		public boolean isEmpty() {
			return ret == null && !this.incompleteFlag;
		}
		@SuppressWarnings("unchecked")
		public static <AVal> AllocResult<AVal> empty() {
			return (AllocResult<AVal>) EMPTY_;
		}
		@SuppressWarnings("unchecked")
		public static <AVal> AllocResult<AVal> incomplete() {
			return (AllocResult<AVal>) INCOMPL_;
		}
		public static <AVal> AllocResult<AVal> v(final AVal v) {
			return new AllocResult<>(v);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected ObjectIdentityResult interpretInstanceCheck(final IValue arg, final Type type) {
		if(type instanceof PrimType) {
			return ObjectIdentityResult.MUST_NOT_BE;
		}
		if(arg.getTag() == RuntimeTag.EMBEDDED) {
			final Object aVal = arg.aVal.value;
			if(aVal instanceof CombinedValue) {
				return super.interpretInstanceCheck(((CombinedValue) aVal).concreteComponent, type).join(
						ai.objectOperations().isInstanceOf((AVal) ((CombinedValue) aVal).abstractComponent, type));
			} else {
				return ai.objectOperations().isInstanceOf((AVal)aVal, type);
			}
		} else {
			return super.interpretInstanceCheck(arg, type);
		}
	}

	private IValue nondetAllocation(final ExecutionState<AHeap, Context> es, final InvokeExpr op) {
		final ReflectiveOperationContext ctxt = MetaInterpreter.makeReflectiveAllocationContext((StaticInvokeExpr) op);
		final StateAccumulator<AHeap> accum = new StateAccumulator<>();
		final AllocResult<AVal> nondetApp = nondetAppAlloc(es, accum, ctxt, op);
		final IValue nondetConcrete = nondetConcreteAlloc(es, accum, op, ctxt);
		final IValue result;
		if(nondetApp.isIncomplete()) {
			throw new PruneExecutionException();
		}
		if(nondetApp.isEmpty()) {
			result = nondetConcrete;
		} else if(nondetConcrete == null) {
			result = convertToConcrete(monads.valueMonad.lift(nondetApp.result()));
		} else {
			result = IValue.merge(nondetConcrete, convertToConcrete(monads.valueMonad.lift(nondetApp.result())));
		}
		assert result != null;
		accum.applyToState(es);
		es.heap.assertSaneStructure();
		return result;
	}

	private IValue nondetConcreteAlloc(final ExecutionState<AHeap, Context> es,
			final StateAccumulator<AHeap> accum, final InvokeExpr op, final ReflectiveOperationContext ctxt) {
		final RefType upperBound = (RefType) ctxt.castHint().orSome(Scene.v().getObjectType());
		if(!BodyManager.enumerateFrameworkClasses(upperBound).iterator().hasNext()) {
			return null;
		}
		final IValue alloced = es.heap.allocateBoundedType(upperBound, op, createAllocationContext(es));
		final Stream<Pair<IValue, SootMethod>> mapped = Stream.iterableStream(this.resolveMethod(AnySubType.v(upperBound),
				MetaInterpreter.getNullaryConstructorRef())).map(m -> new Pair<>(alloced, m));
		final Heap collapsed = es.heap.collapseToSingle();
		final ExecutionState<AHeap, Context> esForConstructor = es.withHeap(collapsed);
		this.handleDispatch(esForConstructor, mapped, Collections.emptyList(), op, accum);
		return alloced;
	}

	@SuppressWarnings("unchecked")
	private AllocResult<AVal> nondetAppAlloc(final @MutatesFork ExecutionState<AHeap, Context> es, final StateAccumulator<AHeap> accum,
			final ReflectiveOperationContext ctxt, final InvokeExpr op) {
		final Context allocCtxt = ai.contextManager().contextForAllocation(Either.right(es), op);
		final AHeap allocState = getAllocState(es);
		final Option<Pair<AHeap, AVal>> alloced_ = ai.allocateUnknownObject(allocState, op, allocCtxt, ctxt);
		if(alloced_.isNone()) {
			return AllocResult.empty();
		}
		final Pair<AHeap, AVal> alloced = alloced_.some();
		final SootMethodRefImpl constrSig = MetaInterpreter.getNullaryConstructorRef();
		final List<SootMethod> constructors = ai.getMethodForRef(constrSig, alloced.getO2());
		final AS initState = stateManipulator.inject(stateManipulator.emptyState(), alloced.getO1());
		final Heap collapsed = es.heap.collapseToSingle();
		boolean hasResult = false;
		for(final SootMethod constr : constructors) {
			final Heap forked = collapsed.fork();
			final Option<MethodResult> result_ = doAbstractCall(es, op, initState, constr, alloced.getO2(), Collections.emptyList(), forked);
			if(result_.isNone()) {
				continue;
			}
			hasResult = true;
			assert result_.some().getReturnValue() == null;
			final MergeContext context = new MergeContext(alloced.getO2(), Collections.emptyList(), constr, true);
			accum.update(forked, getEmbeddedReturnHeap(initState, ((InstrumentedStateImpl<AS>)result_.some().getState()).state, context));
		}
		if(!hasResult) {
			return AllocResult.incomplete();
		} else {
			return AllocResult.v(alloced.getO2());
		}
	}

	public IValue allocateType(final @RequiresForked ExecutionState<AHeap, Context> es, final InvokeExpr op, final String className) {
		if(oracle.classifyType(className) == TypeOwner.APPLICATION) {
			return reflectiveApplicationAllocation(es, op, className);
		} else {
			return allocateConcreteType(es, op, className);
		}
	}

	public IValue allocateConcreteType(final @RequiresForked ExecutionState<AHeap, Context> es, final InvokeExpr op, final String className) {
		final SootClass loadedClass = BodyManager.loadClass(className);
		final IValue allocated = super.allocateObject(es, op, loadedClass);
		final IValue returned = this.interpretCall(es, allocated, op, loadedClass.getMethod("void <init>()").makeRef(), Collections.emptyList());
		assert returned == null;
		return allocated;
	}
	
	private IValue reflectiveApplicationAllocation(final @RequiresForked ExecutionState<AHeap, Context> es, final InvokeExpr op, final String className) {
		final SootClass loadClass = BodyManager.loadClass(className);
		final AHeap toAlloc = getAllocState(es);
		final Context allocContext = ai.contextManager().contextForAllocation(Either.right(es), op);
		final Pair<AHeap, AVal> alloced = ai.abstractObjectAlloc(loadClass.getType(), toAlloc, op, allocContext);
		final AS initState = stateManipulator.inject(stateManipulator.emptyState(), alloced.getO1());
		final SootMethod callee = loadClass.getMethod("void <init>()");
		final AVal abstractReceiver = alloced.getO2();
		final List<Object> argValues = Collections.emptyList();
		final Heap forkedCollapse = es.heap.collapseToSingle().fork();
		final Option<MethodResult> summary_ = doAbstractCall(es, op, initState, callee, abstractReceiver, argValues, forkedCollapse);
		return this.mapOrIncomplete(summary_, summary -> { 
			assert summary.getReturnValue() == null;
			@SuppressWarnings("unchecked")
			final InstrumentedStateImpl<AS> returnState = (InstrumentedStateImpl<AS>) summary.getState();
			final MergeContext ctxt = new MergeContext(abstractReceiver, argValues, callee, true);
			es.replaceHeap(this.getEmbeddedReturnHeap(initState, returnState.state, ctxt));
			es.heap.applyHeap(forkedCollapse);
			return convertToConcrete(abstractReceiver);
		});	
	}

	private EmbeddedState<AHeap> getEmbeddedReturnHeap(final AS callingState, final AS returnedState, final MergeContext context) {
		return new EmbeddedState<>(stateManipulator.merge(stateManipulator.project(callingState), stateManipulator.project(returnedState), context), heapLattice);
	}
	
	private <R> R mapOrIncomplete(final Option<MethodResult> m, final F<MethodResult, R> map) {
		return m.map(map).orSome(() -> {
			this.incompleteExecutionCallback();
			throw new PruneExecutionException();
		});
	}

	private AHeap getAllocState(final ExecutionState<AHeap, Context> es) {
		final AHeap toAlloc;
		if(es.foreignHeap == null) {
			toAlloc = stateManipulator.project(stateManipulator.emptyState());
		} else {
			toAlloc = es.foreignHeap.state;
		}
		return toAlloc;
	}

	private Option<MethodResult> doAbstractCall(final ExecutionState<AHeap, Context> es, final InvokeExpr op, final AS initState, final SootMethod callee, final AVal abstractReceiver,
			final List<Object> argValues, final Heap forked) {
		final InstrumentedState startState = monads.stateMonad.lift(initState, forked);
		final Option<MethodResult> summary;
		final Context calleeContext = ai.contextManager().contextForCall(Either.right(es),
				callee, abstractReceiver, argValues, op);
		forked.assertSaneStructure();
		if(Interpreter.TRACK_CALL_EDGES) {
			CALL_GRAPH.put(this.createCallSiteNode(op, es), callee);
		}
		if(es.rootContext == null) {
			summary = Option.some(ai.interpretToFixpoint(callee, abstractReceiver, argValues, startState, calleeContext));
		} else {
			summary = ai.handleCall(callee, abstractReceiver, argValues, startState, calleeContext, es.rootContext);
			assert summary.isNone() || summary.some() != null : startState;
		}
		return summary;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected IValue interpretArrayRead(final ExecutionState<AHeap, Context> es, final ArrayRef aOp, final IValue base, final IValue index) {
		if(!base.isEmbedded()) {
			return super.interpretArrayRead(es, aOp, base, index);
		}
		final Object aBase = base.aVal.value;
		if(aBase instanceof CombinedValue) {
			final CombinedValue cv = (CombinedValue) aBase;
			IValue readConcrete;
			try {
				readConcrete = super.interpretArrayRead(es, aOp, cv.concreteComponent, index);
			} catch(final OutOfBoundsArrayAccessException | NullPointerException e) {
				readConcrete = null;
			}
			final Option<IValue> readAbstract = this.doAbstractArrayRead(es, aOp, (AVal) cv.abstractComponent, index);
			if(readConcrete == null && readAbstract.isNone()) {
				throw new OutOfBoundsArrayAccessException();
			} else if(readConcrete == null) {
				return readAbstract.some();
			} else if(readAbstract.isNone()) {
				return readConcrete;
			} else {
				return ValueMerger.STRICT_MERGE.merge(readConcrete, readAbstract.some());
			}
		} else {
			return doAbstractArrayRead(es, aOp, (AVal) aBase, index).orSome(() -> { throw new OutOfBoundsArrayAccessException(); });
		}
	}

	private Option<IValue> doAbstractArrayRead(final ExecutionState<AHeap, Context> es, final ArrayRef aOp, final AVal aBase, final IValue index) {
		return ai.objectOperations().readArray(es.foreignHeap.state, aBase, monads.valueMonad.alpha(monads.valueMonad.lift(index)), aOp).map(this::convertToConcrete);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected IValue interpretArrayLength(final ExecutionState<AHeap, Context> es, final IValue base) {
		if(!base.isEmbedded()) {
			return super.interpretArrayLength(es, base);
		}
		final Object abstractArray = base.aVal.value;
		if(abstractArray instanceof CombinedValue) {
			final CombinedValue cv = (CombinedValue) abstractArray;
			final IValue abstractLength = getAbstractArrayLength(es, (AVal) cv.abstractComponent);
			final IValue concreteLength = super.interpretArrayLength(es, cv.concreteComponent);
			return ValueMerger.STRICT_MERGE.merge(abstractLength, concreteLength);
		} else {
			return getAbstractArrayLength(es, (AVal) abstractArray);
		}
	}

	private IValue getAbstractArrayLength(final ExecutionState<AHeap, Context> es, final AVal aArray) {
		return convertToConcrete(monads.valueMonad.lift(ai.objectOperations().arrayLength(aArray, es.foreignHeap.state)));
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected IValue interpretCast(final IValue toCast, final Type castType) {
		if(!toCast.isEmbedded()) {
			return super.interpretCast(toCast, castType);
		}
		final Object abstractCastee = toCast.aVal.value;
		if(abstractCastee instanceof CombinedValue) {
			final CombinedValue cv = (CombinedValue) abstractCastee;
			final IValue casted = cv.concreteComponent.downCast(castType);
			final AVal aCasted = ai.objectOperations().downcast((AVal) cv.abstractComponent, castType);
			if(aCasted instanceof BottomAware && ((BottomAware) aCasted).isBottom()) {
				if(aCasted == null) {
					throw new ClassCastException();
				}
				return casted;
			}
			if(casted == null) {
				return convertToConcrete(monads.valueMonad.lift(aCasted));
			} else {
				return convertToConcrete(new CombinedValue(casted, aCasted));
			}
		} else {
			return convertToConcrete(monads.valueMonad.lift(ai.objectOperations().downcast((AVal) abstractCastee, castType)));
		}
	}
	
	@Override
	public void doArrayWrite(final ExecutionState<AHeap, Context> state, final ArrayRef arrayRef, final IValue arrayPtr,
			final IValue index, final IValue rawRhs, final boolean isWeakWrite) {
		if(!arrayPtr.isEmbedded()) {
			super.doArrayWrite(state, arrayRef, arrayPtr, index, rawRhs, isWeakWrite);
			return;
		}
		final Object base = arrayPtr.aVal.value;
		if(base instanceof CombinedValue) {
			final CombinedValue combinedValue = (CombinedValue) base;
			super.doArrayWrite(state, arrayRef, combinedValue.concreteComponent, index, rawRhs, true);
			final AHeap oldHeap = state.foreignHeap.state;
			@SuppressWarnings("unchecked")
			final AVal aBase = (AVal) combinedValue.abstractComponent;
			final AHeap newHeap = doAbstractArrayWrite(state, arrayRef, index, rawRhs, aBase);
			state.replaceHeap(new EmbeddedState<>(heapLattice.join(oldHeap, newHeap), heapLattice));
		} else {
			@SuppressWarnings("unchecked")
			final AVal aBase = (AVal) base;
			final AHeap newHeap = doAbstractArrayWrite(state, arrayRef, index, rawRhs, aBase);
			state.replaceHeap(new EmbeddedState<>(newHeap, heapLattice));
		}
	}

	private AHeap doAbstractArrayWrite(final ExecutionState<AHeap, Context> state, final ArrayRef arrayRef, final IValue index, final IValue rawRhs, final AVal aBase) {
		final AVal aIndex = monads.valueMonad.alpha(convertToAbstract(index));
		final AHeap newHeap = ai.objectOperations().writeArray(state.foreignHeap.state, aBase, aIndex, convertToAbstract(rawRhs), arrayRef);
		return newHeap;
	}
	
	// INSTRUMENTATION HOOKS
	
	@Override
	protected IValue fieldBasePointerHook(final ExecutionState<AHeap, Context> state, final IValue basePointer, final boolean isRead, final InstanceFieldRef iRef) {
		return instManager.postBase(state, basePointer, new HeapLocation(iRef.getFieldRef()), isRead);
	}
	
	@Override
	protected IValue fieldOutputHook(final ExecutionState<AHeap, Context> state, final IValue basePointer, final IValue outputValue,
			final boolean isRead, final InstanceFieldRef iOp) {
		return instManager.filterFieldOutput(state, basePointer, new HeapLocation(iOp.getFieldRef()), isRead, outputValue);
	}
	
	@Override
	protected IValue arrayBasePointerHook(final ExecutionState<AHeap, Context> state, final IValue basePointer, final boolean isRead, final ArrayRef arrayOp) {
		if(basePointer.isEmbedded()) {
			return basePointer;
		}
		final HeapLocation loc = getArrayLocation(arrayOp);
		return instManager.postBase(state, basePointer, loc, isRead);
	}

	private HeapLocation getArrayLocation(final ArrayRef arrayOp) {
		return new HeapLocation(arrayOp.getBase().getType(), arrayOp.getType());
	}
	
	@Override
	protected IValue arrayOutputHook(final ExecutionState<AHeap, Context> state, final IValue basePointer, final IValue outputValue, final boolean isRead, final ArrayRef arrayOp) {
		if(basePointer.isEmbedded()) {
			return outputValue;
		}
		final HeapLocation loc = getArrayLocation(arrayOp);
		return instManager.filterFieldOutput(state, basePointer, loc, isRead, outputValue);
	}
	
	@Override
	protected P3<IValue, List<IValue>, Option<IValue>> preCallHook(final ExecutionState<AHeap, Context> es, final IValue base, final List<IValue> args, final InvokeExpr op,
			final SootMethod method) {
		final PreCallInstrumentation<AHeap> preCall = instManager.preCallCoop(es.heap, getRawFH(es), base, args, method.makeRef(), op);
		if(preCall.fh != null) {
			es.replaceHeap(preCall.fh);
		}
		return P.p(preCall.receiver, preCall.arguments, preCall.summary);
	}
	
	@Override
	protected IValue postCallHook(final ExecutionState<AHeap, Context> es, final IValue base, final List<IValue> args, final IValue retVal, final InvokeExpr op) {
		final P2<IValue, EmbeddedState<AHeap>> postCall = instManager.postCallCoop(es.heap, getRawFH(es), base, args, retVal, op.getMethodRef(), op);
		if(postCall._2() != null) {
			es.replaceHeap(postCall._2());
		}
		return postCall._1();
	}

	private AHeap getRawFH(final ExecutionState<AHeap, Context> es) {
		return es.foreignHeap != null ? es.foreignHeap.state : stateManipulator.project(stateManipulator.emptyState());
	}
	
	protected void setBranchInterpreter(final BranchInterpreterImpl<AVal> branchInterpreter) {
		this.branchInterpreter = branchInterpreter;
	}
}
