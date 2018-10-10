package meta.application;

import meta.framework.Action;
import meta.framework.Dispatcher;
import meta.framework.Request;
import meta.framework.response.Payload;
import meta.framework.response.Result;

import static intr.Intrinsics.fail;

public class IntegerAddService implements Action {
	private IntegerProvider provider;

	@Override public Result doAction(final int r, final Dispatcher d, final Request req) {
		final int key;
		if(req.getAttributes().getIntAttribute(3) > 0) {
			key = req.getAttributes().getIntAttribute(3);
		} else {
			key = req.getRequestData(0);
		}
		final int toAdd = provider.getInteger(key);
		final int requestedAdd = req.getRequestData(1);
		return new Result() {
			@Override public void setPayload(final Payload x) {
				fail("unsupported");
			}

			@Override public Payload getPayload() {
				return new Payload() {
					@Override public int[] getBytes() {
						return new int[]{ toAdd + requestedAdd };
					}
				};
			}
		};
	}

	public void setIntegerProvider(final IntegerProvider p) {
		this.provider = p;
	}
}
