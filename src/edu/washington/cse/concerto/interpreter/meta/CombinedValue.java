package edu.washington.cse.concerto.interpreter.meta;

import edu.washington.cse.concerto.interpreter.value.IValue;

public class CombinedValue {
	public final IValue concreteComponent;
	public final Object abstractComponent;

	public CombinedValue(final IValue concrete, final Object abs) {
		this.concreteComponent = concrete;
		this.abstractComponent = abs;
	}
	
	@Override
	public String toString() {
		return "[C: " + this.concreteComponent + " & A: " + this.abstractComponent + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((abstractComponent == null) ? 0 : abstractComponent.hashCode());
		result = prime * result + ((concreteComponent == null) ? 0 : concreteComponent.hashCode());
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
		final CombinedValue other = (CombinedValue) obj;
		if(abstractComponent == null) {
			if(other.abstractComponent != null) {
				return false;
			}
		} else if(!abstractComponent.equals(other.abstractComponent)) {
			return false;
		}
		if(concreteComponent == null) {
			if(other.concreteComponent != null) {
				return false;
			}
		} else if(!concreteComponent.equals(other.concreteComponent)) {
			return false;
		}
		return true;
	}
}
