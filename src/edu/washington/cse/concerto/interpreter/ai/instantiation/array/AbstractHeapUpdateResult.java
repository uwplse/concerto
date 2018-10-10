package edu.washington.cse.concerto.interpreter.ai.instantiation.array;

import edu.washington.cse.concerto.interpreter.heap.HeapFaultStatus;
import edu.washington.cse.concerto.interpreter.heap.HeapReadResult;
import fj.F;
import fj.F0;
import fj.F2;
import fj.P;
import fj.P2;
import fj.data.Option;

public class AbstractHeapUpdateResult<B> extends AbstractHeapAccessResult {
	public final B value;

	public AbstractHeapUpdateResult(final B value, final HeapFaultStatus npe, final HeapFaultStatus oob, final boolean infeasible) {
		super(npe, oob, infeasible);
		this.value = value;
	}

	public static <B> AbstractHeapUpdateResult<B> lift(final AbstractHeapAccessResult o) {
		assert o.shouldPrune();
		return lift((B)null, o);
	}

	public static <B> AbstractHeapUpdateResult<B> lift(final B v, final AbstractHeapAccessResult o) {
		return new AbstractHeapUpdateResult<>(v, o.npe, o.oob, o.infeasible);
	}

	public static <B> AbstractHeapUpdateResult<B> lift(final HeapReadResult<B> concrete) {
		return new AbstractHeapUpdateResult<>(concrete.value, concrete.npe, concrete.oob, false);
	}

	public static <B> AbstractHeapUpdateResult<B> of(final B result) {
		return lift(result, AbstractHeapUpdateResult.SAFE);
	}

	public <C> P2<C, AbstractHeapAccessResult> extractAndMerge(final F<B, C> mapper, final F0<C> zero, final AbstractHeapAccessResult res) {
		final AbstractHeapAccessResult joined = AbstractHeapAccessResult.join(this, res);
		if(value == null) {
			return P.p(zero.f(), joined);
		} else {
			return P.p(mapper.f(value), joined);
		}
	}

	public <C> P2<C, AbstractHeapAccessResult> extract(final F<B, C> mapper, final F0<C> zero) {
		if(value == null) {
			return P.p(zero.f(), this);
		} else {
			return P.p(mapper.f(value), this);
		}
	}

	public <C> P2<C, AbstractHeapAccessResult> foldAndExtract(final F2<C, B, C> folder, final C init) {
		return extract(a -> folder.f(init, a), () -> init);
	}

	public <C> AbstractHeapUpdateResult<C> fold(final F2<C, B, C> folder, final C init) {
		if(this.value == null) {
			return new AbstractHeapUpdateResult<>(init, npe.joinWith(HeapFaultStatus.MUST_NOT), oob.joinWith(HeapFaultStatus.MUST_NOT), false);
		} else {
			return new AbstractHeapUpdateResult<>(folder.f(init, this.value), npe.joinWith(HeapFaultStatus.MUST_NOT), oob.joinWith(HeapFaultStatus.MUST_NOT), false);
		}
	}

	@Override public AbstractHeapUpdateResult<B> npe() {
		if(this.npe == HeapFaultStatus.MAY) {
			return this;
		}
		return new AbstractHeapUpdateResult<B>(this.value, this.npe.mark(), oob, infeasible);
	}

	public static <B> AbstractHeapUpdateResult<B> lift(final Option<B> obj, final AbstractHeapAccessResult fault) {
		return obj.map(AbstractHeapUpdateResult::of).orSome(() -> lift(fault));
	}

	public AbstractHeapUpdateResult<B> infeasible() {
		return this;
	}

	public AbstractHeapUpdateResult<B> oob() {
		if(this.oob == HeapFaultStatus.MAY || this.oob == HeapFaultStatus.MUST) {
			return this;
		} else {
			return new AbstractHeapUpdateResult<>(this.value, npe, oob.mark(), this.infeasible);
		}
	}

	public AbstractHeapUpdateResult<B> merge(final AbstractHeapUpdateResult<B> v2, final F2<B, B, B> join) {
		return new AbstractHeapUpdateResult<>(v2.value == null ? this.value : this.value == null ? v2.value : join.f(this.value, v2.value),
				npe.joinWith(v2.npe),
				oob.joinWith(v2.oob),
				this.infeasible && v2.infeasible);
	}

	@Override public String toString() {
		return "AbstractHeapUpdateResult{" + "value=" + value + ", infeasible=" + infeasible + ", npe=" + npe + ", oob=" + oob + '}';
	}
}
