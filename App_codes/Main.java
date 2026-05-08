/*
 * File:        Main.java
 * Project:     Wordly
 * Description: Single-file Swing implementation of the Wordly word-guessing game.
 *              Merged from WordlyGUI.java (UI + game loop), Yus_Code.java (word loading),
 *              and Wordly.java (terminal prototype, excluded as redundant).
 *
 * AI Disclosure: We used Gemini to help with the structure of the code and brainstorming,
 *                but all the code was reviewed and handwritten by the team.
 *
 * Authors:     Team Elements
 * Version:     1.0.0
 *
 * Usage:
 *   Compile:   javac Main.java
 *   Run:       java Main
 *   Package:   jar cfe Wordly.jar Main *.class
 */

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.List;
import javax.swing.*;

/**
 * Main application class for Wordly.
 *
 * <p>Serves as both the entry point and the primary Swing window ({@code JFrame}).
 * Manages game state, UI construction, and the core guess-evaluation loop.
 *
 * <p>Game rules:
 * <ul>
 *   <li>The player has {@value #MAX_ATTEMPTS} attempts to guess a {@value #WORD_LENGTH}-letter word.</li>
 *   <li>Each guess is evaluated with a two-pass algorithm to handle duplicate letters correctly.</li>
 *   <li>Green = correct letter, correct position.</li>
 *   <li>Yellow = correct letter, wrong position.</li>
 *   <li>Red = letter not in the word.</li>
 * </ul>
 *
 * @author  Team Elements
 * @version 1.0.0
 */
public class Main extends JFrame {

    // ------------------------------------------------------------------ //
    //  CONSTANTS                                                           //
    // ------------------------------------------------------------------ //

    /** Maximum number of guesses allowed per round. */
    private static final int MAX_ATTEMPTS = 7;

    /** Required length for all guesses and secret words. */
    private static final int WORD_LENGTH = 5;

    /**
     * Built-in word list used as a fallback when the {@code WORDS} file cannot be found.
     * Prevents a hard crash on startup if the file is missing.
     */
    private static final String[] FALLBACK_WORDS = {
        "CRANE", "STORM", "BEACH", "SHARK", "PLANT",
        "BRAIN", "SKULL", "FLAME", "SWORD", "MAGIC",
        "CHAOS", "WORLD", "PIXEL", "GAMER", "BEAST"
    };

    // ------------------------------------------------------------------ //
    //  GAME STATE                                                          //
    // ------------------------------------------------------------------ //

    /** Full list of valid words loaded from the {@code WORDS} file (or fallback). */
    private List<String> wordBank;

    /** The secret word the player is trying to guess in the current round. */
    private String secretWord = "";

    /** The word used in the previous round; prevents immediate repetition. */
    private String lastWord = "";

    /** Index of the current guess row (0-based). Increments after each valid guess. */
    private int currentAttempt = 0;

    /** Number of consecutive wins in the current session. Resets on loss. */
    private int winStreak = 0;

    /** Total number of rounds played in the current session. */
    private int totalGames = 0;

    /** All letters the player has submitted in guesses this round. */
    private Set<String> guessedLetters = new HashSet<>();

    /** Letters confirmed to be in the correct position (green feedback). */
    private Set<String> correctLetters = new HashSet<>();

    /** Letters present in the word but in the wrong position (yellow feedback). */
    private Set<String> wrongSpotLetters = new HashSet<>();

    /** Letters confirmed to be absent from the word (red feedback). */
    private Set<String> wrongLetters = new HashSet<>();

    // ------------------------------------------------------------------ //
    //  UI COMPONENTS                                                       //
    // ------------------------------------------------------------------ //

    /** The game board. {@code grid[row][col]} maps to attempt row and letter position. */
    private JLabel[][] grid = new JLabel[MAX_ATTEMPTS][WORD_LENGTH];

    /** Text field where the player types each guess. */
    private JTextField inputField;

    /** Displays the current win streak in the header. */
    private JLabel streakLabel;

    /** Displays the total number of games played in the header. */
    private JLabel gamesLabel;

    /** Scrollable panel listing the status of every guessed letter. */
    private JPanel letterStatusPanel;

    /** Shows all letters that have been confirmed in the correct position. */
    private JLabel correctLettersLabel;

    /** Shows the ratio of guessed letters to total alphabet (e.g., "7/26"). */
    private JLabel guessedCountLabel;

    // ------------------------------------------------------------------ //
    //  COLOUR PALETTE                                                      //
    // ------------------------------------------------------------------ //

    private final Color BG_DARK     = new Color(18,  18,  19);  // Primary background
    private final Color GRID_EMPTY  = new Color(45,  45,  46);  // Empty cell border
    private final Color WORD_GREEN  = new Color(38,  115, 70);  // Correct position
    private final Color WORD_YELLOW = new Color(180, 140, 0);   // Wrong position
    private final Color WORD_RED    = new Color(180, 40,  40);  // Not in word
    private final Color ACCENT      = new Color(138, 43,  226); // UI accent (purple)

    // ================================================================== //
    //  ENTRY POINT                                                         //
    // ================================================================== //

    /**
     * Application entry point. Schedules UI construction on the
     * Swing Event Dispatch Thread (EDT) as required by Swing threading rules.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }

    // ================================================================== //
    //  CONSTRUCTOR                                                         //
    // ================================================================== //

    /**
     * Initializes the application: loads the word bank, builds the UI,
     * and starts the first game round.
     *
     * <p>Displays a startup error and exits if no words can be loaded.
     */
    public Main() {
        wordBank = loadWordsFromFile("WORDS");

        if (wordBank.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                "Word list is empty and no fallback words loaded.\n" +
                "Make sure WORDS is in the working directory.",
                "Startup Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        setupUI();
        startNewSession();
        inputField.requestFocus();
    }

    // ================================================================== //
    //  WORD UTILITY  (originally Yus_Code.java)                          //
    // ================================================================== //

    /**
     * Reads the {@code WORDS} file and returns all valid 5-letter words in uppercase.
     *
     * <p>Lines are skipped if they are blank or begin with {@code [} (section headers).
     * If the file does not exist, {@link #FALLBACK_WORDS} is returned instead.
     *
     * @param fileName relative path to the word list file
     * @return non-null list of valid 5-letter words; may be empty only on unexpected error
     */
    private static List<String> loadWordsFromFile(String fileName) {
        List<String> words = new ArrayList<>();
        File file = new File(fileName);
        try {
            if (file.exists()) {
                Scanner reader = new Scanner(file);
                while (reader.hasNextLine()) {
                    String line = reader.nextLine().trim().toUpperCase();
                    if (line.length() == WORD_LENGTH && !line.startsWith("[")) {
                        words.add(line);
                    }
                }
                reader.close();
            } else {
                words.addAll(Arrays.asList(FALLBACK_WORDS));
            }
        } catch (FileNotFoundException e) {
            System.out.println("File error: " + e.getMessage());
            words.addAll(Arrays.asList(FALLBACK_WORDS));
        }
        return words;
    }

    /**
     * Selects a random word from {@code bank}, guaranteed to differ from {@code previous}.
     *
     * <p>If the bank contains only one word, that word is always returned regardless of
     * the previous value.
     *
     * @param bank     the word pool to draw from; must contain at least one entry
     * @param previous the word used in the last round (excluded from selection)
     * @return a randomly selected word from {@code bank}
     */
    private static String getRandomWord(List<String> bank, String previous) {
        if (bank.size() <= 1) return bank.get(0);
        String next;
        do {
            next = bank.get(new Random().nextInt(bank.size()));
        } while (next.equals(previous));
        return next;
    }

    // ================================================================== //
    //  SESSION MANAGEMENT                                                  //
    // ================================================================== //

    /**
     * Resets all per-round state and selects a new secret word.
     *
     * <p>Called once at startup and again after each completed round
     * (win or loss) when the player chooses to play again.
     */
    private void startNewSession() {
        secretWord     = getRandomWord(wordBank, lastWord);
        lastWord       = secretWord;
        currentAttempt = 0;
        totalGames++;

        guessedLetters.clear();
        correctLetters.clear();
        wrongSpotLetters.clear();
        wrongLetters.clear();

        updateStatsDisplay();
        updateLetterDisplay();
    }

    // ================================================================== //
    //  UI SETUP  (originally WordlyGUI.java)                             //
    // ================================================================== //

    /**
     * Constructs and displays the complete Swing UI.
     *
     * <p>Layout regions:
     * <ul>
     *   <li>NORTH — title and session stats (streak, games played)</li>
     *   <li>PAGE_START — color legend</li>
     *   <li>CENTER — game board grid + letter status sidebar</li>
     *   <li>SOUTH — guess input field</li>
     * </ul>
     */
    private void setupUI() {
        setTitle("WORDLY - BRAIN MODE");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(15, 15));
        getContentPane().setBackground(BG_DARK);

        // -- Header --
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_DARK);
        header.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20));

        JLabel titleLabel = new JLabel("WORDLY", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 36));
        titleLabel.setForeground(ACCENT);

        JPanel statsPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        statsPanel.setBackground(BG_DARK);
        streakLabel = createStatLabel("STREAK: 0");
        gamesLabel  = createStatLabel("GAMES: 0");
        statsPanel.add(streakLabel);
        statsPanel.add(gamesLabel);

        header.add(titleLabel, BorderLayout.NORTH);
        header.add(statsPanel, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        // -- Color legend --
        JPanel legend = new JPanel(new GridLayout(1, 3, 10, 0));
        legend.setBackground(BG_DARK);
        legend.setBorder(BorderFactory.createEmptyBorder(0, 20, 10, 20));
        legend.add(createLegendLabel("CORRECT",     WORD_GREEN));
        legend.add(createLegendLabel("WRONG SPOT",  WORD_YELLOW));
        legend.add(createLegendLabel("NOT IN WORD", WORD_RED));
        add(legend, BorderLayout.PAGE_START);

        // -- Center: board + sidebar --
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setBackground(BG_DARK);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // Game board grid
        JPanel board = new JPanel(new GridLayout(MAX_ATTEMPTS, WORD_LENGTH, 10, 10));
        board.setBackground(BG_DARK);
        board.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        for (int r = 0; r < MAX_ATTEMPTS; r++) {
            for (int c = 0; c < WORD_LENGTH; c++) {
                grid[r][c] = new JLabel("", SwingConstants.CENTER);
                grid[r][c].setOpaque(true);
                grid[r][c].setBackground(new Color(35, 35, 36));
                grid[r][c].setForeground(Color.WHITE);
                grid[r][c].setFont(new Font("Arial", Font.BOLD, 36));
                grid[r][c].setBorder(BorderFactory.createLineBorder(GRID_EMPTY, 2));
                grid[r][c].setPreferredSize(new Dimension(75, 75));
                board.add(grid[r][c]);
            }
        }

        // Letter status sidebar
        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
        rightPanel.setBackground(BG_DARK);
        rightPanel.setPreferredSize(new Dimension(250, 400));

        JPanel correctPanel = new JPanel(new BorderLayout());
        correctPanel.setBackground(new Color(30, 30, 30));
        correctPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(WORD_GREEN, 2),
            "CORRECT LETTERS",
            javax.swing.border.TitledBorder.CENTER,
            javax.swing.border.TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 12), WORD_GREEN));
        correctLettersLabel = new JLabel("None yet", SwingConstants.CENTER);
        correctLettersLabel.setFont(new Font("Arial", Font.BOLD, 20));
        correctLettersLabel.setForeground(WORD_GREEN);
        correctPanel.add(correctLettersLabel, BorderLayout.CENTER);

        JPanel guessedPanel = new JPanel(new BorderLayout());
        guessedPanel.setBackground(new Color(30, 30, 30));
        guessedPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(ACCENT, 2),
            "GUESSED LETTERS",
            javax.swing.border.TitledBorder.CENTER,
            javax.swing.border.TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 12), ACCENT));
        guessedCountLabel = new JLabel("0/26", SwingConstants.CENTER);
        guessedCountLabel.setFont(new Font("Arial", Font.BOLD, 18));
        guessedCountLabel.setForeground(ACCENT);
        guessedPanel.add(guessedCountLabel, BorderLayout.CENTER);

        letterStatusPanel = new JPanel();
        letterStatusPanel.setLayout(new BoxLayout(letterStatusPanel, BoxLayout.Y_AXIS));
        letterStatusPanel.setBackground(new Color(30, 30, 30));
        letterStatusPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(100, 100, 100), 1),
            "LETTER STATUS",
            javax.swing.border.TitledBorder.CENTER,
            javax.swing.border.TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 12),
            new Color(180, 180, 180)));

        JScrollPane scrollPane = new JScrollPane(letterStatusPanel);
        scrollPane.setBackground(new Color(30, 30, 30));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setBackground(new Color(40, 40, 40));

        rightPanel.add(correctPanel, BorderLayout.NORTH);
        rightPanel.add(guessedPanel, BorderLayout.CENTER);
        rightPanel.add(scrollPane,   BorderLayout.SOUTH);

        centerPanel.add(board,      BorderLayout.CENTER);
        centerPanel.add(rightPanel, BorderLayout.EAST);
        add(centerPanel, BorderLayout.CENTER);

        // -- Input field --
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBackground(BG_DARK);
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 30, 15, 30));

        JLabel instruction = new JLabel(
            "Enter your 5-letter guess and press ENTER:", SwingConstants.CENTER);
        instruction.setForeground(new Color(180, 180, 180));
        instruction.setFont(new Font("Arial", Font.PLAIN, 12));

        inputField = new JTextField();
        inputField.setFont(new Font("Arial", Font.BOLD, 28));
        inputField.setHorizontalAlignment(JTextField.CENTER);
        inputField.setBackground(new Color(40, 40, 41));
        inputField.setForeground(ACCENT);
        inputField.setCaretColor(ACCENT);
        inputField.setBorder(BorderFactory.createLineBorder(ACCENT, 2));
        inputField.setPreferredSize(new Dimension(300, 50));
        inputField.addActionListener(e -> processGuess());
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                // Prevent input beyond the maximum word length
                if (inputField.getText().length() >= WORD_LENGTH) e.consume();
            }
        });

        inputPanel.add(instruction, BorderLayout.NORTH);
        inputPanel.add(inputField,  BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ================================================================== //
    //  UI HELPERS                                                          //
    // ================================================================== //

    /**
     * Creates a styled label used for session statistics (streak, games played).
     *
     * @param text initial display text
     * @return configured {@code JLabel}
     */
    private JLabel createStatLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 16));
        label.setForeground(ACCENT);
        label.setBackground(new Color(30, 30, 30));
        label.setOpaque(true);
        label.setBorder(BorderFactory.createLineBorder(ACCENT, 1));
        label.setPreferredSize(new Dimension(120, 40));
        return label;
    }

    /**
     * Creates a color-coded label for the legend bar.
     *
     * @param text  label text describing the feedback type
     * @param color background color corresponding to the feedback type
     * @return configured {@code JLabel}
     */
    private JLabel createLegendLabel(String text, Color color) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setOpaque(true);
        l.setBackground(color);
        l.setForeground(Color.WHITE);
        l.setFont(new Font("Arial", Font.BOLD, 13));
        l.setPreferredSize(new Dimension(130, 35));
        l.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
        return l;
    }

    /**
     * Refreshes the streak and games-played labels in the header.
     */
    private void updateStatsDisplay() {
        streakLabel.setText("STREAK: " + winStreak);
        gamesLabel.setText("GAMES: "   + totalGames);
    }

    /**
     * Rebuilds the letter status sidebar to reflect the current round's
     * guessed, correct, wrong-spot, and absent letter sets.
     */
    private void updateLetterDisplay() {
        letterStatusPanel.removeAll();

        correctLettersLabel.setText(
            correctLetters.isEmpty() ? "None yet" : String.join(" ", correctLetters));
        guessedCountLabel.setText(guessedLetters.size() + "/26");

        for (String letter : guessedLetters) {
            JLabel lbl = new JLabel();
            lbl.setFont(new Font("Arial", Font.BOLD, 14));
            lbl.setAlignmentX(Component.CENTER_ALIGNMENT);

            if (correctLetters.contains(letter)) {
                lbl.setText(letter + " - CORRECT");
                lbl.setForeground(WORD_GREEN);
            } else if (wrongSpotLetters.contains(letter)) {
                lbl.setText(letter + " - WRONG SPOT");
                lbl.setForeground(WORD_YELLOW);
            } else {
                lbl.setText(letter + " - NOT IN WORD");
                lbl.setForeground(WORD_RED);
            }

            letterStatusPanel.add(lbl);
            letterStatusPanel.add(Box.createVerticalStrut(5));
        }

        letterStatusPanel.revalidate();
        letterStatusPanel.repaint();
    }

    // ================================================================== //
    //  CORE GAME LOGIC                                                     //
    // ================================================================== //

    /**
     * Processes the player's current guess.
     *
     * <p>Validation is performed first (length and word bank membership).
     * Feedback is then computed using a two-pass algorithm:
     * <ol>
     *   <li><b>Pass 1</b> — identifies exact matches (green) and marks those
     *       positions in {@code secretUsed} to prevent double-counting.</li>
     *   <li><b>Pass 2</b> — for remaining positions, searches for the guessed
     *       letter elsewhere in the secret word (yellow); marks absent letters red.</li>
     * </ol>
     *
     * <p>After feedback is applied, the method checks for a win (all letters green)
     * or a loss (all attempts exhausted) and delegates to
     * {@link #showEndSessionDialog(String)} accordingly.
     */
    private void processGuess() {
        String guess = inputField.getText().toUpperCase().trim();

        if (guess.length() != WORD_LENGTH) {
            showFunError("Need 5 letters, got " + guess.length());
            return;
        }
        if (!wordBank.contains(guess)) {
            String[] errors = {
                "Not in word bank. Try again.",
                "Not a valid word.",
                "Word not recognized. Try another.",
                "Not found in dictionary.",
                "Invalid input. Please try again."
            };
            showFunError(errors[(int)(Math.random() * errors.length)]);
            return;
        }

        char[]    secretChars = secretWord.toCharArray();
        boolean[] secretUsed  = new boolean[WORD_LENGTH];

        // Pass 1: exact matches
        for (int i = 0; i < WORD_LENGTH; i++) {
            char g = guess.charAt(i);
            grid[currentAttempt][i].setText(String.valueOf(g));
            guessedLetters.add(String.valueOf(g));

            if (g == secretChars[i]) {
                grid[currentAttempt][i].setBackground(WORD_GREEN);
                secretUsed[i] = true;
                correctLetters.add(String.valueOf(g));
            }
        }

        // Pass 2: wrong position or absent
        for (int i = 0; i < WORD_LENGTH; i++) {
            if (grid[currentAttempt][i].getBackground().equals(WORD_GREEN)) continue;
            char    g     = guess.charAt(i);
            boolean found = false;
            for (int j = 0; j < WORD_LENGTH; j++) {
                if (!secretUsed[j] && g == secretChars[j]) {
                    grid[currentAttempt][i].setBackground(WORD_YELLOW);
                    secretUsed[j] = true;
                    found = true;
                    wrongSpotLetters.add(String.valueOf(g));
                    break;
                }
            }
            if (!found) {
                grid[currentAttempt][i].setBackground(WORD_RED);
                wrongLetters.add(String.valueOf(g));
            }
        }

        updateLetterDisplay();

        // Win condition
        if (guess.equals(secretWord)) {
            winStreak++;
            String[] winMessages = {
                "Excellent.", "Correct!", "Well done.",
                "Outstanding.", "Brilliant.", "Perfect."
            };
            showEndSessionDialog(
                winMessages[(int)(Math.random() * winMessages.length)]
                + "\nThe word was: " + secretWord);
            return;
        }

        // Loss condition — all attempts exhausted
        if (currentAttempt == MAX_ATTEMPTS - 1) {
            winStreak = 0;
            String[] loseMessages = {
                "Better luck next time.",
                "Incorrect. The word is revealed.",
                "Not quite. Try again.",
                "Game over."
            };
            showEndSessionDialog(
                loseMessages[(int)(Math.random() * loseMessages.length)]
                + "\nThe word was: " + secretWord);
            return;
        }

        currentAttempt++;
        inputField.setText("");
    }

    /**
     * Displays a warning dialog for invalid guess input, then resets the input field.
     *
     * @param message description of the validation failure
     */
    private void showFunError(String message) {
        JOptionPane.showMessageDialog(this, message, "Invalid Input", JOptionPane.WARNING_MESSAGE);
        inputField.setText("");
        inputField.requestFocus();
    }

    /**
     * Displays the end-of-round dialog and handles the player's choice to
     * play again or quit the application.
     *
     * <p>On replay, the board grid is visually reset before
     * {@link #startNewSession()} is called.
     *
     * @param message win or loss message to display to the player
     */
    private void showEndSessionDialog(String message) {
        Object[] options = {"Play Again", "Quit"};
        int choice = JOptionPane.showOptionDialog(this,
            message + "\n\nPlay another round?",
            "Round Complete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null, options, options[0]);

        if (choice == JOptionPane.YES_OPTION) {
            // Reset board cells to their default empty state
            for (int r = 0; r < MAX_ATTEMPTS; r++) {
                for (int c = 0; c < WORD_LENGTH; c++) {
                    grid[r][c].setText("");
                    grid[r][c].setBackground(new Color(35, 35, 36));
                }
            }
            startNewSession();
            inputField.setText("");
            updateStatsDisplay();
            updateLetterDisplay();
            inputField.requestFocus();
        } else {
            JOptionPane.showMessageDialog(this,
                "Thanks for playing.\nFinal Streak: " + winStreak +
                "\nTotal Games: " + totalGames,
                "Goodbye", JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        }
    }
}
