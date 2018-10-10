package meta.framework;

import meta.framework.filter.FilterChain;
import meta.framework.response.Result;

public class Filters {
	private final FilterChain chain;
	public Filters(final FilterAndTarget[] filterImpls) {
		FilterChain curr = new FilterChain() {
			@Override public Result next(final Dispatcher disp, final Request req) {
				return disp.forward(req.getTarget(), req);
			}
		};
		for(int i = filterImpls.length - 1; i >= 0; i--) {
			curr = new ChainElem(filterImpls[i], curr);
		}
		this.chain = curr;
	}

	public Result handleRequest(final Dispatcher disp, final Request req) {
		return chain.next(disp, req);
	}

	private static class ChainElem implements FilterChain {

		private final FilterChain next;
		private final FilterAndTarget impl;

		public ChainElem(final FilterAndTarget filterImpl, final FilterChain curr) {
			this.impl = filterImpl;
			this.next = curr;
		}

		@Override public Result next(final Dispatcher disp, final Request req) {
			if(impl.matches(req.getTarget())) {
				return this.impl.getFilter().filter(disp, req, this.next);
			} else {
				return this.next.next(disp, req);
			}
		}
	}
}
