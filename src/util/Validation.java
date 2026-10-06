package util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Validation {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static boolean isValidName(String name) {
        return name != null && !name.trim().isEmpty() && name.length() <= 100;
    }

    public static boolean isValidQuantity(int qty) {
        return qty > 0 && qty <= 100000;
    }

    public static boolean isValidBatch(String batch) {
        return batch != null && !batch.trim().isEmpty() && batch.length() <= 50;
    }

    public static LocalDate parseDate(String s) {
        return LocalDate.parse(s.trim(), FORMATTER);
    }
}