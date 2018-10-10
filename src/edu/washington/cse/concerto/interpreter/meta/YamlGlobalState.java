package edu.washington.cse.concerto.interpreter.meta;

import edu.washington.cse.concerto.interpreter.state.GlobalState;
import edu.washington.cse.concerto.interpreter.util.YamlParser;
import edu.washington.cse.concerto.interpreter.value.IValue;

import java.util.List;
import java.util.Map;

public class YamlGlobalState implements GlobalState {
	
	private final List<Integer> detStream;
	private final List<Integer> nondetIterator;

	@SuppressWarnings("unchecked")
	public YamlGlobalState(final String yamlFile) {
		final Map<String, Object> conf = YamlParser.parseYamlConf(yamlFile);
		detStream = ((List<Integer>)conf.get("det-stream"));
		if(conf.get("nondet-stream") == null) {
			nondetIterator = null;
		} else {
			nondetIterator = ((List<Integer>)conf.get("nondet-stream"));
		}
	}

	@Override
	public IValue readDeterministic(final int ptr) {
		if(ptr < detStream.size()) {
			return IValue.lift(detStream.get(ptr));
		} else {
			return IValue.lift(0);
		}
	}

	@Override
	public IValue readNonDeterministic(final int ptr) {
		if(nondetIterator == null || ptr >= nondetIterator.size()) {
			return IValue.nondet();
		} else {
			assert ptr < nondetIterator.size();
			return IValue.lift(nondetIterator.get(ptr));
		}
	}
}
