// ============================================================
// MockDictionaryService.java
//
// This is a FAKE dictionary service used only in tests.
// It does NOT make any real API calls.
//
// Why do we need this?
// If tests called the real PyDictionary API every time they ran:
//   - Tests would fail if there's no internet connection
//   - Tests would be slow (waiting for network responses)
//   - Tests would break if the API goes down (which it has — it's deprecated)
//   - We can't control what the API returns, so tests become unpredictable
//
// Instead, MockDictionaryService lets us say:
//   "pretend 'crane' is a valid word"
//   "pretend 'xzqwp' is not a valid word"
// ...and the game code has no idea it's not talking to a real API.
//
// This is standard TDD practice when working with external services.
// ============================================================

import java.util.HashSet;
import java.util.Set;

public class MockDictionaryService implements DictionaryService {

    // The set of words we'll pretend are valid for testing purposes
    // We control exactly what's in here — no network needed
    private final Set<String> validWords = new HashSet<>();

    /**
     * Creates a MockDictionaryService with a default set of known valid words.
     * These words are used across the tests to simulate real API responses.
     */
    public MockDictionaryService() {
        // Add a handful of known valid 5-letter words for testing
        // These represent what the real API would return for valid words
        validWords.add("crane");
        validWords.add("acorn");
        validWords.add("beach");
        validWords.add("storm");
        validWords.add("flint");
        validWords.add("groan");
        validWords.add("bland");
        validWords.add("tower");
        validWords.add("plumb");
        validWords.add("swift");
        validWords.add("brave");
        validWords.add("chess");
        validWords.add("depth");
        validWords.add("ember");
        validWords.add("forge");
        validWords.add("graze");
        validWords.add("hinge");
        validWords.add("joust");
        validWords.add("knave");
        validWords.add("lance");
    }

    /**
     * Simulates an API word lookup by checking our controlled word set.
     * Returns true for words we've added, false for everything else.
     * This mimics the behavior of the real API without any network calls.
     *
     * @param word the word to look up
     * @return true if word is in our test set, false otherwise
     */
    @Override
    public boolean isValidWord(String word) {
        // Normalize to lowercase just like the real API would
        return validWords.contains(word.toLowerCase());
    }

    /**
     * Allows individual tests to add a word to the valid set.
     * Useful when a test needs a specific word to be valid.
     *
     * Example:
     *   mock.addValidWord("blaze");
     *   assertTrue(game.isValidWord("blaze")); // now passes
     */
    public void addValidWord(String word) {
        validWords.add(word.toLowerCase());
    }

    /**
     * Allows individual tests to remove a word from the valid set.
     * Useful when a test needs a specific word to be invalid.
     *
     * Example:
     *   mock.removeValidWord("crane");
     *   assertFalse(game.isValidWord("crane")); // now passes
     */
    public void removeValidWord(String word) {
        validWords.remove(word.toLowerCase());
    }
}
