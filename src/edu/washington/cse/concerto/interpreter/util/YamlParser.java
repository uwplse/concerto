package edu.washington.cse.concerto.interpreter.util;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Map;

public class YamlParser {

	@SuppressWarnings("unchecked")
	public static Map<String, Object> parseYamlConf(final String confFile) {
		try(FileInputStream fis = new FileInputStream(new File(confFile))) {
			return (Map<String, Object>) new Yaml().load(fis);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void dumpYaml(final Object o, final String outputFile) {
		try(Writer w = new PrintWriter(new File(outputFile))) {
			new Yaml().dump(o, w);
		} catch(final IOException e) {
			System.out.println("Failed to write information to " + outputFile);
		}
	}

}
