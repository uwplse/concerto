package edu.washington.cse.concerto.interpreter.ai.instantiation.optiflow;

import edu.washington.cse.concerto.interpreter.ai.BottomAware;
import edu.washington.cse.concerto.interpreter.ai.MonadicLattice;
import edu.washington.cse.concerto.interpreter.ai.instantiation.pta.AbstractAddress;
import edu.washington.cse.concerto.interpreter.ai.instantiation.pta.JValue;
import edu.washington.cse.concerto.interpreter.meta.Monads;
import fj.F;
import fj.data.Set;

public interface PV extends BottomAware {
	public static MonadicLattice<PV, PV, LocalMap> lattice = new MonadicLattice<PV, PV, LocalMap>() {

		@Override
		public PV widen(final PV prev, final PV next) {
			final Class<? extends PV> prevKlass = prev.getClass();
			final Class<? extends PV> nextKlass = next.getClass();
			if(prevKlass == nextKlass) {
				if(prev instanceof JValue) {
					return JValue.lattice.widen((JValue)prev, (JValue) next);
				} else if(prev instanceof PathTree) {
					return PathTree.lattice.widen((PathTree)prev, (PathTree) next);
				} else {
					assert prev instanceof Product;
					return Product.lattice.widen((Product)prev, (Product) next);
				}
			}
			if(prevKlass != Product.class && nextKlass != Product.class) {
				if(prev instanceof JValue) {
					assert next instanceof PathTree;
					return new Product((JValue)prev, (PathTree) next);
				} else {
					assert next instanceof JValue;
					assert prev instanceof PathTree;
					return new Product((JValue)next, (PathTree) prev);
				}
			}
			if(prevKlass == Product.class) {
				final Product prevProd = (Product) prev;
				if(next instanceof PathTree) {
					return new Product(
						prevProd.p1,
						PathTree.lattice.widen(prevProd.p2, (PathTree) next)
					);
				} else {
					return new Product(
						JValue.lattice.widen(prevProd.p1, (JValue) next),
						prevProd.p2
					);
				}
			} else {
				final Product nextProd = (Product) next;
				if(prev instanceof PathTree) {
					return new Product(
						nextProd.p1,
						PathTree.lattice.widen((PathTree) prev, nextProd.p2)
					);
				} else {
					return new Product(
						JValue.lattice.widen((JValue) prev, nextProd.p1),
						nextProd.p2
					);
				}
			}
		}

		@Override
		public PV join(final PV first, final PV second) {
			final Class<? extends PV> klass1 = first.getClass();
			final Class<? extends PV> klass2 = second.getClass();
			if(klass1 == klass2) {
				if(first instanceof JValue) {
					return JValue.lattice.join((JValue)first, (JValue) second);
				} else if(first instanceof PathTree) {
					return PathTree.lattice.join((PathTree)first, (PathTree) second);
				} else {
					assert first instanceof Product;
					return Product.lattice.join((Product)first, (Product) second);
				}
			}
			if(klass1 != Product.class && klass2 != Product.class) {
				if(first instanceof JValue) {
					assert second instanceof PathTree;
					return new Product((JValue)first, (PathTree) second);
				} else {
					assert second instanceof JValue;
					assert first instanceof PathTree;
					return new Product((JValue)second, (PathTree) first);
				}
			}
			if(klass1 == Product.class) {
				return joinWithProduct((Product) first, second);
			} else {
				return joinWithProduct((Product) second, first);
			}
		}

		private PV joinWithProduct(final Product firstP, final PV second) {
			if(second instanceof JValue) {
				return new Product(
					JValue.lattice.join(firstP.p1, (JValue) second),
					firstP.p2
				);
			} else {
				assert second instanceof PathTree;
				return new Product(
					firstP.p1,
					PathTree.lattice.join(firstP.p2, (PathTree) second)
				);
			}
		}

		@Override
		public boolean lessEqual(final PV first, final PV second) {
			if(!(first instanceof Product) && second instanceof Product) {
				final Product product = (Product) second;
				if(first instanceof JValue) {
					return JValue.lattice.lessEqual((JValue) first, product.p1);
				} else {
					return PathTree.lattice.lessEqual((PathTree) first, product.p2);
				}
			}
			if(first.getClass() != second.getClass()) {
				return false;
			}
			if(first instanceof JValue) {
				return JValue.lattice.lessEqual((JValue)first, (JValue) second);
			} else if(second instanceof PathTree) {
				return PathTree.lattice.lessEqual((PathTree)first, (PathTree) second);
			} else {
				final Product p1 = (Product) first;
				final Product p2 = (Product) second;
				return Product.lattice.lessEqual(p1, p2);
			}
		}

		@Override
		public void inject(final Monads<PV, LocalMap> monads) { }
	};
	public static PV bottom = JValue.bottom;

	PV mapAddress(final F<Set<AbstractAddress>, Set<AbstractAddress>> mapper);
}
