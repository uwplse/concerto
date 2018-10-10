package edu.washington.cse.concerto.interpreter.ai;

public class StandardOutResultStream implements ResultStream {
	@Override public void outputAnalysisResult(final Object result) {
		if(result instanceof PrettyPrintable) {
			System.out.println(((PrettyPrintable)result).prettyPrint());
		} else {
			System.out.println(result);
		}
	}
}
