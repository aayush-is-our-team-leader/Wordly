// ============================================================
// DictionaryService.java
//
// This is an INTERFACE — it defines what a dictionary service
// must be able to do, without caring HOW it does it.
//
// Why use an interface here?
// Because in tests we want a FAKE dictionary (no real API calls).
// In the real game we want the REAL PyDictionary API.
// Both implement this same interface, so the game code doesn't 
// need to know which one it's talking to.
//
// This pattern is called "Dependency Injection" and it's exactly
// what makes TDD with external APIs possible.
//
// ⚠️ TEAM NOTE: PyDictionary API is officially DEPRECATED.
// The maintainer shut it down. Consider switching to:
//   - Free Dictionary API: https://dictionaryapi.dev (free, no key)
//   - Merriam-Webster API: https://dictionaryapi.com (free tier)
// The interface below works with any dictionary API — just swap
// the implementation in PyDictionaryService.java.
// ============================================================

public interface DictionaryService {

    /**
     * Checks if a word exists in the dictionary.
     * Returns true if the word is valid, false if not found.
     *
     * @param word the word to look up (should already be lowercase)
     * @return true if word is valid, false otherwise
     */
    boolean isValidWord(String word);
}
