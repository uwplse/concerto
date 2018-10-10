package meta.framework;

import intr.Intrinsics;

import static intr.Intrinsics.*;

public class ObjectGraph {
	public Object[] slots;
	public ObjectGraph() {

	}

	public void init() {
		final int numSlots = read();
		slots = new Object[numSlots];
		final int numObjects = read();
		for(int i = 0; i < numObjects; i++) {
			final int outputSlot = read();
			slots[outputSlot] = allocateType(read());
		}
		// Now do dependency injection
		for(int i = 0; i < numObjects; i++) {
			final int targetSlot = read();
			final Object receiver = slots[targetSlot];
			final int numProps = read();
			for(int j = 0; j < numProps; j++) {
				final int type = read();
				final int methodRef = read();
				if(type == 0) {
					final int value = read();
					invokeObj(receiver, Intrinsics.getClass(receiver), methodRef, value);
				} else if(type == 1) {
					final int streamLen = read();
					final int[] stream = new int[streamLen];
					for(int k = 0; k < streamLen; k++) {
						stream[k] = read();
					}
					invokeObj(receiver, Intrinsics.getClass(receiver), methodRef, stream);
				} else if(type == 2) {
					final int slot = read();
					invokeObj(receiver, Intrinsics.getClass(receiver), methodRef, slots[slot]);
				} else {
					fail("bad injection type");
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T getUnchecked(final int slot) {
		return (T) slots[slot];
	}

	@SuppressWarnings("unchecked")
	public <T> T getChecked(final int slot, final int type) {
		final Object o = slots[slot];
		if(Intrinsics.getClass(o) == type) {
			return (T) o;
		} else {
			fail("wrong runtime type");
			return null;
		}
	}
}
