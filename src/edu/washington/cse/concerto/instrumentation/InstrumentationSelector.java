package edu.washington.cse.concerto.instrumentation;

import edu.washington.cse.concerto.instrumentation.actions.AssignmentAction;
import edu.washington.cse.concerto.instrumentation.actions.FieldReadAction;
import edu.washington.cse.concerto.instrumentation.actions.FieldWriteAction;
import edu.washington.cse.concerto.instrumentation.actions.MethodCallAction;

public interface InstrumentationSelector<AVal, AHeap, AState> {
	MethodCallSelector<ActionHandler<MethodCallAction<AVal, AHeap, AState>>> methodCall();
	FieldWriteSelector<ActionHandler<FieldWriteAction<AVal, AHeap, AState>>> fieldWrite();
	FieldReadSelector<ActionHandler<FieldReadAction<AVal, AHeap, AState>>> fieldRead();
	AssignmentSelector<ActionHandler<AssignmentAction<AVal, AHeap, AState>>> assignment();
	
	MethodDisjunctionSelector<AVal, AHeap, AState> methodCases();
	AssignmentDisjunctionSelector<AVal, AHeap, AState> assignmentCases();
	FieldWriteDisjunctionSelector<AVal, AHeap, AState> fieldWriteCases();
	FieldReadDisjunctionSelector<AVal, AHeap, AState> fieldReadCases();
}
