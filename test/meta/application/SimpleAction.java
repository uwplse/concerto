package meta.application;

import meta.framework.Action;
import meta.framework.Dispatcher;
import meta.framework.FrameworkMain.ResultImpl;
import meta.framework.Request;
import meta.framework.response.Payload;
import meta.framework.response.Result;

public class SimpleAction implements Action {
	public class PayloadImpl1 implements Payload {
		@Override
		public int[] getBytes() {
			return new int[3];
		}
	}
	
	@Override
	public Result doAction(final int r, final Dispatcher d, final Request req) {
		final int x;
		final int f = d.readNondet();
		if(f == 0) {
			x = 3;
		} else {
			x = 4;
		}
		d.printInt(x + 4);
		final Result toReturn = new ResultImpl();
		toReturn.setPayload(new PayloadImpl1());
		return toReturn;
	}
}
