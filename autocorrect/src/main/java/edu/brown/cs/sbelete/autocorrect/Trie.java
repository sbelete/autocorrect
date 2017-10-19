package edu.brown.cs.sbelete.autocorrect;

import java.lang.reflect.Array;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * Trie.
 *
 * @author Simon
 */
public class Trie implements Collection<String> {

  private int size = 0;
  private TrieNode base;

  /**
   * Returns the base of the Trie.
   *
   * @return - returns private root for the Trie
   */
  public TrieNode getBase() {
    return base;
  }

  @Override
  public int size() {
    return size;
  }

  /**
   * Constructor for Trie.
   *
   * @param words - a collection of strings
   */
  public Trie(Collection<String> words) {
    base = makeNode();

    addAll(words);
  }

  /**
   * Nested class for Trie defining the nodes.
   *
   * @author Simon
   *
   */
  protected class TrieNode extends HashMap<Character, TrieNode> {

    // Default serialVersionUID
    private static final long serialVersionUID = 1L;

    // Points to the parent TrieNode
    private TrieNode parent;

    // Check to see if a word exists
    private boolean word = false;

    /**
     * Constructor for TrieNode Uses HashMap constructor with parameters of
     * lowercase letters.
     */
    TrieNode() {
      super();
    }

    /**
     * To see if the word is valid.
     *
     * @return if word is valid
     */
    public boolean validWord() {
      return word;
    }

    /**
     * @return the parent
     */
    public TrieNode getParent() {
      return parent;
    }

    /**
     * @return the word
     */
    public boolean isWord() {
      return word;
    }
  }

  /**
   * Nested class for Trie allowing it to be Iterable.
   *
   * @author Simon
   *
   */
  protected class TrieIterator implements Iterator<String> {

    private String next;
    private StringBuilder sb = new StringBuilder();
    private Deque<Iterator<Entry<Character, TrieNode>>> q = new ArrayDeque<>();

    /**
     * Iterator for the Trie.
     *
     * @param node - trie node
     * @param s string
     */
    public TrieIterator(TrieNode node, String s) {
      sb.append(s);
      q.push(node.entrySet().iterator());

      if (node.isWord()) {
        next = s;
      } else {
        findNext();
      }
    }

    /**
     * Finds the next node.
     */
    private void findNext() {
      next = null;
      Iterator<Entry<Character, TrieNode>> iterator = q.peek();

      while (iterator != null) {
        while (iterator.hasNext()) {
          Entry<Character, TrieNode> item = iterator.next();

          char key = item.getKey();

          sb.append(key);
          TrieNode node = item.getValue();
          iterator = node.entrySet().iterator();

          q.push(iterator);

          if (node.isWord()) {
            next = sb.toString();
            return;
          }
        }

        q.pop();

        if (sb.length() > 0) {
          sb.deleteCharAt(sb.length() - 1);
        }

        iterator = q.peek();
      }
    }

    @Override
    public boolean hasNext() {
      return next != null;
    }

    @Override
    public String next() {
      String ret = next;
      findNext();
      return ret;
    }
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  /**
   * Creates TrieNodes.
   *
   * @return - constructed new Node
   */
  protected TrieNode makeNode() {
    return new TrieNode();
  }

  @Override
  public boolean contains(Object o) {

    if (!(o instanceof String)) {
      return false;
    }

    TrieNode node = getNode((String) o);

    return node != null && node.validWord();
  }

  /**
   * Finds the node for a given word.
   *
   * @param word - word that is being searched for
   * @return - The TrieNode where the word occurs (null otherwise)
   */
  protected TrieNode getNode(String word) {
    TrieNode node = base;

    for (char c : word.toCharArray()) {
      node = node.get(c);

      if (node == null) {
        return null;
      }
    }

    return node;
  }

  /**
   * Checks to see if the string is a prefix Node.
   *
   * @param s - string that is being searched for
   * @return - returns true if it is contained in the Trie
   */
  public boolean prefix(String s) {
    return getNode(s) != null;
  }

  @Override
  public boolean add(String word) {
    TrieNode n = base;

    // Adds word to Trie
    for (char l : word.toCharArray()) {

      TrieNode next = n.get(l);

      if (next == null) {
        next = makeNode();
        n.put(l, next);
        next.parent = n;
      }

      n = next;
    }

    // Check to see if Trie has been added to
    if (n.validWord()) {
      return false;
    } else {
      n.word = true;
      size++;
      return true;
    }
  }

  @Override
  public boolean addAll(Collection<? extends String> w) {
    boolean added = false;

    for (String word : w) {
      if (add(word)) {
        added = true;
      }
    }

    return added;
  }

  @Override
  public Iterator<String> iterator() {
    return new TrieIterator(base, ""); // check
  }

  @Override
  public Object[] toArray() {
    Object[] retArr = new Object[size];
    Iterator<String> iter = iterator();

    int i = 0;

    while (iter.hasNext()) {
      retArr[i] = iter.next();
      i++;
    }

    return retArr;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T[] toArray(T[] a) {
    T[] retArr = null;
    if (size() > a.length) {
      retArr = (T[]) Array.newInstance(a.getClass().getComponentType(), size());
    }
    Iterator<String> iter = iterator();
    int i = 0;
    while (iter.hasNext()) {
      retArr[i++] = (T) iter.next();
    }
    return retArr;
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    // Unneeded method for my implementation
    return false;
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    // Static Trie that doesn't remove elements
    return false;
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    // Static Trie that doesn't remove elements
    return false;
  }

  @Override
  public boolean remove(Object o) {
    // Static Trie that doesn't remove elements
    return false;
  }

  @Override
  public void clear() {
    // Static Trie that doesn't remove elements
  }

}
