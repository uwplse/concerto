package meta.framework;

public class ReqAttributes {
	private final Object[] objectFields;
	private final int[] intFields;

	public ReqAttributes() {
		objectFields = new Object[5];
		intFields = new int[5];
	}

	public ReqAttributes(final Object[] objectFields, final int[] intFields) {
		this.objectFields = objectFields;
		this.intFields = intFields;
	}

	public Object getObjectAttribute(final int x) {
		return objectFields[x];
	}

	public int getIntAttribute(final int x) {
		return intFields[x];
	}

	public ReqAttributes withAttribute(final int k, final Object v) {
		if(k < 0 || k >= objectFields.length) {
			return null;
		}
		final Object[] newAttr = new Object[objectFields.length];
		for(int i = 0; i < objectFields.length; i++) {
			newAttr[i] = objectFields[i];
		}
		newAttr[k] = v;
		return new ReqAttributes(newAttr, intFields);
	}

	public ReqAttributes withAttribute(final int k, final int v) {
		if(k < 0 || k >= intFields.length) {
			return null;
		}
		final int[] newAttr = new int[intFields.length];
		for(int i = 0; i < intFields.length; i++) {
			newAttr[i] = intFields[i];
		}
		newAttr[k] = v;
		return new ReqAttributes(objectFields, newAttr);
	}

	public interface Setter {
		ReqAttributes set(Object o);
		ReqAttributes set(int x);
	}

	public Setter getSetter(final int x) {
		return new Setter() {
			@Override public ReqAttributes set(final Object o) {
				return withAttribute(x, o);
			}

			@Override public ReqAttributes set(final int y) {
				return withAttribute(x, y);
			}
		};
	}


}
