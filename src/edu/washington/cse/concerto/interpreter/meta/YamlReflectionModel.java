package edu.washington.cse.concerto.interpreter.meta;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;
import soot.SootClass;

public class YamlReflectionModel implements ReflectionModel {
	private final List<String> classNames;
	private final List<String> methodSubSigs;
	
	@SuppressWarnings("unchecked")
	public YamlReflectionModel(final String confFile) {
		final Yaml y = new Yaml();
		Map<String,Object> loaded;
		try(FileInputStream fis = new FileInputStream(new File(confFile))) {
			loaded = (Map<String, Object>)y.load(fis);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		this.classNames = (List<String>) loaded.get("classes");
		this.methodSubSigs = (List<String>) loaded.get("methods");
	}
	
	@Override
	public String resolveClassName(final int key) {
		return this.classNames.get(key);
	}
	
	@Override
	public String resolveSignature(final int key) {
    return this.methodSubSigs.get(key);
	}

	@Override public int reverseResolution(final SootClass sootClass) {
		int ret = classNames.indexOf(sootClass.getName());
		if(ret == -1) {
			return (classNames.size() + 1) * -1;
		} else {
			return ret;
		}
	}
}
