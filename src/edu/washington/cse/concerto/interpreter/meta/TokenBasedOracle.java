package edu.washington.cse.concerto.interpreter.meta;

public class TokenBasedOracle extends TypeOracle {
	private final String token;

	public TokenBasedOracle(final String token) {
		this.token = token;
	}

	@Override
	public TypeOwner classifyType(final String className) {
		if(className.contains(this.token)) {
			return TypeOwner.FRAMEWORK;
		} else {
			return TypeOwner.APPLICATION;
		}
	}
	
}
