package edu.washington.cse.concerto.interpreter.meta;

import java.lang.reflect.Type;

public class PackageBasedOracle extends TypeOracle {
	private final String concretePkg;
	private final String abstractPkg;

	public PackageBasedOracle(final String concretePkg, final String abstractPkg) {
		this.concretePkg = concretePkg;
		this.abstractPkg = abstractPkg;
	}

	@Override
	public TypeOwner classifyType(final String className) {
		if(className.startsWith(abstractPkg)) {
			return TypeOwner.APPLICATION;
		} else if(className.startsWith(concretePkg) ){
			return TypeOwner.FRAMEWORK;
		} else {
			return TypeOwner.IGNORE;
		}
	}
}
