package uk.co.myzen.a_z;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
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

	private static final String DEFAULT_TXT = "wordle.txt";

	private static final String[] WORDLE_START_WORDS = { "thump", "blown", "dirge" };

	private static final String[] FIVE_START_WORDS = { "frump", "thegn", "sloyd", "wacke" };

	private static final String[] SCHOLARDLE_START_WORDS = { "biskup", "lenght", "remove" };

	private static final String alphabet = "abcdefghijklmnopqrstuvwxyz";

	private static int wordLength = 0;

	private static enum Action {

		DEFAULT, ABORT, SHOW_HELP, GUESS_TO_ANSWER, DEBUG_1, DEBUG_2, DEBUG_3
	};

	private static Action action = Action.SHOW_HELP;

	private static Map<String, List<String>> wordsMap = new HashMap<String, List<String>>();

	private static Map<String, String> existingResults = new HashMap<String, String>();

	private final List<String> words;

	private final String resourceName;

	private final int[] letterDistributionRanks;

	int debug = 0;

	List<String> guesses = new ArrayList<String>(6);

	private PrintStream output = System.err;

	private boolean showWords = true;

	private boolean showRank = true;

	private char[] containsChars = {};

	private char[] notChars = {};

	private String[] notN;

	private char positions[];

	private String answer = "";

	private String[] resetStringArray() {

		String result[] = new String[wordLength];

		for (int w = 0; w < wordLength; w++) {

			result[w] = "";
		}

		return result;
	}

	private char[] resetCharArray() {

		char result[] = new char[wordLength];

		for (int w = 0; w < wordLength; w++) {

			result[w] = ' ';
		}

		return result;
	}

	private WordlInSix() throws Exception {

		this(DEFAULT_TXT);
	}

	WordlInSix(String name) throws Exception {

		resourceName = name;

		words = loadWords(name);

		notN = resetStringArray();

		positions = resetCharArray();

		int lp = name.lastIndexOf('.');

		String propertyResourceName = name.substring(0, lp) + ".properties";

		letterDistributionRanks = loadLetterDistributionRanks(propertyResourceName);
	}

	private int[] loadLetterDistributionRanks(String name) {

		int[] frequencies = new int[26];

		Properties properties = new Properties();

		ClassLoader cl = Thread.currentThread().getContextClassLoader();

		InputStream is = cl.getResourceAsStream(name);

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

		try {
			if (0 == args.length) {

				main = new WordlInSix(null); // will use DEFAULT_TXT & DEFAULT_PROPERTIES

				action = Action.SHOW_HELP;

			} else {

				// if first parameter is "wordle" (also the default) then load "wordle.txt" &
				// "wordle.properties"
				// an alternative is "scholardle"

				String game = -1 == args[0].indexOf('=') ? args[0] : null;

				main = null == game ? new WordlInSix() : new WordlInSix(game + ".txt");

				if (main.words == null || 0 == main.words.size()) {

					action = Action.ABORT;

				} else {

					action = Action.DEFAULT;

					for (String arg : args) {

						main.parameter(arg);
					}

					if (!main.validatedParameters()) {

						action = Action.ABORT;
					}
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

		} catch (Exception e) {

			System.err.println("Exception: " + e.getMessage());
		}
	}

	private void help() {

		System.out.println("Help with parameters:");
		System.out.println("\tMake the first parameter wordle or scholardle to play different variations of the game");
		System.out.println(
				"\tThe columns are implicitly numbered left to right 1 through 5: thus 1 is first and 5 is last");
		System.out.println("\tUse 1=b to indicate first letter is definitely 'b'");
		System.out.println("\tEliminate letters by using not=abcdefg etc.");
		System.out.println("\tUse 1=a to indicate first letter is definitely 'a'");
		System.out.println("\tUse 5=z to indicate last letter is definitely 'z'");
		System.out.println("\tUse 2=j 3=k 4=l to indicate letter 'j' is in column 2 and 'k' in column 3 and 'l' in 4");
		System.out.println("\tUse contains=iou to indicate letters 'i' and 'o' and 'u' *must* appear in the word");
		System.out.println("\tUse not2=ab to indicate second letter cannot be 'a' or 'b'");
		System.out.println("\tUse not5=y to indicate last letter cannot be 'y'");
		System.out.println("\tUse words=no if you don't want to see possible words");
		System.out.println("\tUse rank=no if you don't want a clue to the likelihood of word possibilities");
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

	private void setOutput(File filePrintStream) {

		if (null == filePrintStream) {

			output = System.err;

		} else {

			try {

				output = new PrintStream(filePrintStream);

			} catch (FileNotFoundException e) {

				e.printStackTrace();

				output = System.err;
			}
		}
	}

	private void parameter(String arg) {

		int indexOfEqualsChar = arg.indexOf('=');

		String value = arg.substring(1 + indexOfEqualsChar);

		setOutput(null); // default initially

		if (arg.startsWith("output=")) {

			File filePrintStream = new File(value.trim());

			setOutput(filePrintStream);

		} else if (arg.startsWith("debug=")) {

			int delimiter = value.indexOf('-');

			debug = Integer.parseInt(-1 == delimiter ? value : value.substring(0, delimiter));

			if (delimiter > -1) {

				File debugExtraA = new File(value.substring(1 + delimiter));

				try {

					FileReader fr = new FileReader(debugExtraA);

					BufferedReader br = new BufferedReader(fr);
					String line;

					while (null != (line = br.readLine())) {

						int lastTab = line.lastIndexOf('\t');

						if (lastTab < 0) {

							continue;
						}

						String key = line.substring(0, lastTab);

						String val = line.substring(1 + lastTab);

						existingResults.put(key, val);
					}

					br.close();

					fr.close();

				} catch (FileNotFoundException e) {

				} catch (IOException e) {

				}
			}

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

	private static List<String> loadWords(String name) throws Exception {

		List<String> result;

		if (wordsMap.containsKey(name)) {

			result = wordsMap.get(name);

		} else {

			result = new ArrayList<String>(2315);

			ClassLoader cl = Thread.currentThread().getContextClassLoader();

			InputStream is = cl.getResourceAsStream(name);

			if (null == is) {

				System.err.println("Cannot locate resource '" + name
						+ "' Did you perhaps mean to use wordle or scholardle for the first parameter?");

			} else {

				InputStreamReader isr = null;

				BufferedReader br = null;

				try {

					isr = new InputStreamReader(is);

					br = new BufferedReader(isr);

					String ln;

					while (null != (ln = br.readLine())) {

						if (ln.startsWith("#")) {

							continue; // ignore comments in property file
						}

						String words[] = ln.split("\\s+");

						for (String word : words) {

							if (word.length() != wordLength) {

								if (0 == wordLength) {

									wordLength = word.length();
								} else {

									throw new Exception("Inconsistent resource file of words");
								}
							}

							result.add(word.toLowerCase());
						}
					}

					// all good

					wordsMap.put(name, result);

				} catch (IOException e) {

				} finally {

					try {
						if (null != br) {

							br.close();
						}

						if (null != isr) {

							isr.close();
						}

					} catch (IOException e) {

						e.printStackTrace();
					}
				}
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

			for (int index = 0; index < wordLength; index++) {

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

			for (int index = 0; index < wordLength; index++) {

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

				if (DEFAULT_TXT.equals(resourceName)) { // improve chances by prioritising certain words

					if ("thump".equals(guesses.get(0))) {

						if (guesses.size() > 1) {

							if (!"blown".equals(guesses.get(1))) {

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

		notN = resetStringArray();

		char emptyPositions[] = resetCharArray();

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

			for (int n = 0; n < wordLength; n++) {

				if (' ' != positions[n]) {

					suggestive.append(1 + n);
					suggestive.append("=");
					suggestive.append(positions[n]);
					suggestive.append(' ');
				}
			}

			for (int n = 0; n < wordLength; n++) {

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

		int r = 0; // By default just select the one at the front of the list

		return rankings.get(r);
	}

	private void debug1(Properties optional) {

		for (char ch : alphabet.toCharArray()) {

			containsChars = new char[] { ch };

			Set<String> candidates = findCandidates();

			int size = candidates.size();

			if (null == optional) {

				output.println(ch + "=" + size);

			} else {

				optional.setProperty(String.valueOf(ch), String.valueOf(size));
			}
		}
	}

	void debug2() {

		String bestWordSoFar[] = new String[5];

		int beginIndex = 0;

		while (guesses.size() > beginIndex && null != guesses.get(beginIndex)) {

			bestWordSoFar[beginIndex] = guesses.get(beginIndex);

			beginIndex++;
		}

		int lowestSoFar = 0;
		int higestTries = 0;
		int failCount = 0;
		int highestFailCount = 0;

		for (int index = beginIndex; index < bestWordSoFar.length; index++) {

			if (null == bestWordSoFar[index]) {

				// needs to be at least as big as the dictionary size
				lowestSoFar = 99999;
				highestFailCount = 99999;

				for (String word : words) {

					if (guesses.contains(word)) {

						// we've already used this word: ignore

						continue;
					}

					higestTries = 0;

					failCount = 0;

					StringBuffer sbExisting = new StringBuffer();

					for (int n = 0; n < index; n++) {

						sbExisting.append(bestWordSoFar[n]);

						sbExisting.append('\t');
					}

					sbExisting.append(word);

					String existingKey = sbExisting.toString();

					if (existingResults.containsKey(existingKey)) {

						// avoid time consuming test of every target answer

						String alreadyDone = existingResults.get(existingKey);

						int posOpenBracket = alreadyDone.indexOf('(');
						int posCloseBracket = alreadyDone.indexOf(')', posOpenBracket);

						higestTries = Integer.parseInt(alreadyDone.substring(0, posOpenBracket - 1));

						failCount = Integer.parseInt(alreadyDone.substring(posOpenBracket + 1, posCloseBracket));

					} else {

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
					}

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

					output.println(
							existingKey + "\t" + higestTries + " (" + failCount + ") " + (highLight ? " <----" : ""));
				}

				if (0 == highestFailCount) {

					break;
				}
			}
		}

		for (int g = 0; g < bestWordSoFar.length; g++) {

			if (null == bestWordSoFar[g]) {

				break;
			}

			output.println("guess" + (1 + g) + "=" + bestWordSoFar[g]);
		}

		// verify solution

		int n = 0;

		failCount = 0;

		for (String targetAnswer : words) {

			answer = targetAnswer;

			guesses.clear();

			for (int g = 0; g < bestWordSoFar.length; g++) {

				if (null == bestWordSoFar[g]) {

					break;
				}

				guesses.add(bestWordSoFar[g]);
			}

			int tries = howManyGuesses();

			n++;

			if (tries > 6) {

				failCount++;
				output.println(n + "\t" + answer + "\t" + tries);
			}
		}

		output.println("Failures: " + failCount);
	}

	private void debug3() throws Exception {

		String[] startWords = new String[0];

		int y = 0;

		final int size = guesses.size();

		if (0 == size) {

			if ("wordle.txt".equals(resourceName)) {

				startWords = WORDLE_START_WORDS;

				y = 7; // demonstrate only 6 guesses needed for wordle

			} else if ("scholardle.txt".equals(resourceName)) {

				startWords = SCHOLARDLE_START_WORDS;

				y = 8; // demonstrate only 7 guesses needed for "new" scholardle

			} else if ("five.txt".equals(resourceName)) {

				startWords = FIVE_START_WORDS;

				y = 10; // demonstrate only 9 guesses needed for "old" scholardle
			}
		}

		if (0 == y) {

			// assume parameters passed are guess1=X guess2=Y guess3=Z ... guessN = etc

			startWords = new String[size];

			for (int w = 0; w < size; w++) {

				startWords[w] = guesses.get(w);
			}

			y = 10; // limit the number of tries
		}

		int x = startWords.length;

		int[] failCounts = new int[x];

		int[][] counts = new int[x][y];

		for (int n = 1; n < 1 + x; n++) {

			counts[n - 1] = new int[y];

			for (String targetAnswer : words) {

				answer = targetAnswer;

				guesses.clear();

				for (int w = 0; w < n; w++) {

					guesses.add(startWords[w]);
				}

				int tries = howManyGuesses();

				if (tries > y) {

					int temp[] = counts[n - 1];

					counts[n - 1] = new int[tries];

					for (int t = 0; t < y; t++) {

						counts[n - 1][t] = temp[t];
					}

					for (int t = y; t < tries; t++) {

						counts[n - 1][t] = 0;
					}

					y = tries;
				}

				counts[n - 1][tries - 1]++;
			}

			for (int f = 6; f < y; f++) {

				failCounts[n - 1] += counts[n - 1][f];
			}
		}

		output.print("#Tries");

		for (int z = 0; z < x; z++) {

			output.print("\t" + startWords[z]);
		}

		output.print("\n");

		for (int c = 0; c < y; c++) {

			output.print((c + 1));

			for (int z = 0; z < x; z++) {

				output.print("\t" + counts[z][c]);
			}

			output.print("\n");
		}

		output.print("===== ");

		for (int z = 0; z < x; z++) {

			output.print("======= ");
		}
		output.print("\n");

		output.print("%PASS");

		final float s = words.size();

		for (int z = 0; z < x; z++) {

			float f = 100 - ((float) failCounts[z] / s);

			output.print(String.format("  %3.3f", f));
		}
		output.print("\n");

		output.print("FAIL");

		for (int z = 0; z < x; z++) {

			output.print(String.format("%8s", "(" + failCounts[z] + ")"));
		}
		output.print("\n");
	}

}
