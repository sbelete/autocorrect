package edu.brown.cs.sbelete.autocorrect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Stream;

public class Autocorrect extends Trie {

	private static int led = 0; // Default LED
	private boolean useSmart = false;
	private boolean useWhitespace = false;
	private boolean useAutocomplete = false;
	private static Map<String, Gram> frequency = new HashMap<>();

	private class Gram {
		private Map<String, Integer> coupled = new HashMap<>();
		private int times;

		public Gram() {
			times = 1;
		}

		public void inc() {
			times++;
		}

		public void next(String after) {
			Integer num = coupled.get(after);
			if (num != null) {
				coupled.put(after, num + 1);
			} else {
				coupled.put(after, 1);
			}
		}

		@SuppressWarnings("unused")
		public Set<String> getFreq(int i) {
			Map<String, Integer> result = new LinkedHashMap<>();
			Stream<Map.Entry<String, Integer>> st = coupled.entrySet().stream();

			st.sorted(Map.Entry.comparingByValue()).forEachOrdered(
					e -> result.put(e.getKey(), e.getValue()));

			return result.keySet();
		}

		public int getFreq(String s) {
			Integer i = coupled.get(s);
			if (i != null) {
				return i;
			}
			return 0;
		}

	}

	/**
	 * Constructor for Autocorrect uses Trie constructor.
	 *
	 * @param words
	 *            - a collection of words to be saved
	 */
	public Autocorrect(Collection<String> words) {
		super(words);
		Gram temp;
		Gram prev = null;
		for (String w : words) {
			temp = frequency.get(w);
			if (temp != null) {
				temp.inc();
			} else {
				temp = frequency.put(w, new Gram());
			}

			if (prev != null) {
				prev.next(w);
			}
			prev = temp;
		}
	}

	private class Frequency implements Comparator<String> {
		private String prev;
		private String current;

		public Frequency(String current, String prev) {
			this.prev = prev;
			this.current = current;
		}

		@Override
		public int compare(String w1, String w2) {
			// less = negative
			String o1 = w1.split(" ")[0];
			String o2 = w2.split(" ")[0];
			if (o1.equals(current)) {
				return -1;
			}
			if (o2.equals(current)) {
				return 1;
			}

			try {
				if (prev != null && !prev.equals("") && !prev.equals(" ")) {
					Gram followed = Autocorrect.frequency.get(prev);

					if (followed != null) {
						int freq1 = followed.getFreq(o1);
						int freq2 = followed.getFreq(o2);

						if (freq1 != freq2) {
							return freq2 - freq1;
						}
					}
				}

				Gram shown1 = Autocorrect.frequency.get(o1);
				Gram shown2 = Autocorrect.frequency.get(o2);

				if (shown1 != null && shown2 != null
						&& shown1.times != shown2.times) {
					return shown2.times - shown1.times;
				}

			} catch (NullPointerException e) {
				System.out.println(o1.length() + " " + o2);
				System.out.println("Error: " + e.toString());
			}
			return o1.compareTo(o2);
		}
	}

	/**
	 * @exception IllegalArgumentException
	 *                if led negative
	 * @param led
	 *            - is the distance we are willing to search
	 */
	public void setLed(int led) {
		if (led < 0) {
			throw new IllegalArgumentException("Can't set LED to < 0");
		}

		Autocorrect.led = led;
	}

	/**
	 * Makes led accessible
	 *
	 * @return - returns the led
	 */
	public int getLed() {
		return led;
	}

	/**
	 * Creates a list of words in within set led
	 *
	 * @param word
	 *            - word to be compared to
	 * @return - a list of words within set led to word
	 */
	public List<String> suggestLed(String word) {
		List<String> suggestions = new ArrayList<>();

		// Returns only exact word if led is zero
		if (led == 0) {
			if (contains(word)) {
				suggestions.add(word);
			}

			return suggestions;
		}

		char[] letters = (" " + word).toCharArray();
		int[][] ledValue = new int[letters.length + led][letters.length];

		for (int y = 1; y < letters.length; y++) {
			ledValue[0][y] = y;
		}

		StringBuilder sb = new StringBuilder(" ");

		getBase().entrySet().forEach(i -> {
			sb.append(i.getKey());

			// calls a helper
				suggestLedHelper(i.getValue(), letters, ledValue, sb,
						suggestions);

				sb.deleteCharAt(sb.length() - 1); // removes the last character
			});

		return suggestions;
	}

	// Helper function for suggestLedHelper
	private static void suggestLedHelper(TrieNode node, char[] letters,
			int[][] ledValue, StringBuilder sb, List<String> suggestions) {
		int sL = sb.length() - 1;
		// base for the recursive loop
		if (sL + 1 > ledValue.length) {
			return;
		}

		int length = letters.length;

		ledValue[sL][0] = sL;
		int min = sL;

		for (int k = 1; k < length; k++) {
			ledValue[sL][k] = ledValue[sL - 1][k - 1];

			if (sb.charAt(sL) != letters[k]) {
				ledValue[sL][k] = Math.min(
						Math.min(ledValue[sL][k], ledValue[sL - 1][k]),
						ledValue[sL][k - 1]) + 1;
			}

			// setting the new min
			if (ledValue[sL][k] < min) {
				min = ledValue[sL][k];
			}
		}

		// If word is in the led then add it to suggestions
		if (ledValue[sL][length - 1] <= led && node.validWord()) {
			suggestions.add(sb.toString().substring(1));
		}

		// Check to see if we are at max distance allowed if not go further
		if (min <= led) {
			for (Entry<Character, TrieNode> e : node.entrySet()) {
				sb.append(e.getKey());

				// Recursive call
				suggestLedHelper(e.getValue(), letters, ledValue, sb,
						suggestions);

				sb.deleteCharAt(sb.length() - 1);
			}
		}
	}

	/**
	 * Finds if a word could use a space
	 *
	 * @param s
	 *            - a word that could have a space
	 * @return - a list of two words
	 */
	public List<String> whitespace(String s) {
		List<String> retSug = new ArrayList<String>();

		// Checks all possible subsets of words
		for (int i = 1; i < s.length(); i++) {
			String w1 = s.substring(0, i);
			String w2 = s.substring(i);

			if (contains(w1) && contains(w2)) {
				retSug.add(w1 + " " + w2);
			}
		}

		return retSug;
	}

	/**
	 * Creates a list of suggested words through auto complete
	 *
	 * @param s
	 *            - string that is prefix of the word
	 * @return - a list of strings that start with the string given
	 */
	public List<String> autocomplete(String s) {
		List<String> suggestions = new ArrayList<String>();
		TrieNode node = getNode(s.trim()); // finds string in node

		if (node == null) {
			return suggestions;
		}

		Iterator<String> iter = new TrieIterator(node, s);
		while (iter.hasNext()) {
			suggestions.add(iter.next()); // add following nodes from current
		}

		return suggestions;
	}

	public static String regex1 = "[^A-Za-z']+";
	public static String regex2 = "[^A-Za-z]+";

	public List<String> wash(String word) {
		List<String> parsed = new ArrayList<String>();
		String[] keepTogether = word.toLowerCase().trim().split(" ");
		for (String w : keepTogether) {
			parsed.add(w.trim().replaceAll(regex2, " "));
		}
		return parsed;
	}

	public static List<String> washLine(String word) {

		List<String> parse = new ArrayList<String>();
		String[] keepTogether = word.toLowerCase().trim()
				.replaceAll(regex1, " ").split(" ");
		for (String w : keepTogether) {
			parse.add(w.trim().replaceAll(regex2, " ").trim());
		}

		return parse;
	}

	public List<String> suggest(String word, String prev) {
		// Comparator for how to sort suggestions
		word = word.trim();
		// Comparator for how to sort suggestions
		Comparator<? super String> comp = new Frequency(word, prev);
		LinkedHashSet<String> suggestions = new LinkedHashSet<String>();
		if (contains(word)) {
			suggestions.add(word);
		}

		if (useAutocomplete) {
			suggestions.addAll(autocomplete(word));
		}

		if (led > 0) {
			suggestions.addAll(suggestLed(word));
			// comp = new Led(word);
		}

		if (useWhitespace) {
			suggestions.addAll(whitespace(word));
		}
		if (useSmart) {
			comp = new Led(word);
		}

		List<String> retSuggestions = new ArrayList<String>(suggestions);

		if (comp != null) {
			Collections.sort(retSuggestions, comp);
		}

		return retSuggestions;
	}

	public void setSmart(int value) {
		if (value == 0) {
			useSmart = false;
		} else if (value == 1) {
			useSmart = true;
		}
	}

	public boolean getSmart() {
		return useSmart;
	}

	public boolean getWhitespace() {
		return useWhitespace;
	}

	public void setWhitespace(int value) {
		if (value == 0) {
			useWhitespace = false;
		} else if (value == 1) {
			useWhitespace = true;
		}
	}

	public boolean getAutocomplete() {
		return useAutocomplete;
	}

	public void setAutocomplete(int value) {
		if (value == 0) {
			useAutocomplete = false;
		} else if (value == 1) {
			useAutocomplete = true;
		}
	}
}
