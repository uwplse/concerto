package edu.washington.cse.concerto.interpreter.meta;

interface PermissionToken {
	static class TokenManager {
		public static final PermissionToken tok = new PermissionToken() { }; 
	}
}
