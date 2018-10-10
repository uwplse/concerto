package edu.washington.cse.concerto.interpreter.ai;

import edu.washington.cse.concerto.instrumentation.InstrumentationManager;
import edu.washington.cse.concerto.interpreter.ai.binop.PrimitiveOperations;
import edu.washington.cse.concerto.interpreter.ai.injection.Injectable;
import edu.washington.cse.concerto.interpreter.ai.injection.NeedsMonads;
import edu.washington.cse.concerto.interpreter.meta.BoundaryInformation;
import edu.washington.cse.concerto.interpreter.meta.InstrumentedState;
import edu.washington.cse.concerto.interpreter.meta.Monads;
import fj.data.Option;
import soot.ArrayType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Type;
import soot.Value;
import soot.grimp.Grimp;
import soot.grimp.NewInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.toolkits.scalar.Pair;

import java.util.Collections;
import java.util.List;

public interface AbstractInterpretation<AVal, AHeap, AS, Context> extends NeedsMonads<AVal, AS>, Interruptible {
	State<AHeap, AS> stateTransformer();
	AbstractionFunction<AVal> alpha();

	void instrument(InstrumentationManager<AVal, AHeap, AS> instManager);
	void setCallHandler(CallHandler<Context> ch);
	
	Lattices<AVal, AHeap, AS> lattices();
	
	/*
	 * FR -> AI entry
	 * Return the state at return from the callee. Should perform fixpoint iteration.
	 * There is no abstract context on the call stack. The call is made under the concrete context [context]
	 */
	MethodResult interpretToFixpoint(SootMethod m, AVal receiver, List<Object> arguments, InstrumentedState calleeState, Context calleeContext);
	/*
	 * AI -> AI call
	 * Return the state in the *callee* after invoking the callee.
	 * 
	 * [m] is the callee
	 * [arguments] and [receiver] are the arguments to the call
	 * [callState] is the state in the caller at the call
	 * [calleeContext] is the context for the callee. It is computed by the context manager
	 * [callerInformation] describes the root context of this call.
	 * + For AI -> AI, this is the state, callsite, and context of the direct callee
	 * + For AI -> FR -> AI, this is the state, callSite, and context of the preceding AI caller
	 * + FR -> AI will not occur, as this is handled by interpretToFixpoint
	 * 
	 *  This method should return None if the abstract interpretation cannot compute a sound return state
	 *  for the calling state, or Some(state) if it can. For simple input/output AIs, this should return
	 *  None, unless this call state  is <= than the existing state at method entry
	 */
	Option<MethodResult> handleCall(SootMethod m, AVal receiver, List<Object> arguments, InstrumentedState callState,
			Context calleeContext, BoundaryInformation<Context> callerContext);
	default List<SootMethod> getCalleesOfCall(final InvokeExpr iie, final AVal receiver) {
		return getMethodForRef(iie.getMethodRef(), receiver);
	}
	List<SootMethod> getMethodForRef(SootMethodRef subSig, AVal receiver);

	
	List<Injectable> monadUsers();
	ContextManager<Context, AVal, AS, AHeap> contextManager();
	
	/**
	 * Return true if this type is handled by the AI
	 * TODO: remove me
	 */
	boolean modelsType(Type type);
	
	Pair<AHeap, AVal> abstractObjectAlloc(RefType t, AHeap inputHeap, Value allocationExpr, Context allocationContext);
	Pair<AHeap, AVal> abstractArrayAlloc(ArrayType t, AHeap inputHeap, Value allocationExpr, List<AVal> sizes, Context allocationContext);
	Option<Pair<AHeap, AVal>> allocateUnknownObject(AHeap inputHeap, Value allocationExpr, Context allocationContext, ReflectiveOperationContext reflContext);
	PrimitiveOperations<AVal> primitiveOperations();
	ObjectOperations<AVal, AHeap> objectOperations();
	
	default void run(final SootClass cls, final SootMethod main, final InstrumentedState initialState, final Monads<AVal, AS> monads) {
		if(!(this.contextManager() instanceof EntryPointContextManager)) {
			throw new UnsupportedOperationException();
		}
		Scene.v().loadClass(cls.getName(), SootClass.BODIES);
		final EntryPointContextManager<Context, AVal, AS, AHeap> ctxt = (EntryPointContextManager<Context, AVal, AS, AHeap>) this.contextManager();
		final Context initialContext = ctxt.initialContext(main);
		final Context initialAllocContext = ctxt.initialAllocationContext(main);
		final SootMethod initMethod = cls.getMethod("void <init>()");
		final NewInvokeExpr mainInvoke = Grimp.v().newNewInvokeExpr(cls.getType(), initMethod.makeRef(), Collections.emptyList());
		
		final EvalResult allocResult = monads.stateMonad.mapToResult(initialState, emptyState -> { 
			final Pair<AHeap, AVal> alloced = this.abstractObjectAlloc(cls.getType(), this.stateTransformer().project(emptyState), mainInvoke, initialAllocContext);
			return new Pair<>(this.stateTransformer().inject(emptyState, alloced.getO1()), monads.valueMonad.lift(alloced.getO2()));
		});
		final MethodResult initResult = this.interpretToFixpoint(initMethod, monads.valueMonad.alpha(allocResult.value), Collections.emptyList(), 
			allocResult.state, ctxt.initialContext(initMethod));
		this.interpretToFixpoint(main, monads.valueMonad.alpha(allocResult.value), Collections.emptyList(), initResult.getState(), initialContext);
	}
	
	default void dischargeProofObligations() { }
}
