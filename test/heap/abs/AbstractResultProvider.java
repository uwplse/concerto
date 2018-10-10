package heap.abs;

import heap.concrete.ResultProvider;

class AbstractResultProvider implements ResultProvider {
	@Override public int provideResult() {
		return 66;
	}

	@Override public int provideOtherResult() {
		return 68;
	}
}
