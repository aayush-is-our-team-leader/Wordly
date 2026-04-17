// ============================================================
// WordyGame.java — REFACTORED with API support
//
// Key change from the previous version:
// isValidWord() no longer checks a hardcoded word list.
// Instead it delegates to a DictionaryService — which can be
// the real PyDictionary API in production, or a mock in tests.
//
// The game receives its DictionaryService through the constructor.
// This is called "Dependency Injection" — the game doesn't create
// its own service, it receives one from the outside.
//
// In production:
//   WordyGame game = new WordyGame(new PyDictionaryService());
//
// In tests:
//   WordyGame game = new WordyGame(new MockDictionaryService());
//
// The game code doesn't know or care which one it gets.
// ============================================================

import java.util.*;
import java.util.regex.Pattern;

public class WordyGame {

    // --------------------------------------------------------
    // Status enum — the three possible results for each letter
    // --------------------------------------------------------
    public enum Status {
        CORRECT,  // right letter, right position (green tile)
        PRESENT,  // right letter, wrong position (yellow tile)
        ABSENT    // letter not in target word at all (gray tile)
    }

    // --------------------------------------------------------
    // LetterResult — one letter from a guess and its feedback
    // --------------------------------------------------------
    public static class LetterResult {
        public final char letter;
        public Status status;

        public LetterResult(char letter, Status status) {
            this.letter = letter;
            this.status = status;
        }
    }

    // --------------------------------------------------------
    // ValidationResult — returned by validateGuess()
    // --------------------------------------------------------
    public static class ValidationResult {
        public final boolean valid;
        public final String error;

        public ValidationResult(boolean valid, String error) {
            this.valid = valid;
            this.error = error;
        }
    }

    // --------------------------------------------------------
    // Constants
    // --------------------------------------------------------
    private static final Pattern VALID_CHARS_PATTERN =
        Pattern.compile("^[a-zA-Z]+$");
    private static final int WORD_LENGTH = 5;

    // --------------------------------------------------------
    // The dictionary service — injected through the constructor
    // This is what replaced the hardcoded WORD_LIST
    // --------------------------------------------------------
    private final DictionaryService dictionaryService;

    // Shared random for picking target words
    private final Random random = new Random();

    // Word pool used to pick random target words each round
    // Still needed since the API validates words but doesn't supply them
    // In a future sprint this could be replaced by an API endpoint
    // that returns a random valid word directly
    private static final List<String> WORD_POOL = Arrays.asList(
        "crane","acorn","beach","storm","flint","groan","bland","tower",
        "plumb","swift","brave","chess","depth","ember","forge","graze",
        "hinge","joust","knave","lance","marsh","noble","ocean","prism",
        "quill","raven","scorn","thorn","ultra","vapor","waltz","xenon",
        "yacht","zonal","abbey","blaze","clamp","drape","erode","fjord",
        "guile","hoist","infer","jaunt","karma","latch","mirth","naive",
        "onset","prowl","qualm","repay","scald","truce","usher","vouch"
    );

    // --------------------------------------------------------
    // Constructor — receives the dictionary service to use
    // --------------------------------------------------------
    /**
     * Creates a WordyGame with the provided dictionary service.
     *
     * For production:
     *   new WordyGame(new PyDictionaryService())
     *
     * For tests:
     *   new WordyGame(new MockDictionaryService())
     */
    public WordyGame(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    // --------------------------------------------------------
    // normalizeGuess — converts input to lowercase
    // --------------------------------------------------------
    /**
     * Normalizes a guess to lowercase for consistent comparison.
     * Handles Test Case 1: typecasing shouldn't matter.
     */
    public String normalizeGuess(String guess) {
        return guess.toLowerCase();
    }

    // --------------------------------------------------------
    // isValidLength — must be exactly 5 characters
    // --------------------------------------------------------
    /**
     * Returns true if the guess is exactly 5 characters long.
     * Handles Test Case 2: guesses must be 5 letters.
     */
    public boolean isValidLength(String guess) {
        return guess.length() == WORD_LENGTH;
    }

    // --------------------------------------------------------
    // isValidWord — delegates to the DictionaryService
    //
    // This is the big change from the hardcoded version.
    // Instead of checking a local list, we ask the service.
    // The service could be the real API or a mock — we don't care.
    // --------------------------------------------------------
    /**
     * Returns true if the dictionary service confirms the word is valid.
     * In production, this calls the PyDictionary API.
     * In tests, this calls the MockDictionaryService.
     * Handles Test Case 3: guesses must be real words.
     */
    public boolean isValidWord(String guess) {
        return dictionaryService.isValidWord(normalizeGuess(guess));
    }

    // --------------------------------------------------------
    // isValidCharacters — no numbers or special characters
    // --------------------------------------------------------
    /**
     * Returns true if the guess contains only alphabetical characters.
     * Handles Test Case 4: no numbers or special characters.
     */
    public boolean isValidCharacters(String guess) {
        return VALID_CHARS_PATTERN.matcher(guess).matches();
    }

    // --------------------------------------------------------
    // getNewWord — picks a random word that isn't the previous one
    // --------------------------------------------------------
    /**
     * Returns a random word from the word pool that is not the previous word.
     * Handles Test Case 5: target word does not repeat consecutively.
     *
     * @param previousWord the last target word, or null if first round
     */
    public String getNewWord(String previousWord) {
        String newWord;
        do {
            newWord = WORD_POOL.get(random.nextInt(WORD_POOL.size()));
        } while (newWord.equals(previousWord));
        return newWord;
    }

    // --------------------------------------------------------
    // evaluateGuess — core green/yellow/gray feedback logic
    // Two-pass approach to handle duplicate letters correctly
    // --------------------------------------------------------
    /**
     * Evaluates a guess against the target word.
     * Returns an array of 5 LetterResult objects (CORRECT/PRESENT/ABSENT).
     *
     * Pass 1 — lock in exact matches (CORRECT / green)
     * Pass 2 — check remaining letters for partial matches (PRESENT / yellow)
     */
    public LetterResult[] evaluateGuess(String guess, String target) {
        String g = normalizeGuess(guess);
        String t = normalizeGuess(target);

        LetterResult[] result = new LetterResult[WORD_LENGTH];
        Map<Character, Integer> remaining = new HashMap<>();

        // Initialize all as ABSENT
        for (int i = 0; i < WORD_LENGTH; i++) {
            result[i] = new LetterResult(g.charAt(i), Status.ABSENT);
        }

        // Pass 1: exact matches
        for (int i = 0; i < WORD_LENGTH; i++) {
            if (g.charAt(i) == t.charAt(i)) {
                result[i].status = Status.CORRECT;
            } else {
                remaining.merge(t.charAt(i), 1, Integer::sum);
            }
        }

        // Pass 2: partial matches
        for (int i = 0; i < WORD_LENGTH; i++) {
            if (result[i].status == Status.CORRECT) continue;
            char c = g.charAt(i);
            if (remaining.getOrDefault(c, 0) > 0) {
                result[i].status = Status.PRESENT;
                remaining.put(c, remaining.get(c) - 1);
            }
        }

        return result;
    }

    // --------------------------------------------------------
    // validateGuess — runs all checks, returns ValidationResult
    // Order: characters → length → dictionary (cheapest first)
    // --------------------------------------------------------
    /**
     * Runs all validation checks on raw player input.
     * Returns ValidationResult with valid=true if all pass,
     * or valid=false with a specific error message if not.
     */
    public ValidationResult validateGuess(String guess) {
        String normalized = normalizeGuess(guess);

        if (!isValidCharacters(normalized)) {
            return new ValidationResult(false,
                "Guess cannot contain numbers or special characters.");
        }
        if (!isValidLength(normalized)) {
            return new ValidationResult(false,
                "Guess must be exactly 5 letters.");
        }
        if (!isValidWord(normalized)) {
            return new ValidationResult(false,
                "Not a valid word. Try again.");
        }

        return new ValidationResult(true, null);
    }
}
