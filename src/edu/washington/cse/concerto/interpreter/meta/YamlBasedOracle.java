package edu.washington.cse.concerto.interpreter.meta;

import edu.washington.cse.concerto.interpreter.util.YamlParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class YamlBasedOracle extends TypeOracle {
	
	private final List<Pattern> appPatterns;
	private final List<Pattern> frameworkPatterns;

	@SuppressWarnings("unchecked")
	public YamlBasedOracle(final String confFile) {
		final Map<String, Object> conf = YamlParser.parseYamlConf(confFile);
		final List<String> patts = (List<String>) conf.get("app-types");
		this.appPatterns = compilePatterns(patts);
		this.frameworkPatterns = compilePatterns((List<String>) conf.get("framework-types"));
	}

	protected List<Pattern> compilePatterns(final List<String> patts) {
		final List<Pattern> toReturn = new ArrayList<>();
		for(final String patt : patts) {
			if(!patt.contains("*")) {
				toReturn.add(Pattern.compile(patt, Pattern.LITERAL));
			} else {
				toReturn.add(Pattern.compile(patt.replace("$", "\\$").replace(".", "\\.").replace("*", ".*")));
			}
		}
		return toReturn;
	}

	@Override
	public TypeOwner classifyType(final String className) {
		for(final Pattern patt : appPatterns) {
			if(patt.matcher(className).matches()) {
				return TypeOwner.APPLICATION;
			}
		}
		for(final Pattern patt : frameworkPatterns) {
			if(patt.matcher(className).matches()) {
				return TypeOwner.FRAMEWORK;
			}
		}
		return TypeOwner.IGNORE;
	}

}
