package edu.washington.cse.concerto.interpreter.ai;

public interface StateUpdater<AS> {
	public static class IdentityUpdater {
		public static <T> StateUpdater<T> v() {
			return new StateUpdater<T>() {
				@Override
				public T updateState(final T state, final Object value) {
					return state;
				}
			};
		}
	}
	public AS updateState(AS state, Object value);
}
