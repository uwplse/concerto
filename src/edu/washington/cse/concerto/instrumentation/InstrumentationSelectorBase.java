package edu.washington.cse.concerto.instrumentation;

public interface InstrumentationSelectorBase<MRet, FWRet, FRRet, ARet> {
	MethodCallSelector<MRet> methodCall();
	FieldWriteSelector<FWRet> fieldWrite();
	FieldReadSelector<FRRet> fieldRead();
	AssignmentSelector<ARet> assignment();
}
