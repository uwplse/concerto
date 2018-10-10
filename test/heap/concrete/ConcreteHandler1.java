package heap.concrete;

public class ConcreteHandler1 implements Handler {
	@Override public void handle(final Main.Container c) {
		Main.Containee containee = new Main.Containee();
		containee.setField(11);
		c.setContainee(containee);
	}
}
