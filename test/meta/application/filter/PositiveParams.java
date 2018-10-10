package meta.application.filter;

import meta.framework.Dispatcher;
import meta.framework.Request;
import meta.framework.filter.Filter;
import meta.framework.filter.FilterChain;
import meta.framework.response.Result;

public class PositiveParams implements Filter {
	@Override public Result filter(final Dispatcher disp, final Request req, final FilterChain chain) {
		final int[] k = new int[req.getParameterCount()];
		for(int i = 0; i < k.length; i++) {
			final int p = req.getRequestData(i);
			if(p < 0) {
				k[i] = 0;
			} else {
				k[i] = p;
			}
		}
		return chain.next(disp, req.withRequestData(k));
	}

	@Override public void init(final int k, final int[] vals) {

	}
}
