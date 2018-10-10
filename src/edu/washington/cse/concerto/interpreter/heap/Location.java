package edu.washington.cse.concerto.interpreter.heap;

import soot.Type;

public class Location {
	public final int contextNumber;
	public final Type type;
	public final int id;
	public final boolean isSummary;

	public Location(final int contextNumber, final Type t, final int id, final boolean isSummary) {
		this.contextNumber = contextNumber;
		this.type = t;
		this.id = id;
		this.isSummary = isSummary;
	}
	
	@Override
	public String toString() {
		return (isSummary ? "\u0142" : "l") + contextNumber + ":" + id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + contextNumber;
		result = prime * result + id;
		result = prime * result + (isSummary ? 1231 : 1237);
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		final Location other = (Location) obj;
		if(contextNumber != other.contextNumber) {
			return false;
		}
		if(id != other.id) {
			return false;
		}
		if(isSummary != other.isSummary) {
			return false;
		}
		if(type == null) {
			if(other.type != null) {
				return false;
			}
		} else if(!type.equals(other.type)) {
			return false;
		}
		return true;
	}
}
