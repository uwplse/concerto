package meta.framework.interpreter.val;

public class MethodKey implements Val {
	private final int key;

	public MethodKey(final int value) {
		this.key = value;
	}

	public int toKey() {
		return this.key;
	}

	@Override public int getType() {
		return 3;
	}
}
