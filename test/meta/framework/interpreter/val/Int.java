package meta.framework.interpreter.val;

public class Int implements Val {
	private final int value;

	public Int(final int value) {
		this.value = value;
	}

	public int toInt() {
		return this.value;
	}

	@Override public int getType() {
		return 2;
	}
}
