package meta.application;

import meta.framework.Action;
import meta.framework.Dispatcher;
import meta.framework.Request;
import meta.framework.response.Result;

public class SourceAction implements Action {
	public static class Holder {
		public int f = 0;
	}
	
	@Override
	public Result doAction(final int r, final Dispatcher d, final Request req) {
		final int a = req.getRequestData(0);
		final Holder h = new Holder();
		h.f = a;
		d.writeDatabase(h.f);
		return d.forward(6, req.withAttribute(2, h));
	}
}
