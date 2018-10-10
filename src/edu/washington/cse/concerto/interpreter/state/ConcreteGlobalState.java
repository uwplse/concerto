package edu.washington.cse.concerto.interpreter.state;

import edu.washington.cse.concerto.interpreter.value.IValue;

import java.io.FileNotFoundException;
import java.util.ArrayList;

public class ConcreteGlobalState extends AbstractGlobalState  {
	private final ArrayList<Integer> nondetStream;

	public ConcreteGlobalState(final String detFile, final String nondetFile) throws FileNotFoundException {
		super(detFile);
		this.nondetStream = this.parseInputFile(nondetFile);
	}
	
	@Override
	public IValue readNonDeterministic(final int det) {
		return readStreamAt(det, this.nondetStream);
	}
}
