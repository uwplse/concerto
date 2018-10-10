package heap.concrete;

public class ConcreteHandler2 implements Handler {
	@Override public void handle(final Main.Container c) {
		ResultProvider rp = c.getResultProvider();
		int result = rp.provideResult();
		Main.Containee cont = new Main.Containee();
		cont.setField(result);
		c.setContainee(cont);
	}
}
