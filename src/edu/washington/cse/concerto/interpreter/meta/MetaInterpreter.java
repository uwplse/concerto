package edu.washington.cse.concerto.interpreter.meta;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import edu.washington.cse.concerto.instrumentation.InstrumentationManager;
import edu.washington.cse.concerto.interpreter.BodyManager;
import edu.washington.cse.concerto.interpreter.HeapProvider;
import edu.washington.cse.concerto.interpreter.Interpreter;
import edu.washington.cse.concerto.interpreter.ReflectionEnvironment;
import edu.washington.cse.concerto.interpreter.ai.AbstractComparison;
import edu.washington.cse.concerto.interpreter.ai.AbstractInterpretation;
import edu.washington.cse.concerto.interpreter.ai.BranchAwareAbstractInterpretation;
import edu.washington.cse.concerto.interpreter.ai.BranchInterpreter;
import edu.washington.cse.concerto.interpreter.ai.CallHandler;
import edu.washington.cse.concerto.interpreter.ai.Concretizable;
import edu.washington.cse.concerto.interpreter.ai.EvalResult;
import edu.washington.cse.concerto.interpreter.ai.IntrinsicHandler;
import edu.washington.cse.concerto.interpreter.ai.MethodResult;
import edu.washington.cse.concerto.interpreter.ai.PathSensitiveAbstractInterpretation;
import edu.washington.cse.concerto.interpreter.ai.PathSensitiveBranchInterpreter;
import edu.washington.cse.concerto.interpreter.ai.PrettyPrintable;
import edu.washington.cse.concerto.interpreter.ai.ReflectiveOperationContext;
import edu.washington.cse.concerto.interpreter.ai.RelationalAbstractInterpretation;
import edu.washington.cse.concerto.interpreter.ai.RelationalCallHandler;
import edu.washington.cse.concerto.interpreter.ai.RelationalPathSensitiveAbstractInterpretation;
import edu.washington.cse.concerto.interpreter.ai.ResultCollectingAbstractInterpretation;
import edu.washington.cse.concerto.interpreter.ai.StateValueUpdater;
import edu.washington.cse.concerto.interpreter.ai.binop.ObjectIdentityResult;
import edu.washington.cse.concerto.interpreter.ai.binop.RelationalPrimitiveOperations;
import edu.washington.cse.concerto.interpreter.ai.injection.Injectable;
import edu.washington.cse.concerto.interpreter.ai.injection.NeedsMonads;
import edu.washington.cse.concerto.interpreter.ai.injection.NeedsStateMonad;
import edu.washington.cse.concerto.interpreter.ai.injection.NeedsValueLattice;
import edu.washington.cse.concerto.interpreter.ai.injection.NeedsValueMonad;
import edu.washington.cse.concerto.interpreter.ai.instantiation.array.ArrayBoundsChecker;
import edu.washington.cse.concerto.interpreter.ai.instantiation.optiflow.OptimisticInformationFlow;
import edu.washington.cse.concerto.interpreter.ai.instantiation.pta.BasicInterpreter;
import edu.washington.cse.concerto.interpreter.heap.Heap;
import edu.washington.cse.concerto.interpreter.lattice.Lattice;
import edu.washington.cse.concerto.interpreter.meta.Monads.InstrumentedStateImpl;
import edu.washington.cse.concerto.interpreter.meta.Monads.PlainStateImpl;
import edu.washington.cse.concerto.interpreter.meta.TypeOracle.TypeOwner;
import edu.washington.cse.concerto.interpreter.state.ExecutionState;
import edu.washington.cse.concerto.interpreter.state.GlobalState;
import edu.washington.cse.concerto.interpreter.state.InMemoryGlobalState;
import edu.washington.cse.concerto.interpreter.state.NondetGlobalState;
import edu.washington.cse.concerto.interpreter.state.PartialConcreteState;
import edu.washington.cse.concerto.interpreter.util.YamlParser;
import edu.washington.cse.concerto.interpreter.value.IValue;
import fj.F;
import fj.F2;
import fj.F3;
import fj.Ord;
import fj.Ordering;
import fj.P;
import fj.P2;
import fj.P3;
import fj.P4;
import fj.data.Either;
import fj.data.Option;
import fj.data.Set;
import fj.data.Stream;
import fj.function.Effect1;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import soot.ArrayType;
import soot.Local;
import soot.PatchingChain;
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
import soot.ValueBox;
import soot.VoidType;
import soot.grimp.internal.GNewInvokeExpr;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.ConditionExpr;
import soot.jimple.DefinitionStmt;
import soot.jimple.EqExpr;
import soot.jimple.GeExpr;
import soot.jimple.GtExpr;
import soot.jimple.IfStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.LeExpr;
import soot.jimple.LtExpr;
import soot.jimple.NeExpr;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.internal.AbstractOpStmt;
import soot.toolkits.scalar.Pair;

import javax.annotation.Nonnull;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class MetaInterpreter<AVal, AHeap, AS, Context> implements CombinedInterpretation {
	private final Lattice<AVal> valueLattice;
	private final Lattice<AHeap> heapLattice;
	private final Monads<AVal, AS> monads;
	private final CooperativeInterpreter<AVal, AHeap, AS, Context> coopInterpreter;
	private final SootClass mainClass;
	private final String mainMethod;
	private final AbstractInterpretation<AVal, AHeap, AS, Context> ai;

	private static final Multimap<P2<Object, InvokeExpr>, SootMethod> CALL_GRAPH = HashMultimap.create();

	public static final P2<int[], int[]> reflectiveCallMax = P.p(new int[] { 0 }, new int[] { 0 });

	private static void registerCall(final Object context, final InvokeExpr iExpr, final SootMethod m) {
		if(Interpreter.TRACK_CALL_EDGES) {
			CALL_GRAPH.put(P.p(context, iExpr), m);
		}
	}

	public static void logInvoke(final Pair<Stream<SootMethod>, Stream<Pair<IValue, SootMethod>>> resolvedCallees, final boolean isConcrete) {
		int[] refCell;
		if(isConcrete) {
			refCell = MetaInterpreter.reflectiveCallMax._2();
		} else {
			refCell = MetaInterpreter.reflectiveCallMax._1();
		}
		Set<SootMethod> m = Set.empty(Ord.ord((a, b) -> Ordering.fromInt(a.getSignature().compareTo(b.getSignature()))));
		Set<SootMethod> absCallees = resolvedCallees.getO1().foldLeft(Set::insert, m);
		Set<SootMethod> allCallees = resolvedCallees.getO2().foldLeft((a, b) -> a.insert(b.getO2()), absCallees);
		int callees = allCallees.size();
		refCell[0] = Math.max(refCell[0], callees);
	}

	@Override public AbstractInterpretation<?, ?, ?, ?> getAbstractInterpretation() {
		return ai;
	}

	private static <AVal, AHeap, AS, Context> void setupBranchInterpreters(
			final AbstractInterpretation<AVal, AHeap, AS, Context> ai,
			final Monads<AVal, AS> monads,
			final F<InstrumentedState, Heap> heapExtractor,
			final Effect1<BranchInterpreterImpl<AVal>> injectBranchInterp,
			final F2<InstrumentedState, AS, InstrumentedState> rewrapState,
			final F<InstrumentedState, AS> projectState
		) {
		final BranchInterpreterImpl<AVal> branchInterpreter = new BranchInterpreterImpl<>(monads.valueMonad, ai);
		if(ai instanceof RelationalPathSensitiveAbstractInterpretation) {
			final RelationalPathSensitiveAbstractInterpretation<AVal, AHeap, AS, Context> rAi = (RelationalPathSensitiveAbstractInterpretation<AVal, AHeap, AS, Context>) ai;
			rAi.setBranchInterpreter(new InstrumentedPathSensitiveBranchInterpreter<>(new PropagatingBranchInterpreter<AVal, InstrumentedState>(rAi, monads.valueMonad) {
					@Override
					protected Heap extractHeap(final InstrumentedState s) {
						return heapExtractor.f(s);
					}

					@Override
					protected InstrumentedState copyForBranch(final InstrumentedState s) {
						return s;
					}
					
					private Map<Unit, InstrumentedState> propagateRelational(final IfStmt stmt, final Map<Unit, InstrumentedState> propagated,
							final F3<InstrumentedState, Value, Value, InstrumentedState> propTrue, final F3<InstrumentedState, Value, Value, InstrumentedState> propFalse) {
						final Map<Unit, InstrumentedState> toReturn = new HashMap<>();
						final Unit trueTarget = stmt.getTarget();
						final ConditionExpr condition = (ConditionExpr) stmt.getCondition();
						for(final Map.Entry<Unit, InstrumentedState> targets : propagated.entrySet()) {
							if(targets.getKey() == trueTarget) {
								toReturn.put(targets.getKey(), propTrue.f(targets.getValue(), condition.getOp1(), condition.getOp2()));
							} else {
								toReturn.put(targets.getKey(), propFalse.f(targets.getValue(), condition.getOp1(), condition.getOp2()));
							}
						}
						return toReturn;
					}
					
					@Override
					public Map<Unit, InstrumentedState> interpretBranch(final IfStmt stmt, final Object op1, final Object op2, final InstrumentedState branchState,
							final StateValueUpdater<InstrumentedState> updater) {
						final Map<Unit, InstrumentedState> propagated = super.interpretBranch(stmt, op1, op2, branchState, updater);
						final ConditionExpr condition = (ConditionExpr) stmt.getCondition();
						final RelationalPrimitiveOperations<AVal, AS> relPrims = rAi.primitiveOperations();
						if(condition instanceof EqExpr) {
							return propagateRelational(stmt, propagated, relPrims::propagateRelationEQ, relPrims::propagateRelationNE);
						} else if(condition instanceof NeExpr) {
							return propagateRelational(stmt, propagated, relPrims::propagateRelationNE, relPrims::propagateRelationEQ);
						} else if(condition instanceof LtExpr) {
							return propagateRelational(stmt, propagated, relPrims::propagateRelationLT, relPrims::propagateRelationGE);
						} else if(condition instanceof LeExpr) {
							return propagateRelational(stmt, propagated, relPrims::propagateRelationLE, relPrims::propagateRelationGT);
						} else if(condition instanceof GtExpr) {
							return propagateRelational(stmt, propagated, relPrims::propagateRelationGT, relPrims::propagateRelationLE);
						} else if(condition instanceof GeExpr) {
							return propagateRelational(stmt, propagated, relPrims::propagateRelationGE, relPrims::propagateRelationLT);
						} else {
							throw new RuntimeException();
						}
					}
					
					@Override
					public List<Unit> interpretBranch(final IfStmt stmt, final Object op1, final Object op2, final HeapProvider stateProvider) {
						final List<Unit> valueRes = super.interpretBranch(stmt, op1, op2, stateProvider);
						final InstrumentedState state = (InstrumentedState)stateProvider.getState();
						final ConditionExpr condition = (ConditionExpr) stmt.getCondition();
						final Option<List<Unit>> mappedUnits = rAi.primitiveOperations().cmpRelational(state, condition.getOp1(), condition.getOp2()).map(cr ->
							AbstractComparison.abstractComparison(condition, cr, unknown(stmt), taken(stmt), fallthrough(stmt))
						);
						mappedUnits.foreachDoEffect(un ->
							assertConsistentTargets(un, valueRes)
						);
						return mappedUnits.orSome(valueRes);
					}

					private void assertConsistentTargets(final List<Unit> un, final List<Unit> valueRes) {
						for(final Unit u : un) {
							assert valueRes.contains(u);
						}
					}
				}, rewrapState, projectState));
			injectBranchInterp.f(new PropagatingBranchInterpreter<AVal, ExecutionState<AHeap, Context>>(rAi, monads.valueMonad) {
				@Override
				protected Heap extractHeap(final ExecutionState<AHeap, Context> s) {
					return s.heap;
				}

				@Override
				protected ExecutionState<AHeap, Context> copyForBranch(final ExecutionState<AHeap, Context> s) {
					return s.fork();
				}
			});
		} else if(ai instanceof PathSensitiveAbstractInterpretation) {
			final PathSensitiveAbstractInterpretation<AVal, AHeap, AS, Context> psAi = (PathSensitiveAbstractInterpretation<AVal, AHeap, AS, Context>)ai;
			psAi.setBranchInterpreter(new InstrumentedPathSensitiveBranchInterpreter<>(new PropagatingBranchInterpreter<AVal, InstrumentedState>(psAi, monads.valueMonad) {
				@Override
				protected Heap extractHeap(final InstrumentedState s) {
					return heapExtractor.f(s);
				}

				@Override
				protected InstrumentedState copyForBranch(final InstrumentedState s) {
					return s;
				}
			}, rewrapState, projectState));
			injectBranchInterp.f(new PropagatingBranchInterpreter<AVal, ExecutionState<AHeap, Context>>(psAi, monads.valueMonad) {
				@Override
				protected Heap extractHeap(final ExecutionState<AHeap, Context> s) {
					return s.heap;
				}

				@Override
				protected ExecutionState<AHeap, Context> copyForBranch(final ExecutionState<AHeap, Context> s) {
					return s.fork();
				}
			});
		} else if(ai instanceof BranchAwareAbstractInterpretation) {
			((BranchAwareAbstractInterpretation<AVal, AHeap, AS, Context>) ai).setBranchInterpreter(new BranchInterpreter<AVal, AS>() {
				@Override
				public List<Unit> interpretBranch(final IfStmt stmt, final Object op1, final Object op2, final InstrumentedState branchState) {
					final InstrumentedStateImpl<?> state_ = (InstrumentedStateImpl<?>) branchState;
					return branchInterpreter.interpretBranch(stmt, op1, op2, new HeapProvider() {
						
						@Override
						public Heap getHeap() {
							return state_.concreteHeap;
						}
						
						@Override
						public Object getState() {
							return branchState;
						}
					});
				}
				
				@Override
				public List<Unit> interpretBranch(final IfStmt stmt, final Object op1, final InstrumentedState branchState) {
					return branchInterpreter.interpretBranch(stmt, op1, new HeapProvider() {
						@Override
						public Heap getHeap() {
							return ((InstrumentedStateImpl<?>)branchState).concreteHeap;
						}

						@Override
						public Object getState() {
							return branchState;
						}
					});
				}
			});
		} else {
			injectBranchInterp.f(branchInterpreter);
		}
	}

	private static <AVal, AHeap, AS, Context> Option<MethodResult> postProcessInvokeResults(final AbstractInterpretation<AVal, AHeap, AS, Context> ai,
			final Monads<AVal, AS> monads, final Stream<Option<MethodResult>> invokeResultStream) {
		return invokeResultStream.map(m1 ->
				m1.map(mResult -> {
					if(mResult.getReturnValue() == null) {
						return new MethodResult(mResult.getState(), ai.alpha().lift(IValue.nullConst()));
					} else {
						return mResult;
					}
				})
		).foldLeft1((m1, m2) -> {
			if(m1.isNone()) {
				return m2;
			} else if(m2.isNone()) {
				return m1;
			} else {
				return m1.map(r1 -> monads.methodResultMonad.join(r1, m2.some()));
			}
		});
	}

	private abstract class AbstractCallHandler<RelationT> {
		protected final AbstractInterpretation<AVal, AHeap, AS, Context> ai;
		private final TypeOracle oracle;
		private final InstrumentationManager<AVal, AHeap, AS> manager;

		private AbstractCallHandler(final AbstractInterpretation<AVal, AHeap, AS, Context> ai, final TypeOracle oracle, final InstrumentationManager<AVal, AHeap, AS> manager) {
			this.ai = ai;
			this.oracle = oracle;
			this.manager = manager;
		}

		final class ForeignContext implements EmbeddedContext {
			private final Context wrapped;

			public ForeignContext(final Context c) {
				this.wrapped = c;
			}
			
			@Override
			public int hashCode() {
				if(wrapped == null) {
					return 131;
				} else {
					return wrapped.hashCode();
				}
			}

			@SuppressWarnings("unchecked")
			@Override
			public boolean equals(final Object obj) {
				if(obj == this) {
					return true;
				}
				if(obj == null) {
					return false;
				}
				if(this.getClass() != obj.getClass()) {
					return false;
				}
				final ForeignContext fc = (ForeignContext) obj;
				if((this.wrapped == null) != (fc.wrapped == null)) {
					return false;
				}
				return this.wrapped == null || this.wrapped.equals(fc.wrapped);
			}
			
			@Override
			public String toString() {
				return "FC[" + Objects.toString(wrapped) + "]";
			}
		}

		protected Option<MethodResult> handleCallInternal(final Context context, final InstanceInvokeExpr iie, final Object receiver,
				final List<Object> arguments, final InstrumentedState callState, final RelationT relationInfo) {
			if(iie instanceof SpecialInvokeExpr && iie.getMethodRef().getSignature().equals("<java.lang.Object: void <init>()>")) {
				return Option.some(new MethodResult(callState, null));
			}
			final ExplicitConcreteMethodCall explCall = new ExplicitConcreteMethodCall(coopInterpreter, iie);
			if(receiver instanceof IValue) {
				return translateToCallee(callState, coopInterpreter.handleConcreteCall(context, explCall, (IValue)receiver, arguments, callState));
			} else if(receiver instanceof CombinedValue) {
				final CombinedValue cVal = (CombinedValue) receiver;
				final Option<MethodResult> concreteResult = translateToCallee(callState, 
						coopInterpreter.handleConcreteCall(context, explCall, cVal.concreteComponent, arguments, callState)
				);
				final Object receiverCast = cVal.abstractComponent;
				final Option<MethodResult> abstractResult = doAbstractCall(context, new ExplicitACall<>(ai, iie, relationInfo), arguments, callState, receiverCast);
				if(abstractResult.isNone() || concreteResult.isNone()) {
					return Option.none();
				}
				return Option.some(monads.methodResultMonad.join(concreteResult.some(), abstractResult.some()));
			} else {
				return doAbstractCall(context, new ExplicitACall<>(ai, iie, relationInfo), arguments, callState, receiver);
			}
		}

		@SuppressWarnings("unchecked")
		private Option<MethodResult> doAbstractCall(final Context context, final AbstractMethodCall<AVal, RelationT> aCall,
				final List<Object> preInstArguments, final InstrumentedState instrCallState, final Object receiver) {
			final InstrumentedStateImpl<AS> callState = (InstrumentedStateImpl<AS>) instrCallState;
			// We make the call from m1 to m2 (here callee) in the following heap:
			// Hm1 o He, where Hm1 is the join over all calling heaps of m1, and He are the heap effects of m1
			// We have collapse Hm1 with He (giving collapsed).
			final Heap collapsed = callState.concreteHeap.collapseToSingle();
			MethodResult accum = null;
			boolean hasResult = false;
			final List<SootMethod> resolved = aCall.resolveMethod((AVal)receiver);
			if(resolved.isEmpty()) {
				return Option.none();
			}
			for(final SootMethod callee : resolved) {
				final AVal abstractReceiver;
				final List<Object> arguments;
				// we now fork, giving us H o {}
				final Heap instrumentationEffectHeap = collapsed.fork();
				final InstrumentedStateImpl<AS> collapsedState;
				registerCall(context, aCall.callExpr(), callee);
				{
					final InstrumentedStateImpl<AS> stateForInstrument = new InstrumentedStateImpl<>(callState.state, instrumentationEffectHeap);
					final P4<AVal, List<Object>, InstrumentedState, Option<Object>> preCallInstr = manager.preCallAI(stateForInstrument, (AVal) receiver,
							preInstArguments, callee.makeRef(), aCall.callExpr());
					collapsedState = (InstrumentedStateImpl<AS>) preCallInstr._3();
					assert collapsedState.concreteHeap == instrumentationEffectHeap && instrumentationEffectHeap.depth() == 2;
					if(preCallInstr._4().isSome()) {
						final MethodResult instrumentedResult = new MethodResult(collapsedState, preCallInstr._4().some());
						hasResult = true;
						if(accum == null) {
							accum = instrumentedResult;
						} else {
							accum = monads.methodResultMonad.join(accum, instrumentedResult);
						}
						continue;
					}
					abstractReceiver = preCallInstr._1();
					arguments = preCallInstr._2();
				}

				final Context calleeContext = ai.contextManager().contextForCall(getCallContext(context, instrCallState), callee, abstractReceiver, arguments, aCall.callExpr());
				if(Interpreter.DEBUG_CALLS) {
					System.out.println(" AI -> AI call: " + callee + " @ " + BodyManager.getHostUnit(aCall.callExpr()));
				}
				final Option<MethodResult> callResult_ = doMethodCall(callee, abstractReceiver, arguments, collapsedState, calleeContext,
						new BoundaryInformation<>(instrCallState, context, aCall.callExpr()), aCall.relationInfo());
				if(Interpreter.DEBUG_CALLS) {
					System.out.println(">> DONE  AI -> AI call: "  + callee);
				}
				if(callResult_.isNone()) {
					continue;
				}
				hasResult = true;
				final MethodResult callResult = callResult_.some();
				// We now have Hm2 o Hr, where Hm2 is the entrace heap to m2, and Hr is the heap effects of m2
				final InstrumentedStateImpl<AS> returnedState = (InstrumentedStateImpl<AS>) callResult.getState();
				final Heap postCall = callState.concreteHeap.copy();
				postCall.assertSaneStructure();
				returnedState.concreteHeap.assertSaneStructure();
				// We copy our input heap Hm1 o He, and apply Hr to apply the heap effects of m2 to our current heap effects
				postCall.applyHeap(returnedState.concreteHeap);
				// Copy the heap from the caller into the callee
				final AS stateAtReturn = mergeInCalleeHeap(callState, ai.stateTransformer().project(returnedState.state), 
					new MergeContext(receiver, arguments, callee, true));
				
				// now let us instrument again
				final InstrumentedStateImpl<AS> postCallPreInst = new InstrumentedStateImpl<AS>(stateAtReturn, postCall);
				final Pair<Object, InstrumentedState> postInst = manager.postCallAI(postCallPreInst, abstractReceiver, arguments, callResult.getReturnValue(), callee.makeRef(), aCall.callExpr());
				// Now return our new state
				final MethodResult postReturnState = new MethodResult(postInst.getO2(), postInst.getO1());
				if(accum == null) {
					accum = postReturnState;
				} else {
					monads.methodResultMonad.join(accum, postReturnState);
				}
			}
			if(!hasResult) {
				return Option.none(); 
			} else {
				return Option.some(accum);
			}
		}

		protected abstract Option<MethodResult> doMethodCall(final SootMethod callee, final AVal abstractReceiver, final List<Object> arguments,
				final InstrumentedStateImpl<AS> collapsedState, final Context calleeContext,
				final BoundaryInformation<Context> callerInformation, final RelationT relationInfo);

		private Either<Pair<Context, InstrumentedState>, ExecutionState<AHeap, Context>> getCallContext(final Context context, final InstrumentedState instrCallState) {
			return Either.<Pair<Context,InstrumentedState>, ExecutionState<AHeap,Context>>left(new Pair<Context, InstrumentedState>(context, instrCallState));
		}

		@SuppressWarnings("unchecked")
		private Option<MethodResult> translateToCallee(final Object callState, final Option<P3<Object, Heap, AHeap>> p3_) {
			return p3_.map(p3 -> {
				assert p3 != null: callState;
				final InstrumentedStateImpl<AS> state = (InstrumentedStateImpl<AS>) callState;
				final AS withNewHeap = mergeInCalleeHeap(state, p3._3(), new MergeContext());
				assert p3._2().depth() == 3 : p3._2();
				final InstrumentedState liftedState = monads.stateMonad.lift(withNewHeap, p3._2().popHeap());
				return new MethodResult(liftedState, p3._1());
			});
		}

		private AS mergeInCalleeHeap(final InstrumentedStateImpl<AS> state, final AHeap newHeap, final MergeContext mergeContext) {
			final AHeap mergedHeap = ai.stateTransformer().merge(ai.stateTransformer().project(state.state), newHeap, mergeContext);
			return ai.stateTransformer().inject(state.state, mergedHeap);
		}

		protected Option<EvalResult> allocTypeInternal(final GNewInvokeExpr op, final InstrumentedState state, final List<Object> constrArgs,
				final Context context, final RelationT relationInfo) {
			final String allocedTypeName = ((RefType) op.getType()).getClassName();
			final TypeOwner owner = oracle.classifyType(allocedTypeName);
			if(owner == TypeOwner.FRAMEWORK) {
				final ConcreteMethodCallMirror constrCall = new ExplicitConcreteMethodCall(coopInterpreter, op);
				return handleConcreteAllocation(state, allocedTypeName, op, context, constrCall, constrArgs);
			} else if(owner == TypeOwner.APPLICATION || owner == TypeOwner.EITHER) {
				return handleAbstractAllocation(allocedTypeName, state, new ExplicitACall<>(ai, op, relationInfo), context, constrArgs);
			} else {
				throw new RuntimeException("Failed to handle a type owner: " + owner);
			}
		}

		@SuppressWarnings("unchecked")
		private Option<EvalResult> handleAbstractAllocation(final String allocedTypeName, final InstrumentedState state, final AbstractMethodCall<AVal, RelationT> aCall,
				final Context context, final List<Object> constrArgs) {
			final InstrumentedStateImpl<AS> instrumented = (InstrumentedStateImpl<AS>)state;
			final Context allocContext = ai.contextManager().contextForAllocation(getAllocationContext(state, context), aCall.callExpr());
			final Pair<AHeap, AVal> alloced = ai.abstractObjectAlloc(BodyManager.loadClass(allocedTypeName).getType(), 
					ai.stateTransformer().project(instrumented.state), aCall.callExpr(), allocContext);
			final InstrumentedState allocedState = new InstrumentedStateImpl<AS>(ai.stateTransformer().inject(instrumented.state, alloced.getO1()), instrumented.concreteHeap);
			final Option<MethodResult> constructorResult = this.doAbstractCall(context, aCall, constrArgs, allocedState, alloced.getO2());
			return constructorResult.map(res -> new EvalResult(res.getState(), monads.valueMonad.lift(alloced.getO2())));
		}

		protected Option<EvalResult> allocTypeInternal(final String allocedTypeName, final InstrumentedState state, final InvokeExpr op,
				final Context context, final RelationT relationInfo) {
			final TypeOwner owner = oracle.classifyType(allocedTypeName);
			if(owner == TypeOwner.FRAMEWORK) {
				final ConcreteMethodCallMirror ice = new ImplicitConcreteMethodCall(BodyManager.loadClass(allocedTypeName), "void <init>()", op, Collections.emptyList());
				final List<Object> constrArgs = Collections.emptyList();
				return handleConcreteAllocation(state, allocedTypeName, op, context, ice, constrArgs);
			} else {
				final AbstractMethodCall<AVal, RelationT> amc = new ImplicitACall<>(ai, BodyManager.loadClass(allocedTypeName).getMethod("void <init>()").makeRef(), op, relationInfo);
				return handleAbstractAllocation(allocedTypeName, state, amc, context, Collections.emptyList());
			}
		}

		@SuppressWarnings("unchecked")
		private Option<EvalResult> handleConcreteAllocation(final InstrumentedState state, final String allocedTypeName, final InvokeExpr op, final Context context,
				final ConcreteMethodCallMirror ice, final List<Object> constrArgs) {
			final InstrumentedStateImpl<AS> instrumented = (InstrumentedStateImpl<AS>) state;
			final Heap h = instrumented.concreteHeap;
			final Heap toMutate = h.fork();
			final SootClass toAlloc = Scene.v().loadClass(allocedTypeName, SootClass.BODIES);
			final Context allocContext = ai.contextManager().contextForAllocation(getAllocationContext(state, context), op);
			final IValue alloced = toMutate.allocate(toAlloc, op, new ForeignContext(allocContext));
			return doConcreteInitCall(context, ice, constrArgs, instrumented, toMutate, alloced);
		}

		Option<EvalResult> doConcreteInitCall(final Context context, final ConcreteMethodCallMirror ice, final List<Object> constrArgs, final InstrumentedStateImpl<AS> instrumented,
				final Heap toMutate, final IValue alloced) {
			final InstrumentedStateImpl<AS> withNewObject = new InstrumentedStateImpl<AS>(instrumented.state, toMutate.popHeap());
			final Option<MethodResult> handled_ = translateToCallee(withNewObject, coopInterpreter.handleConcreteCall(context, ice, alloced, constrArgs, withNewObject));
			return handled_.map(handled -> {
				assert handled.getReturnValue() == null;
				return new EvalResult(handled.getState(), monads.valueMonad.lift(alloced));
			});
		}

		private Either<Pair<Context, InstrumentedState>, ExecutionState<AHeap, Context>> getAllocationContext(final InstrumentedState state, final Context context) {
			return Either.<Pair<Context, InstrumentedState>, ExecutionState<AHeap, Context>>left(new Pair<Context, InstrumentedState>(context, state));
		}

		public EvalResult allocArray(final NewArrayExpr op, final InstrumentedState state, final Object sz, final Context context) {
			final Type baseType = op.getBaseType();
			if(useAbstractAllocation(baseType)) {
				final List<Object> sizeList = Collections.singletonList(sz);
				return doAbstractArrayAllocation(op, state, context, sizeList);
			} else {
				return doConcreteArrayAllocation(op, state, Collections.singletonList(MetaInterpreter.concretize(sz)), context);
			}
		}

		@SuppressWarnings("unchecked")
		private EvalResult doAbstractArrayAllocation(final Value op, final InstrumentedState state, final Context context, final List<Object> sizeList) {
			final InstrumentedStateImpl<AS> instrState = (InstrumentedStateImpl<AS>)state;
			final AHeap projected = ai.stateTransformer().project(instrState.state);
			final List<AVal> toPass = new ArrayList<>();
			for(final Object size : sizeList) {
				toPass.add(monads.valueMonad.alpha(size));
			}
			final Context allocContext = ai.contextManager().contextForAllocation(getAllocationContext(state, context), op);
			final Pair<AHeap, AVal> allocResult = ai.abstractArrayAlloc((ArrayType)op.getType(), projected, op, toPass, allocContext);
			
			final InstrumentedState updatedState = new InstrumentedStateImpl<AS>(ai.stateTransformer().inject(instrState.state, allocResult.getO1()), instrState.concreteHeap);
			return new EvalResult(updatedState, monads.valueMonad.lift(allocResult.getO2()));
		}

		public EvalResult allocArray(final NewMultiArrayExpr op, final InstrumentedState state, final List<Object> sizes, final Context context) {
			final Type baseType = op.getBaseType();
			if(useAbstractAllocation(baseType)) {
				return doAbstractArrayAllocation(op, state, context, sizes);
			} else {
				final List<IValue> concreteSizes = concretize(sizes);
				return doConcreteArrayAllocation(op, state, concreteSizes, context);
			}
		}

		@SuppressWarnings("unchecked")
		private EvalResult doConcreteArrayAllocation(final Value op, final InstrumentedState state, final List<IValue> concreteSizes, final Context rootContext) {
			final InstrumentedStateImpl<AS> instr = (InstrumentedStateImpl<AS>) state;
			final Heap toMutate = instr.concreteHeap.fork();
			final Context allocContext = ai.contextManager().contextForAllocation(getAllocationContext(state, rootContext), op);
			final IValue alloced = toMutate.allocateArray(op.getType(), concreteSizes, op, new ForeignContext(allocContext));
			return new EvalResult(new InstrumentedStateImpl<AS>(instr.state, toMutate.popHeap()), monads.valueMonad.lift(alloced));
		}

		private List<IValue> concretize(final List<Object> sizes) {
			final List<IValue> toReturn = new ArrayList<>();
			for(final Object sz : sizes) {
				toReturn.add(MetaInterpreter.concretize(sz));
			}
			return toReturn;
		}

		private boolean useAbstractAllocation(final Type baseType) {
			return baseType instanceof PrimType || oracle.classifyType(((RefType)baseType).getClassName()) != TypeOwner.FRAMEWORK;
		}

		@SuppressWarnings("unchecked")
		protected Option<EvalResult> allocUnknownType(final InstrumentedState state, final StaticInvokeExpr op, final Context context, final RelationT rel) {
			final ReflectiveOperationContext ctxt = MetaInterpreter.makeReflectiveAllocationContext(op);
			// TODO: just make this object by default FFS
			final RefType upperBound = (RefType)ctxt.castHint().orSome(Scene.v().getObjectType());
			final InstrumentedStateImpl<AS> inst = (InstrumentedStateImpl<AS>) state;
			final EvalResult concrResult;
			final EvalResult absResult;
			
			boolean incomplete = false;
			if(BodyManager.enumerateFrameworkClasses(upperBound).iterator().hasNext()) {
				final Heap forAlloc = inst.concreteHeap.fork();
				final Context allocContext = ai.contextManager().contextForAllocation(getAllocationContext(state, context), op);
				final IValue alloced = forAlloc.allocateBoundedType(upperBound, op, new ForeignContext(allocContext));
				final ConcreteMethodCallMirror call = getConcreteNullaryConstructor(op);
				final Option<EvalResult> concrResult_ = doConcreteInitCall(context, call, Collections.emptyList(), inst, forAlloc, alloced);
				if(concrResult_.isNone()) {
					incomplete = true;
					concrResult = null;
				} else {
					concrResult = concrResult_.some();
				}
			} else {
				concrResult = null;
			}
			{
				final Context allocContext = ai.contextManager().contextForAllocation(getAllocationContext(state, context), op);
				final Option<Pair<AHeap, AVal>> alloced_ = ai.allocateUnknownObject(ai.stateTransformer().project(inst.state), op, allocContext, ctxt);
				if(alloced_.isNone()) {
					absResult = null;
				} else {
					final Pair<AHeap, AVal> alloced = alloced_.some();
					final InstrumentedState allocedState = new InstrumentedStateImpl<AS>(ai.stateTransformer().inject(inst.state, alloced.getO1()), inst.concreteHeap);
					final Option<MethodResult> constructorResult = this.doAbstractCall(context, getImplicitNullaryConstructor(op, rel), Collections.emptyList(), allocedState, alloced.getO2());
					if(constructorResult.isNone()) {
						incomplete = true;
						absResult = null;
					} else {
						absResult = new EvalResult(constructorResult.some().getState(), monads.valueMonad.lift(alloced.getO2()));
					}
				}
			}
			if(incomplete) {
				return Option.none();
			}
			if(absResult == null ) {
				return Option.some(concrResult);
			} else if(concrResult == null) {
				return Option.some(absResult);
			} else {
				return Option.some(new EvalResult(monads.stateMonad.join(absResult.state, concrResult.state), monads.valueMonad.join(absResult.value, concrResult.value)));
			}
		}

		private ImplicitConcreteMethodCall getConcreteNullaryConstructor(final StaticInvokeExpr op) {
			return new ImplicitConcreteMethodCall(Scene.v().getObjectType().getSootClass(), "void <init>()", op, Collections.emptyList());
		}

		private ImplicitACall<AVal, RelationT> getImplicitNullaryConstructor(final StaticInvokeExpr op, final RelationT rel) {
			return new ImplicitACall<>(ai, MetaInterpreter.getNullaryConstructorRef(), op, rel);
		}

		@SuppressWarnings("unchecked")
		protected Option<MethodResult> handleInvoke(final Context callingContext, final StaticInvokeExpr expr, final List<Object> arguments,
				final InstrumentedState callState, final RelationT rel) {
			final IValue arg;
			if(arguments.size() == 3) {
				arg = null;
			} else {
				assert arguments.size() == 4;
				arg = coopInterpreter.convertToConcrete(Collections.singletonList(arguments.get(3)), _n -> expr.getArg(3)).get(0);
			}

			final AVal abstractComponent;
			final IValue concreteComponent;
			if(arguments.get(0) instanceof IValue) {
				concreteComponent = (IValue) arguments.get(0);
				abstractComponent = null;
			} else if(arguments.get(0) instanceof CombinedValue) {
				concreteComponent = ((CombinedValue)arguments.get(0)).concreteComponent;
				abstractComponent = (AVal) ((CombinedValue)arguments.get(0)).abstractComponent;
			} else {
				concreteComponent = null;
				abstractComponent = (AVal) arguments.get(0);
			}
			final Pair<Stream<SootMethod>, Stream<Pair<IValue, SootMethod>>> resolved = coopInterpreter.resolveInvokees(expr,
					abstractComponent, concreteComponent, MetaInterpreter.concretize(arguments.get(1)), MetaInterpreter.concretize(arguments.get(2)), arg,
					MetaInterpreter.makeReflectiveAllocationContext(expr));

			if(Interpreter.LOG_REFLECTION) {
				MetaInterpreter.logInvoke(resolved, false);
			}

			if(resolved.getO1().isNotEmpty() && resolved.getO2().isNotEmpty()) {
				final Option<MethodResult> concrResult = handleConcreteInvoke(callingContext, expr, arguments, callState, resolved.getO2());
				final Option<MethodResult> invokeResult = handleAbstractInvoke(callingContext, expr, arguments, callState, abstractComponent, resolved.getO1(), rel);
				if(concrResult.isNone() || invokeResult.isNone()) {
					return Option.none();
				} else {
					return Option.some(monads.methodResultMonad.join(concrResult.some(), invokeResult.some()));
				}
			} else if(resolved.getO1().isNotEmpty()) {
				return this.handleAbstractInvoke(callingContext, expr, arguments, callState, abstractComponent, resolved.getO1(), rel);
			} else {
				return this.handleConcreteInvoke(callingContext, expr, arguments, callState, resolved.getO2());
			}
		}

		private Option<MethodResult> handleConcreteInvoke(final Context callingContext, final StaticInvokeExpr expr, final List<Object> arguments, final InstrumentedState callState,
				final Stream<Pair<IValue, SootMethod>> stream) {
			final List<Object> argList;
			if(expr.getArgCount() == 4) {
				argList = Collections.singletonList(arguments.get(3));
			} else {
				argList = Collections.emptyList();
			}
			return translateToCallee(callState, coopInterpreter.handleInvokeConcrete(callingContext, expr, stream, argList, callState));
		}

		private Option<MethodResult> handleAbstractInvoke(final Context callingContext, final StaticInvokeExpr expr, final List<Object> arguments, final InstrumentedState callState,
				final AVal abstractComponent, final Stream<SootMethod> resolved, final RelationT relationInfo) {
			final Option<MethodResult> invokeResult = postProcessInvokeResults(ai, monads, resolved.map((final SootMethod m) -> {
				final List<Object> argList;
				if(expr.getArgCount() == 4) {
					argList = Collections.singletonList(arguments.get(3));
				} else {
					argList = Collections.emptyList();
				}
				return this.doAbstractCall(callingContext, new AbstractMethodCall<AVal, RelationT>() {
					
					@Override
					public List<SootMethod> resolveMethod(final AVal receiver) {
						return Collections.singletonList(m);
					}

					@Override
					public InvokeExpr callExpr() {
						return expr;
					}
					
					@Override
					public RelationT relationInfo() {
						return relationInfo;
					}
				}, argList, callState, abstractComponent);
			}));
			return invokeResult;
		}
	}
	
	private class CombinedCallHandler extends AbstractCallHandler<Object> implements CallHandler<Context> {
		
		public CombinedCallHandler(final AbstractInterpretation<AVal, AHeap, AS, Context> ai, final TypeOracle oracle, final InstrumentationManager<AVal, AHeap, AS> manager) {
			super(ai, oracle, manager);
		}

		@Override
		public Option<MethodResult> handleCall(final Context callingContext, final InstanceInvokeExpr iie, final Object receiver, final List<Object> arguments,
				final InstrumentedState callState) {
			return this.handleCallInternal(callingContext, iie, receiver, arguments, callState, null);
		}

		@Override
		public Option<EvalResult> allocType(final GNewInvokeExpr op, final InstrumentedState state, final List<Object> constrArgs, final Context context) {
			return this.allocTypeInternal(op, state, constrArgs, context, null);
		}

		@Override
		public Option<EvalResult> allocType(final String allocedTypeName, final InstrumentedState state, final InvokeExpr op, final Context context) {
			return this.allocTypeInternal(allocedTypeName, state, op, context, null);
		}

		@Override
		public Option<EvalResult> allocUnknownType(final InstrumentedState state, final StaticInvokeExpr op, final Context context) {
			return this.allocUnknownType(state, op, context, null);
		}

		@Override
		public Option<MethodResult> handleInvoke(final Context callingContext, final StaticInvokeExpr expr, final List<Object> arguments, final InstrumentedState callState) {
			return this.handleInvoke(callingContext, expr, arguments, callState, null);
		}

		@Override
		protected Option<MethodResult> doMethodCall(final SootMethod callee, final AVal abstractReceiver, final List<Object> arguments,
				final InstrumentedStateImpl<AS> collapsedState, final Context calleeContext, final BoundaryInformation<Context> callerInformation, final Object relationInfo) {
			return this.ai.handleCall(callee, abstractReceiver, arguments, collapsedState, calleeContext, callerInformation);
		}
	}
	
	private class RelationalCallHandlerImpl<Rel> extends AbstractCallHandler<Option<Rel>> implements RelationalCallHandler<Context, Rel> {
		private final RelationalAbstractInterpretation<AVal, AHeap, AS, Context, Rel> rAi;

		public RelationalCallHandlerImpl(final RelationalAbstractInterpretation<AVal, AHeap, AS, Context, Rel> ai, final TypeOracle oracle, 
				final InstrumentationManager<AVal, AHeap, AS> manager) {
			super(ai, oracle, manager);
			this.rAi = ai;
		}

		@Override
		public Option<MethodResult> handleCall(final Context callingContext, final InstanceInvokeExpr iie, final Object receiver, final List<Object> arguments,
				final InstrumentedState callState) {
			return this.handleCallInternal(callingContext, iie, receiver, arguments, callState, Option.none());
		}

		@Override
		public Option<EvalResult> allocType(final GNewInvokeExpr op, final InstrumentedState state, final List<Object> constrArgs, final Context context) {
			return this.allocTypeInternal(op, state, constrArgs, context, Option.none());
		}

		@Override
		public Option<EvalResult> allocType(final String allocedTypeName, final InstrumentedState state, final InvokeExpr op, final Context context) {
			return this.allocTypeInternal(allocedTypeName, state, op, context, Option.none());
		}

		@Override
		public Option<EvalResult> allocUnknownType(final InstrumentedState state, final StaticInvokeExpr op, final Context context) {
			return this.allocUnknownType(state, op, context, Option.none());
		}

		@Override
		public Option<MethodResult> handleInvoke(final Context callingContext, final StaticInvokeExpr expr, final List<Object> arguments, final InstrumentedState callState) {
			return this.handleInvoke(callingContext, expr, arguments, callState, Option.none());
		}

		@Override
		protected Option<MethodResult> doMethodCall(final SootMethod callee, final AVal abstractReceiver, final List<Object> arguments,
				final InstrumentedStateImpl<AS> collapsedState, final Context calleeContext, final BoundaryInformation<Context> callerInformation, final Option<Rel> relationInfo) {
			if(relationInfo.isSome()) {
				return rAi.handleCall(callee, abstractReceiver, arguments, collapsedState, calleeContext, callerInformation, relationInfo.some());
			} else {
				return rAi.handleCall(callee, abstractReceiver, arguments, collapsedState, calleeContext, callerInformation);
			}
		}

		@Override
		public Option<MethodResult> handleCall(final Context callingContext, final InstanceInvokeExpr iie, final Object receiver, final List<Object> arguments,
				final InstrumentedState callState, final Rel inputRelation) {
			return this.handleCallInternal(callingContext, iie, receiver, arguments, callState, Option.some(inputRelation));
		}

		@Override
		public Option<EvalResult> allocType(final GNewInvokeExpr op, final InstrumentedState state, final List<Object> constrArgs, final Context context, final Rel inputRelation) {
			return this.allocTypeInternal(op, state, constrArgs, context, Option.some(inputRelation));
		}

	}

	private static final class InstrumentedPathSensitiveBranchInterpreter<AVal, AS> implements PathSensitiveBranchInterpreter<AVal, AS> {
		private final PropagatingBranchInterpreter<AVal, InstrumentedState> impl;
		private final F2<InstrumentedState, AS, InstrumentedState> rewrapState;
		private final F<InstrumentedState, AS> projectState;

		private InstrumentedPathSensitiveBranchInterpreter(final PropagatingBranchInterpreter<AVal, InstrumentedState> impl, 
				final F2<InstrumentedState, AS, InstrumentedState> rewrapState,
				final F<InstrumentedState, AS> projectState) {
			this.impl = impl;
			this.rewrapState = rewrapState;
			this.projectState = projectState;
		}

		@Override
		public Map<Unit, InstrumentedState> interpretBranch(final IfStmt stmt, final Object op1, final Object op2,
				final InstrumentedState branchState, final StateValueUpdater<AS> updater) {
			return impl.interpretBranch(stmt, op1, op2, branchState, new StateValueUpdater<InstrumentedState>() {
				
				@Override
				public InstrumentedState updateForValue(final Value v, final InstrumentedState state, final Object value) {
					final AS as = projectState.f(state);
					return rewrapState.f(state, updater.updateForValue(v, as, value));
				}
			});
		}

		@Override
		public Map<Unit, InstrumentedState> interpretBranch(final IfStmt stmt, final Object op1, final InstrumentedState branchState) {
			return impl.interpretBranch(stmt, op1, branchState);
		}
	}

	private interface AbstractMethodCall<AVal, RelationT> {
		List<SootMethod> resolveMethod(AVal receiver);
		InvokeExpr callExpr();
		RelationT relationInfo();
	}
	
	private static class ImplicitACall<AVal, RelT> implements AbstractMethodCall<AVal, RelT> {
		private final SootMethodRef m;
		private final InvokeExpr expr;
		private final AbstractInterpretation<AVal, ?, ?, ?> ai;
		private final RelT relationInfo;

		public ImplicitACall(final AbstractInterpretation<AVal, ?, ?, ?> ai, final SootMethodRef m, final InvokeExpr expr, final RelT relationInfo) {
			this.ai = ai;
			this.m = m;
			this.expr = expr;
			this.relationInfo = relationInfo;
		}
		@Override
		public List<SootMethod> resolveMethod(final AVal receiver) {
			return ai.getMethodForRef(m, receiver);
		}

		@Override
		public InvokeExpr callExpr() {
			return this.expr;
		}
		
		@Override
		public RelT relationInfo() {
			return this.relationInfo;
		}
	}
	
	private static class ExplicitACall<AVal, RelT> implements AbstractMethodCall<AVal, RelT> {
		private final InvokeExpr ie;
		private final AbstractInterpretation<AVal, ?, ?, ?> ai;
		private final RelT relationInfo;

		public ExplicitACall(final AbstractInterpretation<AVal, ?, ?, ?> ai, final InvokeExpr ie, final RelT relationInfo) {
			this.ai = ai;
			this.ie = ie;
			this.relationInfo = relationInfo;
		}
		@Override
		public List<SootMethod> resolveMethod(final AVal receiver) {
			return ai.getCalleesOfCall(ie, receiver);
		}

		@Override
		public InvokeExpr callExpr() {
			return ie;
		}
		
		@Override
		public RelT relationInfo() {
			return this.relationInfo;
		}
	}

	public static Unit getIfFallthrough(final IfStmt stmt) {
		final PatchingChain<Unit> chain = BodyManager.retrieveBody(BodyManager.getHostMethod(stmt)).getUnits();
		final Unit succ = chain.getSuccOf(stmt);
		return succ;
	}

	@SuppressWarnings("unchecked")
	public MetaInterpreter(final AbstractInterpretation<AVal, AHeap, AS, Context> ai, final TypeOracle oracle, final String classPath,
			@Nonnull final String mainClassName, @Nonnull final String mainMethod, final GlobalState state, final ReflectionModel rm) {
		this.mainClass = Interpreter.setupSoot(mainClassName, classPath);
		this.mainMethod = mainMethod;
		
		this.monads = Monads.makeMonads(ai);

		valueLattice = ai.lattices().valueLattice();
		heapLattice = ai.lattices().heapLattice();
		this.ai = ai;

		MetaInterpreter.injectMonads(valueLattice, monads);
		MetaInterpreter.injectMonads(heapLattice, monads);
		MetaInterpreter.injectMonads(ai.lattices().stateLattice(), monads);
		BodyManager.init(oracle);
		
		for(final Injectable nm : ai.monadUsers()) {
			MetaInterpreter.injectMonads(nm, monads);
		}
		ai.inject(monads);
		final InstrumentationManager<AVal, AHeap, AS> manager = new InstrumentationManager<>(monads, ai.stateTransformer(), (final InstrumentedState a) -> {
				final InstrumentedStateImpl<AS> st_ = (InstrumentedStateImpl<AS>) a;
				return new Pair<Heap, AS>(st_.concreteHeap, st_.state);
			}, heapLattice, valueLattice, oracle);
		
		this.coopInterpreter = new CooperativeInterpreter<AVal, AHeap, AS, Context>(state, ai, ai.stateTransformer(), monads, heapLattice, manager, oracle);
		ReflectionEnvironment.init(rm);
		MetaInterpreter.setupBranchInterpreters(ai, monads, impl -> ((InstrumentedStateImpl<?>)impl).concreteHeap, coopInterpreter::setBranchInterpreter,
			(oState, aState) -> new InstrumentedStateImpl<AS>(aState, ((InstrumentedStateImpl<?>)oState).concreteHeap),
			oState -> ((InstrumentedStateImpl<AS>)oState).state);
		if(ai instanceof RelationalAbstractInterpretation) {
			final RelationalAbstractInterpretation<AVal, AHeap, AS, Context, ?> rAi = (RelationalAbstractInterpretation<AVal, AHeap, AS, Context, ?>) ai;
			MetaInterpreter.injectCallHandler(rAi, ai_ -> new RelationalCallHandlerImpl<>(ai_, oracle, manager));
		} else {
			ai.setCallHandler(new CombinedCallHandler(ai, oracle, manager));
		}
		ai.instrument(manager);
	}
	
	private static <AVal, AHeap, AS, Context, R> void injectCallHandler(final RelationalAbstractInterpretation<AVal, AHeap, AS, Context, R> rAi,
			final F<RelationalAbstractInterpretation<AVal, AHeap, AS, Context, R>, RelationalCallHandler<Context, R>> mk) {
		rAi.setCallHandler(mk.f(rAi));
	}

	@SuppressWarnings("unchecked")
	private static <AVal, AS> void injectMonads(final Object obj, final Monads<AVal, AS> monads) {
		if(obj instanceof NeedsMonads) {
			((NeedsMonads<AVal, AS>) obj).inject(monads);
		} else if(obj instanceof NeedsStateMonad) {
			((NeedsStateMonad<AS,AVal>) obj).injectStateMonad(monads.stateMonad);
		} else if(obj instanceof NeedsValueMonad) {
			((NeedsValueMonad<AVal>)obj).injectValueMonad(monads.valueMonad);
		} else if(obj instanceof NeedsValueLattice) {
			((NeedsValueLattice) obj).injectValueLattice(monads.valueMonad);
		}
	}
	
	private static abstract class AbstractNullCallHandler<AVal, AHeap, AS, Context, RelType> {
		protected final AbstractInterpretation<AVal, AHeap, AS, Context> ai;
		private final InstrumentationManager<AVal, AHeap, AS> manager;
		private final Monads<AVal, AS> monads;

		public AbstractNullCallHandler(final AbstractInterpretation<AVal, AHeap, AS, Context> ai, final InstrumentationManager<AVal, AHeap, AS> manager, final Monads<AVal, AS> monads) {
			this.ai = ai;
			this.manager = manager;
			this.monads = monads;
		}
		
		protected Option<MethodResult> handleCallInternal(final Context context, final InstanceInvokeExpr iie, final Object receiver,
				final List<Object> arguments, final InstrumentedState callState, final RelType rel) {
			if(iie instanceof SpecialInvokeExpr && iie.getMethodRef().getSignature().equals("<java.lang.Object: void <init>()>")) {
				return Option.some(new MethodResult(callState, null));
			}
			return doAbstractCall(context, new ExplicitACall<>(ai, iie, rel), arguments, callState, receiver);
		}

		@SuppressWarnings("unchecked")
		private Option<MethodResult> doAbstractCall(final Context context, final AbstractMethodCall<AVal, RelType> aCall,
				final List<Object> preInstArguments, final InstrumentedState instrCallState, final Object receiver) {
			final PlainStateImpl<AS> callState = (PlainStateImpl<AS>) instrCallState;
			MethodResult accum = null;
			boolean hasResult = false;
			final List<SootMethod> resolved = aCall.resolveMethod((AVal)receiver);
			if(resolved.isEmpty()) {
				return Option.none();
			}
			for(final SootMethod callee : resolved) {
				final AVal abstractReceiver;
				final List<Object> arguments;
				final PlainStateImpl<AS> collapsedState;
				registerCall(context, aCall.callExpr(), callee);
				{
					final P4<AVal, List<Object>, InstrumentedState, Option<Object>> preCallInstr = manager.preCallAI(callState, (AVal) receiver,
							preInstArguments, callee.makeRef(), aCall.callExpr());
					collapsedState = (PlainStateImpl<AS>) preCallInstr._3();
					if(preCallInstr._4().isSome()) {
						hasResult = true;
						final MethodResult summarized = new MethodResult(collapsedState, preCallInstr._4().some());
						if(accum == null) {
							accum = summarized;
						} else {
							accum = monads.methodResultMonad.join(accum, summarized);
						}
						continue;
					}
					abstractReceiver = preCallInstr._1();
					arguments = preCallInstr._2();
				}
				final Context calleeContext = ai.contextManager().contextForCall(getCallContext(context, instrCallState), callee, abstractReceiver, arguments, aCall.callExpr());
				final BoundaryInformation<Context> info = new BoundaryInformation<>(instrCallState, context, aCall.callExpr());
				if(Interpreter.DEBUG_CALLS) {
					System.out.println(" AI -> AI call: " + callee + " @ " + BodyManager.getHostUnit(aCall.callExpr()));
				}
				final Option<MethodResult> callResult_ = doAbstractCall(abstractReceiver, arguments, callee, collapsedState, calleeContext, info, aCall.relationInfo());
				if(Interpreter.DEBUG_CALLS) {
					System.out.println(">> DONE  AI -> AI call: "  + callee);
				}
				if(callResult_.isNone()) {
					continue;
				}
				hasResult = true;
				final MethodResult callResult = callResult_.some();
				// We now have Hm2 o Hr, where Hm2 is the entrace heap to m2, and Hr is the heap effects of m2 
				final PlainStateImpl<AS> returnedState = (PlainStateImpl<AS>) callResult.getState();
				// Copy the heap from the caller into the callee
				final AS stateAtReturn = mergeInCalleeHeap(callState, ai.stateTransformer().project(returnedState.state), 
					new MergeContext(receiver, arguments, callee, true));
				
				// now let us instrument again
				final PlainStateImpl<AS> postCallPreInst = new PlainStateImpl<AS>(stateAtReturn);
				final Pair<Object, InstrumentedState> postInst = manager.postCallAI(postCallPreInst, abstractReceiver, arguments, callResult.getReturnValue(), callee.makeRef(), aCall.callExpr());
				// Now return our new state
				final MethodResult postReturnState = new MethodResult(postInst.getO2(), postInst.getO1());
				if(accum == null) {
					accum = postReturnState;
				} else {
					accum = monads.methodResultMonad.join(accum, postReturnState);
				}
			}
			if(!hasResult) {
				assert accum == null;
				return Option.none(); 
			} else {
				assert accum != null;
				return Option.some(accum);
			}
		}

		protected abstract Option<MethodResult> doAbstractCall(final AVal abstractReceiver, final List<Object> arguments, final SootMethod callee,
				final PlainStateImpl<AS> collapsedState, final Context calleeContext, final BoundaryInformation<Context> info, final RelType relType);

		private Either<Pair<Context, InstrumentedState>, ExecutionState<AHeap, Context>> getCallContext(final Context context, final InstrumentedState instrCallState) {
			return Either.<Pair<Context,InstrumentedState>, ExecutionState<AHeap,Context>>left(new Pair<Context, InstrumentedState>(context, instrCallState));
		}

		private AS mergeInCalleeHeap(final PlainStateImpl<AS> state, final AHeap newHeap, final MergeContext mergeContext) {
			final AHeap mergedHeap = ai.stateTransformer().merge(ai.stateTransformer().project(state.state), newHeap, mergeContext);
			return ai.stateTransformer().inject(state.state, mergedHeap);
		}

		protected Option<EvalResult> allocTypeInternal(final GNewInvokeExpr op, final InstrumentedState state, final List<Object> constrArgs, final Context context, final RelType info) {
			final String allocedTypeName = ((RefType) op.getType()).getClassName();
			return handleAbstractAllocation(allocedTypeName, state, new ExplicitACall<>(ai, op, info), context, constrArgs);
		}

		@SuppressWarnings("unchecked")
		private Option<EvalResult> handleAbstractAllocation(final String allocedTypeName, final InstrumentedState state, final AbstractMethodCall<AVal, RelType> aCall,
				final Context context, final List<Object> constrArgs) {
			final PlainStateImpl<AS> instrumented = (PlainStateImpl<AS>)state;
			final Context allocContext = ai.contextManager().contextForAllocation(getAllocationContext(state, context), aCall.callExpr());
			final Pair<AHeap, AVal> alloced = ai.abstractObjectAlloc(BodyManager.loadClass(allocedTypeName).getType(), 
					ai.stateTransformer().project(instrumented.state), aCall.callExpr(), allocContext);
			final InstrumentedState allocedState = new PlainStateImpl<AS>(ai.stateTransformer().inject(instrumented.state, alloced.getO1()));
			final Option<MethodResult> constructorResult = this.doAbstractCall(context, aCall, constrArgs, allocedState, alloced.getO2());
			return constructorResult.map(res -> new EvalResult(res.getState(), monads.valueMonad.lift(alloced.getO2())));
		}

		protected Option<EvalResult> allocTypeInternal(final String allocedTypeName, final InstrumentedState state, final InvokeExpr op, final Context context, final RelType relInfo) {
			final AbstractMethodCall<AVal, RelType> amc = new ImplicitACall<>(ai, BodyManager.loadClass(allocedTypeName).getMethod("void <init>()").makeRef(), op, relInfo);
			return handleAbstractAllocation(allocedTypeName, state, amc, context, Collections.emptyList());
		}

		private Either<Pair<Context, InstrumentedState>, ExecutionState<AHeap, Context>> getAllocationContext(final InstrumentedState state, final Context context) {
			return Either.<Pair<Context, InstrumentedState>, ExecutionState<AHeap, Context>>left(new Pair<Context, InstrumentedState>(context, state));
		}

		public EvalResult allocArray(final NewArrayExpr op, final InstrumentedState state, final Object sz, final Context context) {
			final List<Object> sizeList = Collections.singletonList(sz);
			return doAbstractArrayAllocation(op, state, context, sizeList);
		}

		@SuppressWarnings("unchecked")
		private EvalResult doAbstractArrayAllocation(final Value op, final InstrumentedState state, final Context context, final List<Object> sizeList) {
			final PlainStateImpl<AS> instrState = (PlainStateImpl<AS>)state;
			final AHeap projected = ai.stateTransformer().project(instrState.state);
			final List<AVal> toPass = new ArrayList<>();
			for(final Object size : sizeList) {
				toPass.add(monads.valueMonad.alpha(size));
			}
			final Context allocContext = ai.contextManager().contextForAllocation(getAllocationContext(state, context), op);
			final Pair<AHeap, AVal> allocResult = ai.abstractArrayAlloc((ArrayType)op.getType(), projected, op, toPass, allocContext);
			
			final InstrumentedState updatedState = new PlainStateImpl<AS>(ai.stateTransformer().inject(instrState.state, allocResult.getO1()));
			return new EvalResult(updatedState, monads.valueMonad.lift(allocResult.getO2()));
		}
		
		public EvalResult allocArray(final NewMultiArrayExpr op, final InstrumentedState state, final List<Object> sizes, final Context context) {
			return doAbstractArrayAllocation(op, state, context, sizes);
		}

		@SuppressWarnings("unchecked")
		protected Option<EvalResult> allocUnknownType(final InstrumentedState state, final StaticInvokeExpr op, final Context context, final RelType relInfo) {
			final ReflectiveOperationContext ctxt = MetaInterpreter.makeReflectiveAllocationContext(op);
			// TODO: just make this object by default FFS
			final PlainStateImpl<AS> inst = (PlainStateImpl<AS>) state;
			final EvalResult absResult;
			
			boolean incomplete = false;
			{
				final Context allocContext = ai.contextManager().contextForAllocation(getAllocationContext(state, context), op);
				final Option<Pair<AHeap, AVal>> alloced_ = ai.allocateUnknownObject(ai.stateTransformer().project(inst.state), op, allocContext, ctxt);
				if(alloced_.isNone()) {
					absResult = null;
				} else {
					final Pair<AHeap, AVal> alloced = alloced_.some();
					final InstrumentedState allocedState = new PlainStateImpl<AS>(ai.stateTransformer().inject(inst.state, alloced.getO1()));
					final Option<MethodResult> constructorResult = this.doAbstractCall(context, getImplicitNullaryConstructor(op, relInfo),
							Collections.emptyList(), allocedState, alloced.getO2());
					if(constructorResult.isNone()) {
						incomplete = true;
						absResult = null;
					} else {
						absResult = new EvalResult(constructorResult.some().getState(), monads.valueMonad.lift(alloced.getO2()));
					}
				}
			}
			if(incomplete) {
				return Option.none();
			}
			return Option.some(absResult);
		}

		private ImplicitACall<AVal, RelType> getImplicitNullaryConstructor(final StaticInvokeExpr op, final RelType relInfo) {
			return new ImplicitACall<>(ai, MetaInterpreter.getNullaryConstructorRef(), op, relInfo);
		}

		@SuppressWarnings("unchecked")
		protected Option<MethodResult> handleInvoke(final Context callingContext, final StaticInvokeExpr expr, final List<Object> arguments,
				final InstrumentedState callState, final RelType rel) {
			assert arguments.size() == 4 || arguments.size() == 3;
			assert arguments.size() == expr.getArgCount();
			
			final AVal abstractComponent;
			abstractComponent = (AVal) arguments.get(0);
			final AVal arg;
			if(arguments.size() == 4) {
				arg = (AVal) arguments.get(3);
			} else {
				arg = null;
			}
			final Pair<Stream<SootMethod>, Stream<Pair<IValue, SootMethod>>> resolved = CooperativeInterpreter.resolveInvokees(expr, abstractComponent, null, MetaInterpreter.concretize(arguments.get(1)), MetaInterpreter.concretize(arguments.get(2)), arg,
					t -> true, (av, t) -> ai.objectOperations().isInstanceOf(av, t) != ObjectIdentityResult.MUST_NOT_BE, ai, MetaInterpreter.makeReflectiveAllocationContext(expr), new TypeOracle() {
						@Override public TypeOwner classifyType(final String className) {
							return TypeOwner.APPLICATION;
						}
					});
			
			return this.handleAbstractInvoke(callingContext, expr, arguments, callState, abstractComponent, resolved.getO1(), rel);
		}

		private Option<MethodResult> handleAbstractInvoke(final Context callingContext, final StaticInvokeExpr expr, final List<Object> arguments, final InstrumentedState callState,
				final AVal abstractComponent, final Stream<SootMethod> resolved, final RelType rel) {
			if(resolved.isEmpty()) {
				return Option.none();
			}
			final List<Object> argList;
			if(arguments.size() == 3) {
				argList = Collections.emptyList();
			} else {
				argList = Collections.singletonList(arguments.get(3));
			}
			final Stream<Option<MethodResult>> invokeResultStream = resolved.map((final SootMethod m) -> {
				return this.doAbstractCall(callingContext, new AbstractMethodCall<AVal, RelType>() {

					@Override public List<SootMethod> resolveMethod(final AVal receiver) {
						return Collections.singletonList(m);
					}

					@Override public InvokeExpr callExpr() {
						return expr;
					}

					@Override public RelType relationInfo() {
						return rel;
					}
				}, argList, callState, abstractComponent);
			});
			final Option<MethodResult> invokeResult = postProcessInvokeResults(ai, monads, invokeResultStream);
			return invokeResult;
		}

	}
	
	private static class NonRelationalNullCallHandler<AVal, AHeap, AS, Context> extends AbstractNullCallHandler<AVal, AHeap, AS, Context, Object> implements CallHandler<Context> {

		public NonRelationalNullCallHandler(final AbstractInterpretation<AVal, AHeap, AS, Context> ai, final InstrumentationManager<AVal, AHeap, AS> manager,
				final Monads<AVal, AS> monads) {
			super(ai, manager, monads);
		}

		@Override
		public Option<MethodResult> handleCall(final Context callingContext, final InstanceInvokeExpr iie, final Object receiver,
				final List<Object> arguments, final InstrumentedState callState) {
			return this.handleCallInternal(callingContext, iie, receiver, arguments, callState, null);
		}

		@Override
		public Option<EvalResult> allocType(final GNewInvokeExpr op, final InstrumentedState state, final List<Object> constrArgs, final Context context) {
			return this.allocTypeInternal(op, state, constrArgs, context, null);
		}

		@Override
		public Option<EvalResult> allocType(final String allocedTypeName, final InstrumentedState state, final InvokeExpr op, final Context context) {
			return this.allocTypeInternal(allocedTypeName, state, op, context, null);
		}

		@Override
		public Option<EvalResult> allocUnknownType(final InstrumentedState state, final StaticInvokeExpr op, final Context context) {
			return this.allocUnknownType(state, op, context, null);
		}

		@Override
		public Option<MethodResult> handleInvoke(final Context callingContext, final StaticInvokeExpr expr, final List<Object> arguments, final InstrumentedState callState) {
			return this.handleInvoke(callingContext, expr, arguments, callState, null);
		}

		@Override
		protected Option<MethodResult> doAbstractCall(final AVal abstractReceiver, final List<Object> arguments, final SootMethod callee,
				final PlainStateImpl<AS> collapsedState, final Context calleeContext, final BoundaryInformation<Context> info, final Object relType) {
			return this.ai.handleCall(callee, abstractReceiver, arguments, collapsedState, calleeContext, info);
		}
	}
	
	private static class RelationalNullCallHandler<AVal, AHeap, AS, Context, Relation> extends 
		AbstractNullCallHandler<AVal, AHeap, AS, Context, Option<Relation>> implements RelationalCallHandler<Context, Relation> {

		private final RelationalAbstractInterpretation<AVal, AHeap, AS, Context, Relation> rAi;

		public RelationalNullCallHandler(final RelationalAbstractInterpretation<AVal, AHeap, AS, Context, Relation> ai, 
				final InstrumentationManager<AVal, AHeap, AS> manager, final Monads<AVal, AS> monads) {
			super(ai, manager, monads);
			this.rAi = ai;
		}

		@Override
		public Option<MethodResult> handleCall(final Context callingContext, final InstanceInvokeExpr iie, final Object receiver, final List<Object> arguments,
				final InstrumentedState callState) {
			return this.handleCallInternal(callingContext, iie, receiver, arguments, callState, Option.none());
		}

		@Override
		public Option<EvalResult> allocType(final GNewInvokeExpr op, final InstrumentedState state, final List<Object> constrArgs, final Context context) {
			return this.allocTypeInternal(op, state, constrArgs, context, Option.none());
		}

		@Override
		public Option<EvalResult> allocType(final String allocedTypeName, final InstrumentedState state, final InvokeExpr op, final Context context) {
			return this.allocTypeInternal(allocedTypeName, state, op, context, Option.none());
		}

		@Override
		public Option<EvalResult> allocUnknownType(final InstrumentedState state, final StaticInvokeExpr op, final Context context) {
			return this.allocUnknownType(state, op, context, Option.none());
		}

		@Override
		public Option<MethodResult> handleInvoke(final Context callingContext, final StaticInvokeExpr expr, final List<Object> arguments, final InstrumentedState callState) {
			return this.handleInvoke(callingContext, expr, arguments, callState, Option.none());
		}

		@Override
		public Option<MethodResult> handleCall(final Context callingContext, final InstanceInvokeExpr iie, final Object receiver, final List<Object> arguments,
				final InstrumentedState callState, final Relation inputRelation) {
			return this.handleCallInternal(callingContext, iie, receiver, arguments, callState, Option.some(inputRelation));
		}

		@Override
		public Option<EvalResult> allocType(final GNewInvokeExpr op, final InstrumentedState state, final List<Object> constrArgs, final Context context, final Relation inputRelation) {
			return this.allocTypeInternal(op, state, constrArgs, context, Option.some(inputRelation));
		}

		@Override
		protected Option<MethodResult> doAbstractCall(final AVal abstractReceiver, final List<Object> arguments, final SootMethod callee,
				final PlainStateImpl<AS> collapsedState, final Context calleeContext, final BoundaryInformation<Context> info, final Option<Relation> relType) {
			if(relType.isSome()) {
				return rAi.handleCall(callee, abstractReceiver, arguments, collapsedState, calleeContext, info, relType.some());
			} else {
				return rAi.handleCall(callee, abstractReceiver, arguments, collapsedState, calleeContext, info);
			}
		}
		
	}

	
	public static class NullInterpreter<AVal, AHeap, AS, Context> implements CombinedInterpretation {
		private final SootClass mainClass;
		private final String mainMethodName;
		private final Monads<AVal, AS> monads;
		private final Lattice<AVal> valueLattice;
		private final Lattice<AHeap> heapLattice;
		private final AbstractInterpretation<AVal, AHeap, AS, Context> ai;

		@Override public AbstractInterpretation<?, ?, ?, ?> getAbstractInterpretation() {
			return ai;
		}

		@SuppressWarnings("unchecked")
		public NullInterpreter(final AbstractInterpretation<AVal, AHeap, AS, Context> ai, final String classPath,
				final String mainClassName, final String mainMethod, final ReflectionModel rm) {
			this.ai = ai;
			this.mainClass = Interpreter.setupSoot(mainClassName, classPath);
			this.mainMethodName = mainMethod;
			
			this.monads = Monads.makeNullMonads(ai);

			this.valueLattice = ai.lattices().valueLattice();
			this.heapLattice = ai.lattices().heapLattice();
			MetaInterpreter.injectMonads(heapLattice, monads);
			MetaInterpreter.injectMonads(valueLattice, monads);
			MetaInterpreter.injectMonads(ai.lattices().stateLattice(), monads);
			final TypeOracle oracle = new TypeOracle() {
				
				@Override
				public TypeOwner classifyType(final String className) {
					return TypeOwner.APPLICATION;
				}
			};
			BodyManager.init(oracle);
			
			for(final Injectable nm : ai.monadUsers()) {
				MetaInterpreter.injectMonads(nm, monads);
			}
			ai.inject(monads);
			final InstrumentationManager<AVal, AHeap, AS> manager = new InstrumentationManager<>(monads, ai.stateTransformer(),
					(final InstrumentedState a) -> new Pair<>(null, ((PlainStateImpl<AS>)a).state),
					heapLattice, valueLattice, oracle);
			
			ReflectionEnvironment.init(rm);
			MetaInterpreter.setupBranchInterpreters(ai, monads, st -> { throw new UnsupportedOperationException(); }, bi -> { },
					(oState, aState) -> new PlainStateImpl<>(aState), inst -> ((PlainStateImpl<AS>)inst).state);
			if(ai instanceof RelationalAbstractInterpretation) {
				final RelationalAbstractInterpretation<AVal, AHeap, AS, Context, ?> rAi = (RelationalAbstractInterpretation<AVal, AHeap, AS, Context, ?>) ai;
				MetaInterpreter.injectCallHandler(rAi, ai_ -> new RelationalNullCallHandler<>(ai_,  manager, monads));
			} else {
				ai.setCallHandler(new NonRelationalNullCallHandler<>(ai, manager, monads));	
			}
			ai.instrument(manager);
		}
		
		@Override
		public void run() {
			final SootMethod mainMethod = mainClass.getMethod("void " + this.mainMethodName + "()");
			ai.run(mainClass, mainMethod, new PlainStateImpl<AS>(ai.stateTransformer().emptyState()), monads);
			ai.dischargeProofObligations();
		}

		@Override public void interrupt() {
			this.ai.interrupt();
		}
	}

	
	public static void main(final String[] inArgs) throws FileNotFoundException {
		final OptionParser parser = new OptionParser("t:p:i:l:a:y:ndqe:c:g:s:rv:");
		final OptionSet parse = parser.parse(inArgs);
		final String[] args = parse.nonOptionArguments().toArray(new String[0]);

		final AbstractInterpretation<?, ?, ?, ?> ai;
		if(args[0].equals("array")) {
			ai = new ArrayBoundsChecker();
		} else if(args[0].equals("iflow")) {
			ai = new OptimisticInformationFlow();
		} else if(args[0].equals("pta")) {
			ai = new BasicInterpreter();
		} else {
			System.out.println("Unknown analysis");
			return;
		}

		final TypeOracle oracle;
		if(parse.hasArgument("a")) {
			oracle = new ApplicationTokenBasedOracle((String)parse.valueOf("a"));
		} else if(parse.hasArgument("y")) {
			oracle = new YamlBasedOracle((String)parse.valueOf("y"));
		} else if(parse.has("n")) {
			oracle = null;
		} else {
			final String spec = (String) parse.valueOf("p");
			final String[] parts = spec.split(":");
			oracle = new PackageBasedOracle(parts[0], parts[1]);
		}
		final GlobalState gs;
		if(parse.has("i")) {
			gs = new PartialConcreteState((String) parse.valueOf("i"));
		} else if(parse.has("l")) {
			final Integer[] stream = Stream.<String>arrayStream(((String) parse.valueOf("l")).split(",")).map(Integer::parseInt).toArray().array(Integer[].class);
			gs = new InMemoryGlobalState(stream);
		} else if(parse.has("y")) {
			gs = new YamlGlobalState((String) parse.valueOf("y"));
		} else {
			gs = new NondetGlobalState();
		}
		final ReflectionModel m;
		if(parse.has("y")) {
			m = new YamlReflectionModel((String) parse.valueOf("y"));
		} else {
			m = new NullReflectionModel();
		}
		final CombinedInterpretation ci;
		final String classPath = args[1];
		final String mainClassName = args[2];
		final String mainMethodName = args[3];
		if(parse.has("e")) {
			BodyManager.setInclude(Arrays.asList(((String)parse.valueOf("e")).split(":")));
		}
		if(!parse.has("n")) {
			final MetaInterpreter<?, ?, ?, ?> mi = new MetaInterpreter<>(ai, oracle, classPath, mainClassName, mainMethodName, gs, m);
			ci = mi;
		} else {
			final NullInterpreter<?, ?, ?, ?> ni = new NullInterpreter<>(ai, classPath, mainClassName, mainMethodName, m);
			ci = ni;
		}
		if(parse.has("d")) {
			Interpreter.DEBUG_CALLS = true;
		}
		if(parse.has("q")) {
			IntrinsicHandler.ENABLE_PRINTLN = false;
		}
		final boolean[] timeoutFlag = new boolean[]{false};
		if(parse.has("t")) {
			if(!parse.has("n")) {
				throw new IllegalStateException();
			}
			final Timer timer = new Timer(true);
			timer.schedule(new TimerTask() {
				@Override public void run() {
					timeoutFlag[0] = true;
					ci.interrupt();
				}
			}, Long.parseLong((String) parse.valueOf("t")) * 1000);
		}
		if(parse.has("g")) {
			Interpreter.TRACK_CALL_EDGES = true;
		}
		if(parse.has("r")) {
			Interpreter.LOG_REFLECTION = true;
		}
		List<Object> resultCounts = null;
		if(parse.has("c")) {
			// this is an insane API. Oh well
			if(ci.getAbstractInterpretation() instanceof ResultCollectingAbstractInterpretation) {
				final ResultCollectingAbstractInterpretation rcai = (ResultCollectingAbstractInterpretation) ci.getAbstractInterpretation();
				final List<Object> results = new ArrayList<>();
				rcai.setResultStream(results::add);
				resultCounts = results;
			}
		}
		final long start = System.currentTimeMillis();
		ci.run();
		final long end = System.currentTimeMillis();
		if(parse.has("s")) {
			final String outputStream = (String) parse.valueOf("s");
			if(outputStream.equals("-")) {
				if(timeoutFlag[0]) {
					System.out.println("Timeout");
				} else {
					System.out.println("Analysis time (ms): " + (end - start));
				}
			} else {
				final Map<String, Object> stats = new HashMap<>();
				if(timeoutFlag[0]) {
					stats.put("timeout", true);
					stats.put("runtime", -1);
				} else {
					stats.put("timeout", false);
					stats.put("runtime", end - start);
				}
				YamlParser.dumpYaml(stats, outputStream);
			}
		}
		if(parse.has("c")) {
			// this is an insane API. Oh well
			if(ci.getAbstractInterpretation() instanceof ResultCollectingAbstractInterpretation) {
				assert resultCounts != null;
				final String outFile = (String) parse.valueOf("c");
				if(outFile.equals("-")) {
					System.out.println(resultCounts.size());
				} else {
					YamlParser.dumpYaml(resultCounts.size(), outFile);
				}
				if(parse.has("v")) {
					List<String> output = new ArrayList<>();
					resultCounts.forEach(d -> output.add(d instanceof PrettyPrintable ? ((PrettyPrintable) d).prettyPrint() : d.toString()));
					YamlParser.dumpYaml(output, (String) parse.valueOf("v"));
				}
			}
		}
		if(parse.has("g")) {
			int counter = 0;
			final Map<Object, Integer> canonizationMap = new HashMap<>();
			final Map<Integer, List<String>> callGraph = new HashMap<>();
			final Map<Integer, String> nodeRepr = new HashMap<>();
			int numEdges = 0;
			final List<Integer> calleeSizes = new ArrayList<>();
			int numPolymorphicSites = 0;
			for(final Multimap<?, SootMethod> cgComponent : Arrays.asList(CooperativeInterpreter.CALL_GRAPH, MetaInterpreter.CALL_GRAPH)) {
				for(final Map.Entry<?, Collection<SootMethod>> kv : cgComponent.asMap().entrySet()) {
					final Object key = kv.getKey();
					final Collection<SootMethod> value = kv.getValue();
					if(value.size() > 1) {
						numPolymorphicSites++;
					}
					numEdges += value.size();
					calleeSizes.add(value.size());
					final List<String> calleeList = new ArrayList<>();
					value.stream().map(SootMethod::getSignature).forEach(calleeList::add);
					if(canonizationMap.containsKey(key)) {
						final int intKey = canonizationMap.get(key);
						assert nodeRepr.containsKey(intKey);
						assert callGraph.containsKey(intKey);
						assert nodeRepr.get(intKey).equals(key.toString());
						assert calleeList.containsAll(callGraph.get(intKey));
						assert callGraph.get(intKey).containsAll(calleeList);
						continue;
					}
					final int intKey = counter++;
					canonizationMap.put(key, intKey);
					callGraph.put(intKey, calleeList);
					nodeRepr.put(intKey, key.toString());
				}
			}
			final String outputStream = (String) parse.valueOf("g");
			final int numNodes = nodeRepr.size();
			Collections.sort(calleeSizes);
			if(outputStream.equals("-")) {
				System.out.println(">> Call Graph Info: ");
				System.out.println("# Edges: " + numEdges);
				System.out.println("# Nodes: " + numNodes);
				System.out.println("Avg callees: " + numEdges / ((float) numNodes));
				System.out.println("Median callees: " + calleeSizes.get(calleeSizes.size() / 2));
				System.out.println("# Polymorphic call-sites " + numPolymorphicSites);
				if(Interpreter.LOG_REFLECTION) {
					System.out.println("Mex reflective concrete invoke: " + reflectiveCallMax._2()[0]);
					System.out.println("Mex reflective abstract invoke: " + reflectiveCallMax._1()[0]);
				}
				System.out.println("<< Done");
			} else {
				final Map<String, Object> callGraphInfo = new HashMap<>();
				callGraphInfo.put("callees", callGraph);
				callGraphInfo.put("nodes", nodeRepr);
				callGraphInfo.put("calleeSizes", calleeSizes);
				callGraphInfo.put("numEdges", numEdges);
				callGraphInfo.put("numNodes", numNodes);
				callGraphInfo.put("numPoly", numPolymorphicSites);
				if(Interpreter.LOG_REFLECTION) {
					callGraphInfo.put("max-concrete-invoke", reflectiveCallMax._2()[0]);
					callGraphInfo.put("max-abstract-invoke", reflectiveCallMax._1()[0]);
				}
				YamlParser.dumpYaml(callGraphInfo, outputStream);
			}
		}
	}
	
	// TODO: find a better place
	public static IValue concretize(final Object sz) {
		final IValue toReturn;
		if(sz instanceof IValue) {
			toReturn = (IValue) sz;
		} else if(sz instanceof Concretizable) {
			toReturn = IValue.concretize((Concretizable) sz);
		} else {
			toReturn = IValue.nondet();
		}
		return toReturn;
	}
	
	private static class ReflectiveOperationContextImpl implements ReflectiveOperationContext {
		
		private final Unit parentUnit;
		private final Option<Local> targetLocal;
		private final Option<RefLikeType> castHint;

		public ReflectiveOperationContextImpl(final Unit parentUnit, final Option<Local> targetLocal, final Option<RefLikeType> castHint) {
			this.parentUnit = parentUnit;
			this.targetLocal = targetLocal;
			this.castHint = castHint;
		}

		@Override
		public Unit parentUnit() {
			return this.parentUnit;
		}

		@Override
		public Option<Local> targetLocal() {
			return this.targetLocal;
		}

		@Override
		public Option<RefLikeType> castHint() {
			return this.castHint;
		}
		
	}
	
	public static ReflectiveOperationContext makeReflectiveAllocationContext(final StaticInvokeExpr expr) {
		
		final Unit hostUnit = BodyManager.getHostUnit(expr);
		if(hostUnit instanceof AssignStmt && ((AssignStmt) hostUnit).getLeftOp() instanceof Local) {
			final Pair<Option<RefLikeType>, Boolean> expressionContext = MetaInterpreter.findCastExpression(expr, ((AssignStmt) hostUnit).getRightOp());
			if(expressionContext.getO2()) {
				return new ReflectiveOperationContextImpl(hostUnit, Option.some((Local)((DefinitionStmt) hostUnit).getLeftOp()), expressionContext.getO1());
			} else {
				return new ReflectiveOperationContextImpl(hostUnit, Option.none(), expressionContext.getO1());
			}
		} else if(hostUnit instanceof AbstractOpStmt) {
			final Pair<Option<RefLikeType>, Boolean> expressionContext = MetaInterpreter.findCastExpression(expr, ((AbstractOpStmt) hostUnit).getOp());
			if(expressionContext != null) {
				return new ReflectiveOperationContextImpl(hostUnit, Option.none(), expressionContext.getO1());
			}
		} else {
			for(final ValueBox v : hostUnit.getUseBoxes()) {
				final Pair<Option<RefLikeType>, Boolean> expressionContext = MetaInterpreter.findCastExpression(expr, v.getValue());
				if(expressionContext != null) {
					return new ReflectiveOperationContextImpl(hostUnit, Option.none(), expressionContext.getO1());
				}
			}
		}
		throw new RuntimeException("Could not find invocation " + expr + " in " + hostUnit);
	}

	private static Pair<Option<RefLikeType>, Boolean> findCastExpression(final Value target, final Value root) {
		return MetaInterpreter.findCastExpression(target, true, root);
	}

	private static Pair<Option<RefLikeType>, Boolean> findCastExpression(final Value target, final boolean directFlow, final Value curr) {
		if(curr == target) {
			return new Pair<>(Option.none(), directFlow);
		}
		if(curr instanceof CastExpr && ((CastExpr) curr).getOp() == target) {
			return new Pair<>(Option.some((RefLikeType)curr.getType()), directFlow);
		} else if(curr instanceof CastExpr) {
			return MetaInterpreter.findCastExpression(target, false, ((CastExpr) curr).getOp());
		}
		for(final ValueBox use : curr.getUseBoxes()) {
			final Pair<Option<RefLikeType>, Boolean> ret = MetaInterpreter.findCastExpression(target, false, use.getValue());
			if(ret != null) {
				return ret;
			}
		}
		return null;
	}

	@Override
	public void run() {
		coopInterpreter.start(mainClass, mainMethod);
		this.ai.dischargeProofObligations();
	}

	public static SootMethodRefImpl getNullaryConstructorRef() {
		return new SootMethodRefImpl(Scene.v().getObjectType().getSootClass(), "<init>", Collections.emptyList(), VoidType.v(), false);
	}
}
