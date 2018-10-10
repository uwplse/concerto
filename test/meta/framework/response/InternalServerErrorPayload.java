package meta.framework.response;

public class InternalServerErrorPayload implements Payload {
	@Override public int[] getBytes() {
		return new int[] { 5, 0, 0 };
	}
}
