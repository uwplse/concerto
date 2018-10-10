package meta.framework.interpreter;

import meta.framework.interpreter.val.Val;

import static intr.Intrinsics.fail;

public class LEnvironment {
	public Val[] bindings;

	public LEnvironment(final Val[] bindings) {
		this.bindings = new Val[bindings.length];
		for(int i = 0; i < bindings.length; i++) {
			this.bindings[i] = bindings[i];
		}
	}

	public LEnvironment(int sz) {
		this.bindings = new Val[sz];
	}

	public Val get(int i) {
		if(i >= 0 && i < bindings.length) {
			return bindings[i];
		} else {
			fail("Out of bounds");
			return null;
		}
	}

	public void with(int i, Val x) {
		if(i >= 0 && i < bindings.length) {
			this.bindings[i] = x;
		}
	}

	public LEnvironment copy() {
		return new LEnvironment(this.bindings);
	}

}
