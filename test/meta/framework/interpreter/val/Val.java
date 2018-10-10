package meta.framework.interpreter.val;

public interface Val {
	// 1 - closure
	// 2 - int
	// 3 - method type (secretly an int)
	// 4 - embedded object
	int getType();
}
