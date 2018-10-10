package meta.application;

import meta.framework.Action;
import meta.framework.Dispatcher;
import meta.framework.FrameworkMain.ResultImpl;
import meta.framework.Request;
import meta.framework.db.ORM;
import meta.framework.response.Payload;
import meta.framework.response.Result;

public class ComparisonAction implements Action {
	private IntegerProvider comparandTarget;

	@Override
		public Result doAction(final int r, final Dispatcher d, final Request req) {
			final ORM statistic = d.getObjectGraph().getUnchecked(0);
			statistic.setData(req.getRequestData(0), statistic.readData(req.getRequestData(0)) + 1);
			final int numResults = req.getParameterCount();
			final int[] arr = new int[numResults];
			for(int i = 0; i < arr.length; i++) {
				arr[i] = req.getRequestData(i) > comparandTarget.getInteger(0) ? 1 : 0;
			}
			@SuppressWarnings("unused")
			int sum = 0;
			for(int j = 0; j < arr.length; j++) {
				sum += arr[j];
			}
			final ResultImpl toReturn = new ResultImpl();
			final int finalSum = sum;
			toReturn.setPayload(new Payload() {
				@Override
				public int[] getBytes() {
					return new int[]{
						finalSum
					};
				}
			});
			return toReturn;
		}

		public void setComparandTarget(final IntegerProvider p) {
			this.comparandTarget = p;
		}
}
