package meta.application;

import meta.framework.Action;
import meta.framework.Dispatcher;
import meta.framework.FrameworkMain.ResultImpl;
import meta.framework.response.Payload;
import meta.framework.Request;
import meta.framework.response.Result;

public class UnusedAction implements Action {
	public static class PayloadImpl2 implements Payload {
		@Override
		public int[] getBytes() {
			return new int[9];
		}
	}
	
	@Override
	public Result doAction(final int r, final Dispatcher d, final Request req) {
		final Result res = new ResultImpl();
		res.setPayload(new PayloadImpl2());
		return res;
	}

}
