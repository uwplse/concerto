package meta.framework;

public interface Request {
	int getTarget();
	ReqAttributes getAttributes();
	Request withAttribute(int k, Object v);
	Request withAttribute(int k, int v);
	Request withAttributes(ReqAttributes attr);
	Request withRequestData(int[] requestData);
	int getRequestData(int k);
	int getParameterCount();
}
