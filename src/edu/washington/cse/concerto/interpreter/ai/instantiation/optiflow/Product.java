package edu.washington.cse.concerto.interpreter.ai.instantiation.optiflow;

import edu.washington.cse.concerto.interpreter.ai.instantiation.pta.AbstractAddress;
import edu.washington.cse.concerto.interpreter.ai.instantiation.pta.JValue;
import edu.washington.cse.concerto.interpreter.lattice.Lattice;
import fj.F;
import fj.data.Set;

public class Product implements PV {
	public final JValue p1;
	public final PathTree p2;
	
	public static final Lattice<Product> lattice = new Lattice<Product>() {
		@Override
		public Product widen(final Product prev, final Product next) {
			return new Product(
				JValue.lattice.widen(prev.p1, next.p1),
				PathTree.lattice.widen(prev.p2, next.p2)
			);
		}

		@Override
		public Product join(final Product first, final Product second) {
			return new Product(
				JValue.lattice.join(first.p1, second.p1),
				PathTree.lattice.join(first.p2, second.p2)
			);
		}

		@Override
		public boolean lessEqual(final Product first, final Product second) {
			return JValue.lattice.lessEqual(first.p1, second.p1) && PathTree.lattice.lessEqual(first.p2, second.p2);
		}
	};

	public Product(final JValue p1, final PathTree p2) {
		this.p1 = p1;
		this.p2 = p2;
	}

	@Override
	public boolean isBottom() {
		return p1.isBottom() && p2.isBottom();
	}
	
	@Override
	public String toString() {
		return "(" + p1.toString() + "," + p2.toString() + ")";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((p1 == null) ? 0 : p1.hashCode());
		result = prime * result + ((p2 == null) ? 0 : p2.hashCode());
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
		final Product other = (Product) obj;
		if(p1 == null) {
			if(other.p1 != null) {
				return false;
			}
		} else if(!p1.equals(other.p1)) {
			return false;
		}
		if(p2 == null) {
			if(other.p2 != null) {
				return false;
			}
		} else if(!p2.equals(other.p2)) {
			return false;
		}
		return true;
	}

	@Override public PV mapAddress(final F<Set<AbstractAddress>, Set<AbstractAddress>> mapper) {
		return new Product((JValue) p1.mapAddress(mapper), p2);
	}
}
