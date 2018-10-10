package edu.washington.cse.concerto.interpreter.util;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.options.Options;

public class ReferenceResolution {
	public static void main(final String[] args) {
		final String[] sootArgs = new String[]{
				"-soot-class-path", args[0],
				"-allow-phantom-refs"
		};
		final boolean parse = Options.v().parse(sootArgs);
		final SootClass host = Scene.v().loadClass(args[2], SootClass.SIGNATURES);
		final SootClass arg = Scene.v().loadClass(args[3], SootClass.SIGNATURES);
		final String firstField = args[1].substring(0, 1).toUpperCase() + args[1].substring(1);
		for(final SootMethod m : host.getMethods()) {
			if(!m.isConcrete() || !m.isPublic() || m.getParameterCount() != 1) {
				continue;
			}
			if(!m.getName().startsWith("set")) {
				continue;
			}
			if(!m.getName().substring(3).equals(firstField)) {
				continue;
			}
			if(Scene.v().getOrMakeFastHierarchy().canStoreType(arg.getType(), m.getParameterType(0))) {
				System.out.println(m.getParameterType(0));
				return;
			}
		}
		System.exit(1);
	}
}
