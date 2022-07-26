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

	void add(String word) {

		words.add(word);
	}

	String asKey() {

		return String.valueOf(tries) + " " + String.valueOf(failures);
	}

	static String asKey(int tries, int failures) {

		return String.valueOf(tries) + " " + String.valueOf(failures);
	}

}
