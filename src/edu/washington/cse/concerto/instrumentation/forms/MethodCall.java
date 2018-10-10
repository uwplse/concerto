package edu.washington.cse.concerto.instrumentation.forms;

import java.util.List;

import soot.SootMethodRef;
import edu.washington.cse.concerto.interpreter.EmbeddedState;
import edu.washington.cse.concerto.interpreter.ai.ValueMonad;
import edu.washington.cse.concerto.interpreter.heap.Heap;
import edu.washington.cse.concerto.interpreter.value.IValue;
import fj.F;
import fj.data.Either;

public class MethodCall {
	private static final F<Object, Object> IDENTITY = new F<Object, Object>() {
		@Override
		public Object f(final Object a) {
			return a;
		}
	};
	
	private final EmbeddedState<?> foreignHeap;
	private final Heap heap;
	private final Either<IValue, Object> receiver;
	private final Either<List<IValue>, List<Object>> arguments;
	private final SootMethodRef method;
	private final ValueMonad<?> monad;

	public MethodCall(final IValue baseValue, final List<IValue> concreteArguments, final Heap heap, final EmbeddedState<?> foreignHeap, final SootMethodRef method, final ValueMonad<?> monad) {
		this.arguments = Either.<List<IValue>, List<Object>>left(concreteArguments);
		this.receiver = Either.<IValue, Object>left(baseValue);
		this.heap = heap;
		this.foreignHeap = foreignHeap;
		this.method = method;
		this.monad = monad;
	}
	
	public MethodCall(final Object baseValue, final List<Object> concreteArguments, final Heap heap, final EmbeddedState<?> foreignHeap, final SootMethodRef method) {
		this.arguments = Either.<List<IValue>, List<Object>>right(concreteArguments);
		this.receiver = Either.<IValue, Object>right(baseValue);
		this.heap = heap;
		this.foreignHeap = foreignHeap;
		this.method = method;
		this.monad = null;
	}
	
	public Object getArg(final int num) {
		return this.arguments.either(new F<List<IValue>, Object>() {
			@Override
			public Object f(final List<IValue> a) {
				return monad.lift(a.get(num));
			}
		}, new F<List<Object>, Object>() {
			@Override
			public Object f(final List<Object> a) {
				return a.get(num);
			}
		});
	}
	
	public Object getBasePointer() {
		return receiver.either(new F<IValue, Object>() {
			@Override
			public Object f(final IValue a) {
				return monad.lift(a);
			}
		}, IDENTITY);
	}
	
	public RuntimeValue getBasePointerValue() {
		return new RuntimeValue(receiver, heap, foreignHeap, method.declaringClass().getType());
	}

	public RuntimeValue getArgValue(final int num) {
		return new RuntimeValue(getArg(num), heap, foreignHeap, method.parameterType(num));
	}

	public SootMethodRef getMethod() {
		return this.method;
	}
}
