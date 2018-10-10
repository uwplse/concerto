package meta.framework;

import meta.framework.filter.Filter;

public class GlobFilter implements FilterAndTarget {
	private final Filter f;

	public GlobFilter(final Filter f) {
		this.f = f;
	}

	@Override public boolean matches(final int target) {
		return true;
	}

	@Override public Filter getFilter() {
		return this.f;
	}
}
