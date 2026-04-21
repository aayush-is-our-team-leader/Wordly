import java.io.*;
import java.util.*;

public class Yus_Code {
    public static final int WORD_LENGTH = 5;

    public static List<String> loadWordsFromFile(String fileName) {
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
                words.addAll(Arrays.asList(
                    "CRANE", "STORM", "BEACH", "SHARK", "PLANT", 
                    "BRAIN", "SKULL", "FLAME", "SWORD", "MAGIC",
                    "CHAOS", "WORLD", "PIXEL", "GAMER", "BEAST"
                ));
            }
        } catch (FileNotFoundException e) {
            System.out.println("📁 File error: " + e.getMessage());
        }
        return words;
    }

    public static String getRandomWord(List<String> bank, String previous) {
        if (bank.size() <= 1) return bank.get(0);
        String next;
        do {
            next = bank.get(new Random().nextInt(bank.size()));
        } while (next.equals(previous));
        return next;
    }
}