package meta.application;

import meta.framework.Action;
import meta.framework.Dispatcher;
import meta.framework.FrameworkMain.ResultImpl;
import meta.framework.Request;
import meta.framework.response.Payload;
import meta.framework.response.Result;

import static intr.Intrinsics.nondet;

public class LoopAction implements Action {
	@Override
	public Result doAction(final int r, final Dispatcher d, final Request req) {
		Result toReturn = new ResultImpl();
		for(int i = 0; i < d.readNondet(); i++) {
			toReturn = new ResultImpl();
		}
		toReturn.setPayload(new Payload() {
			@Override
			public int[] getBytes() {
				return new int[6];
			}
		});
		final Payload toSet = new Payload() {
			@Override public int[] getBytes() {
				return new int[5];
			}
		};
		for(int i = 0; i < nondet(); i++) {
			toReturn.setPayload(toSet);
		}
		return toReturn;
	}
}
