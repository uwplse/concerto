package meta.framework;

import meta.framework.response.Result;

public interface Action {
	public Result doAction(int r, Dispatcher d, Request req);
}
