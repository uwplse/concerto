package edu.washington.cse.concerto.interpreter.ai;

import edu.washington.cse.concerto.interpreter.ai.binop.ObjectIdentityResult;
import fj.data.Option;
import fj.data.Stream;
import soot.Scene;
import soot.Type;
import soot.jimple.ArrayRef;

public interface ObjectOperations<AVal, AHeap> {
	Option<Object> readArray(AHeap h, AVal basePointer, AVal index, ArrayRef context);
	AHeap writeArray(AHeap h, AVal basePointer, AVal index, Object value, ArrayRef context);
	AVal arrayLength(AVal basePointer, AHeap h);
	default ObjectIdentityResult isInstanceOf(final AVal a, final Type t) {
		final ObjectIdentityResult nullness = isNull(a);
		if(nullness != ObjectIdentityResult.MUST_NOT_BE) {
			return nullness;
		}
		final Stream<Type> stream = possibleTypes(a);
		if(stream.isEmpty()) {
			return ObjectIdentityResult.MUST_NOT_BE;
		}
		return stream.map(possibleType -> Scene.v().getOrMakeFastHierarchy().canStoreType(possibleType, t) ? ObjectIdentityResult.MUST_BE : ObjectIdentityResult.MUST_NOT_BE).foldLeft1(ObjectIdentityResult::join);
	}
	Stream<Type> possibleTypes(AVal a);
	AVal downcast(AVal v, Type t);
	default ObjectIdentityResult isNull(final AVal a) {
		return ObjectIdentityResult.MAY_BE;
	}
}
