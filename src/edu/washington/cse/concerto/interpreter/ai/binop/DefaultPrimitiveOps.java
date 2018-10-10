package edu.washington.cse.concerto.interpreter.ai.binop;

public abstract class DefaultPrimitiveOps<AVal> implements PrimitiveOperations<AVal> {
	@Override
	public AVal plus(final AVal a, final AVal b) {
		return this.binop(a, b);
	}

	protected abstract AVal binop(final AVal a, final AVal b);

	@Override
	public AVal minus(final AVal a, final AVal b) {
		return this.binop(a, b);
	}

	@Override
	public AVal mult(final AVal a, final AVal b) {
		return this.binop(a, b);
	}

	@Override
	public AVal div(final AVal a, final AVal b) {
		return this.binop(a, b);
	}
}
