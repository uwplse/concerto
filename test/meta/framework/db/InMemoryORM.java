package meta.framework.db;

import static intr.Intrinsics.fail;

public class InMemoryORM implements ORM {
	int[] userPreferences = new int[0];
	@Override public int readData(final int k) {
		if(k < 0) {
			fail("bad index");
		}
		if(userPreferences.length <= k) {
			final int newLen = k + 1;
			final int[] newPref = new int[newLen];
			for(int i = 0; i < userPreferences.length; i++) {
				newPref[i] = userPreferences[i];
			}
			newPref[k] = 0;
			this.userPreferences = newPref;
		}
		return userPreferences[k];
	}

	@Override public void setData(final int k, final int v) {
		if(k < 0) {
			return;
		}
		if(userPreferences.length <= k) {
			final int newLen = k + 1;
			final int[] newPref = new int[newLen];
			for(int i = 0; i < userPreferences.length; i++) {
				newPref[i] = userPreferences[i];
			}
			newPref[k] = 0;
			this.userPreferences = newPref;
		}
	}
}
