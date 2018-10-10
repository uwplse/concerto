package meta.application;

import meta.framework.Action;
import meta.framework.Dispatcher;
import meta.framework.Request;
import meta.framework.response.Result;

public class FaultAction implements Action {
	@Override
	public Result doAction(final int r, final Dispatcher d, final Request req) {
		final int[] reg = d.getRegisteredChain();
		d.printInt(reg[1]);
		return null;
	}
}
