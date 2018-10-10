package edu.washington.cse.concerto.interpreter.ai.instantiation.pta;

import edu.washington.cse.concerto.interpreter.ai.injection.ValueLatticeHolder;
import fj.F;
import fj.Ord;
import fj.data.Option;
import fj.data.TreeMap;

public class AbstractObject {
	public final static ValueLatticeHolder monadHolder = new ValueLatticeHolder();
	public final fj.data.TreeMap<String, Object> fields;
	public static final AbstractObject empty = new AbstractObject(TreeMap.empty(Ord.stringOrd));
	public AbstractObject(final fj.data.TreeMap<String, Object> fields) {
		this.fields = fields;
	}
	
	public AbstractObject put(final String field, final Object value) {
		return new AbstractObject(fields.update(field, new F<Object, Object>() {
			@Override
			public Object f(final Object a) {
				return monadHolder.valueLattice.join(a, value);
			}
		}, value));
	}
	
	public Option<Object> get(final String f) {
		return fields.get(f);
	}
	
	@Override
	public String toString() {
		return fields.map(new F<Object, String>() {
			@Override
			public String f(final Object a) {
				return a.toString();
			}
		}).toString();
	}
}
