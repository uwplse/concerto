package meta.framework;

import meta.framework.response.NotFoundPayload;
import meta.framework.response.Payload;
import meta.framework.response.Result;

public class NotFoundResult implements Result {
	@Override public void setPayload(final Payload x) { }

	@Override public Payload getPayload() {
		return new NotFoundPayload();
	}
}
