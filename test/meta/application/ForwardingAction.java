package meta.application;

import meta.framework.Action;
import meta.framework.Dispatcher;
import meta.framework.Request;
import meta.framework.response.Result;

public class ForwardingAction implements Action {
	@Override
	public Result doAction(final int r, final Dispatcher d, final Request req) {
		return d.forward(1, req);
	}
}
