package edu.brown.cs.sbelete.autocorrect;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to make for Led making it a comparable.
 *
 * @author Simon
 *
 */
public class Led implements Comparator<String> {

  private Map<String, Integer> led = new HashMap<>();
  private String word;

  /**
   * Constructor for Led
   *
   * @param word - word that is being compared to
   */
  public Led(String word) {
    this.word = word;
  }

  /**
   * Calculates the led between string and word.
   *
   * @param s string
   * @return - returns the led value
   */
  public int led(String s) {
    int length1 = s.length();
    int length2 = word.length();

    int[][] ledValue = new int[length1][length2];

    // initialize the ledValue
    for (int x = 1; x < length1; x++) {
      ledValue[x][0] = x;
    }
    for (int y = 1; y < length2; y++) {
      ledValue[0][y] = y;
    }

    // Step through the ledValue
    for (int i = 1; i < length1; i++) {
      for (int k = 1; k < length2; k++) {
        ledValue[i][k] = ledValue[i - 1][k - 1];

        if (s.charAt(i) != word.charAt(k)) {
          ledValue[i][k] = Math.min(Math.min(
            ledValue[i][k], ledValue[i - 1][k]),
            ledValue[i][k - 1]) + 1;
        }
      }
    }

    return ledValue[length1 - 1][length2 - 1];
  }

  @Override
  public int compare(String s1, String s2) {
    if (s1.startsWith(word)) {
      if (!s2.startsWith(word)) {
        return -1;
      }
    } else if (s2.startsWith(word)) {
      return 1;
    }

    int d1 = led.computeIfAbsent(s1, (String x) -> this.led(x));
    int d2 = led.computeIfAbsent(s2, (String y) -> this.led(y));

    if (d1 != d2) {
      return d1 - d2;
    }

    return s1.compareTo(s2);
  }
}
