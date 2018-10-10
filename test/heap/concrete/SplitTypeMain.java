package heap.concrete;

import heap.abs.HandlerAllocAndInvoke;
import heap.abs.HandlerCaller;

import javax.annotation.Nullable;

import static intr.Intrinsics.*;

public class SplitTypeMain {
	public void directCall() {
		Handler h;
		if(nondet() == 1) {
			h = allocateType("heap.concrete.ConcreteHandler1");
		} else {
			h = allocateType("heap.concrete.ConcreteHandler2");
		}
		HandlerCaller hc = allocateType("heap.abs.HandlerCaller");
		assertEqual(hc.callHandlerDirect(h).f, lift(11, 66));
	}

	public void indirectCall() {
		Handler h;
		if(nondet() == 1) {
			h = allocateType("heap.concrete.ConcreteHandler1");
		} else {
			h = allocateType("heap.concrete.ConcreteHandler2");
		}
		HandlerCaller hc = allocateType("heap.abs.HandlerCaller");
		assertEqual(hc.callHandlerInvoke(h).f, lift(11, 66));
	}

	public void allPruneDirect() {
		Handler h;
		if(nondet() == 1) {
			h = allocateType("heap.concrete.ConcreteHandler2");
		} else {
			h = allocateType("heap.concrete.ConcreteHandler3");
		}
		HandlerCaller hc = allocateType("heap.abs.HandlerCaller");
		assertEqual(hc.callHandlerDirect(h).f, lift(68, 66));
	}

	public void allPruneInvoke() {
		Handler h;
		if(nondet() == 1) {
			h = allocateType("heap.concrete.ConcreteHandler2");
		} else {
			h = allocateType("heap.concrete.ConcreteHandler3");
		}
		HandlerCaller hc = allocateType("heap.abs.HandlerCaller");
		assertEqual(hc.callHandlerInvoke(h).f, lift(68, 66));
	}

	public void allocConstructorPrunes() {
		HandlerAllocAndInvoke haai = allocateType("heap.abs.HandlerAllocAndInvoke");
		assertEqual(haai.doCall().getContainee().f, 66);

		assertEqual(haai.secondCall().getContainee().f, 66);
	}
}
