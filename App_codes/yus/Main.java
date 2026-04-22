import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.List;
import javax.swing.*;

/**
 * Main.java — Wordly (single-file version)
 *
 * Merged from:
 *   WordlyGUI.java  — Swing UI + game loop
 *   Yus_Code.java   — word loading + random word picker
 *   Wordly.java     — terminal prototype (logic already covered above; dropped)
 *
 * To compile:  javac Main.java
 * To run:      java Main
 * To jar:      jar cfe Wordly.jar Main Main.class
 *              (set Main-Class: Main in MANIFEST.MF for NativeJavaApp packaging)
 */
public class Main extends JFrame {

    // ------------------------------------------------------------------ //
    //  CONSTANTS                                                           //
    // ------------------------------------------------------------------ //
    private static final int MAX_ATTEMPTS = 7;
    private static final int WORD_LENGTH  = 5;

    // Fallback words when WORDS file is missing
    private static final String[] FALLBACK_WORDS = {
        "CRANE", "STORM", "BEACH", "SHARK", "PLANT",
        "BRAIN", "SKULL", "FLAME", "SWORD", "MAGIC",
        "CHAOS", "WORLD", "PIXEL", "GAMER", "BEAST"
    };

    // ------------------------------------------------------------------ //
    //  GAME STATE                                                          //
    // ------------------------------------------------------------------ //
    private List<String> wordBank;
    private String secretWord    = "";
    private String lastWord      = "";
    private int    currentAttempt = 0;
    private int    winStreak     = 0;
    private int    totalGames    = 0;

    // Letter tracking
    private Set<String> guessedLetters   = new HashSet<>();
    private Set<String> correctLetters   = new HashSet<>();
    private Set<String> wrongSpotLetters = new HashSet<>();
    private Set<String> wrongLetters     = new HashSet<>();

    // ------------------------------------------------------------------ //
    //  UI COMPONENTS                                                       //
    // ------------------------------------------------------------------ //
    private JLabel[][]  grid               = new JLabel[MAX_ATTEMPTS][WORD_LENGTH];
    private JTextField  inputField;
    private JLabel      streakLabel;
    private JLabel      gamesLabel;
    private JPanel      letterStatusPanel;
    private JLabel      correctLettersLabel;
    private JLabel      guessedCountLabel;

    // ------------------------------------------------------------------ //
    //  COLOUR PALETTE                                                      //
    // ------------------------------------------------------------------ //
    private final Color BG_DARK     = new Color(18,  18,  19);
    private final Color GRID_EMPTY  = new Color(45,  45,  46);
    private final Color WORD_GREEN  = new Color(38,  115, 70);
    private final Color WORD_YELLOW = new Color(180, 140, 0);
    private final Color WORD_RED    = new Color(180, 40,  40);
    private final Color ACCENT      = new Color(138, 43,  226);

    // ================================================================== //
    //  ENTRY POINT                                                         //
    // ================================================================== //
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }

    // ================================================================== //
    //  CONSTRUCTOR                                                         //
    // ================================================================== //
    public Main() {
        wordBank = loadWordsFromFile("WORDS");

        if (wordBank.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                "❌ Word list is empty and no fallback words loaded.\n" +
                "Make sure WORDS is in the working directory.",
                "Startup Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        setupUI();          // build UI first
        startNewSession();  // then start game
        inputField.requestFocus();
    }

    // ================================================================== //
    //  WORD UTILITY — from Yus_Code.java                                  //
    // ================================================================== //

    /**
     * Reads the WORDS file and returns all valid 5-letter words (uppercase).
     * Skips blank lines and header lines starting with '['.
     * Falls back to FALLBACK_WORDS if the file is not found.
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
            System.out.println("📁 File error: " + e.getMessage());
            words.addAll(Arrays.asList(FALLBACK_WORDS));
        }
        return words;
    }

    /**
     * Picks a random word different from the previous one.
     * Safety valve: returns the only word if the bank has just one entry.
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
    //  UI SETUP — from WordlyGUI.java                                     //
    // ================================================================== //
    private void setupUI() {
        setTitle("🎯 WORDLY - BRAIN MODE 🎯");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(15, 15));
        getContentPane().setBackground(BG_DARK);

        // ── HEADER ──────────────────────────────────────────────────── //
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_DARK);
        header.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20));

        JLabel titleLabel = new JLabel("🎯 WORDLY 🎯", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 36));
        titleLabel.setForeground(ACCENT);

        JPanel statsPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        statsPanel.setBackground(BG_DARK);
        streakLabel = createStatLabel("🔥 STREAK: 0");
        gamesLabel  = createStatLabel("🎮 GAMES: 0");
        statsPanel.add(streakLabel);
        statsPanel.add(gamesLabel);

        header.add(titleLabel, BorderLayout.NORTH);
        header.add(statsPanel, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        // ── LEGEND ──────────────────────────────────────────────────── //
        JPanel legend = new JPanel(new GridLayout(1, 3, 10, 0));
        legend.setBackground(BG_DARK);
        legend.setBorder(BorderFactory.createEmptyBorder(0, 20, 10, 20));
        legend.add(createLegendLabel("✅ CORRECT",    WORD_GREEN));
        legend.add(createLegendLabel("🤔 WRONG SPOT", WORD_YELLOW));
        legend.add(createLegendLabel("❌ NOPE",       WORD_RED));
        add(legend, BorderLayout.PAGE_START);

        // ── CENTRE: board + right panel ─────────────────────────────── //
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

        // Right panel — letter status sidebar
        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
        rightPanel.setBackground(BG_DARK);
        rightPanel.setPreferredSize(new Dimension(250, 400));

        // Correct letters found
        JPanel correctPanel = new JPanel(new BorderLayout());
        correctPanel.setBackground(new Color(30, 30, 30));
        correctPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(WORD_GREEN, 2),
            "✅ CORRECT LETTERS",
            javax.swing.border.TitledBorder.CENTER,
            javax.swing.border.TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 12), WORD_GREEN));
        correctLettersLabel = new JLabel("None yet", SwingConstants.CENTER);
        correctLettersLabel.setFont(new Font("Arial", Font.BOLD, 20));
        correctLettersLabel.setForeground(WORD_GREEN);
        correctPanel.add(correctLettersLabel, BorderLayout.CENTER);

        // Guessed count
        JPanel guessedPanel = new JPanel(new BorderLayout());
        guessedPanel.setBackground(new Color(30, 30, 30));
        guessedPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(ACCENT, 2),
            "📊 GUESSED LETTERS",
            javax.swing.border.TitledBorder.CENTER,
            javax.swing.border.TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 12), ACCENT));
        guessedCountLabel = new JLabel("0/26", SwingConstants.CENTER);
        guessedCountLabel.setFont(new Font("Arial", Font.BOLD, 18));
        guessedCountLabel.setForeground(ACCENT);
        guessedPanel.add(guessedCountLabel, BorderLayout.CENTER);

        // Scrollable letter status list
        letterStatusPanel = new JPanel();
        letterStatusPanel.setLayout(new BoxLayout(letterStatusPanel, BoxLayout.Y_AXIS));
        letterStatusPanel.setBackground(new Color(30, 30, 30));
        letterStatusPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(100, 100, 100), 1),
            "📋 LETTER STATUS",
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

        // ── INPUT ────────────────────────────────────────────────────── //
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

    private void updateStatsDisplay() {
        streakLabel.setText("🔥 STREAK: " + winStreak);
        gamesLabel.setText("🎮 GAMES: "  + totalGames);
    }

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
                lbl.setText("✅ " + letter + " - CORRECT");
                lbl.setForeground(WORD_GREEN);
            } else if (wrongSpotLetters.contains(letter)) {
                lbl.setText("🤔 " + letter + " - WRONG SPOT");
                lbl.setForeground(WORD_YELLOW);
            } else {
                lbl.setText("❌ " + letter + " - NOT IN WORD");
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
    private void processGuess() {
        String guess = inputField.getText().toUpperCase().trim();

        // Validation
        if (guess.length() != WORD_LENGTH) {
            showFunError("TOO SHORT! 📏 Need 5 letters, got " + guess.length());
            return;
        }
        if (!wordBank.contains(guess)) {
            String[] errors = {
                "🤔 Not in word bank! Try again!",
                "❌ THAT AIN'T A WORD BRO! 💀",
                "🧠 Nah fam, that's not a real word!",
                "📖 Not in my dictionary! Nice try though!",
                "🚫 INVALID WORD! No cap! 🧢",
                "Is that even English? 😅",
                "YOOO that word don't exist! 👻",
                "NOT IN THE DICTIONARY! 📚 FAIL",
                "You made that up! 🤥 Try again!"
            };
            showFunError(errors[(int)(Math.random() * errors.length)]);
            return;
        }

        // Two-pass feedback (handles duplicate letters correctly)
        char[]   secretChars = secretWord.toCharArray();
        boolean[] secretUsed = new boolean[WORD_LENGTH];

        // Pass 1 — exact matches (green)
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

        // Pass 2 — wrong position (yellow) or absent (red)
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

        // Win check
        if (guess.equals(secretWord)) {
            winStreak++;
            String[] winMessages = {
                "🏆 GENIUS!! 🧠✨", "🎉 YOU CRUSHED IT! 🔥",
                "👑 WORD LORD ACTIVATED 👑", "🚀 ABSOLUTELY BRILLIANT! 🚀",
                "💎 LEGENDARY GAMER 💎", "🌟 YOU'RE A WIZARD! 🧙",
                "🎯 PERFECT SHOT! 🎯", "⚡ INSANE SKILLS! ⚡"
            };
            showEndSessionDialog(
                winMessages[(int)(Math.random() * winMessages.length)]
                + "\n✅ The Word Was: " + secretWord);
            return;
        }

        // Loss check (used all attempts)
        if (currentAttempt == MAX_ATTEMPTS - 1) {
            winStreak = 0;
            String[] loseMessages = {
                "💀 GAME OVER! 💀", "😭 SO CLOSE! But not close enough!",
                "🤦 YIKES! Better luck next time!", "📉 NOPE! Not today!",
                "🎪 CLOWN MOMENT! 🤡", "☠️ RIP YOUR STREAK ☠️"
            };
            showEndSessionDialog(
                loseMessages[(int)(Math.random() * loseMessages.length)]
                + "\n❌ The Word Was: " + secretWord);
            return;
        }

        // Next attempt
        currentAttempt++;
        inputField.setText("");
    }

    private void showFunError(String message) {
        JOptionPane.showMessageDialog(this, message, "⚠️ OOPS!", JOptionPane.WARNING_MESSAGE);
        inputField.setText("");
        inputField.requestFocus();
    }

    private void showEndSessionDialog(String message) {
        Object[] options = {"🔄 PLAY AGAIN", "❌ QUIT"};
        int choice = JOptionPane.showOptionDialog(this,
            message + "\n\n🎮 Ready for another round?",
            "🎊 SESSION ENDED 🎊",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null, options, options[0]);

        if (choice == JOptionPane.YES_OPTION) {
            // Reset board
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
                "Thanks for playing!\nFinal Streak: " + winStreak +
                " 🔥\nTotal Games: " + totalGames + " 🎮",
                "👋 See You Later!", JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        }
    }
}
