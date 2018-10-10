package meta.framework.filter;

import meta.framework.Dispatcher;
import meta.framework.Request;
import meta.framework.response.Result;

public interface Filter {
	Result filter(Dispatcher disp, Request req, FilterChain chain);
	void init(int k, int[] vals);
}
