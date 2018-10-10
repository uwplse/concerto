package edu.washington.cse.concerto.interpreter.ai;

import fj.data.Option;
import soot.Local;
import soot.RefLikeType;
import soot.Unit;

public interface ReflectiveOperationContext {
	public Unit parentUnit();
	public Option<Local> targetLocal();
	public Option<RefLikeType> castHint();
}
