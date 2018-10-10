package meta.application;

import meta.framework.Action;
import meta.framework.Dispatcher;
import meta.framework.NotFoundResult;
import meta.framework.Request;
import meta.framework.response.Payload;
import meta.framework.response.Result;

import static intr.Intrinsics.fail;

public class StatefulAction implements Action {
	int[] state;
	@Override public Result doAction(final int r, final Dispatcher d, final Request req) {
		final int key = req.getRequestData(0);
		final int request = req.getRequestData(1);
		final int value = req.getRequestData(2);
		if(request == 0 && key < state.length) {
			state[key] = value;
			return new Result() {
				@Override public void setPayload(final Payload x) {
					fail("unsupported");
				}

				@Override public Payload getPayload() {
					return new Payload() {
						@Override public int[] getBytes() {
							return new int[] { 0 };
						}
					};
				}
			};
		} else if(request == 1 && key < state.length) {
			return d.forward(8, req.withRequestData(new int[]{state[key], value}));
		} else {
			return new NotFoundResult();
		}
	}

	public void setStateLength(final int k) {
		state = new int[k];
	}
}
