package edu.washington.cse.concerto.interpreter.meta;

import soot.ArrayType;
import soot.PrimType;
import soot.RefType;
import soot.Type;

public abstract class TypeOracle {
	public static enum TypeOwner {
		FRAMEWORK,
		APPLICATION,
		EITHER,
		IGNORE
	}
	public abstract TypeOwner classifyType(String className);
	public TypeOwner classifyType(Type t) {
		if(t instanceof PrimType) {
			return TypeOwner.EITHER;
		} else if(t instanceof RefType) {
			return classifyType(((RefType) t).getClassName());
		} else if(t instanceof ArrayType) {
			return classifyType(((ArrayType) t).baseType);
		} else {
			throw new IllegalArgumentException();
		}
	}
}
