package meta.framework.response;

public class NotFoundPayload implements Payload {
	@Override
	public int[] getBytes() {
		return new int[]{ 4, 0, 4};
	}

}
