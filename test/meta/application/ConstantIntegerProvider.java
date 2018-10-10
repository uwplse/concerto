package meta.application;

public class ConstantIntegerProvider implements IntegerProvider {
	private int c;

	@Override public int getInteger(final int key) {
		return c;
	}

	public void setConstant(final int c) {
		this.c = c;
	}
}
