package meta.framework;

import meta.framework.response.Result;

public interface Dispatcher {
	Result forward(int r, Request req);
	Forwarder dispatch(int r);
	void printInt(int a);


	int readNondet();
	int[] getRegisteredChain();
	void writeDatabase(int value);
	void writeDatabase(Object attributes);
	ObjectGraph getObjectGraph();
}
