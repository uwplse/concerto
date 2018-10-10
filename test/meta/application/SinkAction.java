package meta.application;

import meta.framework.Action;
import meta.framework.Dispatcher;
import meta.framework.FrameworkMain.ResultImpl;
import meta.framework.Request;
import meta.framework.response.Payload;
import meta.framework.response.Result;

public class SinkAction implements Action {
	public static void sink(final Object o) { }
	@Override
	public Result doAction(final int r, final Dispatcher d, final Request req) {
		d.writeDatabase(req.getAttributes());
		d.writeDatabase(req.getAttributes().getObjectAttribute(2));
		d.writeDatabase(req.getAttributes().getIntAttribute(3));
		final Result ret = new ResultImpl();
		ret.setPayload(new Payload() {
			@Override
			public int[] getBytes() {
				return new int[11];
			}
		});
		return ret;
	}

}
