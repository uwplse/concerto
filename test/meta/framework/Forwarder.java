package meta.framework;

import meta.framework.response.Result;

public interface Forwarder {
	Result forward(Request req);
}
