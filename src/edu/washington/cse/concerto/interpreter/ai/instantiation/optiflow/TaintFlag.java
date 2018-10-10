package edu.washington.cse.concerto.interpreter.ai.instantiation.optiflow;

import edu.washington.cse.concerto.interpreter.lattice.Lattice;


public enum TaintFlag {
	NoTaint {
		@Override
		public TaintFlag join(final TaintFlag root) {
			return root;
		}
	},
	Taint {
		@Override
		public TaintFlag join(final TaintFlag root) {
			return this;
		}
	};
	
	public static final Lattice<TaintFlag> lattice = new Lattice<TaintFlag>() {
		@Override
		public TaintFlag widen(final TaintFlag prev, final TaintFlag next) {
			return join(prev, next);
		}

		@Override
		public TaintFlag join(final TaintFlag first, final TaintFlag second) {
			return first.join(second);
		}

		@Override
		public boolean lessEqual(final TaintFlag first, final TaintFlag second) {
			return first == NoTaint || second == Taint;
		}
	};

	public TaintFlag join(final TaintFlag root) {
		throw new RuntimeException();
	}
}
