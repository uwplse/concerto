package meta.framework;

import meta.framework.filter.Filter;

public class FilterAndLiteralTargets implements FilterAndTarget {
	private final Filter filter;
	private final int[] targets;

	public FilterAndLiteralTargets(final Filter filter, final int[] targets) {
		this.filter = filter;
		this.targets = targets;
	}

	@Override
	public boolean matches(final int target) {
		for(int i = 0; i < targets.length; i++) {
			if(target == targets[i]) {
				return true;
			}
		}
		return false;
	}

	@Override public Filter getFilter() {
		return this.filter;
	}
}
