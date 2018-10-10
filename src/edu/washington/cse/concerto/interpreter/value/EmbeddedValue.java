package edu.washington.cse.concerto.interpreter.value;

import edu.washington.cse.concerto.interpreter.ai.ValueMonad;

public final class EmbeddedValue {
	public final Object value;
	public final ValueMonad<?> monad;
	
	public EmbeddedValue(final Object v, final ValueMonad<?> m) {
		this.value = v;
		this.monad = m;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((monad == null) ? 0 : monad.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if(this == obj) {
			return true;
		}
		if(obj == null) {
			return false;
		}
		if(getClass() != obj.getClass()) {
			return false;
		}
		final EmbeddedValue other = (EmbeddedValue) obj;
		if(other.monad != this.monad) {
			return false;
		}
		return other.monad.lessEqual(this.value, other.value) && other.monad.lessEqual(other.value, this.value);
	}	
}
