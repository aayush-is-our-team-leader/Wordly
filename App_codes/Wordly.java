import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class Wordly {
    private static final int MAX_ATTEMPTS = 7;
    private static final int WORD_LENGTH = 5;

    public static void main(String[] args) {
        List<String> wordBank = loadWords("WORDS");

        if (wordBank.isEmpty()) {
            System.out.println("❌ Error: Word list is empty or file not found.");
            return;
        }

        Scanner scanner = new Scanner(System.in);
        Random random = new Random();

        String previousWord = null;

        // Replay loop — keeps the game going until the player quits
        while (true) {
            String secretWord = getNewWord(wordBank, previousWord, random);
            previousWord = secretWord; // update so next game won't repeat it

            boolean won = false;
            int correctLetters = 0;

            System.out.println("\n🎮 Welcome to Wordly!");
            System.out.println("Legend: Letter = Correct Position | ? = Wrong Spot | _ = Not in Word\n");

            int attempt = 1;
            while (attempt <= MAX_ATTEMPTS) {
                System.out.print("Attempt " + attempt + "/" + MAX_ATTEMPTS + ": ");
                String guess = scanner.nextLine().trim().toUpperCase();

                // Rule: letters only
                if (!guess.matches("[A-Z]+")) {
                    System.out.println("❌ Letters only — no numbers or special characters.");
                    continue;
                }

                // Rule: exactly 5 letters
                if (guess.length() != WORD_LENGTH) {
                    System.out.println("❌ Invalid: Enter exactly 5 letters.");
                    continue;
                }

                // Rule: must be a real word
                // Note: replace wordBank.contains() with API call when PyDictionary is integrated
                if (!wordBank.contains(guess)) {
                    System.out.println("❌ '" + guess + "' is not a valid word. Try again!");
                    continue;
                }

                // Valid guess — show feedback
                correctLetters = countCorrectLetters(secretWord, guess);
                System.out.println("Result:  " + getFeedback(secretWord, guess));
                System.out.println(getPerformanceFeedback(correctLetters));

                if (guess.equals(secretWord)) {
                    won = true;
                    break;
                }

                attempt++;
            }

            if (won) {
                System.out.println("\n🎉 Genius! You got it!");
            } else {
                System.out.println("\n💀 Game Over. The word was: " + secretWord);
            }

            // Ask to play again
            System.out.print("\nPlay again? (yes/no): ");
            String again = scanner.nextLine().trim().toLowerCase();
            if (!again.equals("yes") && !again.equals("y")) {
                System.out.println("👋 Thanks for playing Wordly!");
                break;
            }
        }

        scanner.close();
    }

    // -------------------------------------------------------
    // loadWords
    // Reads the WORDS file and returns all valid 5-letter words
    // in uppercase. Skips blank lines and header lines.
    // -------------------------------------------------------
    private static List<String> loadWords(String fileName) {
        List<String> words = new ArrayList<>();
        try (Scanner fileScanner = new Scanner(new File(fileName))) {
            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine().trim().toUpperCase();
                if (line.length() == WORD_LENGTH && !line.startsWith("[")) {
                    words.add(line);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("❌ Could not find file: " + fileName);
        }
        return words;
    }

    // -------------------------------------------------------
    // getNewWord
    // Picks a random word different from the previous one.
    // Safety valve prevents infinite loop if word bank has 1 word.
    // -------------------------------------------------------
    private static String getNewWord(List<String> wordBank, String previousWord, Random random) {
        if (wordBank.size() == 1) return wordBank.get(0);
        String newWord;
        do {
            newWord = wordBank.get(random.nextInt(wordBank.size()));
        } while (newWord.equals(previousWord));
        return newWord;
    }

    // -------------------------------------------------------
    // getFeedback
    // Two-pass approach handles duplicate letters correctly.
    // Pass 1: exact matches — show the actual letter (green)
    // Pass 2: wrong-position matches — show "?" (yellow)
    //         absent letters — show "_" (gray)
    // -------------------------------------------------------
    private static String getFeedback(String secret, String guess) {
        String[] result = new String[WORD_LENGTH];
        boolean[] secretUsed = new boolean[WORD_LENGTH];
        boolean[] guessUsed = new boolean[WORD_LENGTH];

        // Pass 1: exact matches
        for (int i = 0; i < WORD_LENGTH; i++) {
            if (guess.charAt(i) == secret.charAt(i)) {
                result[i] = String.valueOf(guess.charAt(i));
                secretUsed[i] = true;
                guessUsed[i] = true;
            }
        }

        // Pass 2: wrong position or absent
        for (int i = 0; i < WORD_LENGTH; i++) {
            if (guessUsed[i]) continue;
            boolean foundMatch = false;
            for (int j = 0; j < WORD_LENGTH; j++) {
                if (!secretUsed[j] && guess.charAt(i) == secret.charAt(j)) {
                    result[i] = "?";
                    secretUsed[j] = true;
                    foundMatch = true;
                    break;
                }
            }
            if (!foundMatch) result[i] = "_";
        }

        return String.join(" ", result);
    }

    // -------------------------------------------------------
    // countCorrectLetters
    // Counts exact-position matches for performance feedback.
    // -------------------------------------------------------
    private static int countCorrectLetters(String secret, String guess) {
        int count = 0;
        for (int i = 0; i < WORD_LENGTH; i++) {
            if (guess.charAt(i) == secret.charAt(i)) count++;
        }
        return count;
    }

    // -------------------------------------------------------
    // getPerformanceFeedback
    // Gen-Z style message based on correct letter count.
    // -------------------------------------------------------
    private static String getPerformanceFeedback(int correctCount) {
        switch (correctCount) {
            case 5: return "✅ Good job";
            case 4: return "🔥 Almost there";
            case 3: return "👀 Looking good";
            case 2: return "📈 Getting better";
            case 1: return "🎯 On the right track";
            default: return "💀 ??? what are we doing";
        }
    }
}
