// ============================================================
// WordyGameTest.java — RED → GREEN with API mocking
//
// This version of the tests uses MockDictionaryService instead
// of a hardcoded word list.
//
// The key rule of TDD with external APIs:
//   NEVER call the real API in your tests.
//
// Why?
//   - No internet = all tests fail for the wrong reason
//   - API goes down (like PyDictionary did) = all tests fail
//   - Tests become slow waiting for network responses
//   - We can't control what the API returns, so tests are unpredictable
//
// Instead we use MockDictionaryService — a fake dictionary that
// we fully control. It behaves exactly like the real API would,
// but instantly, offline, and with predictable responses.
//
// The game code (WordyGame.java) doesn't know or care whether
// it's talking to the real API or the mock — that's the point.
// ============================================================

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class WordyGameTest {

    private WordyGame game;
    private MockDictionaryService mockDictionary;

    // @BeforeEach runs before every single test
    // We create a fresh game AND a fresh mock dictionary each time
    // so tests don't accidentally affect each other
    @BeforeEach
    void setUp() {
        // Create the mock dictionary — this replaces the real PyDictionary API
        mockDictionary = new MockDictionaryService();

        // Inject the mock into the game instead of the real API
        // In production code you'd write: new WordyGame(new PyDictionaryService())
        // In tests we write:             new WordyGame(mockDictionary)
        game = new WordyGame(mockDictionary);
    }

    // ============================================================
    // TEST CASE 1: Typecasing shouldn't matter
    //
    // Whether a player types "CRANE", "crane", or "CrAnE",
    // the game should treat them all the same way.
    // ============================================================

    @Test
    void testNormalizeGuess_uppercaseConvertsToLowercase() {
        // Typing in all caps should still work
        assertEquals("crane", game.normalizeGuess("CRANE"));
    }

    @Test
    void testNormalizeGuess_mixedCaseConvertsToLowercase() {
        // Mixed case should also normalize correctly
        assertEquals("crane", game.normalizeGuess("CrAnE"));
    }

    @Test
    void testNormalizeGuess_alreadyLowercaseUnchanged() {
        // If already lowercase, nothing should change
        assertEquals("crane", game.normalizeGuess("crane"));
    }

    // ============================================================
    // TEST CASE 2: Guesses HAVE to be 5 letters
    // ============================================================

    @Test
    void testIsValidLength_fiveLetterWordReturnsTrue() {
        // Exactly 5 letters — should pass
        assertTrue(game.isValidLength("crane"));
    }

    @Test
    void testIsValidLength_tooShortReturnsFalse() {
        // Only 3 letters — should be rejected
        assertFalse(game.isValidLength("cat"));
    }

    @Test
    void testIsValidLength_tooLongReturnsFalse() {
        // 6 letters — should be rejected
        assertFalse(game.isValidLength("planet"));
    }

    @Test
    void testIsValidLength_emptyStringReturnsFalse() {
        // Empty input — should always be rejected
        assertFalse(game.isValidLength(""));
    }

    // ============================================================
    // TEST CASE 3: Guesses HAVE to be an actual word
    //
    // This is where the API comes in — instead of checking a local
    // list, the game now asks the DictionaryService.
    //
    // In these tests, MockDictionaryService acts as the API.
    // It already knows "crane" is valid and "xzqwp" is not.
    // ============================================================

    @Test
    void testIsValidWord_realWordReturnsTrueViaMock() {
        // MockDictionaryService has "crane" pre-loaded as valid
        // This simulates the real API confirming the word exists
        assertTrue(game.isValidWord("crane"));
    }

    @Test
    void testIsValidWord_madeUpWordReturnsFalseViaMock() {
        // "xzqwp" is not in the mock — simulates API rejecting the word
        assertFalse(game.isValidWord("xzqwp"));
    }

    @Test
    void testIsValidWord_caseInsensitiveCheckViaMock() {
        // The mock normalizes to lowercase — "CRANE" should match "crane"
        assertTrue(game.isValidWord("CRANE"));
    }

    @Test
    void testIsValidWord_wordAddedDynamicallyToMock() {
        // We can add words to the mock mid-test to simulate the API
        // recognizing a new word — useful for edge case testing
        mockDictionary.addValidWord("blaze");
        assertTrue(game.isValidWord("blaze"));
    }

    @Test
    void testIsValidWord_wordRemovedFromMockIsInvalid() {
        // We can also remove words — simulates the API not finding a word
        mockDictionary.removeValidWord("crane");
        assertFalse(game.isValidWord("crane"));
    }

    // ============================================================
    // TEST CASE 4: Guesses cannot include special characters or numbers
    // ============================================================

    @Test
    void testIsValidCharacters_alphabeticalWordReturnsTrue() {
        // All letters — valid input
        assertTrue(game.isValidCharacters("crane"));
    }

    @Test
    void testIsValidCharacters_numberInGuessReturnsFalse() {
        // Contains the number 4 — should be rejected before hitting the API
        assertFalse(game.isValidCharacters("cr4ne"));
    }

    @Test
    void testIsValidCharacters_specialCharInGuessReturnsFalse() {
        // Contains @ — should be rejected before hitting the API
        assertFalse(game.isValidCharacters("cr@ne"));
    }

    @Test
    void testIsValidCharacters_spaceInGuessReturnsFalse() {
        // Contains a space — should be rejected before hitting the API
        assertFalse(game.isValidCharacters("cr ne"));
    }

    // ============================================================
    // TEST CASE 5: Target word does not repeat consecutively
    // ============================================================

    @Test
    void testGetNewWord_returnsFiveLetterWord() {
        // The game's random word should always be 5 letters
        String word = game.getNewWord(null);
        assertEquals(5, word.length());
    }

    @Test
    void testGetNewWord_doesNotRepeatPreviousWord() {
        // New word must be different from the last one
        String previousWord = "crane";
        String newWord = game.getNewWord(previousWord);
        assertNotEquals(previousWord, newWord);
    }

    @Test
    void testGetNewWord_returnedWordIsValidAccordingToMock() {
        // The word the game picks should be recognized as valid
        // by the dictionary service (mock in tests, real API in production)
        String word = game.getNewWord(null);
        assertTrue(game.isValidWord(word));
    }

    // ============================================================
    // BONUS: evaluateGuess — core feedback mechanic
    // This logic doesn't touch the API so no mock needed here
    // ============================================================

    @Test
    void testEvaluateGuess_correctLetterCorrectPosition() {
        // "crane" vs "crane" — first letter 'c' is green
        WordyGame.LetterResult[] result = game.evaluateGuess("crane", "crane");
        assertEquals(WordyGame.Status.CORRECT, result[0].status);
    }

    @Test
    void testEvaluateGuess_correctLetterWrongPosition() {
        // guess "acorn", target "crane" — 'a' is in target but wrong spot
        WordyGame.LetterResult[] result = game.evaluateGuess("acorn", "crane");
        assertEquals(WordyGame.Status.PRESENT, result[0].status);
    }

    @Test
    void testEvaluateGuess_letterNotInTarget() {
        // 'b' is not in "crane" — should be gray
        WordyGame.LetterResult[] result = game.evaluateGuess("bxxxx", "crane");
        assertEquals(WordyGame.Status.ABSENT, result[0].status);
    }

    @Test
    void testEvaluateGuess_returnsFiveResults() {
        // Every guess produces exactly 5 result objects
        WordyGame.LetterResult[] result = game.evaluateGuess("crane", "crane");
        assertEquals(5, result.length);
    }

    @Test
    void testEvaluateGuess_eachResultHasLetterAndStatus() {
        // Every result must have both a letter and a status set
        WordyGame.LetterResult[] result = game.evaluateGuess("crane", "crane");
        for (WordyGame.LetterResult r : result) {
            assertNotNull(r.letter);
            assertNotNull(r.status);
        }
    }

    // ============================================================
    // validateGuess — full pipeline test
    // Tests the combined validation flow using the mock API
    // ============================================================

    @Test
    void testValidateGuess_validWordPassesAllChecks() {
        // "crane" is 5 letters, all alpha, and in the mock dictionary
        WordyGame.ValidationResult result = game.validateGuess("crane");
        assertTrue(result.valid);
        assertNull(result.error);
    }

    @Test
    void testValidateGuess_numberRejectedBeforeAPICall() {
        // "cr4ne" should be caught by character check — API never called
        WordyGame.ValidationResult result = game.validateGuess("cr4ne");
        assertFalse(result.valid);
        assertEquals("Guess cannot contain numbers or special characters.", result.error);
    }

    @Test
    void testValidateGuess_wrongLengthRejectedBeforeAPICall() {
        // "cat" should be caught by length check — API never called
        WordyGame.ValidationResult result = game.validateGuess("cat");
        assertFalse(result.valid);
        assertEquals("Guess must be exactly 5 letters.", result.error);
    }

    @Test
    void testValidateGuess_unknownWordRejectedByMockAPI() {
        // "xzqwp" passes character and length checks,
        // but gets rejected when the mock dictionary doesn't recognize it
        // This simulates the real API returning no result for a made-up word
        WordyGame.ValidationResult result = game.validateGuess("xzqwp");
        assertFalse(result.valid);
        assertEquals("Not a valid word. Try again.", result.error);
    }
}
