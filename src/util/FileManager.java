package util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

public class FileManager {
    private static final String LOG_FILE = "data/logs.txt";

    static {
        new File("data").mkdirs();
    }

    public static void log(String message) {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
            w.write("[" + LocalDateTime.now() + "] " + message);
            w.newLine();
        } catch (IOException e) {
            System.err.println("Log error: " + e.getMessage());
        }
    }

    public static void writeReport(String filename, String content) throws IOException {
        new File("data/reports").mkdirs();
        try (BufferedWriter w = new BufferedWriter(new FileWriter("data/reports/" + filename))) {
            w.write(content);
        }
    }
}