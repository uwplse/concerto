package meta.framework.interpreter.ast;

import meta.framework.interpreter.Ast;
import meta.framework.interpreter.CmpNode;
import meta.framework.interpreter.LispInterpreter;
import meta.framework.interpreter.ast.cmp.EqNode;
import meta.framework.interpreter.ast.cmp.GeNode;
import meta.framework.interpreter.ast.cmp.GtNode;
import meta.framework.interpreter.ast.cmp.LeNode;
import meta.framework.interpreter.ast.cmp.LtNode;
import meta.framework.interpreter.ast.cmp.NeNode;

import static intr.Intrinsics.*;

public class Parser {
	public LispInterpreter parseAst() {
		return parseAst(new InputStream() {
			@Override public int next() {
				return read();
			}
		});
	}

	private interface InputStream {
		int next();
	}

	public LispInterpreter parseAst(final int[] stream) {
		return parseAst(new InputStream() {
			@Override public int next() {
				if(ptr < stream.length) {
					return stream[ptr++];
				} else {
					return -1;
				}
			}

			int ptr = 0;
		});
	}

	private LispInterpreter parseAst(final InputStream inputStream) {
		final int constPoolOffset = inputStream.next();
		final Ast root = parseAstInternal(constPoolOffset, inputStream);
		final int varTableSize = inputStream.next();
		return new LispInterpreter(varTableSize, root);
	}

	private Ast parseAstInternal(final int constPoolOffset, final InputStream inputStream) {
		final int code = inputStream.next();
		if(code == 0) {
			return new NullNode();
		} else if(code == 1) {
			final Ast lop = parseAstInternal(constPoolOffset, inputStream);
			final int cmpCode = inputStream.next();
			final CmpNode n;
			if(cmpCode == 0) {
				n = new LtNode();
			} else if(cmpCode == 1) {
				n = new LeNode();
			} else if(cmpCode == 2) {
				n = new EqNode();
			} else if(cmpCode == 3) {
				n = new NeNode();
			} else if(cmpCode == 4) {
				n = new GeNode();
			} else if(cmpCode == 5 ) {
				n = new GtNode();
			} else {
				fail("Unrecognized comparison type");
				n = null;
			}
			final Ast rop = parseAstInternal(constPoolOffset, inputStream);
			final Ast trueBranch = parseAstInternal(constPoolOffset, inputStream);
			final Ast falseBranch = parseAstInternal(constPoolOffset, inputStream);
			return new IfNode(lop, n, rop, trueBranch, falseBranch);
		} else if(code == 2) {
			return new VarNode(inputStream.next());
		} else if(code == 3) {
			return new ApplyNode(parseAstInternal(constPoolOffset, inputStream), parseAstInternal(constPoolOffset, inputStream));
		} else if(code == 4) {
			return new LambdaNode(inputStream.next(), parseAstInternal(constPoolOffset, inputStream));
		} else if(code == 5) {
			final int type = inputStream.next();
			final Ast recv = parseAstInternal(constPoolOffset, inputStream);
			final Ast method = parseAstInternal(constPoolOffset, inputStream);
			final Ast arg = parseAstInternal(constPoolOffset, inputStream);
			if(type == 0) {
				return new CallIntNode(recv, method, arg);
			} else if(type == 1) {
				return new CallObjNode(recv, method, arg);
			} else {
				fail("bad call type");
				return null;
			}
		} else if(code == 6) {
			return new IntNode(inputStream.next());
		} else if(code == 7) {
			final int numBind = inputStream.next();
			final int[] bindTarget = new int[numBind];
			final Ast[] bindArg = new Ast[numBind];
			for(int i = 0; i < numBind; i++) {
				bindTarget[i] = inputStream.next();
				bindArg[i] = parseAstInternal(constPoolOffset, inputStream);
			}
			final Ast body = parseAstInternal(constPoolOffset, inputStream);
			return new LetNode(bindTarget, bindArg, body);
		} else if(code == 8) {
			return new StringPunNode(inputStream.next() + constPoolOffset);
		} else if(code == 9) {
			final int numItems = inputStream.next();
			final Ast[] items = new Ast[numItems];
			for(int i = 0; i < numItems; i++) {
				items[i] = parseAstInternal(constPoolOffset, inputStream);
			}
			return new IntArrayNode(items);
		} else if(code == 10) {
			final int type = inputStream.next();
			final Ast recv = parseAstInternal(constPoolOffset, inputStream);
			final Ast method = parseAstInternal(constPoolOffset, inputStream);
			if(type == 0) {
				return new CallIntNullNode(recv, method);
			} else if(type == 1) {
				return new CallObjNullNode(recv, method);
			} else {
				fail("bad call type");
				return null;
			}
		} else {
			fail("Bad parse");
			return null;
		}
	}
}
