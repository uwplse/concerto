package meta.framework.interpreter.ast;

import meta.framework.interpreter.val.Val;

public class Embedded implements Val {

	private final Object wrapped;

	public Embedded(final Object wrapped) {
		this.wrapped = wrapped;
	}

	@Override public int getType() {
		return 4;
	}

	public Object getWrapped() {
		return this.wrapped;
	}
}
