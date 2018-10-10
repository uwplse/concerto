package edu.washington.cse.concerto.interpreter.ai.binop;

public class DefaultBranchPropagator<AVal> implements BranchPropagator<AVal> {
	@Override
	public AVal propagateLT(final AVal left, final AVal right) {
		return left;
	}

	@Override
	public AVal propagateLE(final AVal left, final AVal right) {
		return left;
	}

	@Override
	public AVal propagateEQ(final AVal left, final AVal right) {
		return left;
	}

	@Override
	public AVal propagateNE(final AVal left, final AVal right) {
		return left;
	}

	@Override
	public AVal propagateGT(final AVal left, final AVal right) {
		return left;
	}

	@Override
	public AVal propagateGE(final AVal left, final AVal right) {
		return left;
	}

}
