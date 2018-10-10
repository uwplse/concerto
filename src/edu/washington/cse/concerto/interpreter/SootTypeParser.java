package edu.washington.cse.concerto.interpreter;

import java.util.ArrayList;
import java.util.List;

import fj.P;
import fj.P3;
import soot.AnySubType;
import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.DoubleType;
import soot.FloatType;
import soot.IntType;
import soot.LongType;
import soot.RefType;
import soot.ShortType;
import soot.Type;
import soot.VoidType;

public class SootTypeParser {
	public static Type parseType(final String token) {
		final Type t;
		final String rest;
		if(token.startsWith("int")) {
			t = IntType.v();
			rest = token.substring(3);
		} else if(token.startsWith("float")) {
			t = FloatType.v();
			rest = token.substring(5);
		} else if(token.startsWith("double")) {
			t = DoubleType.v();
			rest = token.substring(6);
		} else if(token.startsWith("long")) {
			t = LongType.v();
			rest = token.substring(4);
		} else if(token.startsWith("boolean")) {
			t = BooleanType.v();
			rest = token.substring(7);
		} else if(token.startsWith("short")) {
			t = ShortType.v();
			rest = token.substring(5);
		} else if(token.startsWith("byte")) {
			t = ByteType.v();
			rest = token.substring(4);
		} else if(token.startsWith("char")) {
			t = CharType.v();
			rest = token.substring(4);
		} else if(token.startsWith("Any_subtype_of_")) {
			final RefType tmp = (RefType) parseType(token.substring("Any_subtype_of_".length()));
			return AnySubType.v(tmp);
		} else if(token.startsWith("void")) {
			t = VoidType.v();
			rest = token.substring(4);
		} else {
			final char[] charArray = token.toCharArray();
			int i = 0;
			while(i < charArray.length && isIdentPart(charArray, i)) {
				i++;
			}
			t = RefType.v(token.substring(0, i));
			rest = token.substring(i);
		}
		if(rest.isEmpty()) {
			return t;
		}
		assert rest.startsWith("[]");
		int numDim = 0;
		int start = 0;
		while(rest.indexOf("[]", start) != -1) {
			assert rest.indexOf("[]", start) == start : start + " " + rest;
			start += 2;
			numDim++;
		}
		Type accum = t;
		for(int i = 0; i < numDim; i++) {
			accum = accum.makeArrayType();
		}
		return accum;
	}
	
	public static P3<Type, String, List<Type>> parseSubSig(final String subSig) {
		final String[] retAndRest = subSig.split(" ");
		final Type returnType = parseType(retAndRest[0]);
		final int start = retAndRest[1].indexOf('(');
		final int end = retAndRest[1].indexOf(')', start + 1);
		final String name = retAndRest[1].substring(0, start);
		final List<Type> argTypes = new ArrayList<>();
		if(start != end) {
			for(final String typeStr : retAndRest[1].substring(start + 1, end).split(",")) {
				argTypes.add(parseType(typeStr));
			}
		}
		return P.p(returnType, name, argTypes);
	}
	
	private static boolean isIdentPart(final char[] charArray, final int i) {
		if(i == 0 && Character.isJavaIdentifierStart(charArray[i])) {
			return true;
		} else if(i > 0 && Character.isJavaIdentifierPart(charArray[i])) {
			return true;
		} else if(i > 0 && charArray[i] == '.') {
			return true;
		} else {
			return false;
		}
	}
}
