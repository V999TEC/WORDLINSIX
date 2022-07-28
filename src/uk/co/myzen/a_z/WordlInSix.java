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
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

public class WordlInSix {

	private static Map<Integer, Result> results = null;

	private static final String DEFAULT_TXT = "wordle.txt";

	private static final String[] WORDLE_START_WORDS = { "thump", "blown", "dirge" };

	private static final String[] FIVE_START_WORDS = { "frump", "thegn", "sloyd", "wacke" };

	private static final String[] SCHOLARDLE_START_WORDS = { "biskup", "lenght", "warmed" };

	private static final String alphabet = "abcdefghijklmnopqrstuvwxyz";

	private static enum Action {

		DEFAULT, ABORT, SHOW_HELP, GUESS_TO_ANSWER, DEBUG_1, DEBUG_2, DEBUG_3
	};

	private static Action action = Action.SHOW_HELP;

	private static Map<String, List<String>> wordsMap = new HashMap<String, List<String>>();

	private static Map<String, String> existingResults = new HashMap<String, String>();

	private static long msMainInstanceStart;

	private static WordlInSix mainInstance = null;

	private static List<WordlInSix> instances = null;

	private static Integer threads = 0;

	private static int debug = 0;

	private static String resourceName;

	private static int[] letterDistributionRanks;

	private static int wordLength = 0;

	private final Thread thread;

	private final List<String> words;

	private int higestTries = 0;

	private int highestFailCount = 0;

	private String bestWordSoFar[] = new String[5];

	private boolean terminate = false;

	private boolean waiting = false;

	private boolean ai = false;

	private List<String> guesses = new ArrayList<String>(6);

	private PrintStream output = System.err;

	private boolean showWords = true;

	private boolean showRank = true;

	private char[] containsChars = {};

	private char[] notChars = {};

	private String[] notN;

	private char positions[];

	private String answer = "";

	private static String[] resetStringArray() {

		String result[] = new String[wordLength];

		for (int w = 0; w < wordLength; w++) {

			result[w] = "";
		}

		return result;
	}

	private static char[] resetCharArray() {

		char result[] = new char[wordLength];

		for (int w = 0; w < wordLength; w++) {

			result[w] = ' ';
		}

		return result;
	}

	private WordlInSix() throws Exception {

		this(DEFAULT_TXT);
	}

	private WordlInSix(String name) throws Exception {

		msMainInstanceStart = System.currentTimeMillis();

		thread = Thread.currentThread();

		resourceName = name;

		words = loadWords(name);

		notN = resetStringArray();

		positions = resetCharArray();

		int lp = name.lastIndexOf('.');

		String propertyResourceName = name.substring(0, lp) + ".properties";

		letterDistributionRanks = loadLetterDistributionRanks(propertyResourceName);
	}

	private WordlInSix(int subset, final int threads) {

		thread = Thread.currentThread();

		output = mainInstance.output;

		int mainWordListSize = mainInstance.words.size();

		int subsetSize = mainWordListSize / threads;

		int lo = (subset - 1) * subsetSize;

		int hi = threads == subset ? mainWordListSize : subset * subsetSize;

		System.out.println(thread.getName() + ":" + "\tlo:" + lo + "\thi:" + hi + "\t" + mainInstance.words.get(lo)
				+ "\t" + mainInstance.words.get(hi - 1));

		words = new ArrayList<String>(hi - lo);

		for (int index = lo; index < hi; index++) {

			words.add(mainInstance.words.get(index));
		}

		notN = resetStringArray();

		positions = resetCharArray();
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

		// WordlInSix main;

		try {
			if (0 == args.length) {

				mainInstance = new WordlInSix(null); // will use DEFAULT_TXT & DEFAULT_PROPERTIES

				action = Action.SHOW_HELP;

			} else {

				// if first parameter is "wordle" (also the default) then load "wordle.txt" &
				// "wordle.properties"
				// an alternative is "scholardle"

				String game = -1 == args[0].indexOf('=') ? args[0] : null;

				mainInstance = null == game ? new WordlInSix() : new WordlInSix(game + ".txt");

				if (mainInstance.words == null || 0 == mainInstance.words.size()) {

					action = Action.ABORT;

				} else {

					action = Action.DEFAULT;

					for (String arg : args) {

						mainInstance.parameter(arg);
					}

					if (!mainInstance.validatedParameters()) {

						action = Action.ABORT;
					} else {

					}
				}
			}

			switch (action) {

			case DEBUG_1:

				mainInstance.debug1(null); // display letter frequency
				break;

			case DEBUG_2:

				results = new HashMap<Integer, Result>();

				if (threads > 0) {

					// launch threads if required

					instances = new ArrayList<WordlInSix>(threads);

					for (int thread = 0; thread < threads; thread++) {

						final int subset = 1 + thread;

						Thread e = new Thread() {

							@Override
							public void run() {

								try {

									WordlInSix instance = new WordlInSix(subset, threads);

									instances.add(instance);

									instance.debug2();

								} catch (Exception e) {

									e.printStackTrace();
								}
							}
						};

						e.start();
					}

				} else {

					mainInstance.debug2();
				}

				break;

			case DEBUG_3:

				mainInstance.debug3();
				break;

			case GUESS_TO_ANSWER:

				mainInstance.guessToAnswer();
				break;

			case ABORT:
			case SHOW_HELP:

				mainInstance.help();
				break;

			case DEFAULT:
			default:

				Set<String> candidates = mainInstance.findCandidates();

				// display the candidate words (if words=true | words=yes )

				StringBuffer sb = new StringBuffer();

				for (String arg : args) {

					sb.append(arg);
					sb.append(' ');
				}

				System.err.println("There are " + candidates.size() + " word(s) matching " + sb.toString());

				mainInstance.report(candidates);

				break;
			}
		} catch (Exception e) {

			System.err.println("Exception: " + e.getMessage());

			e.printStackTrace();
		}

	}

	private char[] inferenceCheck() {

		boolean inference = false;

		char[] result = resetCharArray();

		BitSet flags = new BitSet(wordLength);

		flags.set(0, wordLength); // all true initially

		for (int colIndex = 0; colIndex < wordLength; colIndex++) {

			if (' ' != positions[colIndex]) {

				flags.clear(colIndex);
			}
		}

		if (1 == flags.cardinality()) {

			// only 1 column is unknown (the one at flags.nextSetBit(0))

			int emptyIndex = flags.nextSetBit(0);

			// see if there are any columns according to notN[0..wordLength] that only a
			// certain letter is possible
			// out of the subset we know in containsChars

			// go through the contains and see which columns each ch can be in
			// ignore column if already explicitly n=ch

			for (char ch : containsChars) {

				flags.set(0, wordLength); // all set again

				for (int colIndex = 0; colIndex < wordLength; colIndex++) {

					if (-1 != notN[colIndex].indexOf(ch) || (positions[colIndex] != ch && ' ' != positions[colIndex])) {
						// implies ch cannot be in this column

						flags.clear(colIndex);
					}
				}

				if (1 == flags.cardinality() && emptyIndex == flags.nextSetBit(0)) {

					result[emptyIndex] = ch;

					inference = true;

					break;
				}
			}
		}

		return inference ? result : null;
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

			char[] inferred = null;

			if (ai) {

				inferred = inferenceCheck();

				if (null != inferred) {

					int col = 1;

					for (char ch : inferred) {

						if (' ' != ch) {

							System.err.println("Column " + col + " must be '" + ch + "' by deduction");
						}
						col++;
					}
				}
			}

			int count = 0;

			if (showRank) {

				List<String> rankings = rankCandidatesByLetterDistribution(candidates);

				for (String candidate : rankings) {

					int uniqueLetters = countDuplicateLetters(candidate);

					int vowels = countVowels(candidate);

					boolean flag = rejectDueToInference(candidate, inferred);

					System.err.println(
							(++count) + "\t" + candidate + "\t" + vowels + "\t" + uniqueLetters + (flag ? "\tX" : ""));
				}

			} else {

				Set<String> sortedSet = new TreeSet<String>(candidates);

				for (String candidate : sortedSet) {

					System.err.println((++count) + "\t" + candidate);
				}
			}
		}

	}

	private boolean rejectDueToInference(String candidate, char[] inferred) {

		boolean disregard = false;

		if (null != inferred) {

			int colIndex = 0;

			for (char ch : inferred) {

				if (' ' != ch) {

					if (candidate.charAt(colIndex) != ch) {

						System.out.println("Disregarding candidate " + candidate + " due to inference");

						disregard = true;
						break;
					}
				}

				colIndex++;
			}
		}

		return disregard;
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

		if (arg.startsWith("threads=")) {

			threads = Integer.parseInt(null == value ? "0" : value);

		} else if (arg.startsWith("output=")) {

			File filePrintStream = new File(value.trim());

			setOutput(filePrintStream);

		} else if (arg.startsWith("ai=")) {

			ai = "true".equalsIgnoreCase(value);

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

			for (int n = 0; n < wordLength; n++) {

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

		String contains = new String(containsChars);

		String not = new String(notChars);

		for (String word : mainInstance.words) {

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

				new Exception("FATAL: contact author key:null rank5:" + rank5 + " exit:" + candidates.size())
						.printStackTrace();
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

			String candidate = guesses.get(index);

			scoreAgainstKnownAnswer(candidate);

			StringBuffer suggestive = new StringBuffer();

			for (int n = 0; n < guesses.size(); n++) {

				String guess = guesses.get(n);

				if (!"".equals(guess)) {

					suggestive.append("guess");
					suggestive.append(1 + n);
					suggestive.append("=");
					suggestive.append(guess);
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

			////
			char[] inferred = null;

			if (ai) {

				inferred = inferenceCheck();
			}

			if (0 == debug) {

				if (null != inferred) {

					int col = 1;

					for (char ch : inferred) {

						if (' ' != ch) {

							suggestive.append("(" + col + "=" + ch + " by deduction)");
						}
						col++;
					}
				}

				System.err.println(suggestive.toString());
			}

			howMany = guesses.size();

			// check if the latest guess happens to be the answer

			if (guesses.get(howMany - 1).equals(answer)) {

				break;
			}

			if (index + 1 == howMany) {

				nextBestGuess = selectCandidate(inferred);

				if (null == nextBestGuess) {

					break;
				}

				guesses.add(nextBestGuess);
			}
		}

		return howMany;
	}

	private String selectCandidate(char[] inferred) {

		Set<String> candidates = findCandidates();

		if (0 == candidates.size()) {

			return null;
		}

		List<String> rankings = rankCandidatesByLetterDistribution(candidates);

		int r = 0; // By default just select the one at the front of the list

		if (null != inferred) {

			do {

				// before adding as a candidate check for inferences
				// that would eliminate the prime candidate

				String primeCandidate = rankings.get(r);

				boolean disregard = false;

				int colIndex = 0;

				for (char ch : inferred) {

					if (' ' != ch) {

						if (primeCandidate.charAt(colIndex) != ch) {

							System.out.println("Disregarding candidate " + primeCandidate + " due to inference");

							disregard = true;
							break;
						}
					}

					colIndex++;
				}

				if (!disregard) {

					break;
				}

				r++;

			} while (r < rankings.size() - 1);
		}

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

		// String bestWordSoFar[] = new String[5]; // no point in having beyond 5 start
		// words, usually 1 is enough

		int beginIndex = 0;

		while (guesses.size() > beginIndex && null != guesses.get(beginIndex)) {

			bestWordSoFar[beginIndex] = guesses.get(beginIndex);

			beginIndex++;
		}

		int failCount = 0;

		Integer bestResult = null;

		for (int index = beginIndex; index < bestWordSoFar.length; index++) {

			if (null == bestWordSoFar[index]) {

				Integer lowestWordScore = Integer.MAX_VALUE;

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

						for (String targetAnswer : mainInstance.words) {

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

					Integer currentWordScore = 1000 * higestTries + failCount; // bug assumption: failcount < 1000

					boolean highLight = false;
					boolean equalMerit = false;

					if (currentWordScore < lowestWordScore) {

						lowestWordScore = currentWordScore;

						bestWordSoFar[index] = word;

						highLight = true;

					} else if (currentWordScore == lowestWordScore) {

						equalMerit = true;
					}

					output.println(existingKey + "\t" + higestTries + " (" + failCount + ") "
							+ (highLight ? " <----" + thread.getName()
									: (equalMerit ? " <-==-" + thread.getName() : "")));

					if (null != results && (highLight || equalMerit)) {

						if (results.containsKey(lowestWordScore)) {

							Result result = results.get(lowestWordScore);

							result.add(word);

						} else {

							Result result = new Result(word, higestTries, failCount);

							results.put(lowestWordScore, result);
						}
					}
				}

				if (lowestWordScore < 6001) {

					// found a solution :-)

					terminate = true;
					break;
				}
			}

			// wait for concurrent thread(s) to catch up to this point

			// are we the last thread that is not waiting?

			if (null != instances) {

				for (WordlInSix wis : instances) {

					if (wis.thread == thread) {

						continue; // ignore ourself as we implicitly know we are not waiting
					}

					if (!wis.waiting) { // another thread is still active: we can wait

						waiting = true;
						break;
					}
				}
			}

			if (!waiting) {

				// assume all the other threads are in a waiting state
				// furthermore assume our Results map is complete at the current index level

				// find the best result so far (the on with the lowest key)

				Integer winnerKey = Integer.MAX_VALUE;

				for (Integer key : results.keySet()) {

					if (key < winnerKey) {

						winnerKey = key;
					}
				}

				// has this improved upon the previous level's best word
				// (i.e., index-1 where index >0) ?

				if (null == bestResult || winnerKey < bestResult) {

					bestResult = winnerKey;

				} else {

					terminate = true; // by default assume no improvement in score
				}

				if (terminate) {

					Result finalResult = results.get(bestResult);

					System.out.println(
							thread.getName() + ": at index " + index + " no further improvement in score better than ("
									+ finalResult.asStringKey() + ") " + finalResult.words.get(0));

					bestWordSoFar[index] = null;

				} else {

					Result winningResult = results.get(winnerKey);

					if (winningResult.words.size() > 1) {

						System.out.print(thread.getName() + ": at index " + index + " tie ("
								+ winningResult.asStringKey() + ") for optimal word: ");

						bestWordSoFar[index] = null; // hopefully replaced by a valid word

						for (int a = 0; a < winningResult.words.size(); a++) {

							boolean matchExistingGuess = false;

							for (int c = 0; c < index; c++) {

								if (bestWordSoFar[c].equals(winningResult.words.get(a))) {

									// disallow this option- can't choose a word already guessed

									matchExistingGuess = true;
									break;
								}
							}

							if (matchExistingGuess) {

								System.out.print(" (not: " + winningResult.words.get(a) + "), ");

							} else {

								bestWordSoFar[index] = winningResult.words.get(a);

								System.out.print(bestWordSoFar[index] + ", ");
							}
						}

						System.out.println("");

						if (null == bestWordSoFar[index]) {

							terminate = true;
						}

					} else {

						System.out.println(thread.getName() + ": at index " + index + " (" + winningResult.asStringKey()
								+ ")  optimal word: " + winningResult.words.get(0));

						bestWordSoFar[index] = winningResult.words.get(0);
					}

				}

				if (null != instances) {

					// now inject same results so far into all threads

					for (WordlInSix wis : instances) {

						for (int i = 0; i < (index + 1); i++) {

							wis.bestWordSoFar[i] = bestWordSoFar[i];

							if (terminate) {

								wis.bestWordSoFar[index] = null;
							}
						}
					}

					// we are now responsible for interrupting all the other waiting threads

					for (WordlInSix wis : instances) {

						if (wis.thread != thread) {

							if (terminate) {

								wis.terminate = true;
							}

							wis.thread.interrupt();
						}
					}

				}

			} else {

				do {

					// wait indefinitely until interrupt.
					// last remaining non-waiting thread is responsible for interrupting other
					// waiting threads

					try {

						System.out.println(thread.getName() + ": waiting to synchronise");
						Thread.sleep(2000 * threads);

					} catch (InterruptedException e) {

						System.out.println(thread.getName() + ": now awake");
						waiting = false;
					}

					if (terminate) {

						System.out.println(thread.getName() + ": about to terminate");
						break;
					}

				} while (waiting);
			}

			if (terminate) {

				break;
			}

		}

		// only one thread need do the following

		if (0 == threads || thread.equals(instances.get(0).thread)) {

			for (int g = 0; g < bestWordSoFar.length; g++) {

				if (null == bestWordSoFar[g]) {

					break;
				}

				output.println("guess" + (1 + g) + "=" + bestWordSoFar[g]);
			}

			// verify solution

			int n = 0;

			failCount = 0;

			for (String targetAnswer : mainInstance.words) {

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
			output.println("dictionary: " + resourceName);
			output.println("Threads: " + threads);

			long millis = System.currentTimeMillis() - msMainInstanceStart;

			String hms = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
					TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
					TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1));
			output.println("Elapsed: " + hms);
		}
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

			y = 15; // limit the number of tries
		}

		int x = startWords.length;

		int[] failCounts = new int[x];

		int[][] counts = new int[x][y];

		for (int n = 1; n < 1 + x; n++) {

			counts[n - 1] = new int[y];

			for (String targetAnswer : mainInstance.words) {

				answer = targetAnswer;

				guesses.clear();

				for (int w = 0; w < n; w++) {

					guesses.add(startWords[w]);
				}

				int tries = howManyGuesses();

				if (1 == tries && !answer.equals(startWords[0])) {

					System.err.println("Investigate guess1: " + answer + " (cannot be 1st try)");
				}

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

			if (0 == counts[0][c]) {

				break;
			}
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

			output.print(String.format(" %3.4f", f));
		}
		output.print("\n");

		output.print("FAIL");

		for (int z = 0; z < x; z++) {

			output.print(String.format("%8s", "(" + failCounts[z] + ")"));
		}
		output.print("\n");
	}

}
