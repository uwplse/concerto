package meta.framework.filter;

import meta.framework.Dispatcher;
import meta.framework.Request;
import meta.framework.response.Result;

public interface FilterChain {
	Result next(Dispatcher disp, Request req);
}
