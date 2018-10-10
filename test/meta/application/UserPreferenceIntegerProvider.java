package meta.application;

import meta.framework.db.ORM;

public class UserPreferenceIntegerProvider implements IntegerProvider {
	private ORM databaseOrm;

	@Override public int getInteger(final int key) {
		return this.databaseOrm.readData(key);
	}

	public void setDatabaseOrm(final ORM o) {
		this.databaseOrm = o;
	}
}
