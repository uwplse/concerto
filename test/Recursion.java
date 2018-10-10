import static intr.Intrinsics.*;

public class Recursion {
	public void basicRecursion() {
		assertEqual(recursive(0), nondet());
		assertEqual(complexRecursion(4), 4);
		assertEqual(complexRecursion(10), 10);
	}
	
	public int recursive(final int i) {
		if(nondet() == 1) {
			return recursive(i + 1) + 1;
		} else {
			return 4;
		}
	}
	
	public int complexRecursion(final int i) {
		if(nondet() == 1) {
			return cycle(i - 1);
		} else {
			return i;
		}
	}

	private int cycle(final int i) {
		return complexRecursion(i + 1);
	}
	
	public void heapRecursion() {
		final Node head = new Node();
		Node it = head;
		for(int i = 0; i < 2; i++) {
			it.next = new Node();
			it = it.next; 
		}
		final Node last = head.next.next;
		final Node middle = head.next;
		final Container c = new Container();
		final Node r = maybeTruncate(head, c);
		assertEqual(r.d, lift(0,3));
		assertEqual(r.f, nondet());
		assertEqual(c.i, nondet());
		assertEqual(r.next.d, lift(0,3));
		assertEqual(r.next.f, nondet());
		assertEqual(last.d, lift(0, 3));
		assertEqual(last.f, nondet());
		assertEqual(middle.d, lift(0, 3));
		assertEqual(middle.f, nondet());
	}
	
	private Node maybeTruncate(final Node o, final Container c) {
		if(o == null) {
			return null;
		}
		o.d = 3;
		Node g;
		if(nondet() == 3) {
			g = maybeTruncate(o.next, c);
		} else {
			g = new Node();
		}
		o.f = c.i++;
		o.next = g;
		return o;
	}

	public static class Container {
		int i = 0;
	}
	
	public static class Node {
		public Node next = null;
		public int f = 0;
		public int d = 0;
	}
	
	public void expandedCycle() {
		assertEqual(firstCycle(10), lift(3,6));
	}

	private int firstCycle(final int i) {
		if(nondet() == 0) {
			return firstCycle(i - 1);
		} else if(i == 3) {
			return secondCycle(i);
		} else {
			return 3;
		}
	}

	private int secondCycle(final int i) {
		if(i == 2) {
			return secondCycle(i+1);
		} else {
			return 6;
		}
	}
	
	public void withLoop() {
		Node n = null;
		for(int i = 0; i < 3; i++) {
			final Node it = new Node();
			it.next = n;
			it.d = i;
			n = it;
		}
		assertEqual(recursionWithLoop(n, 1), lift(0,4));
	}

	private int recursionWithLoop(final Node n, final int i) {
		Node it = n;
		int k = 0;
		while(it != null && nondet() != 0) {
			k = recursionWithLoop(it, 3);
			it = it.next;
		}
		if(it.d == 1) {
			return k;	
		} else {
			return 4;
		}
	}
	
	public void withLoopCondition() {
		Node n = null;
		for(int i = 0; i < 3; i++) {
			final Node it = new Node();
			it.next = n;
			n = it;
		}
		assertEqual(recursionInCondition(n, 1), lift(0,4));
	}

	private int recursionInCondition(final Node n, final int i) {
		Node it = n;
		while(nondet() != 0 || recursionInCondition(it, 0) != 0) {
			it = it.next;
		}
		if(it.d == 3) {
			return 0;
		} else {
			return 4;
		}
	}
}
