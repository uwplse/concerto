package edu.washington.cse.concerto.interpreter;

import edu.washington.cse.concerto.interpreter.meta.TypeOracle;
import edu.washington.cse.concerto.interpreter.meta.TypeOracle.TypeOwner;
import soot.Body;
import soot.FastHierarchy;
import soot.PackManager;
import soot.PatchingChain;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SourceLocator;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.grimp.Grimp;
import soot.jimple.InvokeExpr;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class BodyManager {
	private static TypeOracle oracle;

	public static Body retrieveBody(final SootMethod method) {
		Body body = method.retrieveActiveBody();
		if(BodyManager.instrumented.add(method)) {
			method.setActiveBody(Grimp.v().newBody(body, "gb"));
			PackManager.v().getPack("gop").apply(body = method.getActiveBody());
			populateHostMap(body, method);
		}
		return body;
	}

	public static SootMethod getHostMethod(final InvokeExpr op) {
		return hostMethod.get(op);
	}
	
	private static void populateHostMap(final Body b, final SootMethod m) {
		final PatchingChain<Unit> units = b.getUnits();
		for(final Unit u : units) {
			unitHosts.put(u, m);
			for(final ValueBox ub : u.getUseBoxes()) {
				populateHostMap(ub.getValue(), m, u);	
			}
		}
	}

	private static void populateHostMap(final Value value, final SootMethod m, final Unit u) {
		if(value instanceof InvokeExpr) {
			hostMethod.put((InvokeExpr) value, m);
			hostUnit.put((InvokeExpr)value, u);
		}
	}

	private static final Map<InvokeExpr, SootMethod> hostMethod = new HashMap<>();
	private static final Map<InvokeExpr, Unit> hostUnit = new HashMap<>();
	private static final Map<Unit, SootMethod> unitHosts = new HashMap<>();
	private static final Set<SootMethod> instrumented = new HashSet<>();
	
	public static Unit getHostUnit(final InvokeExpr op) {
		return hostUnit.get(op);
	}
	
	public static SootClass loadClass(final RefType t) {
		return loadClass(t.getClassName());
	}
	
	public static SootClass loadClass(final String typeName) {
		return Scene.v().loadClass(typeName, SootClass.BODIES);
	}

	public static SootMethod getHostMethod(final Unit currUnit) {
		return unitHosts.get(currUnit);
	}
	
	private final static Set<String> classCache = new HashSet<>(); 

	public static void reset() {
		hostMethod.clear();
		hostUnit.clear();
		unitHosts.clear();
		instrumented.clear();
		oracle = null;
		classCache.clear();
		includePackages.clear();
	}
	
	public static void init(final TypeOracle oracle) {
		BodyManager.oracle = oracle;
	}

	public static Iterable<SootClass> enumerateApplicationClasses(final RefType... bounds) {
		return enumerate(TypeOwner.FRAMEWORK, bounds);
	}
	
	public static Iterable<SootClass> enumerateFrameworkClasses(final RefType...bounds) {
		return enumerate(TypeOwner.APPLICATION, bounds);
	}

	private static Iterable<SootClass> enumerate(final TypeOwner exclOwner, final RefType... bounds) {
		return new Iterable<SootClass>() {
			@Override
			public Iterator<SootClass> iterator() {
				return new Iterator<SootClass>() {
					Iterator<String> classIt = getAllClasses().iterator();
					SootClass next = null;
					{
						findNext();
					}
					
					@Override
					public SootClass next() {
						final SootClass toRet = next;
						findNext();
						return toRet;
					}
					
					private void findNext() {
						next = null;
						outer_search: while(classIt.hasNext()) {
							final String klass = classIt.next();
							final TypeOwner to = oracle.classifyType(klass);
							if(to == TypeOwner.IGNORE) {
								continue outer_search;
							}
							if(to != exclOwner) {
								final SootClass candidate = BodyManager.loadClass(klass);
								if(candidate.isAbstract() || hasPhantomRoot(candidate)) {
									continue outer_search;
								}
								final FastHierarchy fh = Scene.v().getOrMakeFastHierarchy();
								for(final RefType t : bounds) {
									if(!fh.canStoreType(candidate.getType(), t)) {
										continue outer_search;
									}
								}
								next = candidate;
								return;
							}
						}
						
					}

					private boolean hasPhantomRoot(final SootClass candidate) {
						return candidate.getSuperclassUnsafe() == null || !candidate.getSuperclass().getName().equals("java.lang.Object");
					}

					@Override
					public boolean hasNext() {
						return next != null;
					}
				};
			}
		};
	}

	private static final Set<String> includePackages = new HashSet<>();

	public static void setInclude(final Collection<String> s) {
		includePackages.clear();
		includePackages.addAll(s);
	}

	private static Set<String> getAllClasses() {
		if(classCache.isEmpty()) {
			if(includePackages.isEmpty()) {
				for(final String classPath : SourceLocator.v().classPath()) {
					classCache.addAll(SourceLocator.v().getClassesUnder(classPath));
				}
			} else {
				for(final String classPath : SourceLocator.v().classPath()) {
					SourceLocator.v().getClassesUnder(classPath).stream().filter(st -> {
						for(final String includedPkg : includePackages) {
							if(st.startsWith(includedPkg)) {
								return true;
							}
						}
						return false;
					}).forEach(classCache::add);
				}
			}
		}
		return classCache;
	}
}
