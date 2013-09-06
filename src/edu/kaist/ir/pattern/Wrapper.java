package edu.kaist.ir.pattern;

public class Wrapper {

	public static Sequence getArraySequence(String str) {
		return new ArraySequence(str.split(" "));
	}

	public static Sequence getArraySequence(String[] arr) {
		return new ArraySequence(arr);
	}

	public static Sequence getStringSequence(String str) {
		return new StringSequence(str);
	}

}
