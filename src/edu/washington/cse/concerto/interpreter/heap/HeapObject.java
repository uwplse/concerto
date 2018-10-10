package edu.washington.cse.concerto.interpreter.heap;

import com.google.common.annotations.VisibleForTesting;
import edu.washington.cse.concerto.interpreter.value.Copyable;
import edu.washington.cse.concerto.interpreter.value.IValue;
import fj.data.Stream;
import soot.SootClass;
import soot.Type;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class HeapObject implements Copyable<HeapObject> {
	@VisibleForTesting
	public final Map<String, IValue> fieldMap;
	final IValue[] arrayField;
	IValue nondetContents;
	final boolean isNondetSize;
	final int arrayLength;
	final boolean isSummaryObject;
	
	/*
	 * Fresh array of deterministic length
	 */
	HeapObject(final int length, final IValue defaultVal) {
		this.arrayLength = length;
		this.arrayField = new IValue[length];
		this.fieldMap = null;
		this.isNondetSize = false;
		Arrays.fill(arrayField, defaultVal);
		isSummaryObject = false;
	}
	
	/*
	 * Fresh array of non-deterministic length
	 */
	HeapObject(final boolean isSummaryObject, final IValue nondetContents) {
		this.arrayLength = -1;
		this.arrayField = null;
		this.fieldMap = null;
		this.nondetContents = nondetContents;
		this.isNondetSize = true;
		this.isSummaryObject = isSummaryObject;
	}
	
	/*
	 * Array copy constructor
	 */
	HeapObject(final IValue[] arrayField, final IValue nondetContents) {
		this.arrayField = arrayField;
		this.arrayLength = arrayField.length;
		this.nondetContents = nondetContents;
		this.fieldMap = null;
		this.isNondetSize = false;
		isSummaryObject = false;
	}
	
	/*
	 * Fresh object
	 */
	HeapObject() {
		arrayField = null;
		arrayLength = -1;
		fieldMap = new HashMap<>();
		isNondetSize = false;
		isSummaryObject = false;
	}
	
	/*
	 * Object copy constructor
	 */
	public HeapObject(final Map<String, IValue> fieldMap) {
		this.fieldMap = fieldMap;
		this.arrayField = null;
		this.arrayLength = -1;
		isNondetSize = false;
		isSummaryObject = false;
	}

	@Override
	public HeapObject copy() {
		if(arrayField != null) {
			return new HeapObject(Arrays.copyOf(arrayField, arrayField.length), nondetContents);
		} else if(isNondetSize) {
			assert nondetContents != null;
			return new HeapObject(isSummaryObject, nondetContents);
		} else {
			return new HeapObject(new HashMap<>(fieldMap));
		}
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(arrayField);
		result = prime * result + arrayLength;
		result = prime * result + ((fieldMap == null) ? 0 : fieldMap.hashCode());
		result = prime * result + (isNondetSize ? 1231 : 1237);
		result = prime * result + (isSummaryObject ? 1231 : 1237);
		result = prime * result + ((nondetContents == null) ? 0 : nondetContents.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if(this == obj) {
			return true;
		}
		if(obj == null) {
			return false;
		}
		if(getClass() != obj.getClass()) {
			return false;
		}
		final HeapObject other = (HeapObject) obj;
		if(!Arrays.equals(arrayField, other.arrayField)) {
			return false;
		}
		if(arrayLength != other.arrayLength) {
			return false;
		}
		if(fieldMap == null) {
			if(other.fieldMap != null) {
				return false;
			}
		} else if(!fieldMap.equals(other.fieldMap)) {
			return false;
		}
		if(isNondetSize != other.isNondetSize) {
			return false;
		}
		if(isSummaryObject != other.isSummaryObject) {
			return false;
		}
		if(nondetContents == null) {
			if(other.nondetContents != null) {
				return false;
			}
		} else if(!nondetContents.equals(other.nondetContents)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		if(arrayField != null) {
			sb.append("[");
			if(arrayLength != 0) {
				sb.append(arrayField[0]);
				for(int i = 1; i < arrayField.length; i++) {
					sb.append(",").append(arrayField[i]);
				}
				if(nondetContents != null) {
					sb.append(",*=").append(nondetContents);
				}
			} else if(nondetContents != null) {
				sb.append("*=").append(nondetContents);
			}
			sb.append("]");
		} else if(isNondetSize) {
			sb.append("[?*=").append(nondetContents).append("]");
		} else {
			sb.append(fieldMap.toString());
		}
		if(this.isSummaryObject) {
			sb.append("S");
		}
		return sb.toString();
	}

	public boolean lessEqual(final HeapObject other) {
		if(other == null) {
			return false;
		}
		if(fieldMap != null) {
			if(other.fieldMap == null) {
				return false;
			}
			for(final String s : fieldMap.keySet()) {
				if(!other.fieldMap.containsKey(s)) { 
					return false;
				}
				if(!this.fieldMap.get(s).lessEqual(other.fieldMap.get(s))) {
					return false;
				}
			}
			return true;
		} else if(!isNondetSize) {
			if(other.isNondetSize) {
				assert other.nondetContents != null;
				for(final IValue v : arrayField) {
					assert v != null;
					if(!v.lessEqual(other.nondetContents)) {
						return false;
					}
				}
			} else {
				if(other.arrayLength != this.arrayLength) {
					return false;
				}
				for(int i = 0; i < this.arrayLength; i++) {
					if(!this.arrayField[i].lessEqual(other.arrayField[i])) {
						return false;
					}
				}
				if(this.nondetContents != null) {
					if(other.nondetContents == null) {
						return false;
					}
					if(!this.nondetContents.lessEqual(other.nondetContents)) {
						return false;
					}
				}
			}
			return true;
		} else {
			assert isNondetSize;
			if(this.isSummaryObject && !other.isSummaryObject) {
				return false;
			}
			return this.nondetContents.lessEqual(other.nondetContents);
		}
	}

	public void forEachField(final HeapFieldAction heapFieldAction) {
		if(fieldMap != null) {
			for(final Map.Entry<String, IValue> kv : fieldMap.entrySet()) {
				heapFieldAction.accept(kv.getKey(), kv.getValue());
			}
		}
		if(arrayField != null) {
			for(final IValue v : arrayField) {
				heapFieldAction.accept("*", v);
			}
		}
		if(nondetContents != null) {
			heapFieldAction.accept("*", nondetContents);
		}
	}
	
	public Stream<IValue> fieldValueStream() {
		return Stream.iterableStream(new Iterable<IValue>() {
			@Override
			public Iterator<IValue> iterator() {
				return new Iterator<IValue>() {
					int state = 0;
					Iterator<IValue> fieldMapIterator = fieldMap != null ? fieldMap.values().iterator() : null;
					int arrayFieldInd = 0;
					
					IValue next = null;
					{
						findNext();
					}
					
					@Override
					public boolean hasNext() {
						return next != null; 
					}
					private void findNext() {
						if(state == 4) {
							next = null;
							return;
						}
						if(state == 0) {
							if(fieldMapIterator == null || !fieldMapIterator.hasNext()) {
								state++;
								findNext();
								return;
							}
							next = fieldMapIterator.next();
							return;
						} else if(state == 1) {
							if(arrayField == null || arrayFieldInd >= arrayField.length) {
								state++;
								findNext();
								return;
							}
							next = arrayField[arrayFieldInd++];
							return;
						} else if(state == 3) {
							state++;
							if(nondetContents == null) {
								next = null;
								return;
							}
							next = nondetContents;
							return;
						}
					}
					@Override
					public IValue next() {
						final IValue toRet = next;
						findNext();
						return toRet;
					}
				};
			}
		});
	}

	public static HeapObject forClass(final SootClass sootClass) {
		return new HeapObject();
	}
	
	public static HeapObject forTypeBound(final Type upperBound) {
		return new HeapObject();
	}
}
