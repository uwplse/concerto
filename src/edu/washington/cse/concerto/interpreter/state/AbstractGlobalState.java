package edu.washington.cse.concerto.interpreter.state;

import edu.washington.cse.concerto.interpreter.value.IValue;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public abstract class AbstractGlobalState implements GlobalState {
	private final ArrayList<Integer> detStream;

	protected AbstractGlobalState(final String detFile) {
		this.detStream = parseInputFile(detFile);
	}

	protected ArrayList<Integer> parseInputFile(final String detFile) {
		try(Scanner source = new Scanner(new File(detFile))) {
			final ArrayList<Integer> detStream = new ArrayList<>();
			while(source.hasNextInt()) {
				detStream.add(source.nextInt());
			}
			return detStream;
		} catch (final FileNotFoundException e) {
			return null;
		}
	}

	@Override
	public IValue readDeterministic(final int value) {
		final ArrayList<Integer> detStream = this.detStream;
		return readStreamAt(value, detStream);
	}

	protected IValue readStreamAt(final int value, final ArrayList<Integer> detStream) {
		if(value < detStream.size()) {
			return IValue.lift(detStream.get(value));
		} else {
			return IValue.lift(0);
		}
	}
}