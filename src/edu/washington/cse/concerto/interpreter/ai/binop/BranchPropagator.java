package edu.washington.cse.concerto.interpreter.ai.binop;

public interface BranchPropagator<AVal> {
	// What can we learn about left, if we know that left op right is true, where op is one of =, !=, >, etc.
	default public AVal propagateLT(final AVal left, final AVal right) {
		return left;
	}
	default public AVal propagateLE(final AVal left, final AVal right) {
		return left;
	}

	default public AVal propagateEQ(final AVal left, final AVal right) {
		return left;
	}
	default public AVal propagateNE(final AVal left, final AVal right) {
		return left;
	}

	default public AVal propagateGT(final AVal left, final AVal right) {
		return left;
	}
	default public AVal propagateGE(final AVal left, final AVal right) {
		return left;
	}
}