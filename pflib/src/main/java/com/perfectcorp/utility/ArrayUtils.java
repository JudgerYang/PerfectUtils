package com.perfectcorp.utility;

public class ArrayUtils {

	/**
	 * Create a integer array from start to end.
	 * No matter start/end is negative or equal.
	 * 
	 * @param start
	 * @param end
	 * @return
	 */
	public static int[] createIntArray(int start, int end) {
		int arrayLength = Math.abs(end - start) + 1;
		int[] array = new int[arrayLength];

		if (end < start) {
			for (int i = 0; i < arrayLength; i++) {
				array[i] = start - i;
			}
		} else {
			for (int i = 0; i < arrayLength; i++) {
				array[i] = start + i;
			}
		}

		return array;
	}

	public static int[] repeatArray(int[] array, int repeat) {
		return repeatArray(array, repeat, false, null);
	}

	public static int[] repeatArray(int[] array, int repeat, boolean append, int... value) {
		int[] newArray;
		int newArrayLength = array.length * repeat;
		if (append) {
			newArray = new int[newArrayLength + value.length];
		} else {
			newArray = new int[newArrayLength];
		}

		for (int i = 0; i < newArrayLength; i++) {
			newArray[i] = array[i % array.length];
		}
		if (append) {
			for (int i = newArrayLength; i < newArray.length; i++) {
				newArray[i] = value[i  - newArrayLength];
			}
		}

		return newArray;
	}

	public static <T> T last(T[] array) {
		return array[array.length - 1];
	}
}
