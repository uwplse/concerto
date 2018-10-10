package meta.application;

public class NullConstantIntegerProvider implements IntegerProvider {
	@Override public int getInteger(final int key) {
		return 4;
	}
}
