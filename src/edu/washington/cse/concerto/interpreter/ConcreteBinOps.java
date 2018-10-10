package edu.washington.cse.concerto.interpreter;

import soot.Value;
import soot.jimple.ConditionExpr;
import edu.washington.cse.concerto.interpreter.ai.AbstractComparison;
import edu.washington.cse.concerto.interpreter.ai.CompareResult;
import edu.washington.cse.concerto.interpreter.ai.ValueMonad;
import edu.washington.cse.concerto.interpreter.ai.binop.PrimitiveOperations;
import edu.washington.cse.concerto.interpreter.value.EmbeddedValue;
import edu.washington.cse.concerto.interpreter.value.IValue;
import edu.washington.cse.concerto.interpreter.value.IValue.RuntimeTag;

class ConcreteBinOps {
	public static final BinOp ADD = new BinOp() {
		@Override
		public IValue apply(final IValue v1, final IValue v2, final HeapProvider state) {
			return IValue.lift(v1.asInt() + v2.asInt());
		}

		@Override
		public <AVal> IValue apply(final AVal a1, final AVal a2, final Value expr, final ValueMonad<?> monad, final PrimitiveOperations<AVal> op) {
			return new IValue(new EmbeddedValue(op.plus(a1, a2), monad));
		}
	};

	public static final BinOp LE = new BinOp() {
		@Override
		public IValue apply(final IValue v1, final IValue v2, final HeapProvider state) {
			return IValue.lift(v1.asInt() <= v2.asInt());
		}

		@Override
		public <AVal> IValue apply(final AVal a1, final AVal a2, final Value expr, final ValueMonad<?> monad, final PrimitiveOperations<AVal> op) {
			final CompareResult res = op.cmp(a1, a2);
			return AbstractComparison.abstractComparison((ConditionExpr) expr, res, IValue.nondet(), IValue.lift(true), IValue.lift(false));
		}
	};

	public static final BinOp GE = new BinOp() {
		@Override
		public IValue apply(final IValue v1, final IValue v2, final HeapProvider state) {
			return IValue.lift(v1.asInt() >= v2.asInt());
		}

		@Override
		public <AVal> IValue apply(final AVal a1, final AVal a2, final Value expr, final ValueMonad<?> monad, final PrimitiveOperations<AVal> op) {
			final CompareResult res = op.cmp(a1, a2);
			return AbstractComparison.abstractComparison((ConditionExpr) expr, res, IValue.nondet(), IValue.lift(true), IValue.lift(false));
		}
	};

	public static final BinOp SUB = new BinOp() {
		@Override
		public IValue apply(final IValue v1, final IValue v2, final HeapProvider state) {
			return IValue.lift(v1.asInt() - v2.asInt());
		}

		@Override
		public <AVal> IValue apply(final AVal a1, final AVal a2, final Value expr, final ValueMonad<?> monad, final PrimitiveOperations<AVal> op) {
			return new IValue(new EmbeddedValue(op.minus(a1, a2), monad));
		}
	};

	public static final BinOp GT = new BinOp() {
		@Override
		public IValue apply(final IValue v1, final IValue v2, final HeapProvider state) {
			return IValue.lift(v1.asInt() > v2.asInt());
		}

		@Override
		public <AVal> IValue apply(final AVal a1, final AVal a2, final Value expr, final ValueMonad<?> monad, final PrimitiveOperations<AVal> op) {
			final CompareResult res = op.cmp(a1, a2);
			return AbstractComparison.abstractComparison((ConditionExpr) expr, res, IValue.nondet(), IValue.lift(true), IValue.lift(false));
		}
	};

	public static final BinOp MUL = new BinOp() {
		@Override
		public IValue apply(final IValue v1, final IValue v2, final HeapProvider state) {
			return IValue.lift(v1.asInt() * v2.asInt());
		}

		@Override
		public <AVal> IValue apply(final AVal a1, final AVal a2, final Value expr, final ValueMonad<?> monad, final PrimitiveOperations<AVal> op) {
			return new IValue(new EmbeddedValue(op.mult(a1, a2), monad));
		}
	};

	public static final BinOp DIV = new BinOp() {
		@Override
		public IValue apply(final IValue v1, final IValue v2, final HeapProvider state) {
			return IValue.lift(v1.asInt() / v2.asInt());
		}

		@Override
		public <AVal> IValue apply(final AVal a1, final AVal a2, final Value expr, final ValueMonad<?> monad, final PrimitiveOperations<AVal> op) {
			return new IValue(new EmbeddedValue(op.div(a1, a2), monad));
		}
	};

	public static final BinOp EQ = new BinOp() {
	   @Override
		public IValue apply(final IValue v1, final IValue v2, final HeapProvider state) {
	  	 if(v1.getTag() == RuntimeTag.INT) {
	  		 assert v2.getTag() == RuntimeTag.INT;
	  		 return IValue.lift(v1.asInt() == v2.asInt());
	  	 } else if(v1.getTag() == RuntimeTag.NULL || v2.getTag() == RuntimeTag.NULL) {
	  		 return IValue.lift(v1.getTag() == v2.getTag());
	  	 } else {
	  		 if(state.getHeap().isSummary(v1.getLocation()) || state.getHeap().isSummary(v2.getLocation())) {
	  			 return IValue.nondet();
	  		 }
	  		 return IValue.lift(v1.getLocation() == v2.getLocation());
	  	 }
	   }
	   
		@Override
		public <AVal> IValue apply(final AVal a1, final AVal a2, final Value expr, final ValueMonad<?> monad, final PrimitiveOperations<AVal> op) {
			final CompareResult res = op.cmp(a1, a2);
			return AbstractComparison.abstractComparison((ConditionExpr) expr, res, IValue.nondet(), IValue.lift(true), IValue.lift(false));
		}
	};

	
	public static final BinOp LT = new BinOp() {
	   @Override
		public IValue apply(final IValue v1, final IValue v2, final HeapProvider state) {
	     return IValue.lift(v1.asInt() < v2.asInt());
	   }
	
	   @Override
		public <AVal> IValue apply(final AVal a1, final AVal a2, final Value expr, final ValueMonad<?> monad, final PrimitiveOperations<AVal> op) {
	     final CompareResult res = op.cmp(a1, a2);
	     return AbstractComparison.abstractComparison((ConditionExpr)expr, res, IValue.nondet(), IValue.lift(true), IValue.lift(false));
	   }
	};


	
	public static final BinOp NE = new BinOp() {
		@Override
		public IValue apply(final IValue v1, final IValue v2, final HeapProvider state) {
			if(v1.getTag() == RuntimeTag.INT) {
				assert v2.getTag() == RuntimeTag.INT;
				return IValue.lift(v1.asInt() != v2.asInt());
			} else if(v1.getTag() == RuntimeTag.NULL || v2.getTag() == RuntimeTag.NULL) {
				return IValue.lift(v1.getTag() != v2.getTag());
			} else {
				if(state.getHeap().isSummary(v1.getLocation()) || state.getHeap().isSummary(v2.getLocation())) {
					return IValue.nondet();
				}
				return IValue.lift(v1.getLocation() != v2.getLocation());
			}
		}
		@Override
		public <AVal> IValue apply(final AVal a1, final AVal a2, final Value expr, final ValueMonad<?> monad, final PrimitiveOperations<AVal> op) {
			final CompareResult res = op.cmp(a1, a2);
			return AbstractComparison.abstractComparison((ConditionExpr) expr, res, IValue.nondet(), IValue.lift(true), IValue.lift(false));
		}
	};
}