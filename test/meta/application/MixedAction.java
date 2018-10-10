package meta.application;

import meta.framework.Action;
import meta.framework.Dispatcher;
import meta.framework.FrameworkMain.ResultImpl;
import meta.framework.Request;
import meta.framework.response.NotFoundPayload;
import meta.framework.response.Payload;
import meta.framework.response.Result;

import static intr.Intrinsics.fail;

public class MixedAction implements Action {
	@Override
	public Result doAction(final int r, final Dispatcher d, final Request req) {
		if(req.getRequestData(0) == 4) {
			final Result res = new ResultImpl();
			res.setPayload(new NotFoundPayload());
			return res;
		} else {
			final Payload p = new Payload() {
				@Override
				public int[] getBytes() {
					return new int[]{
							1, 2, 3, 4, 5, 6
					};
				}
			};
			return new Result() {
				
				@Override
				public void setPayload(final Payload x) {
					fail("unsupported");
				}
				
				@Override
				public Payload getPayload() {
					return p;
				}
			};
		}
	}

}
