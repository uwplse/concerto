package edu.washington.cse.concerto.interpreter.heap;

public class HeapAccessResult {
	public final HeapFaultStatus npe;
	public final HeapFaultStatus oob;
	public HeapAccessResult(final HeapFaultStatus npe, final HeapFaultStatus oob) {
		this.oob = oob;
		this.npe = npe;
	}
}
