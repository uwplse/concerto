package edu.washington.cse.concerto.interpreter.heap;

public enum HeapFaultStatus {
	MAY,
	MUST {
		@Override
		public HeapFaultStatus unmark() {
			return MAY;
		}
	},
	MUST_NOT {
		@Override
		public HeapFaultStatus mark() {
			return MAY;
		}
		
		@Override
		public boolean isPossible() {
			return false;
		}
	},
	BOTTOM {
		@Override
		public HeapFaultStatus mark() {
			return MUST;
		}
		@Override
		public HeapFaultStatus unmark() {
			return MUST_NOT;
		}
		
		@Override
		public boolean isPossible() {
			return false;
		}
	};
	
	public HeapFaultStatus mark() {
		return this;
	}
	public HeapFaultStatus unmark() {
		return this;
	}
	
	public boolean isPossible() {
		return true;
	}

	public static HeapFaultStatus join(final HeapFaultStatus a, final HeapFaultStatus b) {
		if(a == BOTTOM) {
			return b;
		} else if(b == BOTTOM) {
			return a;
		} else if(a == MAY || b == MAY) {
			return MAY;
		} else if(b != a) {
			return MAY;
		} else {
			return a;
		}
	}

	public HeapFaultStatus joinWith(final HeapFaultStatus other) {
		return join(this, other);
	}
}
