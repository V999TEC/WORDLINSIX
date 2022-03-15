package uk.co.myzen.a_z;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

public class WordlInSix {

	private static final String alphabet = "abcdefghijklmnopqrstuvwxyz";

	private static int[] letterDistributionRanks;

	private static enum Action {

		DEFAULT, ABORT, SHOW_HELP, GUESS_TO_ANSWER, DEBUG_1, DEBUG_2, DEBUG_3
	};

	private static Action action = Action.SHOW_HELP;

	private static WordlInSix instance = null;

	private static List<String> words = null;

	private int debug = 0;

	private boolean showWords = false;

	private boolean showRank = false;

	private List<String> guesses = new ArrayList<String>(6);

	private char[] containsChars = {};

	private char[] notChars = {};

	private String[] notN = { "", "", "", "", "" };

	private char positions[] = { ' ', ' ', ' ', ' ', ' ' };

	private String answer = "";

	private static WordlInSix getInstance(String nonDefaultName) {

		if (null == instance) {

			if (null == nonDefaultName || 0 == nonDefaultName.trim().length()) {

				instance = new WordlInSix();

			} else {

				instance = new WordlInSix(nonDefaultName + ".txt");
			}
		}

		letterDistributionRanks = instance.loadLetterDistributionRanks();

		return instance;
	}

	private WordlInSix() {

		words = loadWords("words.txt");
	}

	private WordlInSix(String name) {

		words = loadWords(name);
	}

	private int[] loadLetterDistributionRanks() {

		int[] frequencies = new int[26];

		Properties properties = new Properties();

		ClassLoader cl = Thread.currentThread().getContextClassLoader();

		InputStream is = cl.getResourceAsStream("frequencies.properties");

		try {

			if (null == is) {

				debug1(properties);

			} else {

				properties.load(is);
			}

			for (int index = 0; index < 26; index++) {

				String key = alphabet.substring(index, 1 + index);

				String value = properties.getProperty(key, "0").trim();

				frequencies[index] = Integer.parseInt(value);
			}

		} catch (IOException e) {

			e.printStackTrace();
		}

		return frequencies;
	}

	public static void main(String[] args) {

		WordlInSix main;

		if (0 == args.length) {

			main = getInstance(null);

			action = Action.SHOW_HELP;

		} else {

			// if first parameter is "words" (also default) then load "words.txt"
			// an alternative is "scholardle"

			main = getInstance(-1 == args[0].indexOf('=') ? args[0] : null);

			action = Action.DEFAULT;

			for (String arg : args) {

				main.parameter(arg);
			}

			if (!main.validatedParameters()) {

				action = Action.ABORT;
			}
		}

		switch (action) {

		case DEBUG_1:

			main.debug1(null); // display letter frequency
			break;

		case DEBUG_2:

			main.debug2();
			break;

		case DEBUG_3:

			main.debug3();
			break;

		case GUESS_TO_ANSWER:

			main.guessToAnswer();
			break;

		case ABORT:
		case SHOW_HELP:

			main.help();
			break;

		case DEFAULT:
		default:

			Set<String> candidates = main.findCandidates();

			// display the candidate words (if words=true | words=yes )

			StringBuffer sb = new StringBuffer();

			for (String arg : args) {

				sb.append(arg);
				sb.append(' ');
			}

			System.err.println("There are " + candidates.size() + " word(s) matching " + sb.toString());

			main.report(candidates);

			break;
		}
	}

	private void help() {

		System.out.println("Tips:");
		System.out.println("\tUse words=yes or words=true to show possible words");
		System.out.println("\tUse rank=yes or rank=true to rank each word based on letter probability");
		System.out.println("\tUse 1=b to indicate first letter is definitely b");
		System.out.println("\tEliminate letters by using not=abcdefg etc.");
		System.out.println("\tUse 5=z to indicate last letter is definitely z");
		System.out.println("\tUse 2=j 3=k  to indicate letters 'jk' are found between positions 2 and 3");
		System.out.println("\tUse 3=j 4=k  to indicate letters 'jk' are found between positions 3 and 4");
		System.out.println("\tUse contains=iou to indicate letters i and o and u *must* appear in the word");
		System.out.println("\tUse not2=ab to indicate second letter cannot be a or b");
		System.out.println("\tUse not5=y to indicate fifth letter cannot be y");
	}

	private void report(Set<String> candidates) {

		if (showWords) {

			int count = 0;

			if (showRank) {

				List<String> rankings = rankCandidatesByLetterDistribution(candidates);

				for (String candidate : rankings) {

					int uniqueLetters = countDuplicateLetters(candidate);

					int vowels = countVowels(candidate);

					System.err.println((++count) + "\t" + candidate + "\t" + vowels + "\t" + uniqueLetters);
				}

			} else {

				Set<String> sortedSet = new TreeSet<String>(candidates);

				for (String candidate : sortedSet) {

					System.err.println((++count) + "\t" + candidate);
				}
			}
		}

	}

	private int countDuplicateLetters(String word) {

		char[] chrs = word.toCharArray();

		Set<Character> unique = new HashSet<Character>();

		for (char c : chrs) {

			unique.add(c);
		}

		return chrs.length - unique.size();
	}

	private int countVowels(String word) {

		int count = 0;

		char[] chrs = word.toCharArray();

		for (char c : chrs) {

			if ("aeiouy".indexOf(c) > -1) {

				count++;
			}
		}

		return count;
	}

	private void parameter(String arg) {

		int indexOfEqualsChar = arg.indexOf('=');

		String value = arg.substring(1 + indexOfEqualsChar);

		if (arg.startsWith("debug=")) {

			debug = Integer.parseInt(value);

			if (1 == debug) {

				// generate letter distribution for the WORDLE dictionary

				action = Action.DEBUG_1;

			} else if (2 == debug) {

				action = Action.DEBUG_2;

			} else if (3 == debug) {

				action = Action.DEBUG_3;

			} else {

				action = Action.ABORT;
			}

		} else if (arg.startsWith("answer=")) {

			answer = value;

			action = Action.GUESS_TO_ANSWER;

		} else if (arg.startsWith("rank=")) {

			showRank = (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes"));

		} else if (arg.startsWith("words=")) {

			showWords = (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes"));

		} else if (arg.startsWith("guess")) {

			// extract number between 'guess' and '='

			int guessNumber = Integer.parseInt(arg.substring(5, indexOfEqualsChar));

			guesses.add(guessNumber - 1, value);

		} else if (arg.startsWith("contains=")) {

			containsChars = value.toCharArray();

		} else if (arg.startsWith("not=")) {

			notChars = value.toCharArray();

		} else {

			for (int n = 0; n < 5; n++) {

				String numEquals = String.valueOf(1 + n) + "=";

				if (arg.startsWith(numEquals)) {

					if (1 != value.length()) {

						System.err.println("Error:  parameter " + numEquals + value
								+ " is not allowed. Try again with single letter only");
						return;
					}

					positions[n] = value.charAt(0);

				} else if (arg.startsWith("not" + numEquals)) {

					notN[n] = value;

				}
			}
		}
	}

	private boolean validatedParameters() {

		String contains = null == containsChars ? "" : new String(containsChars);

		if (contains.length() > 5) {

			System.err
					.println("Error:  parameter contains=" + contains + " looks wrong. Try again with 1 to 5 letters");
			return false;
		}

		for (char ch : positions) {

			if (' ' != ch) {

				if (-1 == contains.indexOf(ch)) {

					System.err.println("Error:  parameter contains=" + contains
							+ " looks wrong or missing. Expecting at least letter " + ch);
					return false;
				}
			}
		}

		StringBuffer wp = new StringBuffer();

		for (String test : notN) {

			wp.append(test);
		}

		String wrongPositions = wp.toString();
		String correctPositions = new String(positions);

		for (char ch : wrongPositions.toCharArray()) {

			if (-1 == contains.indexOf(ch)) {

				System.err.println("Error:  parameter contains=" + contains
						+ " looks wrong. The list must include all letters from notN=");
				return false;
			}
		}

		for (char ch : correctPositions.toCharArray()) {

			if (' ' != ch && -1 == contains.indexOf(ch)) {

				System.err.println("Error:  parameter contains=" + contains
						+ " looks wrong. The list must include all letters from N=");
				return false;
			}
		}

		return true;
	}

	private static List<String> loadWords(String name) {

		List<String> result = new ArrayList<String>(2315);

		ClassLoader cl = Thread.currentThread().getContextClassLoader();

		InputStream is = cl.getResourceAsStream(name);

		InputStreamReader isr = new InputStreamReader(is);

		BufferedReader br = new BufferedReader(isr);

		String ln;

		try {

			while (null != (ln = br.readLine())) {

				result.add(ln);
			}

		} catch (IOException e) {

			e.printStackTrace();

		} finally {

			try {
				br.close();

				isr.close();

			} catch (IOException e) {

				e.printStackTrace();
			}

		}

		return result;
	}

	private Set<String> findCandidates() {

		Set<String> candidates = new HashSet<String>();

		String contains = containsChars.toString();

		String not = notChars.toString();

		for (String word : words) {

			boolean match;

			// check the word contains all the letters in contains=

			if (!"".equals(contains)) {

				match = true;

				for (int n = 0; n < containsChars.length; n++) {

					if (-1 == word.indexOf(containsChars[n])) {

						match = false;
						break;
					}
				}

				if (!match) {

					continue;
				}
			}

			// check the word does NOT contain any of the letters in not=

			if (!"".equals(not)) {

				match = false;

				for (int n = 0; n < notChars.length; n++) {

					if (-1 != word.indexOf(notChars[n])) {

						match = true;
						break;
					}
				}

				if (match) {

					continue;
				}
			}

			// now eliminate candidate if it has desired letters but in the wrong positions

			match = false;

			for (int index = 0; index < 5; index++) {

				if (0 == notN[index].length()) {

					continue;
				}

				char[] notNsOr = notN[index].toCharArray();

				char c = word.charAt(index);

				for (int i = 0; i < notNsOr.length; i++) {

					if (c == notNsOr[i]) {

						match = true;
						break;
					}
				}

				if (match) {

					break;
				}
			}

			if (match) {

				continue;
			}

			match = true;

			for (int index = 0; index < 5; index++) {

				char c = positions[index];

				if (' ' != c) {

					if (c != word.charAt(index)) {

						match = false;
						break;
					}
				}
			}

			if (match) {

				candidates.add(word);
			}
		}

		return candidates;
	}

	private int charToIndex(char c) {

		int n = c;

		n -= 97; // to get index in range 0..25

		return n;
	}

	private int calculateLetterDistributionRank(String word) {

		int accumulator = 0;

		for (char c : word.toCharArray()) {

			accumulator += letterDistributionRanks[charToIndex(c)];
		}

		return accumulator;
	}

	private List<String> rankCandidatesByLetterDistribution(Set<String> candidates) {

		Map<Integer, String> rankings = new HashMap<Integer, String>(candidates.size());

		for (String candidate : candidates) {

			int rank = calculateLetterDistributionRank(candidate);

			String pad5 = "0000" + String.valueOf(rank);

			String rank5 = pad5.substring(pad5.length() - 5);

			Integer key = null;

			for (int dup = 0; dup < 100; dup++) {

				String pad2 = "0" + String.valueOf(dup);

				String dup2 = pad2.substring(pad2.length() - 2);

				key = Integer.valueOf(rank5 + dup2);

				if (!rankings.containsKey(key)) {

					break;
				}
			}

			if (null == key) {

				System.err.println("FAIL: contact author key:null rank5:" + rank5);
				System.exit(candidates.size());
			}
			// static analysis shows that key cannot be null because wordlist is constant
			// and data has been verified

			rankings.put(key, candidate);
		}

		TreeSet<Integer> orderedSet = new TreeSet<Integer>(rankings.keySet());

		List<String> orderedList = new ArrayList<String>(candidates.size());

		Iterator<Integer> iter = orderedSet.descendingIterator();

		while (iter.hasNext()) {

			Integer key = iter.next();

			String word = rankings.get(key);
			orderedList.add(word);
		}

		List<String> hybridList = new ArrayList<String>(candidates.size());

		List<List<String>> categories = new ArrayList<List<String>>();

		for (int n = 0; n < 4; n++) {

			List<String> category = new ArrayList<String>();

			categories.add(category);
		}

		// create content in the hybridList according to categories

		for (int index = 0; index < candidates.size(); index++) {

			String candidate = orderedList.get(index);

			int dups = countDuplicateLetters(candidate);

			int vowelCount = countVowels(candidate);

			if (guesses.size() > 0) {

				if ("thump".equals(guesses.get(0))) {

					if (guesses.size() > 1) {

						if (!"blown".equals(guesses.get(1))) {

							// improve chances by prioritising these words

							if ("state".equals(candidate)) {

								categories.get(0).add(candidate);

							} else if ("masse".equals(candidate)) {

								categories.get(0).add(candidate);

							} else if ("verge".equals(candidate)) {

								categories.get(0).add(candidate);

							} else if ("grill".equals(candidate)) {

								categories.get(0).add(candidate);
							}
						}
					}
				}
			}

			if (0 == dups) {

				if (2 == vowelCount) {

					categories.get(0).add(candidate);

				} else {

					categories.get(1).add(candidate);
				}

			} else {

				if (2 == vowelCount) {

					categories.get(2).add(candidate);

				} else {

					categories.get(3).add(candidate);
				}
			}
		}

		// build hybridList

		for (int n = 0; n < categories.size(); n++) {

			hybridList.addAll(categories.get(n));
		}

		return hybridList;
	}

	private void scoreAgainstKnownAnswer(String word) {

		// append to parameters accordingly

		char[] chr = word.toCharArray();

		String not = new String(notChars);

		String contains = new String(containsChars);

		for (int index = 0; index < chr.length; index++) {

			char c = chr[index];

			// is character in answer at all ?

			int pos = answer.indexOf(c);

			if (pos < 0) { // char not found in word

				pos = not.indexOf(c);

				if (-1 == pos) {

					not = not + c;

					notChars = not.toCharArray();
				}

			} else { // character is in the answer at least once

				pos = contains.indexOf(c);

				if (-1 == pos) {

					contains = contains + c;

					containsChars = contains.toCharArray();
				}

				if (c == answer.charAt(index)) { // green

					positions[index] = c;

				} else { // right character but wrong position

					pos = notN[index].indexOf(c);

					if (-1 == pos) {

						notN[index] = notN[index] + c;
					}
				}
			}
		}

		containsChars = contains.toCharArray();

	}

	private void guessToAnswer() {

		int count = howManyGuesses();

		System.err.println("Algorithm needed " + count + " guesses");
	}

	private int howManyGuesses() {

		int howMany = -1;

		if (0 == guesses.size()) {

			System.err.println("When answer=" + answer + " specified, need at least parameter guess1=xxxxx");

			return 0;
		}

		notChars = new char[] {};

		String not = new String(notChars);

		containsChars = new char[] {};

		notN = new String[] { "", "", "", "", "" };

		char emptyPositions[] = { ' ', ' ', ' ', ' ', ' ' };

		positions = emptyPositions;

		String nextBestGuess = "";

		for (int index = 0; index < guesses.size(); index++) {

			String contains = new String(containsChars);

			scoreAgainstKnownAnswer(guesses.get(index));

			StringBuffer suggestive = new StringBuffer();

			for (int n = 0; n < guesses.size(); n++) {

				if (!"".equals(guesses.get(n))) {

					suggestive.append("guess");
					suggestive.append(1 + n);
					suggestive.append("=");
					suggestive.append(guesses.get(n));
					suggestive.append(' ');
				}
			}

			if (!"".equals(not)) {

				suggestive.append("not=");
				suggestive.append(not);
				suggestive.append(' ');
			}

			if (!"".equals(contains)) {

				suggestive.append("contains=");
				suggestive.append(contains);
				suggestive.append(' ');
			}

			for (int n = 0; n < 5; n++) {

				if (' ' != positions[n]) {

					suggestive.append(1 + n);
					suggestive.append("=");
					suggestive.append(positions[n]);
					suggestive.append(' ');
				}
			}

			for (int n = 0; n < 5; n++) {

				if (!"".equals(notN[n])) {

					// no point in showing not3= if there has been a 3=

					if (' ' == positions[n]) {

						suggestive.append("not");
						suggestive.append(1 + n);
						suggestive.append("=");
						suggestive.append(notN[n]);
						suggestive.append(' ');
					}
				}
			}

			if (0 == debug) {

				System.err.println(suggestive.toString());
			}
			howMany = guesses.size();

			// check if the latest guess happens to be the answer

			if (guesses.get(howMany - 1).equals(answer)) {

				break;
			}

			if (index + 1 == howMany) {

				nextBestGuess = selectCandidate();

				if (null == nextBestGuess) {

					break;
				}

				guesses.add(nextBestGuess);
			}
		}

		return howMany;
	}

	private String selectCandidate() {

		Set<String> candidates = findCandidates();

		if (0 == candidates.size()) {

			return null;
		}

		List<String> rankings = rankCandidatesByLetterDistribution(candidates);

		// just select the one at the front of the list

		return rankings.get(0);
	}

	private void debug1(Properties optional) {

		for (char ch : alphabet.toCharArray()) {

			containsChars = new char[] { ch };

			Set<String> candidates = findCandidates();

			int size = candidates.size();

			if (null == optional) {

				System.err.println(ch + "=" + size);

			} else {

				optional.setProperty(String.valueOf(ch), String.valueOf(size));
			}
		}
	}

	private void debug2() {

		// "thump";
		// "blown";
		// "dirge";

		String bestWordSoFar[] = { "", "", "" };

		int lowestSoFar = 0;
		int higestTries = 0;
		int failCount = 0;
		int highestFailCount = 0;

		for (int index = 0; index < bestWordSoFar.length; index++) {

			if ("".equals(bestWordSoFar[index])) {

				// needs to be at least as big as the dictionary size
				lowestSoFar = 99999;
				highestFailCount = 99999;

				for (String word : words) {

					higestTries = 0;

					failCount = 0;

					for (String targetAnswer : words) {

						answer = targetAnswer;

						guesses.clear();

						for (int n = 0; n < index; n++) {

							guesses.add(bestWordSoFar[n]);
						}

						guesses.add(word);

						int tries = howManyGuesses();

						if (tries > 6) {

							failCount++;
						}

						if (tries > higestTries) {

							higestTries = tries;
						}
					}

					StringBuffer sb = new StringBuffer();

					for (int n = 0; n < index; n++) {

						sb.append(bestWordSoFar[n]);
						sb.append("\t");
					}

					sb.append(word);
					sb.append("\t");

					boolean highLight = false;

					if (higestTries <= lowestSoFar) {

						if (higestTries < lowestSoFar) {

							lowestSoFar = higestTries;
							highestFailCount = 99999;
						}

						if (failCount < highestFailCount) {

							highestFailCount = failCount;
							bestWordSoFar[index] = word;
							highLight = true;
						}
					}

					System.err.println(
							sb.toString() + higestTries + " (" + failCount + ") " + (highLight ? " <----" : ""));
				}
			}
		}

		for (int g = 0; g < bestWordSoFar.length; g++) {

			System.err.println("guess" + (1 + g) + "=" + bestWordSoFar[g]);
		}

		// verify solution

		int n = 0;

		failCount = 0;

		for (String targetAnswer : words) {

			answer = targetAnswer;

			guesses.clear();

			for (int g = 0; g < bestWordSoFar.length; g++) {

				guesses.add(bestWordSoFar[g]);
			}

			int tries = howManyGuesses();

			n++;

			if (tries > 6) {

				failCount++;
				System.err.println(n + "\t" + answer + "\t" + tries);
			}
		}

		System.err.println("Failures: " + failCount);
	}

	private void debug3() {

		int[][] counts = new int[3][7];

		String[] startWords = new String[] { "thump", "blown", "dirge" };

		for (int n = 1; n < 4; n++) {

			counts[n - 1] = new int[] { 0, 0, 0, 0, 0, 0, 0 };

			for (String targetAnswer : words) {

				answer = targetAnswer;

				guesses.clear();

				for (int w = 0; w < n; w++) {

					guesses.add(startWords[w]);
				}

				int tries = howManyGuesses();

				counts[n - 1][tries - 1]++;
			}
		}

		System.err.println("#Tries\t" + startWords[0] + " +\t" + startWords[1] + " +\t" + startWords[2]);

		for (int c = 0; c < 7; c++) {

			System.err.println((c + 1) + "\t" + counts[0][c] + "\t" + counts[1][c] + "\t" + counts[2][c]);
		}
	}

}
