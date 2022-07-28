package uk.co.myzen.a_z;

import java.util.ArrayList;
import java.util.List;

class Result {

	final int tries;
	final int failures;

	final List<String> words;

	Result(String word, int tries, int failures) {

		this.tries = tries;
		this.failures = failures;

		words = new ArrayList<String>();

		words.add(word);
	}

	Result(String word, Integer key) {

		this(word, key / 1000, key % 1000);
	}

	void add(String word) {

		words.add(word);
	}

	String asStringKey() {

		return String.valueOf(tries) + " " + String.valueOf(failures);
	}

	Integer asIntegerKey() {

		return 1000 * tries + failures;
	}

	static String asKey(int tries, int failures) {

		return String.valueOf(tries) + " " + String.valueOf(failures);
	}

}
