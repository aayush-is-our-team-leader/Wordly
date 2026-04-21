import java.awt.*;
import java.awt.event.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.*;

public class WordlyGUI extends JFrame {
    private List<String> wordBank;
    private String secretWord = "";
    private String lastWord = ""; 
    private JLabel[][] grid = new JLabel[6][5];
    private JTextField inputField;
    private int currentAttempt = 0;
    private int winStreak = 0;
    private int totalGames = 0;

    // NEW: Letter tracking
    private Set<String> guessedLetters = new HashSet<>();
    private Set<String> correctLetters = new HashSet<>();
    private Set<String> wrongSpotLetters = new HashSet<>();
    private Set<String> wrongLetters = new HashSet<>();
    
    // NEW: Display panels
    private JPanel letterStatusPanel;
    private JLabel correctLettersLabel;
    private JLabel guessedCountLabel;

    private final Color BG_DARK = new Color(18, 18, 19);
    private final Color GRID_EMPTY = new Color(45, 45, 46);
    private final Color WORD_GREEN = new Color(38, 115, 70);
    private final Color WORD_YELLOW = new Color(180, 140, 0);
    private final Color WORD_RED = new Color(180, 40, 40);
    private final Color ACCENT = new Color(138, 43, 226);

    private JLabel streakLabel;
    private JLabel gamesLabel;

    public WordlyGUI() {
        wordBank = Yus_Code.loadWordsFromFile("WORDS");
        setupUI();              // ✅ BUILD UI FIRST
        startNewSession();      // ✅ THEN START GAME
        inputField.requestFocus();
    }

    private void startNewSession() {
        secretWord = Yus_Code.getRandomWord(wordBank, lastWord);
        lastWord = secretWord; 
        currentAttempt = 0;
        totalGames++;
        
        // NEW: Reset letter tracking
        guessedLetters.clear();
        correctLetters.clear();
        wrongSpotLetters.clear();
        wrongLetters.clear();
        
        updateStatsDisplay();
        updateLetterDisplay();
    }

    private void setupUI() {
        setTitle("🎯 WORDLY - BRAIN MODE 🎯");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(15, 15));
        getContentPane().setBackground(BG_DARK);

        // ========== HEADER PANEL ==========
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_DARK);
        header.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20));

        // Title + Stats
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(BG_DARK);
        JLabel titleLabel = new JLabel("🎯 WORDLY 🎯", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 36));
        titleLabel.setForeground(ACCENT);
        titlePanel.add(titleLabel, BorderLayout.CENTER);

        JPanel statsPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        statsPanel.setBackground(BG_DARK);
        streakLabel = createStatLabel("🔥 STREAK: 0");
        gamesLabel = createStatLabel("🎮 GAMES: 0");
        statsPanel.add(streakLabel);
        statsPanel.add(gamesLabel);
        
        header.add(titlePanel, BorderLayout.NORTH);
        header.add(statsPanel, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        // ========== LEGEND PANEL ==========
        JPanel legend = new JPanel(new GridLayout(1, 3, 10, 0));
        legend.setBackground(BG_DARK);
        legend.setBorder(BorderFactory.createEmptyBorder(0, 20, 10, 20));
        legend.add(createLegendLabel("✅ CORRECT", WORD_GREEN));
        legend.add(createLegendLabel("🤔 WRONG SPOT", WORD_YELLOW));
        legend.add(createLegendLabel("❌ NOPE", WORD_RED));
        add(legend, BorderLayout.PAGE_START);

        // ========== MAIN CENTER PANEL (Game Board + Letter Status) ==========
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setBackground(BG_DARK);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // Game Board
        JPanel board = new JPanel(new GridLayout(6, 5, 10, 10));
        board.setBackground(BG_DARK);
        board.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 5; c++) {
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

        // NEW: Right side panel with letter status
        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
        rightPanel.setBackground(BG_DARK);
        rightPanel.setPreferredSize(new Dimension(250, 400));

        // Correct Letters Found
        JPanel correctPanel = new JPanel(new BorderLayout());
        correctPanel.setBackground(new Color(30, 30, 30));
        correctPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(WORD_GREEN, 2),
            "✅ CORRECT LETTERS",
            javax.swing.border.TitledBorder.CENTER,
            javax.swing.border.TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 12),
            WORD_GREEN
        ));
        correctLettersLabel = new JLabel("None yet", SwingConstants.CENTER);
        correctLettersLabel.setFont(new Font("Arial", Font.BOLD, 20));
        correctLettersLabel.setForeground(WORD_GREEN);
        correctPanel.add(correctLettersLabel, BorderLayout.CENTER);

        // Guessed Count
        JPanel guessedPanel = new JPanel(new BorderLayout());
        guessedPanel.setBackground(new Color(30, 30, 30));
        guessedPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(ACCENT, 2),
            "📊 GUESSED LETTERS",
            javax.swing.border.TitledBorder.CENTER,
            javax.swing.border.TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 12),
            ACCENT
        ));
        guessedCountLabel = new JLabel("0/26", SwingConstants.CENTER);
        guessedCountLabel.setFont(new Font("Arial", Font.BOLD, 18));
        guessedCountLabel.setForeground(ACCENT);
        guessedPanel.add(guessedCountLabel, BorderLayout.CENTER);

        // Letter Status Display
        letterStatusPanel = new JPanel();
        letterStatusPanel.setLayout(new BoxLayout(letterStatusPanel, BoxLayout.Y_AXIS));
        letterStatusPanel.setBackground(new Color(30, 30, 30));
        letterStatusPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(100, 100, 100), 1),
            "📋 LETTER STATUS",
            javax.swing.border.TitledBorder.CENTER,
            javax.swing.border.TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 12),
            new Color(180, 180, 180)
        ));
        
        JScrollPane scrollPane = new JScrollPane(letterStatusPanel);
        scrollPane.setBackground(new Color(30, 30, 30));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setBackground(new Color(40, 40, 40));

        rightPanel.add(correctPanel, BorderLayout.NORTH);
        rightPanel.add(guessedPanel, BorderLayout.CENTER);
        rightPanel.add(scrollPane, BorderLayout.SOUTH);

        centerPanel.add(board, BorderLayout.CENTER);
        centerPanel.add(rightPanel, BorderLayout.EAST);
        add(centerPanel, BorderLayout.CENTER);

        // ========== INPUT PANEL ==========
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBackground(BG_DARK);
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 30, 15, 30));

        JLabel instructionLabel = new JLabel("Enter your 5-letter guess and press ENTER:", SwingConstants.CENTER);
        instructionLabel.setForeground(new Color(180, 180, 180));
        instructionLabel.setFont(new Font("Arial", Font.PLAIN, 12));

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
                if (inputField.getText().length() >= 5) {
                    e.consume();
                }
            }
        });

        inputPanel.add(instructionLabel, BorderLayout.NORTH);
        inputPanel.add(inputField, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

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
        gamesLabel.setText("🎮 GAMES: " + totalGames);
    }

    // NEW: Update letter status display
    private void updateLetterDisplay() {
        letterStatusPanel.removeAll();

        if (correctLetters.isEmpty()) {
            correctLettersLabel.setText("None yet");
        } else {
            correctLettersLabel.setText(String.join(" ", correctLetters));
        }

        guessedCountLabel.setText(guessedLetters.size() + "/26");

        // Display all guessed letters with their status
        if (!guessedLetters.isEmpty()) {
            for (String letter : guessedLetters) {
                JLabel letterLabel = new JLabel();
                letterLabel.setFont(new Font("Arial", Font.BOLD, 14));
                letterLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

                if (correctLetters.contains(letter)) {
                    letterLabel.setText("✅ " + letter + " - CORRECT");
                    letterLabel.setForeground(WORD_GREEN);
                } else if (wrongSpotLetters.contains(letter)) {
                    letterLabel.setText("🤔 " + letter + " - WRONG SPOT");
                    letterLabel.setForeground(WORD_YELLOW);
                } else {
                    letterLabel.setText("❌ " + letter + " - NOT IN WORD");
                    letterLabel.setForeground(WORD_RED);
                }

                letterStatusPanel.add(letterLabel);
                letterStatusPanel.add(Box.createVerticalStrut(5));
            }
        }

        letterStatusPanel.revalidate();
        letterStatusPanel.repaint();
    }

    private void processGuess() {
        String guess = inputField.getText().toUpperCase().trim();

        if (guess.length() != 5) {
            showFunError("TOO SHORT! 📏 Need 5 letters, got " + guess.length());
            return;
        }

        if (!wordBank.contains(guess)) {
            String[] errorMessages = {
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
            showFunError(errorMessages[(int)(Math.random() * errorMessages.length)]);
            return;
        }

        char[] secretChars = secretWord.toCharArray();
        boolean[] secretUsed = new boolean[5];

        // First pass - correct positions
        for (int i = 0; i < 5; i++) {
            char g = guess.charAt(i);
            grid[currentAttempt][i].setText(String.valueOf(g));
            guessedLetters.add(String.valueOf(g));
            
            if (g == secretChars[i]) {
                grid[currentAttempt][i].setBackground(WORD_GREEN);
                secretUsed[i] = true;
                correctLetters.add(String.valueOf(g));
            }
        }

        // Second pass - wrong positions
        for (int i = 0; i < 5; i++) {
            if (grid[currentAttempt][i].getBackground().equals(WORD_GREEN)) continue;
            char g = guess.charAt(i);
            boolean found = false;
            for (int j = 0; j < 5; j++) {
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

        if (guess.equals(secretWord)) {
            winStreak++;
            String[] winMessages = {
                "🏆 GENIUS!! 🧠✨",
                "🎉 YOU CRUSHED IT! 🔥",
                "👑 WORD LORD ACTIVATED 👑",
                "🚀 ABSOLUTELY BRILLIANT! 🚀",
                "💎 LEGENDARY GAMER 💎",
                "🌟 YOU'RE A WIZARD! 🧙",
                "🎯 PERFECT SHOT! 🎯",
                "⚡ INSANE SKILLS! ⚡"
            };
            String winMsg = winMessages[(int)(Math.random() * winMessages.length)];
            showEndSessionDialog(winMsg + "\n✅ The Word Was: " + secretWord);
        } else if (currentAttempt == 5) {
            winStreak = 0;
            String[] loseMessages = {
                "💀 GAME OVER! 💀",
                "😭 SO CLOSE! But not close enough!",
                "🤦 YIKES! Better luck next time!",
                "📉 NOPE! Not today!",
                "🎪 CLOWN MOMENT! 🤡",
                "☠️ RIP YOUR STREAK ☠️"
            };
            String loseMsg = loseMessages[(int)(Math.random() * loseMessages.length)];
            showEndSessionDialog(loseMsg + "\n❌ The Word Was: " + secretWord);
        } else {
            currentAttempt++;
            inputField.setText("");
        }
    }

    private void showFunError(String message) {
        JOptionPane.showMessageDialog(this, message, "⚠️ OOPS!", JOptionPane.WARNING_MESSAGE);
        inputField.setText("");
        inputField.requestFocus();
    }

    private void showEndSessionDialog(String message) {
        Object[] options = {"🔄 PLAY AGAIN", "❌ QUIT"};
        int n = JOptionPane.showOptionDialog(this,
                message + "\n\n🎮 Ready for another round?",
                "🎊 SESSION ENDED 🎊",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        if (n == JOptionPane.YES_OPTION) {
            startNewSession();
            inputField.setText("");
            for (int r = 0; r < 6; r++) {
                for (int c = 0; c < 5; c++) {
                    grid[r][c].setText("");
                    grid[r][c].setBackground(new Color(35, 35, 36));
                }
            }
            updateStatsDisplay();
            updateLetterDisplay();
            inputField.requestFocus();
        } else {
            JOptionPane.showMessageDialog(this, 
                "Thanks for playing! Final Streak: " + winStreak + " 🔥\nTotal Games: " + totalGames + " 🎮", 
                "👋 See You Later!", 
                JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(WordlyGUI::new);
    }
}