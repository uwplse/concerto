import static intr.Intrinsics.*;

public class Loop {
	private static class Node {
		int i = 0;
		Node next = null;
	}
	
	public void main() {
		final Node head = new Node();
		Node it = head;
		int i = 0;
		while(i < 3) {
			it.next = new Node();
			it.i = 3;
			it = it.next;
			i++;
		}
		it = head;
		while(i < nondet()) {
			it.i = 4;
			it = it.next;
		}
		assertEqual(head.next.next.next.i, lift(0, 4));
	}
	
	public void returnBackup() {
		final Node n = loopWithMutation();
		assertEqual(n.i, lift(4,5));
	}
	
	private Node loopWithMutation() {
		final Node toReturn = new Node();
		toReturn.i = 5;
		if(nondet() == 3) {
			return toReturn;
		}
		while(nondet() == 0) {
			toReturn.i = 4;
		}
		return toReturn;
	}
	
	public void nestedLoop() {
		final Node head = new Node();
		Node it = head;
		int i = 0;
		while(i < 3) {
			it.next = new Node();
			it.i = 3;
			it = it.next;
			i++;
		}
		it = head;
		while(i < nondet()) {
			int j = 0;
			while(j < nondet()) {
				it.i++;
				j++;
			}
			it = it.next;
		}
		assertEqual(head.next.next.next.i, nondet());
	}
	
	public void loopConditionMutation() {
		int j = 7;
		int i = 0;
		while(nondet() == 4 || (j++ == 6 && j++ == 7)) {
			j = 3;
			i++;
		}
		assertEqual(j, lift(8, 4));
		assertEqual(i, nondet());
	}
}
