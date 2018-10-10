package edu.washington.cse.concerto.interpreter.state;

import edu.washington.cse.concerto.interpreter.value.Copyable;
import edu.washington.cse.concerto.interpreter.value.IValue;

import java.util.Objects;

public class StateReader implements Copyable<StateReader> {
	private int detOffset;
	private int nondetOffset;

	public StateReader(final int offset, final int nondetOffset) {
		this.detOffset = offset;
		this.nondetOffset = nondetOffset;
	}

	public StateReader fork() {
		return new StateReader(detOffset, nondetOffset);

	}
	public IValue readDeterministic(final GlobalState state) {
		if(detOffset == -1) {
			return IValue.nondet();
		} else {
			return state.readDeterministic(detOffset++);
		}
	}

	public IValue readNonDeterministic(final GlobalState state) {
		if(nondetOffset == -1) {
			return IValue.nondet();
		} else {
			return state.readNonDeterministic(nondetOffset++);
		}
	}
	public boolean lessEqual(final StateReader other) {
		return (other.nondetOffset == this.nondetOffset || other.nondetOffset == -1) &&
				(other.detOffset == this.detOffset || other.detOffset == -1);
	}

	@Override public boolean equals(final Object o) {
		if(this == o)
			return true;
		if(o == null || getClass() != o.getClass())
			return false;
		final StateReader that = (StateReader) o;
		return detOffset == that.detOffset && nondetOffset == that.nondetOffset;
	}

	@Override public int hashCode() {

		return Objects.hash(detOffset, nondetOffset);
	}

	public StateReader joinWith(final StateReader other) {
		if(this.equals(other)) {
			return this;
		}
		return new StateReader(detOffset == other.detOffset ? other.detOffset : -1, nondetOffset == other.nondetOffset ? other.nondetOffset : -1);
	}

	public StateReader widenWith(final StateReader other) {
		return this.joinWith(other);
	}

	@Override public StateReader copy() {
		return fork();
	}
}
