package meta.framework;

import meta.framework.filter.Filter;

public interface FilterAndTarget {
	boolean matches(int target);
	Filter getFilter();
}
