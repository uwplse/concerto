package meta.framework.filter;

import meta.framework.Dispatcher;
import meta.framework.FrameworkMain;
import meta.framework.Request;
import meta.framework.interpreter.LispInterpreter;
import meta.framework.interpreter.ast.Parser;
import meta.framework.response.InternalServerErrorPayload;
import meta.framework.response.NotFoundPayload;
import meta.framework.response.Payload;
import meta.framework.response.ResponseStream;
import meta.framework.response.Result;

public class InterpreterFilter implements Filter {
	private LispInterpreter interpreter;

	@Override public Result filter(final Dispatcher disp, final Request req, final FilterChain chain) {
		final ResponseHolder h = new ResponseHolder();
		final int i = this.interpreter.interpret(disp, req, h);
		if(i == 0) {
			return chain.next(disp, req);
		} else if(i == -1) {
			final FrameworkMain.ResultImpl res = new FrameworkMain.ResultImpl();
			res.setPayload(new NotFoundPayload());
			return res;
		} else if(i == 1) {
			return h.response;
		} else {
			final FrameworkMain.ResultImpl res = new FrameworkMain.ResultImpl();
			res.setPayload(new InternalServerErrorPayload());
			return res;
		}
	}

	@Override public void init(final int k, final int[] vals) {
		if(k == 0) {
			final Parser p = new Parser();
			final LispInterpreter ast  = p.parseAst(vals);
			this.interpreter = ast;
		}
	}
	private static class ResponseHolder implements ResponseStream {

		public ResponseHolder() {
			this.response = null;
		}

		public FrameworkMain.ResultImpl response;

		@Override public void respondWith(final int[] payload) {
			this.response = new FrameworkMain.ResultImpl();
			this.response.setPayload(new Payload() {
				@Override public int[] getBytes() {
					return payload;
				}
			});
		}
	}
}
