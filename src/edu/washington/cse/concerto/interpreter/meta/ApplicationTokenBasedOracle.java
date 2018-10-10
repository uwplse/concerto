package edu.washington.cse.concerto.interpreter.meta;

public class ApplicationTokenBasedOracle extends TypeOracle {

	private final String token;

	public ApplicationTokenBasedOracle(final String token) {
		this.token = token;
	}

	@Override
	public TypeOwner classifyType(final String className) {
		if(className.contains(token)) {
			return TypeOwner.APPLICATION;
		} else {
			return TypeOwner.FRAMEWORK;
		}
	}

}
