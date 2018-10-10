package meta.framework.db;

public class ValidatingDatabase implements ORM {
	private ORM wrapped;

	@Override public int readData(final int k) {
		return wrapped.readData(validate(k));
	}

	@Override public void setData(final int k, final int v) {
		wrapped.setData(validate(k), v);
	}
	
	public void setWrappedProvider(final ORM o) {
		this.wrapped = o;
	}

	private int validate(final int k) {
		return sanitize(k);
	}

	private int sanitize(final int k) {
		return k;
	}
}
