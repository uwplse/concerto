import static intr.Intrinsics.*;

public class AbstractReceiver {
	private static class LinkedList {
		AbstractionInterface value;
		LinkedList next;
		
		public LinkedList() {
			this.value = null;
			this.next = null;
		}
	}
	
	private static interface AbstractionInterface {
		public void setField(int j);
		public int getField();
	}
	
	private static class Implementation1 implements AbstractionInterface {
		private int f;
		public Implementation1(final int i) {
			this.f = i;
		}
		@Override
		public void setField(final int j) {
			this.f = j;
		}
		@Override
		public int getField() {
			return this.f;
		}
	}
	
	private static class Implementation2 implements AbstractionInterface {
		private int g;
		public Implementation2(final int i) {
			this.g = i;
		}
		@Override
		public void setField(final int j) {
			this.g = j;
		}
		@Override
		public int getField() {
			return this.g;
		}
	}

	public void main() {
		final int i = 0;
		final LinkedList l = new LinkedList();
		final AbstractionInterface other = allocate();
		LinkedList it = l;
		while(i < nondet()) {
			final LinkedList next = new LinkedList();
			next.value = allocate();
			next.next = null;
			it.next = next;
			it = next;
		}
		l.next.next.value.setField(5);
		assertEqual(other.getField(), lift(2,3,4));
		assertEqual(l.next.value.getField(), lift(2,3,4,5));
	}
	
	public AbstractionInterface allocate() {
		if(nondet() == 1) {
			return new Implementation1(2);
		} else if(nondet() == 2) {
			return new Implementation2(3);
		} else {
			return new Implementation1(4);
		}
	}
}
