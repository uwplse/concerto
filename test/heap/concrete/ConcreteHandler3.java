package heap.concrete;

public class ConcreteHandler3 implements Handler {
	@Override public void handle(final Main.Container c) {
		Main.Containee cont = new Main.Containee();
		cont.setField(c.getResultProvider().provideOtherResult());
		c.setContainee(cont);
	}
}
